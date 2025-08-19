package com.example.examplemod.core;

import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Система контроля движения игрока во время боевых действий
 * Согласно Base_Rules.txt ЧАСТЬ V: в фазе подготовки/замаха игрок неподвижен
 */
public class PlayerMovementController {
    
    private static final Map<UUID, MovementRestriction> restrictedPlayers = new HashMap<>();
    
    public static class MovementRestriction {
        private final PlayerState restrictingState;
        private final long startTime;
        private final boolean allowLooking;
        private final boolean allowAttackMovement; // Движение во время атаки (1.5 блока вперед)
        
        public MovementRestriction(PlayerState restrictingState, boolean allowLooking, boolean allowAttackMovement) {
            this.restrictingState = restrictingState;
            this.startTime = System.currentTimeMillis();
            this.allowLooking = allowLooking;
            this.allowAttackMovement = allowAttackMovement;
        }
        
        public PlayerState getRestrictingState() { return restrictingState; }
        public long getStartTime() { return startTime; }
        public boolean isLookingAllowed() { return allowLooking; }
        public boolean isAttackMovementAllowed() { return allowAttackMovement; }
        
        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
    }
    
    /**
     * Применяет ограничение движения на основе состояния игрока
     */
    public static void applyMovementRestriction(UUID playerId, PlayerState state) {
        MovementRestriction restriction = switch (state) {
            // Магические состояния - полная неподвижность
            case MAGIC_PREPARING -> new MovementRestriction(state, true, false);
            case MAGIC_CASTING -> new MovementRestriction(state, true, false);
            case QTE_TRANSITION -> new MovementRestriction(state, true, false);
            
            // Ближний бой - неподвижность до атаки
            case MELEE_PREPARING -> new MovementRestriction(state, true, false);
            case MELEE_CHARGING -> new MovementRestriction(state, true, false);
            case MELEE_ATTACKING -> new MovementRestriction(state, true, true); // Разрешено движение атаки
            
            // Защитные действия - неподвижность
            case PARRYING -> new MovementRestriction(state, true, false);
            case BLOCKING -> new MovementRestriction(state, true, false);
            case DODGING -> new MovementRestriction(state, false, true); // Движение уклонения разрешено
            
            // Остальные состояния не ограничивают движение
            default -> null;
        };
        
        if (restriction != null) {
            restrictedPlayers.put(playerId, restriction);
        } else {
            restrictedPlayers.remove(playerId);
        }
    }
    
    /**
     * Снимает ограничение движения
     */
    public static void removeMovementRestriction(UUID playerId) {
        restrictedPlayers.remove(playerId);
    }
    
    /**
     * Проверяет, может ли игрок двигаться
     */
    public static boolean canPlayerMove(UUID playerId) {
        MovementRestriction restriction = restrictedPlayers.get(playerId);
        return restriction == null;
    }
    
    /**
     * Проверяет, может ли игрок поворачивать камеру
     */
    public static boolean canPlayerLook(UUID playerId) {
        MovementRestriction restriction = restrictedPlayers.get(playerId);
        return restriction == null || restriction.isLookingAllowed();
    }
    
    /**
     * Проверяет, может ли игрок двигаться во время атаки (анимационное движение)
     */
    public static boolean canPlayerAttackMove(UUID playerId) {
        MovementRestriction restriction = restrictedPlayers.get(playerId);
        return restriction != null && restriction.isAttackMovementAllowed();
    }
    
    /**
     * Получает текущее ограничение движения игрока
     */
    public static MovementRestriction getMovementRestriction(UUID playerId) {
        return restrictedPlayers.get(playerId);
    }
    
    /**
     * Применяет анимационное движение для атаки (1.5 блока вперед)
     */
    public static void applyAttackMovement(Player player, DirectionalAttackSystem.AttackDirection direction) {
        if (!canPlayerAttackMove(player.getUUID())) {
            return; // Движение не разрешено
        }
        
        // Вычисляем направление движения на основе направления взгляда и типа атаки
        float distance = switch (direction) {
            case LEFT_ATTACK -> 1.0f;      // Быстрые атаки - меньше движения
            case RIGHT_ATTACK -> 1.2f;     // Средние атаки
            case TOP_ATTACK -> 1.5f;       // Мощные атаки - больше движения вперед
            case THRUST_ATTACK -> 1.8f;    // Колющие атаки - максимальное движение
        };
        
        // Движение в направлении взгляда игрока
        double yaw = Math.toRadians(player.getYRot());
        double deltaX = -Math.sin(yaw) * distance;
        double deltaZ = Math.cos(yaw) * distance;
        
        // Применяем движение плавно в течение анимации атаки
        player.setDeltaMovement(player.getDeltaMovement().add(deltaX * 0.1, 0, deltaZ * 0.1));
    }
    
    /**
     * Синхронизирует ограничения движения с состоянием игрока из PlayerStateMachine
     */
    public static void syncWithPlayerState(UUID playerId, PlayerState currentState) {
        applyMovementRestriction(playerId, currentState);
    }
    
    /**
     * Проверяет, должен ли игрок быть неподвижен в текущем состоянии
     */
    public static boolean shouldRestrictMovement(PlayerState state) {
        return switch (state) {
            case MAGIC_PREPARING, MAGIC_CASTING, QTE_TRANSITION,
                 MELEE_PREPARING, MELEE_CHARGING,
                 PARRYING, BLOCKING -> true;
            case MELEE_ATTACKING, DODGING -> false; // Разрешено анимационное движение
            default -> false; // Остальные состояния не ограничивают движение
        };
    }
    
    /**
     * Применяет движение уклонения
     */
    public static void applyDodgeMovement(Player player, DodgeDirection dodgeDirection) {
        if (!canPlayerAttackMove(player.getUUID())) {
            return;
        }
        
        float distance = 2.0f; // Расстояние уклонения
        double yaw = Math.toRadians(player.getYRot());
        
        double deltaX = 0, deltaZ = 0;
        
        switch (dodgeDirection) {
            case FORWARD -> {
                deltaX = -Math.sin(yaw) * distance;
                deltaZ = Math.cos(yaw) * distance;
            }
            case BACKWARD -> {
                deltaX = Math.sin(yaw) * distance;
                deltaZ = -Math.cos(yaw) * distance;
            }
            case LEFT -> {
                deltaX = -Math.cos(yaw) * distance;
                deltaZ = -Math.sin(yaw) * distance;
            }
            case RIGHT -> {
                deltaX = Math.cos(yaw) * distance;
                deltaZ = Math.sin(yaw) * distance;
            }
        }
        
        // Применяем импульс движения для уклонения
        player.setDeltaMovement(player.getDeltaMovement().add(deltaX * 0.15, 0, deltaZ * 0.15));
    }
    
    public enum DodgeDirection {
        FORWARD, BACKWARD, LEFT, RIGHT
    }
    
    /**
     * Очищает все ограничения движения (для отладки)
     */
    public static void clearAllRestrictions() {
        restrictedPlayers.clear();
    }
    
    /**
     * Получает статистику ограничений движения
     */
    public static Map<String, Object> getRestrictionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("restrictedPlayers", restrictedPlayers.size());
        
        if (!restrictedPlayers.isEmpty()) {
            Map<String, Object> details = new HashMap<>();
            restrictedPlayers.forEach((playerId, restriction) -> {
                details.put(playerId.toString(), Map.of(
                    "state", restriction.getRestrictingState().name(),
                    "duration", restriction.getDuration(),
                    "allowLooking", restriction.isLookingAllowed(),
                    "allowAttackMovement", restriction.isAttackMovementAllowed()
                ));
            });
            stats.put("details", details);
        }
        
        return stats;
    }
}