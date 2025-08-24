package com.example.examplemod.core;

/**
 * Заглушки для совместимости со старым кодом
 * ВРЕМЕННЫЙ ФАЙЛ - для исправления компиляции
 */
public class PlayerStateCompatibility {
    
    /**
     * Временные методы для PlayerState для совместимости
     */
    public static class StateExtensions {
        public static boolean isMeleeState(PlayerState state) {
            return state.isAttackState();
        }
        
        public static boolean isDefensiveState(PlayerState state) {
            return state == PlayerState.BLOCKING || 
                   state == PlayerState.PARRYING || 
                   state == PlayerState.DODGING;
        }
    }
    
    /**
     * Заглушки для отсутствующих состояний (используются в старом коде)
     */
    public static class LegacyStates {
        // Эти состояния заменены на Gothic систему, но нужны для компиляции
        public static final PlayerState IDLE = PlayerState.PEACEFUL;
        public static final PlayerState MELEE_PREPARING = PlayerState.ATTACK_WINDUP;
        public static final PlayerState MELEE_CHARGING = PlayerState.ATTACK_WINDUP;
        public static final PlayerState MELEE_ATTACKING = PlayerState.ATTACK_ACTIVE;
        public static final PlayerState MELEE_RECOVERY = PlayerState.ATTACK_RECOVERY;
        public static final PlayerState DEFENSIVE_ACTION = PlayerState.COMBAT_STANCE;
        public static final PlayerState DEFENSIVE_RECOVERY = PlayerState.COMBAT_STANCE;
        public static final PlayerState MAGIC_COOLDOWN = PlayerState.COMBAT_STANCE;
        public static final PlayerState COOLDOWN = PlayerState.COMBAT_STANCE;
    }
}