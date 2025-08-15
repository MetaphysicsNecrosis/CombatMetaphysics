package com.example.examplemod.core;

public enum PlayerState {
    // Базовые состояния
    IDLE,
    INTERRUPTED,
    
    // Магические состояния
    MAGIC_PREPARING,
    MAGIC_CASTING,
    QTE_TRANSITION,
    MAGIC_COOLDOWN,
    
    // Ближний бой состояния
    MELEE_PREPARING,
    MELEE_CHARGING,
    MELEE_ATTACKING,
    MELEE_RECOVERY,
    
    // Защитные действия
    DEFENSIVE_ACTION,
    BLOCKING,
    PARRYING, 
    DODGING,
    DEFENSIVE_RECOVERY,
    
    // Общий кулдаун
    COOLDOWN;

    public boolean canTransitionTo(PlayerState newState) {
        return switch (this) {
            case IDLE -> newState == MAGIC_PREPARING || newState == MELEE_PREPARING || newState == DEFENSIVE_ACTION;
            
            // Магические переходы
            case MAGIC_PREPARING -> newState == MAGIC_CASTING || newState == INTERRUPTED;
            case MAGIC_CASTING -> newState == QTE_TRANSITION || newState == MAGIC_COOLDOWN || newState == INTERRUPTED;
            case QTE_TRANSITION -> newState == MAGIC_CASTING || newState == MAGIC_COOLDOWN || newState == INTERRUPTED;
            case MAGIC_COOLDOWN -> newState == COOLDOWN;
            
            // Ближний бой переходы  
            case MELEE_PREPARING -> newState == MELEE_CHARGING || newState == INTERRUPTED;
            case MELEE_CHARGING -> newState == MELEE_ATTACKING || newState == INTERRUPTED;
            case MELEE_ATTACKING -> newState == MELEE_RECOVERY || newState == INTERRUPTED;
            case MELEE_RECOVERY -> newState == COOLDOWN;
            
            // Защитные переходы (НЕ прерываются)
            case DEFENSIVE_ACTION -> newState == BLOCKING || newState == PARRYING || newState == DODGING;
            case BLOCKING, PARRYING, DODGING -> newState == DEFENSIVE_RECOVERY;
            case DEFENSIVE_RECOVERY -> newState == COOLDOWN;
            
            // Финальные переходы
            case COOLDOWN -> newState == IDLE;
            case INTERRUPTED -> newState == IDLE;
        };
    }

    public boolean isActive() {
        return this != IDLE && this != INTERRUPTED;
    }

    public boolean canBeInterrupted() {
        // Защитные действия НЕ могут быть прерваны согласно диаграмме состояний
        return this == MAGIC_PREPARING || this == MAGIC_CASTING || this == QTE_TRANSITION ||
               this == MELEE_PREPARING || this == MELEE_CHARGING || this == MELEE_ATTACKING;
    }
    
    public boolean isMagicState() {
        return this == MAGIC_PREPARING || this == MAGIC_CASTING || this == QTE_TRANSITION || this == MAGIC_COOLDOWN;
    }
    
    public boolean isMeleeState() {
        return this == MELEE_PREPARING || this == MELEE_CHARGING || this == MELEE_ATTACKING || this == MELEE_RECOVERY;
    }
    
    public boolean isDefensiveState() {
        return this == DEFENSIVE_ACTION || this == BLOCKING || this == PARRYING || this == DODGING || this == DEFENSIVE_RECOVERY;
    }
    
    public CombatType getCombatType() {
        if (isMagicState()) return CombatType.MAGIC;
        if (isMeleeState()) return CombatType.MELEE;
        if (isDefensiveState()) return CombatType.DEFENSIVE;
        return CombatType.NONE;
    }
    
    public enum CombatType {
        NONE, MAGIC, MELEE, DEFENSIVE
    }
}