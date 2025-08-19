package com.example.examplemod.core;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Гибридная state machine для combat системы, интегрирующая все подсистемы
 * Согласно CLAUDE.md: поддерживает магию, ближний бой и защитные действия
 */
public class PlayerStateMachine {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStateMachine.class);
    
    // Статические экземпляры для каждого игрока
    private static final Map<UUID, PlayerStateMachine> instances = new HashMap<>();
    
    private final UUID playerId;
    private PlayerState currentState;
    private long stateChangeTime;
    private String currentAction;
    private Player playerInstance; // Кэшируем Player объект для движения
    
    // Интегрированные системы
    private final ResourceManager resourceManager;
    private final ActionResolver actionResolver;
    private final DirectionalAttackSystem attackSystem;
    private final DefensiveActionsManager defenseSystem;
    private final InterruptionSystem interruptionSystem;
    private final RecoveryPeriodSystem recoverySystem;
    
    private PlayerStateMachine(UUID playerId, ResourceManager resourceManager) {
        this.playerId = playerId;
        this.currentState = PlayerState.IDLE;
        this.stateChangeTime = System.currentTimeMillis();
        this.currentAction = "";
        
        // Инициализируем интегрированные системы
        this.resourceManager = resourceManager;
        this.actionResolver = new ActionResolver();
        this.attackSystem = new DirectionalAttackSystem(resourceManager);
        this.defenseSystem = new DefensiveActionsManager(resourceManager);
        this.interruptionSystem = new InterruptionSystem(this);
        this.recoverySystem = new RecoveryPeriodSystem();
    }
    
    /**
     * Получает или создает экземпляр state machine для игрока
     */
    public static PlayerStateMachine getInstance(UUID playerId, ResourceManager resourceManager) {
        return instances.computeIfAbsent(playerId, id -> new PlayerStateMachine(id, resourceManager));
    }
    
    /**
     * Получает существующий экземпляр state machine для игрока
     */
    public static PlayerStateMachine getInstance(UUID playerId) {
        return instances.get(playerId);
    }
    
    /**
     * Удаляет экземпляр state machine для игрока
     */
    public static void removeInstance(UUID playerId) {
        instances.remove(playerId);
    }
    
    /**
     * Базовый переход состояния с проверкой через ActionResolver
     */
    public boolean tryTransition(PlayerState newState, String action) {
        return tryTransition(newState, action, ActionResolver.ActionType.MAGIC_CAST);
    }
    
    /**
     * Переход состояния с указанием типа действия
     */
    public boolean tryTransition(PlayerState newState, String action, ActionResolver.ActionType actionType) {
        // Проверяем период восстановления
        if (!recoverySystem.canTransitionTo(playerId, newState)) {
            String blockingReason = recoverySystem.getBlockingReason(playerId, newState);
            LOGGER.debug("State transition blocked by recovery period for player {}: {}", playerId, blockingReason);
            return false;
        }
        
        // Создаем запрос на действие
        ActionResolver.ActionRequest request = new ActionResolver.ActionRequest(
            playerId, actionType, newState, action);
        
        // Проверяем через ActionResolver
        ActionResolver.ResolutionResult result = actionResolver.resolveAction(request, currentState);
        
        if (result.isAllowed()) {
            PlayerState oldState = currentState;
            currentState = newState;
            stateChangeTime = System.currentTimeMillis();
            currentAction = action != null ? action : "";
            
            // Синхронизируем ограничения движения
            PlayerMovementController.syncWithPlayerState(playerId, newState);
            
            LOGGER.debug("Player {} state transition: {} -> {} (action: {}, type: {})", 
                playerId, oldState, newState, action, actionType);
            
            // Уведомляем систему прерываний об успешном переходе
            if (oldState == PlayerState.INTERRUPTED) {
                interruptionSystem.clearInterruption(playerId);
                recoverySystem.clearRecoveryPeriod(playerId); // Очищаем период восстановления при выходе из прерывания
            }
            
            return true;
        } else {
            LOGGER.debug("State transition denied for player {}: {}", playerId, result.getReason());
            return false;
        }
    }
    
    /**
     * Принудительный переход состояния (для системных действий)
     */
    public void forceTransition(PlayerState newState, String reason) {
        PlayerState oldState = currentState;
        currentState = newState;
        stateChangeTime = System.currentTimeMillis();
        currentAction = reason != null ? reason : "";
        
        // Синхронизируем ограничения движения при принудительном переходе
        PlayerMovementController.syncWithPlayerState(playerId, newState);
        
        LOGGER.warn("Forced state transition for player {}: {} -> {} (reason: {})", 
            playerId, oldState, newState, reason);
        
        // Очищаем действие в ActionResolver
        actionResolver.clearAction(playerId);
        
        // Очищаем системы при выходе из прерванного состояния
        if (oldState == PlayerState.INTERRUPTED) {
            interruptionSystem.clearInterruption(playerId);
            recoverySystem.clearRecoveryPeriod(playerId);
        }
    }
    
    /**
     * Прерывание через InterruptionSystem
     */
    public boolean interrupt(InterruptionSystem.InterruptionType type, String reason) {
        PlayerState stateBeforeInterruption = currentState;
        
        InterruptionSystem.InterruptionRequest request = 
            new InterruptionSystem.InterruptionRequest(playerId, null, type, reason);
        
        InterruptionSystem.InterruptionResult result = interruptionSystem.processInterruption(request);
        
        if (result.isSuccess()) {
            // Запускаем период восстановления согласно Base_Rules.txt
            recoverySystem.processInterruptionRecovery(playerId, stateBeforeInterruption, type, reason);
            
            // Синхронизируем ограничения движения для прерванного состояния
            PlayerMovementController.syncWithPlayerState(playerId, result.getResultingState());
        }
        
        return result.isSuccess();
    }
    
    // === МАГИЧЕСКИЕ ДЕЙСТВИЯ ===
    
    /**
     * Начинает подготовку магического заклинания
     */
    public boolean startMagicPreparation(String spellName, float manaCost) {
        if (!resourceManager.canReserveMana(manaCost)) {
            return false;
        }
        
        if (tryTransition(PlayerState.MAGIC_PREPARING, "Preparing spell: " + spellName, 
                         ActionResolver.ActionType.MAGIC_CAST)) {
            resourceManager.tryReserveMana(manaCost, "Magic preparation: " + spellName);
            return true;
        }
        return false;
    }
    
    /**
     * Переходит к кастованию заклинания
     */
    public boolean startMagicCasting(String spellName) {
        return tryTransition(PlayerState.MAGIC_CASTING, "Casting spell: " + spellName, 
                           ActionResolver.ActionType.MAGIC_CAST);
    }
    
    /**
     * Активирует QTE переход
     */
    public boolean startQTETransition(String qteType) {
        return tryTransition(PlayerState.QTE_TRANSITION, "QTE: " + qteType, 
                           ActionResolver.ActionType.MAGIC_CAST);
    }
    
    // === БЛИЖНИЙ БОЙ ===
    
    /**
     * Начинает подготовку атаки ближнего боя
     */
    public boolean startMeleePreparation(DirectionalAttackSystem.AttackDirection direction) {
        if (tryTransition(PlayerState.MELEE_PREPARING, "Preparing melee attack: " + direction, 
                         ActionResolver.ActionType.MELEE_ATTACK)) {
            return attackSystem.startCharging(playerId, direction);
        }
        return false;
    }
    
    /**
     * Переходит к зарядке атаки
     */
    public boolean startMeleeCharging() {
        return tryTransition(PlayerState.MELEE_CHARGING, "Charging melee attack", 
                           ActionResolver.ActionType.MELEE_ATTACK);
    }
    
    /**
     * Выполняет атаку ближнего боя
     */
    public DirectionalAttackSystem.AttackResult executeMeleeAttack() {
        if (tryTransition(PlayerState.MELEE_ATTACKING, "Executing melee attack", 
                         ActionResolver.ActionType.MELEE_ATTACK)) {
            DirectionalAttackSystem.AttackResult result = attackSystem.executeAttack(playerId);
            
            // Применяем анимационное движение если атака успешна
            if (result.isSuccess()) {
                DirectionalAttackSystem.AttackData attackData = attackSystem.getCurrentAttack(playerId);
                if (attackData != null) {
                    // Получаем Player объект для движения (предполагается, что он доступен в контексте)
                    applyAttackMovement(attackData.getDirection());
                }
            }
            
            return result;
        }
        return new DirectionalAttackSystem.AttackResult(false, "Invalid state for melee attack", 0, 0, false);
    }
    
    /**
     * Устанавливает Player объект для движения
     */
    public void setPlayerInstance(Player player) {
        this.playerInstance = player;
    }
    
    
    /**
     * Применяет движение атаки
     */
    private void applyAttackMovement(DirectionalAttackSystem.AttackDirection direction) {
        Player player = this.playerInstance;
        if (player != null) {
            PlayerMovementController.applyAttackMovement(player, direction);
            LOGGER.debug("Applied attack movement for player {} with direction {}", playerId, direction);
        } else {
            LOGGER.warn("Cannot apply attack movement - no player instance for {}", playerId);
        }
    }
    
    // === ЗАЩИТНЫЕ ДЕЙСТВИЯ ===
    
    /**
     * Активирует защитное действие
     */
    public boolean startDefensiveAction(DefensiveActionsManager.DefensiveType type) {
        if (tryTransition(PlayerState.DEFENSIVE_ACTION, "Starting defensive action: " + type, 
                         ActionResolver.ActionType.DEFENSIVE_ACTION)) {
            // Сразу переходим к выполнению защиты
            return executeDefense(type);
        }
        return false;
    }
    
    /**
     * Переходит к конкретному защитному состоянию
     */
    public boolean executeDefense(DefensiveActionsManager.DefensiveType type) {
        PlayerState targetState = switch (type) {
            case PARRY -> PlayerState.PARRYING;
            case BLOCK -> PlayerState.BLOCKING;
            case DODGE -> PlayerState.DODGING;
        };
        
        if (tryTransition(targetState, "Executing defense: " + type, 
                         ActionResolver.ActionType.DEFENSIVE_ACTION)) {
            // Активируем защитную систему
            boolean success = defenseSystem.activateDefense(playerId, type);
            
            // Применяем движение уклонения если нужно
            if (success && type == DefensiveActionsManager.DefensiveType.DODGE) {
                applyDodgeMovement();
            }
            
            return success;
        }
        return false;
    }
    
    /**
     * Применяет движение уклонения
     */
    private void applyDodgeMovement() {
        Player player = this.playerInstance;
        if (player != null) {
            // Определяем направление уклонения на основе ввода игрока
            // TODO: Получить направление от input system
            PlayerMovementController.DodgeDirection direction = PlayerMovementController.DodgeDirection.BACKWARD;
            PlayerMovementController.applyDodgeMovement(player, direction);
            LOGGER.debug("Applied dodge movement for player {}", playerId);
        } else {
            LOGGER.warn("Cannot apply dodge movement - no player instance for {}", playerId);
        }
    }
    
    // === СИСТЕМА ВОССТАНОВЛЕНИЯ ===
    
    /**
     * Переходит к восстановлению после действия
     */
    public boolean startRecovery(String actionType) {
        PlayerState recoveryState = switch (currentState.getCombatType()) {
            case MAGIC -> PlayerState.MAGIC_COOLDOWN;
            case MELEE -> PlayerState.MELEE_RECOVERY;
            case DEFENSIVE -> PlayerState.DEFENSIVE_RECOVERY;
            default -> PlayerState.COOLDOWN;
        };
        
        return tryTransition(recoveryState, "Recovery from: " + actionType);
    }
    
    // === МЕТОДЫ ДЛЯ ACTIONRESOLVER ===
    
    /**
     * Проверяет, был ли игрок прерван
     */
    public boolean wasInterrupted(UUID playerId) {
        return interruptionSystem.isInterrupted(playerId);
    }
    
    // === СИСТЕМА TICK И ОБСЛУЖИВАНИЕ ===
    
    /**
     * Обновляет состояние всех систем игрока
     */
    public void tick() {
        // Обновляем системы восстановления
        recoverySystem.tick();
        
        // Очищаем истекшие защитные действия
        defenseSystem.advancedTick();
        
        // Очищаем истекшие атаки
        attackSystem.cleanupExpiredAttacks(10000); // 10 секунд максимальный возраст
        attackSystem.cleanupExpiredCombos();
        
        // Очищаем истекшие прерывания
        interruptionSystem.cleanupExpiredInterruptions(5000); // 5 секунд
        
        // Автоматический переход из INTERRUPTED в IDLE при завершении периода восстановления
        if (currentState == PlayerState.INTERRUPTED && !recoverySystem.isInRecoveryPeriod(playerId)) {
            tryTransition(PlayerState.IDLE, "Recovery period completed");
        }
    }
    
    // === ГЕТТЕРЫ ДЛЯ ИНТЕГРИРОВАННЫХ СИСТЕМ ===
    
    public RecoveryPeriodSystem getRecoverySystem() { 
        return recoverySystem; 
    }
    
    public DirectionalAttackSystem getAttackSystem() { 
        return attackSystem; 
    }
    
    public DefensiveActionsManager getDefenseSystem() { 
        return defenseSystem; 
    }
    
    public InterruptionSystem getInterruptionSystem() { 
        return interruptionSystem; 
    }
    
    public ResourceManager getResourceManager() { 
        return resourceManager; 
    }
    
    // === ПОЛНАЯ ИНТЕГРАЦИЯ С ДВИЖЕНИЕМ ===
    
    /**
     * Очищает все состояния игрока при отключении или сбросе
     */
    public void cleanup() {
        // Очищаем все ограничения движения
        PlayerMovementController.removeMovementRestriction(playerId);
        
        // Очищаем периоды восстановления
        recoverySystem.clearRecoveryPeriod(playerId);
        
        // Очищаем прерывания
        interruptionSystem.clearInterruption(playerId);
        
        // Отменяем активные действия
        if (attackSystem.isCharging(playerId) || attackSystem.isAttacking(playerId)) {
            attackSystem.cancelCharging(playerId);
        }
        
        if (defenseSystem.isDefending(playerId)) {
            defenseSystem.deactivateDefense(playerId);
        }
        
        // Принудительно переводим в IDLE
        forceTransition(PlayerState.IDLE, "Player cleanup");
        
        LOGGER.info("Cleaned up combat state for player {}", playerId);
    }
    
    /**
     * Получает детальную информацию о состоянии игрока для отладки
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // Базовая информация о состоянии
        info.put("playerId", playerId.toString());
        info.put("currentState", currentState.name());
        info.put("timeInState", getTimeInCurrentState());
        info.put("currentAction", currentAction);
        
        // Информация о движении
        PlayerMovementController.MovementRestriction restriction = 
            PlayerMovementController.getMovementRestriction(playerId);
        info.put("movementRestricted", restriction != null);
        if (restriction != null) {
            info.put("restrictionType", restriction.getRestrictingState().name());
            info.put("restrictionDuration", restriction.getDuration());
        }
        
        // Информация о восстановлении
        info.put("inRecoveryPeriod", recoverySystem.isInRecoveryPeriod(playerId));
        if (recoverySystem.isInRecoveryPeriod(playerId)) {
            RecoveryPeriodSystem.RecoveryPeriod recovery = recoverySystem.getCurrentRecovery(playerId);
            info.put("recoveryType", recovery.getType().name());
            info.put("recoveryProgress", recovery.getRecoveryProgress());
            info.put("recoveryRemainingMs", recovery.getRemainingTime());
        }
        
        // Информация о боевых системах
        info.put("isCharging", attackSystem.isCharging(playerId));
        info.put("isAttacking", attackSystem.isAttacking(playerId));
        info.put("isDefending", defenseSystem.isDefending(playerId));
        info.put("isInterrupted", interruptionSystem.isInterrupted(playerId));
        
        // Ресурсы
        info.put("currentStamina", resourceManager.getCurrentStamina(playerId));
        info.put("currentMana", resourceManager.getCurrentMana(playerId));
        info.put("reservedMana", resourceManager.getReservedMana());
        
        return info;
    }
    
    /**
     * Статический метод для полной очистки всех игроков
     */
    public static void cleanupAllPlayers() {
        instances.values().forEach(PlayerStateMachine::cleanup);
        PlayerMovementController.clearAllRestrictions();
        LOGGER.info("Cleaned up all player combat states");
    }
    
    /**
     * Получает все активные экземпляры state machine
     */
    public static Map<UUID, PlayerStateMachine> getAllInstances() {
        return new HashMap<>(instances);
    }
    
    
    /**
     * Получает время начала QTE для игрока
     */
    public long getQteStartTime(UUID playerId) {
        // Возвращаем время перехода в QTE_TRANSITION состояние
        if (currentState == PlayerState.QTE_TRANSITION) {
            return stateChangeTime;
        }
        return System.currentTimeMillis(); // Fallback
    }
    
    /**
     * Завершает действие и возвращается к IDLE
     */
    public boolean completeAction() {
        // Очищаем соответствующие системы
        if (currentState.isMeleeState()) {
            attackSystem.completeAttack(playerId);
        }
        if (currentState.isDefensiveState()) {
            defenseSystem.deactivateDefense(playerId);
        }
        
        // Очищаем действие в ActionResolver
        actionResolver.clearAction(playerId);
        
        return tryTransition(PlayerState.IDLE, "Action completed");
    }
    
    // === TICK СИСТЕМА ===
    
    
    /**
     * Проверяет и исправляет некорректные состояния
     */
    private void validateAndFixState() {
        long timeInState = getTimeInCurrentState();
        
        // Если игрок застрял в состоянии слишком долго - принудительный сброс
        if (timeInState > 30000) { // 30 секунд
            LOGGER.warn("Player {} stuck in state {} for {}ms, forcing reset", 
                playerId, currentState, timeInState);
            
            // Очищаем все ограничения движения при принудительном сбросе
            PlayerMovementController.removeMovementRestriction(playerId);
            recoverySystem.clearRecoveryPeriod(playerId);
            
            forceTransition(PlayerState.IDLE, "Stuck state auto-reset");
            return;
        }
        
        // Проверяем соответствие состояния и подсистем
        switch (currentState) {
            case BLOCKING, PARRYING, DODGING -> {
                if (!defenseSystem.isDefending(playerId)) {
                    LOGGER.debug("Defense state {} but no active defense, transitioning to recovery", currentState);
                    tryTransition(PlayerState.DEFENSIVE_RECOVERY, "Defense system mismatch");
                }
            }
            case MELEE_CHARGING -> {
                if (!attackSystem.isCharging(playerId)) {
                    LOGGER.debug("Melee charging state but no active charge, resetting");
                    forceTransition(PlayerState.IDLE, "Melee system mismatch");
                }
            }
        }
        
        // Проверяем соответствие ограничений движения текущему состоянию
        boolean shouldRestrict = PlayerMovementController.shouldRestrictMovement(currentState);
        boolean isRestricted = !PlayerMovementController.canPlayerMove(playerId);
        
        if (shouldRestrict && !isRestricted) {
            LOGGER.debug("Player {} should be restricted in state {} but isn't - fixing", playerId, currentState);
            PlayerMovementController.syncWithPlayerState(playerId, currentState);
        } else if (!shouldRestrict && isRestricted) {
            LOGGER.debug("Player {} is restricted in state {} but shouldn't be - fixing", playerId, currentState);
            PlayerMovementController.removeMovementRestriction(playerId);
        }
    }
    
    private void checkStateTimeouts() {
        long timeInState = getTimeInCurrentState();
        
        // Автоматические переходы по таймауту
        switch (currentState) {
            case DEFENSIVE_ACTION -> {
                if (timeInState > 500) { // 500ms для выбора защиты
                    forceTransition(PlayerState.IDLE, "Defense action timeout - no defense selected");
                }
            }
            case BLOCKING, PARRYING, DODGING -> {
                // Проверяем через defense system
                defenseSystem.tick(); // Обновляем защитную систему
                if (!defenseSystem.isDefending(playerId)) {
                    tryTransition(PlayerState.DEFENSIVE_RECOVERY, "Defense action completed");
                }
            }
            case MAGIC_COOLDOWN, MELEE_RECOVERY, DEFENSIVE_RECOVERY -> {
                if (timeInState > 3000) { // 3 секунды
                    tryTransition(PlayerState.COOLDOWN, "Recovery timeout");
                }
            }
            case COOLDOWN -> {
                if (timeInState > 1000) { // 1 секунда
                    completeAction();
                }
            }
            case INTERRUPTED -> {
                if (timeInState > 2000) { // 2 секунды
                    completeAction();
                }
            }
        }
    }
    
    // === ГЕТТЕРЫ ===
    
    public PlayerState getCurrentState() { return currentState; }
    public long getStateChangeTime() { return stateChangeTime; }
    public long getTimeInCurrentState() { return System.currentTimeMillis() - stateChangeTime; }
    public String getCurrentAction() { return currentAction; }
    public UUID getPlayerId() { return playerId; }
    public boolean isActive() { return currentState.isActive(); }
    public boolean canStartNewAction() { return currentState == PlayerState.IDLE; }
    
    public Player getPlayerInstance() { return playerInstance; }
    public ActionResolver getActionResolver() { return actionResolver; }
}