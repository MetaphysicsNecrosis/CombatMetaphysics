package com.example.examplemod.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Система восстановительных периодов после прерываний
 * Согласно Base_Rules.txt ЧАСТЬ III: "у исполнителя запускается восстановительный период 
 * (в который он не может сразу начать то же или похожие действия)"
 */
public class RecoveryPeriodSystem {
    
    public enum RecoveryType {
        // Магические действия
        SPELL_INTERRUPTION(2000L, PlayerState.MAGIC_PREPARING, PlayerState.MAGIC_CASTING),
        QTE_FAILURE(3000L, PlayerState.QTE_TRANSITION),
        MANA_DEPLETION(1500L, PlayerState.MAGIC_PREPARING, PlayerState.MAGIC_CASTING),
        
        // Ближний бой  
        MELEE_INTERRUPTION(1500L, PlayerState.MELEE_PREPARING, PlayerState.MELEE_CHARGING, PlayerState.MELEE_ATTACKING),
        CHARGED_ATTACK_INTERRUPTED(2500L, PlayerState.MELEE_CHARGING), // Заряженные атаки наказываются сильнее
        STAMINA_EXHAUSTION(2000L, PlayerState.MELEE_PREPARING, PlayerState.MELEE_ATTACKING),
        
        // Защитные действия
        FAILED_PARRY(1000L, PlayerState.PARRYING),
        BROKEN_BLOCK(800L, PlayerState.BLOCKING),
        DODGE_COOLDOWN(1200L, PlayerState.DODGING),
        
        // Общие восстановления
        GENERAL_INTERRUPTION(1000L), // Для неспецифичных прерываний
        HEAVY_DAMAGE_RECOVERY(2000L), // После тяжелого урона
        ENVIRONMENTAL_RECOVERY(1500L); // После environmental hazards
        
        private final long durationMs;
        private final PlayerState[] blockedStates;
        
        RecoveryType(long durationMs, PlayerState... blockedStates) {
            this.durationMs = durationMs;
            this.blockedStates = blockedStates;
        }
        
        public long getDurationMs() { return durationMs; }
        public PlayerState[] getBlockedStates() { return blockedStates; }
        
        public boolean blocksState(PlayerState state) {
            for (PlayerState blocked : blockedStates) {
                if (blocked == state) return true;
            }
            return false;
        }
    }
    
    public static class RecoveryPeriod {
        private final UUID playerId;
        private final RecoveryType type;
        private final long startTime;
        private final String reason;
        private boolean isActive;
        
        public RecoveryPeriod(UUID playerId, RecoveryType type, String reason) {
            this.playerId = playerId;
            this.type = type;
            this.startTime = System.currentTimeMillis();
            this.reason = reason;
            this.isActive = true;
        }
        
        public UUID getPlayerId() { return playerId; }
        public RecoveryType getType() { return type; }
        public long getStartTime() { return startTime; }
        public String getReason() { return reason; }
        public boolean isActive() { return isActive; }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public long getRemainingTime() {
            return Math.max(0, type.getDurationMs() - getElapsedTime());
        }
        
        public boolean hasExpired() {
            return getElapsedTime() >= type.getDurationMs();
        }
        
        public void deactivate() {
            this.isActive = false;
        }
        
        public boolean blocksTransitionTo(PlayerState targetState) {
            return isActive && !hasExpired() && type.blocksState(targetState);
        }
        
        public float getRecoveryProgress() {
            return Math.min(1.0f, (float) getElapsedTime() / type.getDurationMs());
        }
    }
    
    // Активные периоды восстановления по игрокам
    private final Map<UUID, RecoveryPeriod> activeRecoveries = new HashMap<>();
    
    /**
     * Запускает период восстановления для игрока
     */
    public void startRecoveryPeriod(UUID playerId, RecoveryType type, String reason) {
        RecoveryPeriod recovery = new RecoveryPeriod(playerId, type, reason);
        activeRecoveries.put(playerId, recovery);
    }
    
    /**
     * Проверяет, может ли игрок перейти в указанное состояние
     */
    public boolean canTransitionTo(UUID playerId, PlayerState targetState) {
        RecoveryPeriod recovery = activeRecoveries.get(playerId);
        if (recovery == null) {
            return true; // Нет активного восстановления
        }
        
        if (recovery.hasExpired()) {
            // Автоматически очищаем истекший период
            clearRecoveryPeriod(playerId);
            return true;
        }
        
        return !recovery.blocksTransitionTo(targetState);
    }
    
    /**
     * Получает причину блокировки перехода состояния
     */
    public String getBlockingReason(UUID playerId, PlayerState targetState) {
        RecoveryPeriod recovery = activeRecoveries.get(playerId);
        if (recovery == null || !recovery.blocksTransitionTo(targetState)) {
            return null;
        }
        
        long remainingMs = recovery.getRemainingTime();
        return String.format("Recovery period active: %s (%.1fs remaining)", 
                recovery.getReason(), remainingMs / 1000.0f);
    }
    
    /**
     * Принудительно очищает период восстановления
     */
    public void clearRecoveryPeriod(UUID playerId) {
        RecoveryPeriod recovery = activeRecoveries.remove(playerId);
        if (recovery != null) {
            recovery.deactivate();
        }
    }
    
    /**
     * Получает текущий период восстановления игрока
     */
    public RecoveryPeriod getCurrentRecovery(UUID playerId) {
        return activeRecoveries.get(playerId);
    }
    
    /**
     * Проверяет, находится ли игрок в периоде восстановления
     */
    public boolean isInRecoveryPeriod(UUID playerId) {
        RecoveryPeriod recovery = activeRecoveries.get(playerId);
        return recovery != null && recovery.isActive() && !recovery.hasExpired();
    }
    
    /**
     * Получает прогресс восстановления (0.0 - 1.0)
     */
    public float getRecoveryProgress(UUID playerId) {
        RecoveryPeriod recovery = activeRecoveries.get(playerId);
        return recovery != null ? recovery.getRecoveryProgress() : 1.0f;
    }
    
    /**
     * Очищает истекшие периоды восстановления
     */
    public void tick() {
        activeRecoveries.entrySet().removeIf(entry -> {
            RecoveryPeriod recovery = entry.getValue();
            if (recovery.hasExpired()) {
                recovery.deactivate();
                return true;
            }
            return false;
        });
    }
    
    // ============== ИНТЕГРАЦИЯ С СИСТЕМАМИ ПРЕРЫВАНИЙ ==============
    
    /**
     * Автоматически назначает тип восстановления на основе прерывания
     */
    public RecoveryType determineRecoveryType(PlayerState interruptedState, 
                                            InterruptionSystem.InterruptionType interruptionType) {
        return switch (interruptedState) {
            case MAGIC_PREPARING, MAGIC_CASTING -> {
                if (interruptionType == InterruptionSystem.InterruptionType.RESOURCE_DEPLETION) {
                    yield RecoveryType.MANA_DEPLETION;
                } else {
                    yield RecoveryType.SPELL_INTERRUPTION;
                }
            }
            case QTE_TRANSITION -> RecoveryType.QTE_FAILURE;
            
            case MELEE_PREPARING, MELEE_ATTACKING -> {
                if (interruptionType == InterruptionSystem.InterruptionType.RESOURCE_DEPLETION) {
                    yield RecoveryType.STAMINA_EXHAUSTION;
                } else {
                    yield RecoveryType.MELEE_INTERRUPTION;
                }
            }
            case MELEE_CHARGING -> RecoveryType.CHARGED_ATTACK_INTERRUPTED;
            
            case PARRYING -> RecoveryType.FAILED_PARRY;
            case BLOCKING -> RecoveryType.BROKEN_BLOCK;
            case DODGING -> RecoveryType.DODGE_COOLDOWN;
            
            default -> {
                if (interruptionType == InterruptionSystem.InterruptionType.ENVIRONMENTAL_HAZARD) {
                    yield RecoveryType.ENVIRONMENTAL_RECOVERY;
                } else if (interruptionType.getPriority() >= 
                          InterruptionSystem.InterruptionType.HEAVY_PHYSICAL_HIT.getPriority()) {
                    yield RecoveryType.HEAVY_DAMAGE_RECOVERY;
                } else {
                    yield RecoveryType.GENERAL_INTERRUPTION;
                }
            }
        };
    }
    
    /**
     * Обрабатывает прерывание и назначает соответствующий период восстановления
     */
    public void processInterruptionRecovery(UUID playerId, PlayerState interruptedState, 
                                          InterruptionSystem.InterruptionType interruptionType,
                                          String additionalInfo) {
        RecoveryType recoveryType = determineRecoveryType(interruptedState, interruptionType);
        String reason = String.format("Interrupted from %s by %s%s", 
                interruptedState.name(), 
                interruptionType.getDescription(),
                additionalInfo != null ? " (" + additionalInfo + ")" : "");
        
        startRecoveryPeriod(playerId, recoveryType, reason);
    }
    
    /**
     * Специальная обработка для QTE failures
     */
    public void processQteFailure(UUID playerId, float qteScore, String spellName) {
        RecoveryType recoveryType = qteScore < 0.3f ? 
            RecoveryType.QTE_FAILURE : RecoveryType.SPELL_INTERRUPTION;
            
        String reason = String.format("QTE failure for %s (score: %.1f%%)", 
                spellName, qteScore * 100);
                
        startRecoveryPeriod(playerId, recoveryType, reason);
    }
    
    /**
     * Обработка восстановления после заряженных атак
     */
    public void processChargedAttackRecovery(UUID playerId, DirectionalAttackSystem.AttackData attackData, 
                                           boolean wasInterrupted) {
        if (wasInterrupted && attackData.isChargedAttack()) {
            String reason = String.format("Charged %s attack interrupted (%.1fs charge)", 
                    attackData.getDirection().name(), 
                    attackData.getChargeDuration() / 1000.0f);
            startRecoveryPeriod(playerId, RecoveryType.CHARGED_ATTACK_INTERRUPTED, reason);
        }
    }
    
    /**
     * Ускоренное восстановление после успешных защитных действий
     */
    public void processSuccessfulDefenseBonus(UUID playerId, DefensiveActionsManager.DefenseResult defenseResult) {
        if (defenseResult.isSuccess() && defenseResult.hasCounterOpportunity()) {
            // Успешное парирование сокращает текущий период восстановления
            RecoveryPeriod currentRecovery = activeRecoveries.get(playerId);
            if (currentRecovery != null) {
                // Сокращаем оставшееся время на 50%
                RecoveryType newType = createReducedRecoveryType(currentRecovery.getType(), 0.5f);
                String newReason = currentRecovery.getReason() + " (reduced by successful parry)";
                
                clearRecoveryPeriod(playerId);
                startRecoveryPeriod(playerId, newType, newReason);
            }
        }
    }
    
    private RecoveryType createReducedRecoveryType(RecoveryType originalType, float reductionFactor) {
        // Для простоты используем ближайший подходящий тип с меньшей длительностью
        // В полной реализации можно создать динамические типы восстановления
        return switch (originalType) {
            case SPELL_INTERRUPTION -> RecoveryType.MANA_DEPLETION;
            case MELEE_INTERRUPTION -> RecoveryType.GENERAL_INTERRUPTION;
            case CHARGED_ATTACK_INTERRUPTED -> RecoveryType.MELEE_INTERRUPTION;
            case QTE_FAILURE -> RecoveryType.SPELL_INTERRUPTION;
            default -> RecoveryType.GENERAL_INTERRUPTION;
        };
    }
    
    /**
     * Получает статистику системы восстановления
     */
    public Map<String, Object> getRecoveryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeRecoveries", activeRecoveries.size());
        
        if (!activeRecoveries.isEmpty()) {
            Map<String, Object> details = new HashMap<>();
            activeRecoveries.forEach((playerId, recovery) -> {
                details.put(playerId.toString(), Map.of(
                    "type", recovery.getType().name(),
                    "reason", recovery.getReason(),
                    "progress", recovery.getRecoveryProgress(),
                    "remainingMs", recovery.getRemainingTime()
                ));
            });
            stats.put("details", details);
        }
        
        return stats;
    }
}