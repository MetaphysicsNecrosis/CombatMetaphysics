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
    
    // Интегрированные системы
    private final ResourceManager resourceManager;
    private final ActionResolver actionResolver;
    private final DirectionalAttackSystem attackSystem;
    private final DefensiveActionsManager defenseSystem;
    private final InterruptionSystem interruptionSystem;
    
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
            
            LOGGER.debug("Player {} state transition: {} -> {} (action: {}, type: {})", 
                playerId, oldState, newState, action, actionType);
            
            // Уведомляем систему прерываний об успешном переходе
            if (oldState == PlayerState.INTERRUPTED) {
                interruptionSystem.clearInterruption(playerId);
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
        
        LOGGER.warn("Forced state transition for player {}: {} -> {} (reason: {})", 
            playerId, oldState, newState, reason);
        
        // Очищаем действие в ActionResolver
        actionResolver.clearAction(playerId);
    }
    
    /**
     * Прерывание через InterruptionSystem
     */
    public boolean interrupt(InterruptionSystem.InterruptionType type, String reason) {
        InterruptionSystem.InterruptionRequest request = 
            new InterruptionSystem.InterruptionRequest(playerId, null, type, reason);
        
        InterruptionSystem.InterruptionResult result = interruptionSystem.processInterruption(request);
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
            return attackSystem.executeAttack(playerId);
        }
        return new DirectionalAttackSystem.AttackResult(false, "Invalid state for melee attack", 0, 0, false);
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
            return defenseSystem.activateDefense(playerId, type);
        }
        return false;
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
     * Обновляет state machine и все подсистемы
     */
    public void tick() {
        // Обновляем ресурсы
        resourceManager.tick();
        
        // Обновляем защитную систему
        defenseSystem.tick();
        
        // Очищаем устаревшие данные
        attackSystem.cleanupExpiredAttacks(30000); // 30 секунд
        interruptionSystem.cleanupExpiredInterruptions(60000); // 60 секунд
        actionResolver.cleanupExpiredActions(30000); // 30 секунд
        
        // Проверяем автопереходы состояний по таймауту
        checkStateTimeouts();
        
        // Принудительно проверяем состояние каждые 100 тиков (5 секунд)
        if (System.currentTimeMillis() % 5000 < 50) {
            validateAndFixState();
        }
    }
    
    /**
     * Проверяет и исправляет некорректные состояния
     */
    private void validateAndFixState() {
        long timeInState = getTimeInCurrentState();
        
        // Если игрок застрял в состоянии слишком долго - принудительный сброс
        if (timeInState > 30000) { // 30 секунд
            LOGGER.warn("Player {} stuck in state {} for {}ms, forcing reset", 
                playerId, currentState, timeInState);
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
    
    // Доступ к подсистемам
    public ResourceManager getResourceManager() { return resourceManager; }
    public ActionResolver getActionResolver() { return actionResolver; }
    public DirectionalAttackSystem getAttackSystem() { return attackSystem; }
    public DefensiveActionsManager getDefenseSystem() { return defenseSystem; }
    public InterruptionSystem getInterruptionSystem() { return interruptionSystem; }
}