package com.example.examplemod.api;

import com.example.examplemod.core.*;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Простой API для взаимодействия с Gothic Combat System
 * Предоставляет удобные методы для интеграции с UI, input handlers и другими системами
 */
public class GothicCombatAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(GothicCombatAPI.class);
    
    // === БАЗОВЫЕ ДЕЙСТВИЯ ===
    
    /**
     * Атакует в указанном направлении
     */
    public static CombatResult attack(Player player, AttackDirection direction) {
        UUID playerId = player.getUUID();
        PlayerStateMachine stateMachine = getStateMachine(player);
        
        // Устанавливаем Player объект для hit detection
        GothicAttackSystem.setPlayerInstance(playerId, player);
        
        // Конвертируем направление
        GothicAttackSystem.AttackDirection gothicDirection = convertAttackDirection(direction);
        
        // Выполняем атаку
        GothicAttackSystem.AttackResult result = stateMachine.startGothicAttack(gothicDirection);
        
        return new CombatResult(
            result.isSuccess(),
            result.getMessage(),
            result.isCombo() ? "Combo x" + result.getComboLength() : null
        );
    }
    
    /**
     * Защищается указанным способом
     */
    public static CombatResult defend(Player player, DefenseType defenseType) {
        PlayerStateMachine stateMachine = getStateMachine(player);
        
        // Конвертируем тип защиты
        GothicDefenseSystem.DefenseType gothicDefense = convertDefenseType(defenseType);
        
        // Выполняем защиту
        GothicDefenseSystem.DefenseActionResult result = stateMachine.startDefense(gothicDefense);
        
        String extraInfo = null;
        if (result.canCounterattack()) {
            extraInfo = "Counterattack available!";
        }
        
        return new CombatResult(
            result.isSuccess(),
            result.getMessage(),
            extraInfo
        );
    }
    
    /**
     * Принудительно входит в боевую стойку
     */
    public static CombatResult enterCombatStance(Player player) {
        PlayerStateMachine stateMachine = getStateMachine(player);
        PlayerStateMachine.StateTransitionResult result = stateMachine.transitionTo(PlayerState.COMBAT_STANCE, "Manual activation");
        boolean success = result.isSuccess();
        
        return new CombatResult(
            success,
            success ? "Entered combat stance" : "Already in combat",
            null
        );
    }
    
    /**
     * Выходит в мирное состояние (если возможно)
     */
    public static CombatResult exitToPeaceful(Player player) {
        PlayerStateMachine stateMachine = getStateMachine(player);
        PlayerStateMachine.StateTransitionResult result = stateMachine.transitionTo(PlayerState.PEACEFUL, "Manual exit to peaceful");
        boolean success = result.isSuccess();
        
        return new CombatResult(
            success,
            success ? "Returned to peaceful state" : "Cannot exit combat now",
            null
        );
    }
    
    // === ИНФОРМАЦИОННЫЕ МЕТОДЫ ===
    
    /**
     * Получает текущую информацию о боевом состоянии игрока
     */
    public static CombatInfo getCombatInfo(Player player) {
        UUID playerId = player.getUUID();
        PlayerStateMachine stateMachine = getStateMachine(player);
        StaminaManager staminaManager = stateMachine.getStaminaManager();
        
        PlayerState currentState = stateMachine.getCurrentState();
        
        return new CombatInfo(
            currentState,
            currentState.isCombatState(),
            currentState.canMove(),
            currentState.isVulnerable(),
            staminaManager.getCurrentStamina(playerId),
            staminaManager.getMaxStamina(playerId),
            staminaManager.getStaminaLevel(playerId),
            staminaManager.isExhausted(playerId),
            stateMachine.getAttackSystem().hasActiveAttack(playerId),
            stateMachine.getDefenseSystem().hasActiveDefense(playerId),
            stateMachine.getTimeInCurrentState()
        );
    }
    
    /**
     * Проверяет, может ли игрок выполнить действие
     */
    public static boolean canPerformAction(Player player, CombatAction action) {
        UUID playerId = player.getUUID();
        PlayerStateMachine stateMachine = getStateMachine(player);
        PlayerState currentState = stateMachine.getCurrentState();
        StaminaManager staminaManager = stateMachine.getStaminaManager();
        
        return switch (action) {
            case ATTACK -> (currentState == PlayerState.COMBAT_STANCE || currentState == PlayerState.COMBO_WINDOW) 
                          && !staminaManager.isExhausted(playerId);
            case DEFEND -> currentState == PlayerState.COMBAT_STANCE;
            case MOVE -> currentState.canMove();
            case ENTER_COMBAT -> currentState == PlayerState.PEACEFUL;
            case EXIT_COMBAT -> currentState == PlayerState.COMBAT_STANCE;
        };
    }
    
    /**
     * Получает детальную информацию для отладки
     */
    public static Map<String, Object> getDebugInfo(Player player) {
        PlayerStateMachine stateMachine = getStateMachine(player);
        return stateMachine.getDebugInfo();
    }
    
    // === УТИЛИТЫ ===
    
    private static PlayerStateMachine getStateMachine(Player player) {
        return PlayerStateMachine.getInstance(player.getUUID(), null); // TODO: Передать ResourceManager
    }
    
    private static GothicAttackSystem.AttackDirection convertAttackDirection(AttackDirection direction) {
        return switch (direction) {
            case LEFT -> GothicAttackSystem.AttackDirection.LEFT;
            case RIGHT -> GothicAttackSystem.AttackDirection.RIGHT;
            case TOP -> GothicAttackSystem.AttackDirection.TOP;
            case THRUST -> GothicAttackSystem.AttackDirection.THRUST;
        };
    }
    
    private static GothicDefenseSystem.DefenseType convertDefenseType(DefenseType defenseType) {
        return switch (defenseType) {
            case BLOCK -> GothicDefenseSystem.DefenseType.BLOCK;
            case PARRY -> GothicDefenseSystem.DefenseType.PARRY;
            case DODGE -> GothicDefenseSystem.DefenseType.DODGE;
        };
    }
    
    // === РЕЗУЛЬТАТЫ И ТИПЫ ===
    
    /**
     * Результат выполнения боевого действия
     */
    public static class CombatResult {
        private final boolean success;
        private final String message;
        private final String extraInfo;
        
        public CombatResult(boolean success, String message, String extraInfo) {
            this.success = success;
            this.message = message;
            this.extraInfo = extraInfo;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getExtraInfo() { return extraInfo; }
        public boolean hasExtraInfo() { return extraInfo != null; }
    }
    
    /**
     * Информация о текущем боевом состоянии игрока
     */
    public static class CombatInfo {
        private final PlayerState currentState;
        private final boolean inCombat;
        private final boolean canMove;
        private final boolean isVulnerable;
        private final float currentStamina;
        private final float maxStamina;
        private final StaminaManager.StaminaLevel staminaLevel;
        private final boolean isExhausted;
        private final boolean isAttacking;
        private final boolean isDefending;
        private final long timeInState;
        
        public CombatInfo(PlayerState currentState, boolean inCombat, boolean canMove, boolean isVulnerable,
                         float currentStamina, float maxStamina, StaminaManager.StaminaLevel staminaLevel,
                         boolean isExhausted, boolean isAttacking, boolean isDefending, long timeInState) {
            this.currentState = currentState;
            this.inCombat = inCombat;
            this.canMove = canMove;
            this.isVulnerable = isVulnerable;
            this.currentStamina = currentStamina;
            this.maxStamina = maxStamina;
            this.staminaLevel = staminaLevel;
            this.isExhausted = isExhausted;
            this.isAttacking = isAttacking;
            this.isDefending = isDefending;
            this.timeInState = timeInState;
        }
        
        // Геттеры
        public PlayerState getCurrentState() { return currentState; }
        public boolean isInCombat() { return inCombat; }
        public boolean canMove() { return canMove; }
        public boolean isVulnerable() { return isVulnerable; }
        public float getCurrentStamina() { return currentStamina; }
        public float getMaxStamina() { return maxStamina; }
        public float getStaminaPercentage() { return currentStamina / maxStamina; }
        public StaminaManager.StaminaLevel getStaminaLevel() { return staminaLevel; }
        public boolean isExhausted() { return isExhausted; }
        public boolean isAttacking() { return isAttacking; }
        public boolean isDefending() { return isDefending; }
        public long getTimeInState() { return timeInState; }
        
        /**
         * Получает цвет для UI индикатора выносливости
         */
        public int getStaminaColor() {
            return switch (staminaLevel) {
                case FULL, HIGH -> 0x00FF00;        // Зеленый
                case MEDIUM -> 0xFFFF00;            // Желтый
                case LOW -> 0xFF8800;               // Оранжевый
                case CRITICAL -> 0xFF0000;          // Красный
                case EXHAUSTED -> 0x800000;         // Темно-красный
            };
        }
        
        /**
         * Получает текстовое описание состояния
         */
        public String getStateDescription() {
            return switch (currentState) {
                case PEACEFUL -> "Peaceful";
                case COMBAT_STANCE -> "Ready for combat";
                case ATTACK_WINDUP -> "Winding up attack";
                case ATTACK_ACTIVE -> "Attacking!";
                case ATTACK_RECOVERY -> "Recovering from attack";
                case COMBO_WINDOW -> "Combo available!";
                case BLOCKING -> "Blocking";
                case PARRYING -> "Parrying";
                case DODGING -> "Dodging";
                case EXHAUSTED -> "Exhausted";
                case STUNNED -> "Stunned";
                default -> currentState.name();
            };
        }
    }
    
    /**
     * Направления атаки (упрощенные для API)
     */
    public enum AttackDirection {
        LEFT, RIGHT, TOP, THRUST
    }
    
    /**
     * Типы защиты (упрощенные для API)
     */
    public enum DefenseType {
        BLOCK, PARRY, DODGE
    }
    
    /**
     * Типы боевых действий
     */
    public enum CombatAction {
        ATTACK, DEFEND, MOVE, ENTER_COMBAT, EXIT_COMBAT
    }
}