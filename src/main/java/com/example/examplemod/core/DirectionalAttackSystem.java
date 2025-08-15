package com.example.examplemod.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gothic-style система направленных атак с удержанием кнопки
 * Согласно CLAUDE.md: HOLD_ATTACK_KEY → MELEE_PREPARING → RELEASE_KEY → MELEE_ATTACKING
 */
public class DirectionalAttackSystem {
    
    public enum AttackDirection {
        LEFT_ATTACK(1.2f, 15f, 0.8f, true),    // fast, low damage, hard to parry
        RIGHT_ATTACK(1.0f, 25f, 1.0f, false),  // medium speed, medium damage
        TOP_ATTACK(0.6f, 40f, 1.5f, false),    // slow, high damage, breaks blocks
        THRUST_ATTACK(1.4f, 20f, 0.9f, false); // fast, piercing, ignores some armor
        
        private final float speedMultiplier;
        private final float baseDamage;
        private final float staminaCost;
        private final boolean hardToParry;
        
        AttackDirection(float speedMultiplier, float baseDamage, float staminaCost, boolean hardToParry) {
            this.speedMultiplier = speedMultiplier;
            this.baseDamage = baseDamage;
            this.staminaCost = staminaCost;
            this.hardToParry = hardToParry;
        }
        
        public float getSpeedMultiplier() { return speedMultiplier; }
        public float getBaseDamage() { return baseDamage; }
        public float getStaminaCost() { return staminaCost; }
        public boolean isHardToParry() { return hardToParry; }
        
        public boolean canBreakBlocks() {
            return this == TOP_ATTACK;
        }
        
        public boolean ignoresSomeArmor() {
            return this == THRUST_ATTACK;
        }
    }
    
    public static class AttackData {
        private final UUID playerId;
        private final AttackDirection direction;
        private final long chargeStartTime;
        private final long chargeReleaseTime;
        private boolean isCharging;
        private boolean isExecuting;
        
        public AttackData(UUID playerId, AttackDirection direction, long chargeStartTime) {
            this.playerId = playerId;
            this.direction = direction;
            this.chargeStartTime = chargeStartTime;
            this.chargeReleaseTime = 0;
            this.isCharging = true;
            this.isExecuting = false;
        }
        
        public AttackData(AttackData original, long releaseTime) {
            this.playerId = original.playerId;
            this.direction = original.direction;
            this.chargeStartTime = original.chargeStartTime;
            this.chargeReleaseTime = releaseTime;
            this.isCharging = false;
            this.isExecuting = true;
        }
        
        public UUID getPlayerId() { return playerId; }
        public AttackDirection getDirection() { return direction; }
        public long getChargeStartTime() { return chargeStartTime; }
        public long getChargeReleaseTime() { return chargeReleaseTime; }
        public boolean isCharging() { return isCharging; }
        public boolean isExecuting() { return isExecuting; }
        
        public long getChargeDuration() {
            if (isCharging) {
                return System.currentTimeMillis() - chargeStartTime;
            } else {
                return chargeReleaseTime - chargeStartTime;
            }
        }
        
        public boolean isChargedAttack() {
            return getChargeDuration() > 2000; // > 2 секунд согласно CLAUDE.md
        }
        
        public float getChargeMultiplier() {
            long duration = getChargeDuration();
            if (duration < 500) return 1.0f;
            if (duration < 1000) return 1.1f;
            if (duration < 2000) return 1.25f;
            return 1.5f; // Заряженная атака
        }
        
        public float getKnockbackMultiplier() {
            return isChargedAttack() ? 2.0f : 1.0f;
        }
        
        public boolean causesVulnerability() {
            return isChargedAttack(); // Заряженные атаки создают уязвимость согласно CLAUDE.md
        }
        
        public void markAsExecuting(long releaseTime) {
            this.isCharging = false;
            this.isExecuting = true;
        }
        
        public void markAsCompleted() {
            this.isCharging = false;
            this.isExecuting = false;
        }
    }
    
    private final Map<UUID, AttackData> activeAttacks = new HashMap<>();
    private final ResourceManager resourceManager;
    
    public DirectionalAttackSystem(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
    
    /**
     * Начинает заряжание атаки в указанном направлении
     */
    public boolean startCharging(UUID playerId, AttackDirection direction) {
        // Проверяем, достаточно ли выносливости для атаки
        if (!resourceManager.canUseStamina(direction.getStaminaCost())) {
            return false;
        }
        
        // Проверяем, не атакует ли игрок уже
        if (activeAttacks.containsKey(playerId)) {
            return false;
        }
        
        AttackData attackData = new AttackData(playerId, direction, System.currentTimeMillis());
        activeAttacks.put(playerId, attackData);
        
        return true;
    }
    
    /**
     * Отменяет заряжание атаки
     */
    public void cancelCharging(UUID playerId) {
        activeAttacks.remove(playerId);
    }
    
    /**
     * Выполняет атаку (при отпускании кнопки)
     */
    public AttackResult executeAttack(UUID playerId) {
        AttackData attackData = activeAttacks.get(playerId);
        if (attackData == null || !attackData.isCharging()) {
            return new AttackResult(false, "No charging attack found", 0, 0, false);
        }
        
        // Рассчитываем параметры атаки
        float baseDamage = attackData.getDirection().getBaseDamage();
        float chargeMultiplier = attackData.getChargeMultiplier();
        float finalDamage = baseDamage * chargeMultiplier;
        
        float staminaCost = attackData.getDirection().getStaminaCost();
        if (attackData.isChargedAttack()) {
            staminaCost *= 1.5f; // Заряженные атаки требуют больше выносливости
        }
        
        // Проверяем выносливость перед выполнением
        if (!resourceManager.tryUseStamina(staminaCost, "directional attack")) {
            cancelCharging(playerId);
            return new AttackResult(false, "Insufficient stamina", 0, 0, false);
        }
        
        // Помечаем атаку как выполняемую
        attackData.markAsExecuting(System.currentTimeMillis());
        
        return new AttackResult(true, "Attack executed", finalDamage, 
            attackData.getKnockbackMultiplier(), attackData.causesVulnerability());
    }
    
    /**
     * Завершает выполнение атаки
     */
    public void completeAttack(UUID playerId) {
        AttackData attackData = activeAttacks.get(playerId);
        if (attackData != null && attackData.isExecuting()) {
            attackData.markAsCompleted();
            activeAttacks.remove(playerId);
        }
    }
    
    /**
     * Получает данные о текущей атаке игрока
     */
    public AttackData getCurrentAttack(UUID playerId) {
        return activeAttacks.get(playerId);
    }
    
    /**
     * Проверяет, заряжает ли игрок атаку
     */
    public boolean isCharging(UUID playerId) {
        AttackData attackData = activeAttacks.get(playerId);
        return attackData != null && attackData.isCharging();
    }
    
    /**
     * Проверяет, выполняет ли игрок атаку
     */
    public boolean isAttacking(UUID playerId) {
        AttackData attackData = activeAttacks.get(playerId);
        return attackData != null && attackData.isExecuting();
    }
    
    /**
     * Получает прогресс заряжания атаки (0.0 - 1.0)
     */
    public float getChargeProgress(UUID playerId) {
        AttackData attackData = activeAttacks.get(playerId);
        if (attackData == null || !attackData.isCharging()) {
            return 0.0f;
        }
        
        long duration = attackData.getChargeDuration();
        return Math.min(1.0f, duration / 2000.0f); // Максимум на 2 секундах
    }
    
    /**
     * Очищает все устаревшие атаки
     */
    public void cleanupExpiredAttacks(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        activeAttacks.entrySet().removeIf(entry -> {
            AttackData attack = entry.getValue();
            long age = currentTime - attack.getChargeStartTime();
            return age > maxAgeMs;
        });
    }
    
    /**
     * Результат выполнения атаки
     */
    public static class AttackResult {
        private final boolean success;
        private final String message;
        private final float damage;
        private final float knockbackMultiplier;
        private final boolean causesVulnerability;
        
        public AttackResult(boolean success, String message, float damage, 
                          float knockbackMultiplier, boolean causesVulnerability) {
            this.success = success;
            this.message = message;
            this.damage = damage;
            this.knockbackMultiplier = knockbackMultiplier;
            this.causesVulnerability = causesVulnerability;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public float getDamage() { return damage; }
        public float getKnockbackMultiplier() { return knockbackMultiplier; }
        public boolean causesVulnerability() { return causesVulnerability; }
    }
    
    // ============== ИНТЕГРАЦИЯ С PLAYER STATE MACHINE ==============
    
    /**
     * Обрабатывает изменение направления атаки во время подготовки
     */
    public boolean changeAttackDirection(UUID playerId, AttackDirection newDirection) {
        AttackData currentAttack = activeAttacks.get(playerId);
        if (currentAttack == null || !currentAttack.isCharging()) {
            return false;
        }
        
        // Проверяем, хватит ли выносливости для нового направления
        float newStaminaCost = newDirection.getStaminaCost();
        if (!resourceManager.canUseStamina(newStaminaCost)) {
            return false; // Не хватает выносливости для нового направления
        }
        
        // Создаем новую атаку с тем же временем начала зарядки
        AttackData newAttack = new AttackData(playerId, newDirection, currentAttack.getChargeStartTime());
        activeAttacks.put(playerId, newAttack);
        
        return true;
    }
    
    /**
     * Интегрируется с InterruptionSystem для прерывания атак
     */
    public boolean canBeInterrupted(UUID playerId, InterruptionSystem.InterruptionType interruptionType) {
        AttackData attackData = activeAttacks.get(playerId);
        if (attackData == null) {
            return true; // Нет активной атаки - может быть прерван
        }
        
        // Заряженные атаки более уязвимы для прерываний
        if (attackData.isChargedAttack()) {
            return interruptionType.getPriority() >= InterruptionSystem.InterruptionType.MAGICAL_DISRUPTION.getPriority();
        }
        
        // Быстрые атаки сложнее прервать
        if (attackData.getDirection() == AttackDirection.LEFT_ATTACK || 
            attackData.getDirection() == AttackDirection.THRUST_ATTACK) {
            return interruptionType.getPriority() >= InterruptionSystem.InterruptionType.HEAVY_PHYSICAL_HIT.getPriority();
        }
        
        // Обычные атаки имеют среднюю защиту от прерываний
        return interruptionType.getPriority() >= InterruptionSystem.InterruptionType.MAGICAL_DISRUPTION.getPriority();
    }
    
    /**
     * Принудительно прерывает атаку игрока
     */
    public void forceInterruptAttack(UUID playerId, String reason) {
        AttackData attackData = activeAttacks.remove(playerId);
        if (attackData != null) {
            // При прерывании заряженной атаки - часть выносливости все равно тратится
            if (attackData.isChargedAttack()) {
                float penaltyStamina = attackData.getDirection().getStaminaCost() * 0.5f;
                resourceManager.tryUseStamina(penaltyStamina, "interrupted charged attack penalty");
            }
        }
    }
    
    /**
     * Вычисляет урон с учетом защиты цели
     */
    public float calculateDamageAgainstDefense(AttackData attackData, DefensiveActionsManager.DefensiveType defenseType) {
        float baseDamage = attackData.getDirection().getBaseDamage();
        float chargeMultiplier = attackData.getChargeMultiplier();
        
        switch (defenseType) {
            case BLOCK:
                // TOP_ATTACK пробивает блокирование
                if (attackData.getDirection().canBreakBlocks()) {
                    return baseDamage * chargeMultiplier * 0.8f; // Небольшое снижение урона
                } else {
                    return baseDamage * chargeMultiplier * 0.3f; // Сильное снижение при блоке
                }
                
            case PARRY:
                // LEFT_ATTACK сложно парировать
                if (attackData.getDirection().isHardToParry()) {
                    return baseDamage * chargeMultiplier * 0.7f; // Парирование менее эффективно
                } else {
                    return 0; // Успешное парирование полностью блокирует урон
                }
                
            case DODGE:
                return 0; // Успешное уклонение полностью избегает урона
                
            default:
                return baseDamage * chargeMultiplier; // Полный урон без защиты
        }
    }
    
    /**
     * Вычисляет шанс прерывания противника при попадании
     */
    public float calculateInterruptionChance(AttackData attackData) {
        float baseChance = 0.3f; // 30% базовый шанс
        
        // Заряженные атаки имеют больший шанс прерывания
        if (attackData.isChargedAttack()) {
            baseChance += 0.4f; // +40% для заряженных атак
        }
        
        // TOP_ATTACK имеет высокий шанс прерывания
        if (attackData.getDirection() == AttackDirection.TOP_ATTACK) {
            baseChance += 0.3f; // +30% для мощных атак сверху
        }
        
        // THRUST_ATTACK имеет средний шанс прерывания
        if (attackData.getDirection() == AttackDirection.THRUST_ATTACK) {
            baseChance += 0.2f; // +20% для колющих атак
        }
        
        return Math.min(1.0f, baseChance); // Максимум 100%
    }
    
    /**
     * Создает ритмичное комбо для ближнего боя (аналог QTE для магии)
     */
    public static class MeleeComboState {
        private final UUID playerId;
        private final AttackDirection lastDirection;
        private final long lastAttackTime;
        private final int comboLength;
        
        public MeleeComboState(UUID playerId, AttackDirection lastDirection, int comboLength) {
            this.playerId = playerId;
            this.lastDirection = lastDirection;
            this.lastAttackTime = System.currentTimeMillis();
            this.comboLength = comboLength;
        }
        
        public boolean canContinueCombo() {
            long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
            return timeSinceLastAttack < 1000 && comboLength < 4; // Макс 4 атаки в комбо
        }
        
        public AttackDirection getNextComboDirection() {
            // Алгоритм выбора следующего направления в комбо
            return switch (lastDirection) {
                case LEFT_ATTACK -> AttackDirection.RIGHT_ATTACK;
                case RIGHT_ATTACK -> AttackDirection.TOP_ATTACK;
                case TOP_ATTACK -> AttackDirection.THRUST_ATTACK;
                case THRUST_ATTACK -> AttackDirection.LEFT_ATTACK;
            };
        }
        
        public float getComboMultiplier() {
            return 1.0f + (comboLength * 0.15f); // +15% урона за каждую атаку в комбо
        }
    }
    
    private final Map<UUID, MeleeComboState> activeCombos = new HashMap<>();
    
    /**
     * Начинает или продолжает ритмичное комбо
     */
    public boolean tryStartCombo(UUID playerId, AttackDirection direction) {
        MeleeComboState currentCombo = activeCombos.get(playerId);
        
        if (currentCombo == null) {
            // Начинаем новое комбо
            activeCombos.put(playerId, new MeleeComboState(playerId, direction, 1));
            return true;
        }
        
        if (currentCombo.canContinueCombo()) {
            // Продолжаем существующее комбо
            AttackDirection expectedNext = currentCombo.getNextComboDirection();
            if (direction == expectedNext) {
                activeCombos.put(playerId, new MeleeComboState(playerId, direction, currentCombo.comboLength + 1));
                return true;
            } else {
                // Неправильное направление - прерываем комбо
                activeCombos.remove(playerId);
                return false;
            }
        } else {
            // Комбо истекло - начинаем новое
            activeCombos.put(playerId, new MeleeComboState(playerId, direction, 1));
            return true;
        }
    }
    
    /**
     * Получает текущий мультипликатор комбо
     */
    public float getCurrentComboMultiplier(UUID playerId) {
        MeleeComboState combo = activeCombos.get(playerId);
        return combo != null ? combo.getComboMultiplier() : 1.0f;
    }
    
    /**
     * Очищает истекшие комбо
     */
    public void cleanupExpiredCombos() {
        long currentTime = System.currentTimeMillis();
        activeCombos.entrySet().removeIf(entry -> {
            MeleeComboState combo = entry.getValue();
            return currentTime - combo.lastAttackTime > 2000; // 2 секунды на продолжение комбо
        });
    }
}