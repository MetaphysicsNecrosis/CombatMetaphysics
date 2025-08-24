package com.example.examplemod.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Система разрешения конфликтов действий и приоритетов прерываний
 * Согласно CLAUDE.md: Physical Hit > Magical Hit > Mass AoE
 * + алгоритмы выполнения для каждого состояния
 */
public class ActionResolver {
    
    public enum ActionType {
        // Базовые действия
        MAGIC_CAST(10),
        MELEE_ATTACK(20),
        DEFENSIVE_ACTION(5),
        
        // Прерывающие действия (чем больше значение, тем выше приоритет)
        MASS_AOE_HIT(100),
        MAGICAL_HIT(200), 
        PHYSICAL_HIT(300);
        
        private final int priority;
        
        ActionType(int priority) {
            this.priority = priority;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public boolean canInterrupt(ActionType other) {
            return this.priority > other.priority;
        }
    }
    
    public static class ActionRequest {
        private final UUID playerId;
        private final ActionType actionType;
        private final PlayerState targetState;
        private final long timestamp;
        private final String reason;
        
        public ActionRequest(UUID playerId, ActionType actionType, PlayerState targetState, String reason) {
            this.playerId = playerId;
            this.actionType = actionType;
            this.targetState = targetState;
            this.timestamp = System.currentTimeMillis();
            this.reason = reason;
        }
        
        public UUID getPlayerId() { return playerId; }
        public ActionType getActionType() { return actionType; }
        public PlayerState getTargetState() { return targetState; }
        public long getTimestamp() { return timestamp; }
        public String getReason() { return reason; }
    }
    
    public static class ResolutionResult {
        private final boolean allowed;
        private final String reason;
        private final ActionType winningAction;
        private final ActionType losingAction;
        
        public ResolutionResult(boolean allowed, String reason, ActionType winningAction, ActionType losingAction) {
            this.allowed = allowed;
            this.reason = reason;
            this.winningAction = winningAction;
            this.losingAction = losingAction;
        }
        
        public boolean isAllowed() { return allowed; }
        public String getReason() { return reason; }
        public ActionType getWinningAction() { return winningAction; }
        public ActionType getLosingAction() { return losingAction; }
    }
    
    private final Map<UUID, ActionRequest> currentActions = new ConcurrentHashMap<>();
    
    /**
     * Проверяет возможность выполнения действия с учетом приоритетов
     */
    public ResolutionResult resolveAction(ActionRequest newRequest, PlayerState currentState) {
        UUID playerId = newRequest.getPlayerId();
        ActionRequest currentAction = currentActions.get(playerId);
        
        // Если нет текущего действия и состояние позволяет переход
        if (currentAction == null) {
            if (currentState.canTransitionTo(newRequest.getTargetState())) {
                currentActions.put(playerId, newRequest);
                return new ResolutionResult(true, "Action allowed", newRequest.getActionType(), null);
            } else {
                return new ResolutionResult(false, 
                    "Invalid state transition from " + currentState + " to " + newRequest.getTargetState(),
                    null, newRequest.getActionType());
            }
        }
        
        // Есть текущее действие - проверяем приоритеты
        ActionType currentActionType = currentAction.getActionType();
        ActionType newActionType = newRequest.getActionType();
        
        // Проверяем, может ли новое действие прервать текущее
        if (newActionType.canInterrupt(currentActionType)) {
            // Дополнительная проверка - можно ли прервать текущее состояние
            if (currentState.canBeInterrupted()) {
                currentActions.put(playerId, newRequest);
                return new ResolutionResult(true, 
                    String.format("Action %s interrupted by higher priority %s", 
                        currentActionType, newActionType),
                    newActionType, currentActionType);
            } else {
                return new ResolutionResult(false,
                    "Current state " + currentState + " cannot be interrupted",
                    currentActionType, newActionType);
            }
        }
        
        // Новое действие не может прервать текущее
        return new ResolutionResult(false,
            String.format("Action %s has lower priority than current %s", 
                newActionType, currentActionType),
            currentActionType, newActionType);
    }
    
    /**
     * Очищает текущее действие игрока (когда действие завершено)
     */
    public void clearAction(UUID playerId) {
        currentActions.remove(playerId);
    }
    
    /**
     * Получает текущее действие игрока
     */
    public ActionRequest getCurrentAction(UUID playerId) {
        return currentActions.get(playerId);
    }
    
    /**
     * Проверяет, выполняет ли игрок действие определенного типа
     */
    public boolean isPerformingAction(UUID playerId, ActionType actionType) {
        ActionRequest current = currentActions.get(playerId);
        return current != null && current.getActionType() == actionType;
    }
    
    /**
     * Получает количество активных действий
     */
    public int getActiveActionsCount() {
        return currentActions.size();
    }
    
    /**
     * Очищает все устаревшие действия (старше указанного времени)
     */
    public void cleanupExpiredActions(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        currentActions.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getTimestamp() > maxAgeMs
        );
    }
    
    /**
     * Создает запрос на действие магии
     */
    public static ActionRequest createMagicRequest(UUID playerId, PlayerState targetState, String reason) {
        return new ActionRequest(playerId, ActionType.MAGIC_CAST, targetState, reason);
    }
    
    /**
     * Создает запрос на атаку ближнего боя
     */
    public static ActionRequest createMeleeRequest(UUID playerId, PlayerState targetState, String reason) {
        return new ActionRequest(playerId, ActionType.MELEE_ATTACK, targetState, reason);
    }
    
    /**
     * Создает запрос на защитное действие
     */
    public static ActionRequest createDefensiveRequest(UUID playerId, PlayerState targetState, String reason) {
        return new ActionRequest(playerId, ActionType.DEFENSIVE_ACTION, targetState, reason);
    }
    
    /**
     * Создает запрос на физическое прерывание (наивысший приоритет)
     */
    public static ActionRequest createPhysicalHitRequest(UUID playerId, String reason) {
        return new ActionRequest(playerId, ActionType.PHYSICAL_HIT, PlayerState.INTERRUPTED, reason);
    }
    
    /**
     * Создает запрос на магическое прерывание
     */
    public static ActionRequest createMagicalHitRequest(UUID playerId, String reason) {
        return new ActionRequest(playerId, ActionType.MAGICAL_HIT, PlayerState.INTERRUPTED, reason);
    }
    
    /**
     * Создает запрос на массовое прерывание (наименьший приоритет среди прерываний)
     */
    public static ActionRequest createMassAoERequest(UUID playerId, String reason) {
        return new ActionRequest(playerId, ActionType.MASS_AOE_HIT, PlayerState.INTERRUPTED, reason);
    }
    
    // ============== АЛГОРИТМЫ ВЫПОЛНЕНИЯ ДЕЙСТВИЙ ==============
    
    /**
     * Интерфейс для выполнения действий в конкретном состоянии
     */
    @FunctionalInterface
    public interface ActionExecutor {
        ActionExecutionResult execute(UUID playerId, PlayerStateMachine stateMachine, Object... params);
    }
    
    /**
     * Результат выполнения действия
     */
    public static class ActionExecutionResult {
        private final boolean success;
        private final String message;
        private final PlayerState nextState;
        private final float resourceCost;
        private final long duration; // в миллисекундах
        
        public ActionExecutionResult(boolean success, String message, PlayerState nextState, 
                                   float resourceCost, long duration) {
            this.success = success;
            this.message = message;
            this.nextState = nextState;
            this.resourceCost = resourceCost;
            this.duration = duration;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public PlayerState getNextState() { return nextState; }
        public float getResourceCost() { return resourceCost; }
        public long getDuration() { return duration; }
        
        public static ActionExecutionResult success(String message, PlayerState nextState, 
                                                  float resourceCost, long duration) {
            return new ActionExecutionResult(true, message, nextState, resourceCost, duration);
        }
        
        public static ActionExecutionResult failure(String message) {
            return new ActionExecutionResult(false, message, null, 0, 0);
        }
    }
    
    // Алгоритмы для каждого типа состояния
    private static final Map<PlayerState, ActionExecutor> STATE_EXECUTORS = new ConcurrentHashMap<>();
    
    static {
        // Магические состояния
        STATE_EXECUTORS.put(PlayerState.MAGIC_PREPARING, ActionResolver::executeMagicPreparing);
        STATE_EXECUTORS.put(PlayerState.MAGIC_CASTING, ActionResolver::executeMagicCasting);
        STATE_EXECUTORS.put(PlayerState.QTE_TRANSITION, ActionResolver::executeQteAction);
        STATE_EXECUTORS.put(PlayerState.MAGIC_COOLDOWN, ActionResolver::executeMagicCooldown);
        
        // Состояния ближнего боя
        STATE_EXECUTORS.put(PlayerState.MELEE_PREPARING, ActionResolver::executeMeleePreparing);
        STATE_EXECUTORS.put(PlayerState.MELEE_CHARGING, ActionResolver::executeMeleeCharging);
        STATE_EXECUTORS.put(PlayerState.MELEE_ATTACKING, ActionResolver::executeMeleeAttacking);
        STATE_EXECUTORS.put(PlayerState.MELEE_RECOVERY, ActionResolver::executeMeleeRecovery);
        
        // Защитные состояния
        STATE_EXECUTORS.put(PlayerState.BLOCKING, ActionResolver::executeBlocking);
        STATE_EXECUTORS.put(PlayerState.PARRYING, ActionResolver::executeParrying);
        STATE_EXECUTORS.put(PlayerState.DODGING, ActionResolver::executeDodging);
        STATE_EXECUTORS.put(PlayerState.DEFENSIVE_RECOVERY, ActionResolver::executeDefensiveRecovery);
        
        // Базовые состояния
        STATE_EXECUTORS.put(PlayerState.IDLE, ActionResolver::executeIdleState);
        STATE_EXECUTORS.put(PlayerState.INTERRUPTED, ActionResolver::executeInterrupted);
    }
    
    /**
     * Выполняет действие в текущем состоянии игрока
     */
    public ActionExecutionResult executeCurrentStateAction(UUID playerId, PlayerStateMachine stateMachine, 
                                                          Object... params) {
        PlayerState currentState = stateMachine.getCurrentState();
        ActionExecutor executor = STATE_EXECUTORS.get(currentState);
        
        if (executor == null) {
            return ActionExecutionResult.failure("No executor found for state: " + currentState);
        }
        
        return executor.execute(playerId, stateMachine, params);
    }
    
    // ============== РЕАЛИЗАЦИИ АЛГОРИТМОВ ==============
    
    private static ActionExecutionResult executeMagicPreparing(UUID playerId, PlayerStateMachine stateMachine, 
                                                             Object... params) {
        // Проверяем, достаточно ли маны для заклинания
        float requiredMana = params.length > 0 ? (Float) params[0] : 25.0f;
        
        if (!stateMachine.getResourceManager().hasMana(playerId, requiredMana)) {
            return ActionExecutionResult.failure("Недостаточно маны для заклинания");
        }
        
        // Резервируем ману (двухслойная система из CLAUDE.md)
        boolean reserved = stateMachine.getResourceManager().reserveMana(playerId, requiredMana);
        if (!reserved) {
            return ActionExecutionResult.failure("Не удалось зарезервировать ману");
        }
        
        // Переходим к произнесению заклинания
        return ActionExecutionResult.success(
            "Мана зарезервирована, начинаем произнесение", 
            PlayerState.MAGIC_CASTING, 
            requiredMana, 
            1500 // 1.5 секунды на произнесение
        );
    }
    
    private static ActionExecutionResult executeMagicCasting(UUID playerId, PlayerStateMachine stateMachine, 
                                                           Object... params) {
        // В процессе произнесения заклинания игрок уязвим для прерываний
        String spellType = params.length > 0 ? (String) params[0] : "basic_spell";
        
        // Check if player was interrupted (Gothic system)
        if (stateMachine.getCurrentState() == PlayerState.INTERRUPTED) {
            return ActionExecutionResult.failure("Произнесение заклинания прервано");
        }
        
        // Успешное произнесение - переходим к QTE для комбо или к кулдауну
        boolean hasComboOption = params.length > 1 && (Boolean) params[1];
        
        if (hasComboOption) {
            return ActionExecutionResult.success(
                "Заклинание произнесено, начинается QTE для комбо",
                PlayerState.QTE_TRANSITION,
                0, // дополнительных ресурсов не тратим
                800 // 0.8 секунды на QTE
            );
        } else {
            return ActionExecutionResult.success(
                "Заклинание произнесено, переход к кулдауну",
                PlayerState.MAGIC_COOLDOWN,
                0,
                2000 // 2 секунды кулдауна
            );
        }
    }
    
    private static ActionExecutionResult executeQteAction(UUID playerId, PlayerStateMachine stateMachine, 
                                                        Object... params) {
        // QTE для переходов между заклинаниями в комбо
        long inputTimestamp = params.length > 0 ? (Long) params[0] : System.currentTimeMillis();
        
        // Get QTE timing (simplified for Gothic system)
        long qteStartTime = stateMachine.getStateChangeTime();
        long timingDiff = Math.abs(inputTimestamp - qteStartTime);
        
        // Оценка качества QTE
        float qteScore = calculateQteScore(timingDiff);
        
        if (qteScore >= 0.5f) {
            String nextSpell = params.length > 1 ? (String) params[1] : "combo_spell";
            
            // Успешное QTE - продолжаем комбо
            return ActionExecutionResult.success(
                String.format("QTE успешно! Качество: %.0f%%, следующее заклинание: %s", 
                    qteScore * 100, nextSpell),
                PlayerState.MAGIC_PREPARING, // возвращаемся к подготовке следующего заклинания
                qteScore >= 0.9f ? 15.0f : 20.0f, // бонус к эффективности при отличном QTE
                500 // быстрый переход к следующему заклинанию
            );
        } else {
            return ActionExecutionResult.success(
                String.format("QTE провалено (качество: %.0f%%), комбо прервано", qteScore * 100),
                PlayerState.MAGIC_COOLDOWN,
                0,
                3000 // увеличенный кулдаун при провале QTE
            );
        }
    }
    
    private static ActionExecutionResult executeMeleePreparing(UUID playerId, PlayerStateMachine stateMachine, 
                                                             Object... params) {
        // Gothic-style подготовка атаки с выбором направления
        DirectionalAttackSystem.AttackDirection direction = params.length > 0 ? 
            (DirectionalAttackSystem.AttackDirection) params[0] : 
            DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
        
        // Проверяем выносливость
        float staminaCost = getAttackStaminaCost(direction);
        if (!stateMachine.getResourceManager().hasStamina(playerId, staminaCost)) {
            return ActionExecutionResult.failure("Недостаточно выносливости для атаки");
        }
        
        // Начинаем зарядку атаки
        return ActionExecutionResult.success(
            "Подготовка атаки " + direction.name() + ", удерживайте для зарядки",
            PlayerState.MELEE_CHARGING,
            staminaCost * 0.3f, // часть выносливости тратится на подготовку
            500 // минимальное время подготовки
        );
    }
    
    private static ActionExecutionResult executeMeleeCharging(UUID playerId, PlayerStateMachine stateMachine, 
                                                            Object... params) {
        // Зарядка атаки увеличивает урон но делает игрока уязвимым
        long chargeTime = params.length > 0 ? (Long) params[0] : 1000L;
        DirectionalAttackSystem.AttackDirection direction = params.length > 1 ? 
            (DirectionalAttackSystem.AttackDirection) params[1] : 
            DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
        
        // Расчет мощности в зависимости от времени зарядки
        float chargeMultiplier = Math.min(2.0f, 1.0f + (chargeTime / 2000.0f)); // максимум x2 за 2 секунды
        
        return ActionExecutionResult.success(
            String.format("Атака заряжена на %.0f%%, готова к выполнению", chargeMultiplier * 100),
            PlayerState.MELEE_ATTACKING,
            0, // дополнительная выносливость будет потрачена при ударе
            300 + (chargeTime / 10) // время выполнения атаки зависит от зарядки
        );
    }
    
    private static ActionExecutionResult executeMeleeAttacking(UUID playerId, PlayerStateMachine stateMachine, 
                                                             Object... params) {
        // Выполнение направленной атаки
        DirectionalAttackSystem.AttackDirection direction = params.length > 0 ? 
            (DirectionalAttackSystem.AttackDirection) params[0] : 
            DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
        float chargeMultiplier = params.length > 1 ? (Float) params[1] : 1.0f;
        
        // Финальная трата выносливости
        float finalStaminaCost = getAttackStaminaCost(direction) * 0.7f; // оставшиеся 70%
        
        // Расчет урона
        float baseDamage = getAttackBaseDamage(direction);
        float finalDamage = baseDamage * chargeMultiplier;
        
        return ActionExecutionResult.success(
            String.format("Выполнена %s атака, урон: %.1f", direction.name(), finalDamage),
            PlayerState.MELEE_RECOVERY,
            finalStaminaCost,
            200 // короткое время восстановления
        );
    }
    
    private static ActionExecutionResult executeParrying(UUID playerId, PlayerStateMachine stateMachine, 
                                                        Object... params) {
        // Парирование с timing window (Gothic-style)
        long parryTimestamp = params.length > 0 ? (Long) params[0] : System.currentTimeMillis();
        long incomingAttackTime = params.length > 1 ? (Long) params[1] : parryTimestamp;
        
        long timingDiff = Math.abs(parryTimestamp - incomingAttackTime);
        boolean perfectParry = timingDiff <= 100; // 100ms для perfect parry
        
        if (perfectParry) {
            return ActionExecutionResult.success(
                "Идеальное парирование! Доступна контратака",
                PlayerState.IDLE, // сразу готов к контратаке
                5.0f, // минимальная трата выносливости
                0 // немедленное восстановление
            );
        } else if (timingDiff <= 300) {
            return ActionExecutionResult.success(
                "Успешное парирование",
                PlayerState.DEFENSIVE_RECOVERY,
                10.0f,
                500 // время восстановления
            );
        } else {
            return ActionExecutionResult.failure("Парирование не удалось - плохой тайминг");
        }
    }
    
    // Вспомогательные методы
    private static float calculateQteScore(long timingDiff) {
        if (timingDiff <= 50) return 1.0f;   // 90-100%
        if (timingDiff <= 150) return 0.8f;  // 70-89%
        if (timingDiff <= 300) return 0.6f;  // 50-69%
        return 0.3f; // <50%
    }
    
    private static float getAttackStaminaCost(DirectionalAttackSystem.AttackDirection direction) {
        return switch (direction) {
            case LEFT_ATTACK -> 15.0f;   // быстрая атака
            case RIGHT_ATTACK -> 20.0f;  // средняя атака
            case TOP_ATTACK -> 30.0f;    // мощная атака
            case THRUST_ATTACK -> 18.0f; // колющая атака
        };
    }
    
    private static float getAttackBaseDamage(DirectionalAttackSystem.AttackDirection direction) {
        return switch (direction) {
            case LEFT_ATTACK -> 8.0f;   // низкий урон, но быстро
            case RIGHT_ATTACK -> 12.0f; // средний урон
            case TOP_ATTACK -> 18.0f;   // высокий урон, но медленно
            case THRUST_ATTACK -> 10.0f; // пробивающий урон
        };
    }
    
    // Остальные методы-заглушки для полноты
    private static ActionExecutionResult executeMagicCooldown(UUID playerId, PlayerStateMachine stateMachine, Object... params) {
        return ActionExecutionResult.success("Восстановление после магии", PlayerState.IDLE, 0, 0);
    }
    
    private static ActionExecutionResult executeMeleeRecovery(UUID playerId, PlayerStateMachine stateMachine, Object... params) {
        return ActionExecutionResult.success("Восстановление после атаки", PlayerState.IDLE, 0, 0);
    }
    
    private static ActionExecutionResult executeBlocking(UUID playerId, PlayerStateMachine stateMachine, Object... params) {
        return ActionExecutionResult.success("Блокирование активно", PlayerState.BLOCKING, 2.0f, 0);
    }
    
    private static ActionExecutionResult executeDodging(UUID playerId, PlayerStateMachine stateMachine, Object... params) {
        return ActionExecutionResult.success("Уклонение выполнено", PlayerState.IDLE, 25.0f, 300);
    }
    
    private static ActionExecutionResult executeDefensiveRecovery(UUID playerId, PlayerStateMachine stateMachine, Object... params) {
        return ActionExecutionResult.success("Восстановление после защиты", PlayerState.IDLE, 0, 0);
    }
    
    private static ActionExecutionResult executeIdleState(UUID playerId, PlayerStateMachine stateMachine, Object... params) {
        return ActionExecutionResult.success("Готов к действию", PlayerState.IDLE, 0, 0);
    }
    
    private static ActionExecutionResult executeInterrupted(UUID playerId, PlayerStateMachine stateMachine, Object... params) {
        return ActionExecutionResult.success("Восстановление после прерывания", PlayerState.IDLE, 0, 1000);
    }
}