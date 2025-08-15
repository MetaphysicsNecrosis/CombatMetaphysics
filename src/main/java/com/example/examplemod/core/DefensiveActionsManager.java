package com.example.examplemod.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Система защитных действий: парирование, блокирование, уклонения
 * Согласно CLAUDE.md:
 * - Парирование: активная защита с timing window
 * - Блокирование: пассивная защита с выносливостью  
 * - Уклонения: i-frames с позиционированием
 */
public class DefensiveActionsManager {
    
    public enum DefensiveType {
        PARRY(0.2f, 5f, 30f, true),     // timing window 200ms, low stamina, high effectiveness
        BLOCK(0f, 15f, 50f, false),     // no timing, medium stamina, medium effectiveness  
        DODGE(0.3f, 25f, 0f, true);     // i-frames 300ms, high stamina, full avoidance
        
        private final float timingWindow; // Окно для успешного выполнения (в секундах)
        private final float staminaCost;
        private final float effectiveness; // % поглощения урона
        private final boolean requiresTiming;
        
        DefensiveType(float timingWindow, float staminaCost, float effectiveness, boolean requiresTiming) {
            this.timingWindow = timingWindow;
            this.staminaCost = staminaCost;
            this.effectiveness = effectiveness;
            this.requiresTiming = requiresTiming;
        }
        
        public float getTimingWindow() { return timingWindow; }
        public float getStaminaCost() { return staminaCost; }
        public float getEffectiveness() { return effectiveness; }
        public boolean requiresTiming() { return requiresTiming; }
    }
    
    public static class DefensiveAction {
        private final UUID playerId;
        private final DefensiveType type;
        private final long activationTime;
        private final long duration;
        private boolean isActive;
        private boolean wasSuccessful;
        private float damageAbsorbed;
        
        public DefensiveAction(UUID playerId, DefensiveType type, long duration) {
            this.playerId = playerId;
            this.type = type;
            this.activationTime = System.currentTimeMillis();
            this.duration = duration;
            this.isActive = true;
            this.wasSuccessful = false;
            this.damageAbsorbed = 0f;
        }
        
        public UUID getPlayerId() { return playerId; }
        public DefensiveType getType() { return type; }
        public long getActivationTime() { return activationTime; }
        public long getDuration() { return duration; }
        public boolean isActive() { return isActive; }
        public boolean wasSuccessful() { return wasSuccessful; }
        public float getDamageAbsorbed() { return damageAbsorbed; }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - activationTime;
        }
        
        public boolean isInTimingWindow() {
            if (!type.requiresTiming()) return true;
            
            long elapsed = getElapsedTime();
            return elapsed <= (type.getTimingWindow() * 1000);
        }
        
        public boolean hasExpired() {
            return getElapsedTime() >= duration;
        }
        
        public void markSuccessful(float damageAbsorbed) {
            this.wasSuccessful = true;
            this.damageAbsorbed = damageAbsorbed;
        }
        
        public void deactivate() {
            this.isActive = false;
        }
        
        public float getTimingAccuracy() {
            if (!type.requiresTiming()) return 1.0f;
            
            long elapsed = getElapsedTime();
            float window = type.getTimingWindow() * 1000;
            
            if (elapsed > window) return 0f;
            
            // Чем ближе к началу окна, тем лучше точность
            return 1.0f - (elapsed / window);
        }
    }
    
    public static class DefenseResult {
        private final boolean success;
        private final float damageReduction;
        private final boolean stunAttacker;
        private final boolean counterOpportunity;
        private final boolean invulnerability;
        private final String message;
        
        public DefenseResult(boolean success, float damageReduction, boolean stunAttacker, 
                           boolean counterOpportunity, boolean invulnerability, String message) {
            this.success = success;
            this.damageReduction = damageReduction;
            this.stunAttacker = stunAttacker;
            this.counterOpportunity = counterOpportunity;
            this.invulnerability = invulnerability;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public float getDamageReduction() { return damageReduction; }
        public boolean shouldStunAttacker() { return stunAttacker; }
        public boolean hasCounterOpportunity() { return counterOpportunity; }
        public boolean hasInvulnerability() { return invulnerability; }
        public String getMessage() { return message; }
    }
    
    private final Map<UUID, DefensiveAction> activeDefenses = new HashMap<>();
    private final ResourceManager resourceManager;
    
    public DefensiveActionsManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
    
    /**
     * Активирует защитное действие
     */
    public boolean activateDefense(UUID playerId, DefensiveType type) {
        // Проверяем выносливость
        if (!resourceManager.canUseStamina(type.getStaminaCost())) {
            return false;
        }
        
        // Проверяем, не выполняет ли игрок уже защитное действие
        if (activeDefenses.containsKey(playerId)) {
            return false;
        }
        
        // Определяем длительность действия
        long duration = switch (type) {
            case PARRY -> 500; // 500ms окно
            case BLOCK -> 5000; // 5 секунд удержания
            case DODGE -> 800; // 800ms уклонения с i-frames
        };
        
        // Списываем выносливость
        if (!resourceManager.tryUseStamina(type.getStaminaCost(), "defensive action")) {
            return false;
        }
        
        DefensiveAction action = new DefensiveAction(playerId, type, duration);
        activeDefenses.put(playerId, action);
        
        return true;
    }
    
    /**
     * Обрабатывает входящую атаку против защищающегося игрока
     */
    public DefenseResult processIncomingAttack(UUID defenderId, float incomingDamage, 
                                             boolean isHardToParry, DirectionalAttackSystem.AttackDirection attackDirection) {
        DefensiveAction defense = activeDefenses.get(defenderId);
        
        if (defense == null || !defense.isActive() || defense.hasExpired()) {
            return new DefenseResult(false, 0f, false, false, false, "No active defense");
        }
        
        DefensiveType type = defense.getType();
        
        return switch (type) {
            case PARRY -> processParry(defense, incomingDamage, isHardToParry);
            case BLOCK -> processBlock(defense, incomingDamage, attackDirection);
            case DODGE -> processDodge(defense, incomingDamage);
        };
    }
    
    private DefenseResult processParry(DefensiveAction defense, float incomingDamage, boolean isHardToParry) {
        // Проверяем timing window
        if (!defense.isInTimingWindow()) {
            return new DefenseResult(false, 0f, false, false, false, "Parry timing missed");
        }
        
        // Сложные для парирования атаки труднее отбить
        float parryChance = isHardToParry ? 0.3f : 0.8f;
        float timingAccuracy = defense.getTimingAccuracy();
        
        if (timingAccuracy * parryChance >= 0.5f) {
            defense.markSuccessful(incomingDamage);
            return new DefenseResult(true, 1.0f, true, true, false, 
                "Perfect parry! Attacker stunned, counter opportunity available");
        } else {
            return new DefenseResult(false, 0.2f, false, false, false, 
                "Failed parry, reduced damage");
        }
    }
    
    private DefenseResult processBlock(DefensiveAction defense, float incomingDamage, DirectionalAttackSystem.AttackDirection attackDirection) {
        // Блокирование всегда работает, но с разной эффективностью
        float effectiveness = defense.getType().getEffectiveness() / 100f;
        
        // TOP_ATTACK пробивает блоки согласно CLAUDE.md
        if (attackDirection != null && attackDirection.canBreakBlocks()) {
            effectiveness *= 0.3f; // Сильно уменьшенная эффективность против пробивающих атак
            defense.markSuccessful(incomingDamage * (1f - effectiveness));
            return new DefenseResult(true, effectiveness, false, false, false,
                "Block partially effective against block-breaking attack");
        }
        
        defense.markSuccessful(incomingDamage * (1f - effectiveness));
        return new DefenseResult(true, effectiveness, false, false, false, "Attack blocked");
    }
    
    private DefenseResult processDodge(DefensiveAction defense, float incomingDamage) {
        // Уклонение дает i-frames только в начале
        long elapsed = defense.getElapsedTime();
        
        if (elapsed <= 300) { // 300ms i-frames
            defense.markSuccessful(incomingDamage);
            return new DefenseResult(true, 1.0f, false, false, true, "Perfect dodge! Invulnerability frames");
        } else {
            return new DefenseResult(false, 0f, false, false, false, "Dodge too late");
        }
    }
    
    /**
     * Деактивирует защитное действие
     */
    public void deactivateDefense(UUID playerId) {
        DefensiveAction defense = activeDefenses.get(playerId);
        if (defense != null) {
            defense.deactivate();
            activeDefenses.remove(playerId);
        }
    }
    
    /**
     * Получает текущее защитное действие игрока
     */
    public DefensiveAction getCurrentDefense(UUID playerId) {
        return activeDefenses.get(playerId);
    }
    
    /**
     * Проверяет, выполняет ли игрок защитное действие
     */
    public boolean isDefending(UUID playerId) {
        DefensiveAction defense = activeDefenses.get(playerId);
        return defense != null && defense.isActive() && !defense.hasExpired();
    }
    
    /**
     * Проверяет, находится ли игрок в i-frames (уклонение)
     */
    public boolean hasInvulnerabilityFrames(UUID playerId) {
        DefensiveAction defense = activeDefenses.get(playerId);
        if (defense == null || defense.getType() != DefensiveType.DODGE) {
            return false;
        }
        
        return defense.isActive() && defense.getElapsedTime() <= 300; // 300ms i-frames
    }
    
    /**
     * Получает оставшееся время защитного действия
     */
    public long getRemainingTime(UUID playerId) {
        DefensiveAction defense = activeDefenses.get(playerId);
        if (defense == null) return 0;
        
        return Math.max(0, defense.getDuration() - defense.getElapsedTime());
    }
    
    /**
     * Очищает устаревшие защитные действия
     */
    public void tick() {
        activeDefenses.entrySet().removeIf(entry -> {
            DefensiveAction defense = entry.getValue();
            if (defense.hasExpired()) {
                defense.deactivate();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Проверяет, есть ли возможность контратаки после успешного парирования
     */
    public boolean hasCounterOpportunity(UUID playerId) {
        DefensiveAction defense = activeDefenses.get(playerId);
        return defense != null && defense.getType() == DefensiveType.PARRY && 
               defense.wasSuccessful() && defense.getElapsedTime() <= 1000; // 1 секунда на контратаку
    }
    
    // ============== ИНТЕГРАЦИЯ С COMBAT СИСТЕМАМИ ==============
    
    /**
     * Интеграция с InterruptionSystem - защитные действия могут блокировать прерывания
     */
    public boolean canBlockInterruption(UUID playerId, InterruptionSystem.InterruptionType interruptionType) {
        DefensiveAction defense = activeDefenses.get(playerId);
        if (defense == null || !defense.isActive()) {
            return false;
        }
        
        return switch (defense.getType()) {
            case PARRY -> {
                // Успешное парирование блокирует большинство прерываний
                if (defense.wasSuccessful()) {
                    yield interruptionType.getPriority() < InterruptionSystem.InterruptionType.ENVIRONMENTAL_HAZARD.getPriority();
                }
                yield false;
            }
            case BLOCK -> {
                // Блокирование защищает от слабых прерываний
                yield interruptionType.getPriority() <= InterruptionSystem.InterruptionType.MAGICAL_DISRUPTION.getPriority();
            }
            case DODGE -> {
                // i-frames защищают от всех прерываний кроме environmental
                if (hasInvulnerabilityFrames(playerId)) {
                    yield interruptionType.getPriority() < InterruptionSystem.InterruptionType.ENVIRONMENTAL_HAZARD.getPriority();
                }
                yield false;
            }
        };
    }
    
    /**
     * Расчет стоимости выносливости для продолжительного блокирования
     */
    public float calculateBlockingStaminaDrain(UUID playerId, float damageAbsorbed) {
        DefensiveAction defense = activeDefenses.get(playerId);
        if (defense == null || defense.getType() != DefensiveType.BLOCK) {
            return 0f;
        }
        
        // Базовая трата выносливости в зависимости от поглощенного урона
        float baseDrain = damageAbsorbed * 0.1f; // 10% от урона
        
        // Дополнительная трата за длительность блокирования
        long blockTime = defense.getElapsedTime();
        float timeDrain = (blockTime / 1000f) * 2f; // 2 выносливости в секунду
        
        return baseDrain + timeDrain;
    }
    
    /**
     * Создает возможность для магических контратак после защитных действий
     */
    public boolean canPerformMagicalCounter(UUID playerId) {
        DefensiveAction defense = activeDefenses.get(playerId);
        if (defense == null) return false;
        
        return switch (defense.getType()) {
            case PARRY -> {
                // После успешного парирования доступны быстрые заклинания
                yield defense.wasSuccessful() && defense.getElapsedTime() <= 800;
            }
            case DODGE -> {
                // После уклонения можно использовать магию с бонусом
                yield defense.wasSuccessful() && defense.getElapsedTime() <= 1200;
            }
            default -> false;
        };
    }
    
    /**
     * Вычисляет бонус к скорости каста после защитного действия
     */
    public float getCounterCastSpeedBonus(UUID playerId) {
        DefensiveAction defense = activeDefenses.get(playerId);
        if (defense == null || !defense.wasSuccessful()) {
            return 1.0f; // Нет бонуса
        }
        
        return switch (defense.getType()) {
            case PARRY -> {
                float timingAccuracy = defense.getTimingAccuracy();
                yield 1.0f + (timingAccuracy * 0.5f); // До +50% скорости каста
            }
            case DODGE -> 1.3f; // +30% скорости каста после уклонения
            default -> 1.0f;
        };
    }
    
    /**
     * Система цепочек защитных действий (аналог комбо)
     */
    public static class DefensiveChain {
        private final UUID playerId;
        private final DefensiveType[] expectedSequence;
        private final long[] timingWindows;
        private int currentStep;
        private long lastActionTime;
        
        public DefensiveChain(UUID playerId, DefensiveType[] sequence, long[] windows) {
            this.playerId = playerId;
            this.expectedSequence = sequence;
            this.timingWindows = windows;
            this.currentStep = 0;
            this.lastActionTime = System.currentTimeMillis();
        }
        
        public boolean tryAdvanceChain(DefensiveType actionType) {
            if (currentStep >= expectedSequence.length) {
                return false; // Цепочка завершена
            }
            
            long timeSinceLastAction = System.currentTimeMillis() - lastActionTime;
            if (timeSinceLastAction > timingWindows[currentStep]) {
                return false; // Слишком поздно
            }
            
            if (expectedSequence[currentStep] == actionType) {
                currentStep++;
                lastActionTime = System.currentTimeMillis();
                return true;
            }
            
            return false; // Неправильное действие
        }
        
        public boolean isComplete() {
            return currentStep >= expectedSequence.length;
        }
        
        public float getChainMultiplier() {
            return 1.0f + (currentStep * 0.2f); // +20% эффективности за каждый шаг
        }
    }
    
    private final Map<UUID, DefensiveChain> activeChains = new HashMap<>();
    
    /**
     * Предопределенные цепочки защитных действий
     */
    private static final DefensiveType[][] DEFENSIVE_CHAINS = {
        {DefensiveType.DODGE, DefensiveType.PARRY},          // Уклонение -> Парирование
        {DefensiveType.BLOCK, DefensiveType.DODGE, DefensiveType.PARRY}, // Полная защитная цепочка
        {DefensiveType.PARRY, DefensiveType.DODGE}           // Парирование -> Уклонение
    };
    
    private static final long[][] CHAIN_WINDOWS = {
        {1500L, 800L},        // 1.5с на парирование после уклонения
        {2000L, 1000L, 800L}, // Времена для полной цепочки
        {1200L, 1000L}        // 1.2с на уклонение после парирования
    };
    
    /**
     * Пытается начать или продолжить защитную цепочку
     */
    public boolean tryAdvanceDefensiveChain(UUID playerId, DefensiveType actionType) {
        DefensiveChain currentChain = activeChains.get(playerId);
        
        if (currentChain != null) {
            boolean advanced = currentChain.tryAdvanceChain(actionType);
            if (currentChain.isComplete()) {
                activeChains.remove(playerId);
                // Награждаем за завершение цепочки
                return true;
            }
            return advanced;
        }
        
        // Пытаемся начать новую цепочку
        for (int i = 0; i < DEFENSIVE_CHAINS.length; i++) {
            if (DEFENSIVE_CHAINS[i][0] == actionType) {
                DefensiveChain newChain = new DefensiveChain(playerId, DEFENSIVE_CHAINS[i], CHAIN_WINDOWS[i]);
                newChain.tryAdvanceChain(actionType);
                activeChains.put(playerId, newChain);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Получает мультипликатор эффективности от текущей цепочки
     */
    public float getCurrentChainMultiplier(UUID playerId) {
        DefensiveChain chain = activeChains.get(playerId);
        return chain != null ? chain.getChainMultiplier() : 1.0f;
    }
    
    /**
     * Интеграция с DirectionalAttackSystem - расчет эффективности защиты
     */
    public float calculateDefenseEffectiveness(UUID playerId, DirectionalAttackSystem.AttackData attackData) {
        DefensiveAction defense = activeDefenses.get(playerId);
        if (defense == null || !defense.isActive()) {
            return 0f; // Нет защиты
        }
        
        float baseEffectiveness = defense.getType().getEffectiveness() / 100f;
        
        // Модификаторы в зависимости от направления атаки
        DirectionalAttackSystem.AttackDirection direction = attackData.getDirection();
        
        switch (defense.getType()) {
            case PARRY:
                if (direction.isHardToParry()) {
                    baseEffectiveness *= 0.5f; // Сложные атаки труднее парировать
                }
                baseEffectiveness *= defense.getTimingAccuracy(); // Точность тайминга
                break;
                
            case BLOCK:
                if (direction.canBreakBlocks()) {
                    baseEffectiveness *= 0.3f; // Пробивающие атаки снижают эффективность блока
                }
                if (attackData.isChargedAttack()) {
                    baseEffectiveness *= 0.7f; // Заряженные атаки сильнее пробивают блок
                }
                break;
                
            case DODGE:
                // Уклонение либо работает полностью (i-frames), либо не работает
                if (hasInvulnerabilityFrames(playerId)) {
                    baseEffectiveness = 1.0f;
                } else {
                    baseEffectiveness = 0f;
                }
                break;
        }
        
        // Применяем мультипликатор от защитных цепочек
        baseEffectiveness *= getCurrentChainMultiplier(playerId);
        
        return Math.min(1.0f, baseEffectiveness);
    }
    
    /**
     * Очищает истекшие защитные цепочки
     */
    public void cleanupExpiredChains() {
        long currentTime = System.currentTimeMillis();
        activeChains.entrySet().removeIf(entry -> {
            DefensiveChain chain = entry.getValue();
            return currentTime - chain.lastActionTime > 3000; // 3 секунды на продолжение цепочки
        });
    }
    
    /**
     * Расширенный tick с дополнительной логикой
     */
    public void advancedTick() {
        // Базовая логика очистки
        tick();
        
        // Очистка цепочек
        cleanupExpiredChains();
        
        // Дополнительная трата выносливости для длительного блокирования
        for (DefensiveAction defense : activeDefenses.values()) {
            if (defense.getType() == DefensiveType.BLOCK && defense.isActive()) {
                float drainPerTick = 0.5f; // 0.5 выносливости за тик блокирования
                resourceManager.tryUseStamina(drainPerTick, "continuous blocking");
            }
        }
    }
}