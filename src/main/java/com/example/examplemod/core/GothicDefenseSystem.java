package com.example.examplemod.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gothic-style система защиты: блокирование, парирование и уклонение
 * Интегрирована с системой выносливости и состояниями игрока
 */
public class GothicDefenseSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(GothicDefenseSystem.class);
    
    // Активные защитные действия
    private final Map<UUID, DefenseData> activeDefenses = new ConcurrentHashMap<>();
    
    // Статистика парирований для балансировки
    private final Map<UUID, ParryStats> parryStats = new ConcurrentHashMap<>();
    
    public enum DefenseType {
        BLOCK,      // Активный блок с расходом выносливости
        PARRY,      // Точное парирование с контратакой
        DODGE       // Уклонение с i-frames
    }
    
    public enum DefenseResult {
        SUCCESS,        // Успешная защита
        PARTIAL,        // Частичная защита (блок сломан)
        FAILED,         // Неудачная защита
        PERFECT,        // Идеальное парирование
        STUNNED         // Игрок оглушен
    }
    
    /**
     * Данные активной защиты
     */
    public static class DefenseData {
        private final UUID playerId;
        private final DefenseType type;
        private final long startTime;
        private final float staminaCost;
        
        // Параметры защиты
        private float blockStrength;        // Прочность блока (уменьшается при ударах)
        private boolean isActive;
        private long lastHitTime;
        private int consecutiveBlocks;
        
        // Окна точности
        private long parryWindowStart;
        private long parryWindowEnd;
        
        public DefenseData(UUID playerId, DefenseType type, float staminaCost) {
            this.playerId = playerId;
            this.type = type;
            this.startTime = System.currentTimeMillis();
            this.staminaCost = staminaCost;
            this.isActive = true;
            this.blockStrength = 100.0f;
            this.consecutiveBlocks = 0;
            this.lastHitTime = 0;
            
            // Настройка окна парирования
            if (type == DefenseType.PARRY) {
                this.parryWindowStart = startTime;
                this.parryWindowEnd = startTime + PlayerState.PARRYING.getTypicalDuration();
            }
        }
        
        // Геттеры
        public UUID getPlayerId() { return playerId; }
        public DefenseType getType() { return type; }
        public long getStartTime() { return startTime; }
        public long getActiveTime() { return System.currentTimeMillis() - startTime; }
        public float getStaminaCost() { return staminaCost; }
        public float getBlockStrength() { return blockStrength; }
        public boolean isActive() { return isActive; }
        public int getConsecutiveBlocks() { return consecutiveBlocks; }
        
        public boolean isInParryWindow() {
            if (type != DefenseType.PARRY) return false;
            long now = System.currentTimeMillis();
            return now >= parryWindowStart && now <= parryWindowEnd;
        }
        
        public void reduceBlockStrength(float amount) {
            this.blockStrength = Math.max(0, blockStrength - amount);
            if (blockStrength <= 0) {
                this.isActive = false;
            }
        }
        
        public void registerHit() {
            this.lastHitTime = System.currentTimeMillis();
            this.consecutiveBlocks++;
        }
        
        public void deactivate() {
            this.isActive = false;
        }
    }
    
    /**
     * Статистика парирования для адаптивной сложности
     */
    public static class ParryStats {
        private final UUID playerId;
        private int totalAttempts;
        private int successfulParries;
        private int perfectParries;
        private long averageReactionTime;
        private List<Long> recentReactions;
        
        public ParryStats(UUID playerId) {
            this.playerId = playerId;
            this.totalAttempts = 0;
            this.successfulParries = 0;
            this.perfectParries = 0;
            this.averageReactionTime = 0;
            this.recentReactions = new ArrayList<>();
        }
        
        public void recordParryAttempt(long reactionTime, boolean success, boolean perfect) {
            totalAttempts++;
            if (success) successfulParries++;
            if (perfect) perfectParries++;
            
            recentReactions.add(reactionTime);
            if (recentReactions.size() > 20) {
                recentReactions.remove(0);
            }
            
            // Обновляем среднее время реакции
            averageReactionTime = (long) recentReactions.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        }
        
        public float getSuccessRate() {
            return totalAttempts > 0 ? (float) successfulParries / totalAttempts : 0;
        }
        
        public float getPerfectRate() {
            return totalAttempts > 0 ? (float) perfectParries / totalAttempts : 0;
        }
        
        public long getAverageReactionTime() { return averageReactionTime; }
        public int getTotalAttempts() { return totalAttempts; }
    }
    
    /**
     * Результат защитного действия
     */
    public static class DefenseActionResult {
        private final DefenseResult result;
        private final float damageReduced;
        private final float staminaConsumed;
        private final boolean canCounterattack;
        private final String message;
        
        public DefenseActionResult(DefenseResult result, float damageReduced, 
                                 float staminaConsumed, boolean canCounterattack, String message) {
            this.result = result;
            this.damageReduced = damageReduced;
            this.staminaConsumed = staminaConsumed;
            this.canCounterattack = canCounterattack;
            this.message = message;
        }
        
        public DefenseResult getResult() { return result; }
        public float getDamageReduced() { return damageReduced; }
        public float getStaminaConsumed() { return staminaConsumed; }
        public boolean canCounterattack() { return canCounterattack; }
        public String getMessage() { return message; }
        
        public boolean isSuccess() {
            return result == DefenseResult.SUCCESS || result == DefenseResult.PERFECT;
        }
        
        public boolean isPerfectTiming() {
            return result == DefenseResult.PERFECT;
        }
        
        // Factory methods
        public static DefenseActionResult failed(String message) {
            return new DefenseActionResult(DefenseResult.FAILED, 0, 0, false, message);
        }
        
        public static DefenseActionResult success(String message, float damageReduced, float stamina, boolean counter) {
            return new DefenseActionResult(DefenseResult.SUCCESS, damageReduced, stamina, counter, message);
        }
    }
    
    /**
     * Execute defense through state machine integration
     */
    public DefenseActionResult executeDefense(DefenseType type, StaminaManager staminaManager, Player player) {
        if (player == null) {
            return DefenseActionResult.failed("No player instance");
        }
        
        UUID playerId = player.getUUID();
        return startDefense(playerId, type, staminaManager);
    }
    
    /**
     * Check if player has active defense
     */
    public boolean hasActiveDefense(UUID playerId) {
        return activeDefenses.containsKey(playerId);
    }
    
    /**
     * Get remaining time for active defense (for UI)
     */
    public long getRemainingTime(UUID playerId) {
        DefenseData data = activeDefenses.get(playerId);
        if (data == null) return 0;
        
        long elapsed = System.currentTimeMillis() - data.getStartTime();
        long duration = switch (data.getType()) {
            case PARRY -> 150;
            case DODGE -> 300;  
            case BLOCK -> 1500;
        };
        
        return Math.max(0, duration - elapsed);
    }
    
    /**
     * Начинает защитное действие
     */
    public DefenseActionResult startDefense(UUID playerId, DefenseType type, 
                                           StaminaManager staminaManager) {
        // Проверяем, нет ли уже активной защиты
        if (activeDefenses.containsKey(playerId)) {
            return new DefenseActionResult(DefenseResult.FAILED, 0, 0, false, 
                                         "Defense already active");
        }
        
        // Рассчитываем стоимость выносливости
        float staminaCost = calculateStaminaCost(type);
        
        // Проверяем выносливость
        if (!staminaManager.canConsumeStamina(playerId, staminaCost)) {
            return new DefenseActionResult(DefenseResult.FAILED, 0, 0, false, 
                                         "Insufficient stamina");
        }
        
        // Тратим выносливость
        staminaManager.tryConsumeStamina(playerId, staminaCost, "Defense: " + type);
        
        // Создаем защитное действие
        DefenseData defenseData = new DefenseData(playerId, type, staminaCost);
        activeDefenses.put(playerId, defenseData);
        
        LOGGER.debug("Player {} started {} defense", playerId, type);
        
        return new DefenseActionResult(DefenseResult.SUCCESS, 0, staminaCost, false, 
                                     "Defense activated: " + type);
    }
    
    /**
     * Обрабатывает входящую атаку против защищающегося игрока
     */
    public DefenseActionResult processIncomingAttack(UUID defenderId, float incomingDamage, 
                                                   GothicAttackSystem.AttackDirection attackDirection,
                                                   StaminaManager staminaManager) {
        DefenseData defense = activeDefenses.get(defenderId);
        if (defense == null || !defense.isActive()) {
            return new DefenseActionResult(DefenseResult.FAILED, 0, 0, false, 
                                         "No active defense");
        }
        
        return switch (defense.getType()) {
            case BLOCK -> processBlock(defense, incomingDamage, staminaManager);
            case PARRY -> processParry(defense, incomingDamage, attackDirection);
            case DODGE -> processDodge(defense, incomingDamage);
        };
    }
    
    /**
     * Обрабатывает блокирование
     */
    private DefenseActionResult processBlock(DefenseData defense, float incomingDamage, 
                                           StaminaManager staminaManager) {
        UUID playerId = defense.getPlayerId();
        defense.registerHit();
        
        // Рассчитываем эффективность блока
        float blockEfficiency = calculateBlockEfficiency(defense);
        float damageReduced = incomingDamage * blockEfficiency;
        float damageThrough = incomingDamage - damageReduced;
        
        // Уменьшаем прочность блока
        float strengthLoss = incomingDamage * 0.3f;
        defense.reduceBlockStrength(strengthLoss);
        
        // Дополнительная стоимость выносливости за блок
        float additionalStaminaCost = incomingDamage * 0.5f;
        boolean hasStamina = staminaManager.tryConsumeStamina(playerId, additionalStaminaCost, 
                                                            "Block impact");
        
        if (!hasStamina || defense.getBlockStrength() <= 0) {
            // Блок сломан
            defense.deactivate();
            LOGGER.debug("Player {} block broken", playerId);
            
            return new DefenseActionResult(DefenseResult.PARTIAL, damageReduced * 0.5f, 
                                         additionalStaminaCost, false, "Block broken");
        }
        
        LOGGER.debug("Player {} successfully blocked attack (efficiency: {:.1f}%)", 
                    playerId, blockEfficiency * 100);
        
        return new DefenseActionResult(DefenseResult.SUCCESS, damageReduced, 
                                     additionalStaminaCost, false, "Attack blocked");
    }
    
    /**
     * Обрабатывает парирование
     */
    private DefenseActionResult processParry(DefenseData defense, float incomingDamage, 
                                           GothicAttackSystem.AttackDirection attackDirection) {
        UUID playerId = defense.getPlayerId();
        long reactionTime = System.currentTimeMillis() - defense.getStartTime();
        
        // Получаем статистику игрока
        ParryStats stats = parryStats.computeIfAbsent(playerId, ParryStats::new);
        
        boolean isInWindow = defense.isInParryWindow();
        boolean isPerfect = reactionTime <= 50; // Идеальное парирование в первые 50ms
        
        stats.recordParryAttempt(reactionTime, isInWindow, isPerfect);
        
        if (!isInWindow) {
            // Неудачное парирование
            defense.deactivate();
            return new DefenseActionResult(DefenseResult.FAILED, 0, 0, false, 
                                         "Parry timing missed");
        }
        
        if (isPerfect) {
            // Идеальное парирование - полная защита + контратака
            defense.deactivate();
            LOGGER.debug("Player {} perfect parry!", playerId);
            
            return new DefenseActionResult(DefenseResult.PERFECT, incomingDamage, 0, true, 
                                         "Perfect parry! Counterattack available");
        } else {
            // Обычное успешное парирование
            defense.deactivate();
            float damageReduced = incomingDamage * 0.8f; // 80% урона поглощается
            
            LOGGER.debug("Player {} successful parry", playerId);
            
            return new DefenseActionResult(DefenseResult.SUCCESS, damageReduced, 0, true, 
                                         "Parry successful");
        }
    }
    
    /**
     * Обрабатывает уклонение
     */
    private DefenseActionResult processDodge(DefenseData defense, float incomingDamage) {
        UUID playerId = defense.getPlayerId();
        long dodgeTime = defense.getActiveTime();
        
        // i-frames в первые 300ms уклонения
        if (dodgeTime <= PlayerState.DODGING.getTypicalDuration()) {
            LOGGER.debug("Player {} dodge successful (i-frames)", playerId);
            return new DefenseActionResult(DefenseResult.SUCCESS, incomingDamage, 0, false, 
                                         "Dodge successful");
        } else {
            // Уклонение закончилось
            return new DefenseActionResult(DefenseResult.FAILED, 0, 0, false, 
                                         "Dodge window expired");
        }
    }
    
    /**
     * Рассчитывает эффективность блока
     */
    private float calculateBlockEfficiency(DefenseData defense) {
        float baseEfficiency = 0.7f; // Базовая эффективность 70%
        
        // Снижение эффективности от поломки блока
        float strengthMultiplier = defense.getBlockStrength() / 100.0f;
        
        // Штраф за множественные удары
        float consecutivePenalty = Math.max(0.1f, 1.0f - (defense.getConsecutiveBlocks() * 0.1f));
        
        return baseEfficiency * strengthMultiplier * consecutivePenalty;
    }
    
    /**
     * Рассчитывает стоимость защиты в выносливости
     */
    private float calculateStaminaCost(DefenseType type) {
        return switch (type) {
            case BLOCK -> 20.0f;    // Начальная стоимость блока
            case PARRY -> 15.0f;    // Парирование дешевле
            case DODGE -> 25.0f;    // Уклонение дороже всего
        };
    }
    
    /**
     * Обновляет все активные защиты
     */
    public void tick() {
        Iterator<Map.Entry<UUID, DefenseData>> iterator = activeDefenses.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, DefenseData> entry = iterator.next();
            DefenseData defense = entry.getValue();
            
            boolean shouldRemove = checkDefenseTimeout(defense);
            
            if (shouldRemove) {
                iterator.remove();
                LOGGER.debug("Defense expired for player {}", defense.getPlayerId());
            }
        }
    }
    
    /**
     * Проверяет таймаут защитного действия
     */
    private boolean checkDefenseTimeout(DefenseData defense) {
        long activeTime = defense.getActiveTime();
        
        return switch (defense.getType()) {
            case BLOCK -> activeTime > PlayerState.BLOCKING.getTypicalDuration() || !defense.isActive();
            case PARRY -> activeTime > PlayerState.PARRYING.getTypicalDuration();
            case DODGE -> activeTime > PlayerState.DODGING.getTypicalDuration();
        };
    }
    
    // === ГЕТТЕРЫ И СЛУЖЕБНЫЕ МЕТОДЫ ===
    
    public boolean isDefending(UUID playerId) {
        DefenseData defense = activeDefenses.get(playerId);
        return defense != null && defense.isActive();
    }
    
    public DefenseData getCurrentDefense(UUID playerId) {
        return activeDefenses.get(playerId);
    }
    
    public ParryStats getParryStats(UUID playerId) {
        return parryStats.get(playerId);
    }
    
    /**
     * Принудительно отменяет защиту
     */
    public void cancelDefense(UUID playerId, String reason) {
        DefenseData removed = activeDefenses.remove(playerId);
        if (removed != null) {
            LOGGER.debug("Defense cancelled for player {}: {}", playerId, reason);
        }
    }
    
    /**
     * Очищает все защиты игрока
     */
    public void clearPlayerDefenses(UUID playerId) {
        activeDefenses.remove(playerId);
    }
    
    // === МЕТОДЫ ДЛЯ СОВМЕСТИМОСТИ СО СТАРОЙ СИСТЕМОЙ ===
    
    /**
     * Активирует защиту (совместимость со старым DefensiveActionsManager)
     */
    public boolean activateDefense(UUID playerId, Object defenseType) {
        // Заглушка для совместимости - старая система не используется
        return false;
    }
    
    /**
     * Деактивирует защиту (совместимость)
     */
    public void deactivateDefense(UUID playerId) {
        cancelDefense(playerId, "Defense deactivated");
    }
    
    /**
     * Получает отладочную информацию
     */
    public Map<String, Object> getDebugInfo(UUID playerId) {
        Map<String, Object> info = new HashMap<>();
        
        DefenseData defense = activeDefenses.get(playerId);
        if (defense != null) {
            info.put("isDefending", true);
            info.put("defenseType", defense.getType().name());
            info.put("activeTime", defense.getActiveTime());
            info.put("blockStrength", defense.getBlockStrength());
            info.put("consecutiveBlocks", defense.getConsecutiveBlocks());
            info.put("isActive", defense.isActive());
            
            if (defense.getType() == DefenseType.PARRY) {
                info.put("inParryWindow", defense.isInParryWindow());
            }
        } else {
            info.put("isDefending", false);
        }
        
        ParryStats stats = parryStats.get(playerId);
        if (stats != null) {
            info.put("parryStats", Map.of(
                "totalAttempts", stats.getTotalAttempts(),
                "successRate", stats.getSuccessRate() * 100,
                "perfectRate", stats.getPerfectRate() * 100,
                "averageReactionTime", stats.getAverageReactionTime()
            ));
        }
        
        return info;
    }
    
    /**
     * Проверяет возможность контратаки после защиты
     */
    public boolean canCounterattack(UUID playerId) {
        DefenseData defense = activeDefenses.get(playerId);
        if (defense == null) return false;
        
        return switch (defense.getType()) {
            case PARRY -> true;  // Парирование всегда позволяет контратаку
            case BLOCK -> defense.getConsecutiveBlocks() == 1 && defense.isActive(); // Только после первого блока
            case DODGE -> false; // Уклонение не дает контратаки
        };
    }
}