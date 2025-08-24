package com.example.examplemod.core;

import java.util.*;

/**
 * Система прерываний с приоритетами
 * Согласно CLAUDE.md: Physical Hit > Magical Hit > Mass AoE
 * Управляет конфликтами действий и принудительными прерываниями
 */
public class InterruptionSystem {
    
    public enum InterruptionType {
        // УРОВЕНЬ 1: Непреодолимые (Gothic-стиль из CLAUDE.md)
        ENVIRONMENTAL_HAZARD(1000, "Environmental hazard (lava, fall damage)"),
        SCRIPT_FORCED(1000, "Script-forced event"),
        ADMIN_COMMAND(1000, "Admin server command"),
        
        // УРОВЕНЬ 2: Тяжелые физические воздействия (Gothic)
        HEAVY_PHYSICAL_HIT(300, "Heavy weapon/charged attack"),
        STUNNING_BLOW(280, "Critical hit with stun"),
        KNOCKDOWN_EFFECT(260, "Knockdown attack"),
        
        // УРОВЕНЬ 3: Магические воздействия
        DISPEL_EFFECT(200, "Magical dispel"),
        SILENCE_EFFECT(180, "Magic prohibition"),
        MAGICAL_DISRUPTION(160, "Magical interference"),
        
        // УРОВЕНЬ 4: Массовые эффекты
        AOE_BLAST(100, "Explosive spell"),
        FEAR_EFFECT(80, "Fear magic"),
        CROWD_CONTROL(60, "Mass control"),
        
        // Системные прерывания (низкий приоритет)
        RESOURCE_DEPLETION(50, "Insufficient resources"),
        STATE_TIMEOUT(25, "Action timeout"),
        EXTERNAL_FORCE(10, "External interruption");
        
        private final int priority;
        private final String description;
        
        InterruptionType(int priority, String description) {
            this.priority = priority;
            this.description = description;
        }
        
        public int getPriority() { return priority; }
        public String getDescription() { return description; }
        
        public boolean canOverride(InterruptionType other) {
            return this.priority > other.priority;
        }
        
        public int getLevel() {
            if (priority >= 1000) return 1; // Непреодолимые
            if (priority >= 260) return 2;  // Тяжелые физические
            if (priority >= 160) return 3;  // Магические
            if (priority >= 60) return 4;   // Массовые
            return 5; // Системные
        }
        
        public boolean isPhysical() {
            return this == HEAVY_PHYSICAL_HIT || this == STUNNING_BLOW || this == KNOCKDOWN_EFFECT;
        }
        
        public boolean isMagical() {
            return this == DISPEL_EFFECT || this == SILENCE_EFFECT || this == MAGICAL_DISRUPTION;
        }
        
        public boolean isMassEffect() {
            return this == AOE_BLAST || this == FEAR_EFFECT || this == CROWD_CONTROL;
        }
    }
    
    public static class InterruptionRequest {
        private final UUID targetPlayerId;
        private final UUID sourcePlayerId; // null для системных прерываний
        private final InterruptionType type;
        private final long timestamp;
        private final String reason;
        private final Map<String, Object> metadata;
        
        public InterruptionRequest(UUID targetPlayerId, UUID sourcePlayerId, InterruptionType type, String reason) {
            this.targetPlayerId = targetPlayerId;
            this.sourcePlayerId = sourcePlayerId;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.reason = reason;
            this.metadata = new HashMap<>();
        }
        
        public UUID getTargetPlayerId() { return targetPlayerId; }
        public UUID getSourcePlayerId() { return sourcePlayerId; }
        public InterruptionType getType() { return type; }
        public long getTimestamp() { return timestamp; }
        public String getReason() { return reason; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        public InterruptionRequest withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public boolean isSystemInterruption() {
            return sourcePlayerId == null;
        }
    }
    
    public static class InterruptionResult {
        private final boolean success;
        private final InterruptionType appliedType;
        private final InterruptionType rejectedType;
        private final String message;
        private final PlayerState resultingState;
        
        public InterruptionResult(boolean success, InterruptionType appliedType, InterruptionType rejectedType, 
                                String message, PlayerState resultingState) {
            this.success = success;
            this.appliedType = appliedType;
            this.rejectedType = rejectedType;
            this.message = message;
            this.resultingState = resultingState;
        }
        
        public boolean isSuccess() { return success; }
        public InterruptionType getAppliedType() { return appliedType; }
        public InterruptionType getRejectedType() { return rejectedType; }
        public String getMessage() { return message; }
        public PlayerState getResultingState() { return resultingState; }
    }
    
    // Активные прерывания по игрокам
    private final Map<UUID, InterruptionRequest> activeInterruptions = new HashMap<>();
    
    // Очередь приоритетов для конфликтующих прерываний
    private final Map<UUID, PriorityQueue<InterruptionRequest>> interruptionQueue = new HashMap<>();
    
    private final PlayerStateMachine stateMachine;
    
    public InterruptionSystem(PlayerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }
    
    /**
     * Попытка прервать действие игрока
     */
    public InterruptionResult processInterruption(InterruptionRequest request) {
        UUID targetId = request.getTargetPlayerId();
        
        // Получаем state machine для конкретного игрока
        PlayerStateMachine targetStateMachine = PlayerStateMachine.getInstance(targetId);
        if (targetStateMachine == null) {
            return new InterruptionResult(false, null, request.getType(),
                "No state machine found for player", PlayerState.IDLE);
        }
        
        PlayerState currentState = targetStateMachine.getCurrentState();
        
        // Проверяем, можно ли прервать текущее состояние
        if (!currentState.canBeInterrupted()) {
            return new InterruptionResult(false, null, request.getType(),
                "Current state " + currentState + " cannot be interrupted", currentState);
        }
        
        // Проверяем активные прерывания
        InterruptionRequest activeInterruption = activeInterruptions.get(targetId);
        
        if (activeInterruption == null) {
            // Нет активного прерывания - применяем новое
            return applyInterruption(request, currentState);
        }
        
        // Есть активное прерывание - сравниваем приоритеты
        InterruptionType activeType = activeInterruption.getType();
        InterruptionType newType = request.getType();
        
        if (newType.canOverride(activeType)) {
            // Новое прерывание имеет больший приоритет
            return applyInterruption(request, currentState);
        } else {
            // Активное прерывание имеет больший приоритет - добавляем в очередь
            addToQueue(request);
            return new InterruptionResult(false, activeType, newType,
                "Lower priority interruption queued", currentState);
        }
    }
    
    private InterruptionResult applyInterruption(InterruptionRequest request, PlayerState currentState) {
        UUID targetId = request.getTargetPlayerId();
        
        // Получаем state machine для конкретного игрока
        PlayerStateMachine targetStateMachine = PlayerStateMachine.getInstance(targetId);
        if (targetStateMachine == null) {
            return new InterruptionResult(false, null, request.getType(),
                "No state machine found for player", currentState);
        }
        
        // Сохраняем активное прерывание
        activeInterruptions.put(targetId, request);
        
        // Переводим игрока в состояние прерывания
        PlayerStateMachine.StateTransitionResult result = targetStateMachine.transitionTo(PlayerState.INTERRUPTED, 
            "Interrupted by " + request.getType().getDescription(), 0);
        boolean stateChanged = result.isSuccess();
        
        if (stateChanged) {
            return new InterruptionResult(true, request.getType(), null,
                request.getType().getDescription(), PlayerState.INTERRUPTED);
        } else {
            // Не удалось изменить состояние
            activeInterruptions.remove(targetId);
            return new InterruptionResult(false, null, request.getType(),
                "Failed to change state to INTERRUPTED", currentState);
        }
    }
    
    private void addToQueue(InterruptionRequest request) {
        UUID targetId = request.getTargetPlayerId();
        
        interruptionQueue.computeIfAbsent(targetId, k -> 
            new PriorityQueue<>((a, b) -> Integer.compare(b.getType().getPriority(), a.getType().getPriority()))
        ).offer(request);
    }
    
    /**
     * Очищает прерывание игрока (когда игрок восстанавливается)
     */
    public void clearInterruption(UUID playerId) {
        activeInterruptions.remove(playerId);
        
        // Проверяем очередь на наличие ожидающих прерываний
        PriorityQueue<InterruptionRequest> queue = interruptionQueue.get(playerId);
        if (queue != null && !queue.isEmpty()) {
            InterruptionRequest nextRequest = queue.poll();
            processInterruption(nextRequest);
        }
    }
    
    /**
     * Получает активное прерывание игрока
     */
    public InterruptionRequest getActiveInterruption(UUID playerId) {
        return activeInterruptions.get(playerId);
    }
    
    /**
     * Проверяет, прерван ли игрок
     */
    public boolean isInterrupted(UUID playerId) {
        return activeInterruptions.containsKey(playerId);
    }
    
    /**
     * Получает количество ожидающих прерываний для игрока
     */
    public int getQueuedInterruptionsCount(UUID playerId) {
        PriorityQueue<InterruptionRequest> queue = interruptionQueue.get(playerId);
        return queue != null ? queue.size() : 0;
    }
    
    /**
     * Очищает устаревшие прерывания
     */
    public void cleanupExpiredInterruptions(long maxAgeMs) {
        long currentTime = System.currentTimeMillis();
        
        // Очищаем активные прерывания
        activeInterruptions.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getTimestamp() > maxAgeMs
        );
        
        // Очищаем очереди
        interruptionQueue.values().forEach(queue -> 
            queue.removeIf(request -> currentTime - request.getTimestamp() > maxAgeMs)
        );
        
        // Удаляем пустые очереди
        interruptionQueue.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    
    /**
     * Принудительно прерывает всех игроков (для массовых эффектов)
     */
    public Map<UUID, InterruptionResult> massInterruption(InterruptionType type, String reason, 
                                                         Collection<UUID> targetPlayers) {
        Map<UUID, InterruptionResult> results = new HashMap<>();
        
        for (UUID playerId : targetPlayers) {
            InterruptionRequest request = new InterruptionRequest(playerId, null, type, reason);
            InterruptionResult result = processInterruption(request);
            results.put(playerId, result);
        }
        
        return results;
    }
    
    // Фабричные методы для создания различных типов прерываний
    
    public static InterruptionRequest createPhysicalHit(UUID targetId, UUID attackerId, float damage) {
        return new InterruptionRequest(targetId, attackerId, InterruptionType.HEAVY_PHYSICAL_HIT, 
            "Physical attack for " + damage + " damage")
            .withMetadata("damage", damage)
            .withMetadata("attackType", "physical");
    }
    
    public static InterruptionRequest createMagicalHit(UUID targetId, UUID casterId, String spellName) {
        return new InterruptionRequest(targetId, casterId, InterruptionType.MAGICAL_DISRUPTION,
            "Magical attack: " + spellName)
            .withMetadata("spellName", spellName)
            .withMetadata("attackType", "magical");
    }
    
    public static InterruptionRequest createMassAoE(UUID targetId, String effectName) {
        return new InterruptionRequest(targetId, null, InterruptionType.AOE_BLAST,
            "Area effect: " + effectName)
            .withMetadata("effectName", effectName)
            .withMetadata("attackType", "aoe");
    }
    
    public static InterruptionRequest createResourceDepletion(UUID targetId, String resourceType) {
        return new InterruptionRequest(targetId, null, InterruptionType.RESOURCE_DEPLETION,
            "Insufficient " + resourceType)
            .withMetadata("resourceType", resourceType);
    }
    
    public static InterruptionRequest createTimeout(UUID targetId, String actionType) {
        return new InterruptionRequest(targetId, null, InterruptionType.STATE_TIMEOUT,
            actionType + " timeout")
            .withMetadata("actionType", actionType);
    }
    
    // ============== ИНТЕГРАЦИЯ С КОНКРЕТНЫМИ ДЕЙСТВИЯМИ ==============
    
    private DirectionalAttackSystem attackSystem;
    private DefensiveActionsManager defenseManager;
    
    public void setAttackSystem(DirectionalAttackSystem attackSystem) {
        this.attackSystem = attackSystem;
    }
    
    public void setDefenseManager(DefensiveActionsManager defenseManager) {
        this.defenseManager = defenseManager;
    }
    
    /**
     * Проверяет иммунитеты состояния к прерываниям (Gothic-принципы)
     */
    public boolean hasStateImmunity(UUID playerId, InterruptionType interruptionType) {
        PlayerStateMachine targetStateMachine = PlayerStateMachine.getInstance(playerId);
        if (targetStateMachine == null) return false;
        
        PlayerState currentState = targetStateMachine.getCurrentState();
        
        // Защитные состояния имеют иммунитеты согласно CLAUDE.md
        switch (currentState) {
            case BLOCKING:
                return interruptionType == InterruptionType.CROWD_CONTROL || 
                       interruptionType == InterruptionType.FEAR_EFFECT;
                       
            case PARRYING:
                if (defenseManager != null && defenseManager.hasCounterOpportunity(playerId)) {
                    // Perfect parry дает временную защиту
                    return interruptionType.getLevel() >= 3; // Защита от магических и массовых
                }
                return interruptionType == InterruptionType.AOE_BLAST ||
                       interruptionType == InterruptionType.MAGICAL_DISRUPTION;
                       
            case DODGING:
                if (defenseManager != null && defenseManager.hasInvulnerabilityFrames(playerId)) {
                    // i-frames защищают от всего кроме environmental
                    return interruptionType.getLevel() > 1;
                }
                return false;
                
            case QTE_TRANSITION:
                // QTE состояния имеют частичную защиту
                return interruptionType == InterruptionType.CROWD_CONTROL;
                
            case MELEE_ATTACKING:
                if (attackSystem != null) {
                    DirectionalAttackSystem.AttackData attack = attackSystem.getCurrentAttack(playerId);
                    if (attack != null && attack.isExecuting()) {
                        // Быстрые атаки сложнее прервать
                        DirectionalAttackSystem.AttackDirection direction = attack.getDirection();
                        if (direction == DirectionalAttackSystem.AttackDirection.LEFT_ATTACK ||
                            direction == DirectionalAttackSystem.AttackDirection.THRUST_ATTACK) {
                            return interruptionType == InterruptionType.FEAR_EFFECT;
                        }
                    }
                }
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Расширенная обработка прерываний с учетом иммунитетов
     */
    public InterruptionResult processInterruptionWithImmunities(InterruptionRequest request) {
        UUID targetId = request.getTargetPlayerId();
        
        // Проверяем иммунитеты состояния
        if (hasStateImmunity(targetId, request.getType())) {
            return new InterruptionResult(false, null, request.getType(),
                "Target has state immunity to " + request.getType(), 
                PlayerStateMachine.getInstance(targetId).getCurrentState());
        }
        
        // Проверяем защитные действия
        if (defenseManager != null && 
            defenseManager.canBlockInterruption(targetId, request.getType())) {
            return new InterruptionResult(false, null, request.getType(),
                "Interruption blocked by defensive action",
                PlayerStateMachine.getInstance(targetId).getCurrentState());
        }
        
        // Базовая обработка прерывания
        return processInterruption(request);
    }
    
    /**
     * Применяет прерывание от направленной атаки
     */
    public InterruptionResult processAttackInterruption(UUID attackerId, UUID targetId, 
                                                       DirectionalAttackSystem.AttackData attackData) {
        InterruptionType interruptionType;
        
        // Определяем тип прерывания на основе атаки
        if (attackData.isChargedAttack()) {
            interruptionType = InterruptionType.HEAVY_PHYSICAL_HIT;
        } else {
            DirectionalAttackSystem.AttackDirection direction = attackData.getDirection();
            if (direction == DirectionalAttackSystem.AttackDirection.TOP_ATTACK) {
                interruptionType = InterruptionType.STUNNING_BLOW; // Мощные атаки сверху
            } else {
                interruptionType = InterruptionType.HEAVY_PHYSICAL_HIT; // Обычные физические атаки
            }
        }
        
        // Вычисляем шанс прерывания
        float interruptionChance = attackSystem.calculateInterruptionChance(attackData);
        
        // Проверяем, происходит ли прерывание
        if (Math.random() > interruptionChance) {
            return new InterruptionResult(false, null, interruptionType,
                "Attack didn't cause interruption (chance: " + (interruptionChance * 100) + "%)",
                PlayerStateMachine.getInstance(targetId).getCurrentState());
        }
        
        String reason = String.format("Interrupted by %s attack (%.0f%% chance)", 
            attackData.getDirection().name(), interruptionChance * 100);
            
        InterruptionRequest request = new InterruptionRequest(targetId, attackerId, interruptionType, reason)
            .withMetadata("attackDirection", attackData.getDirection())
            .withMetadata("chargedAttack", attackData.isChargedAttack())
            .withMetadata("interruptionChance", interruptionChance);
        
        return processInterruptionWithImmunities(request);
    }
    
    /**
     * Применяет магическое прерывание
     */
    public InterruptionResult processMagicalInterruption(UUID casterId, UUID targetId, 
                                                       String spellName, boolean isDispel) {
        InterruptionType interruptionType = isDispel ? 
            InterruptionType.DISPEL_EFFECT : 
            InterruptionType.MAGICAL_DISRUPTION;
            
        String reason = isDispel ? "Dispelled by " + spellName : "Disrupted by " + spellName;
        
        InterruptionRequest request = new InterruptionRequest(targetId, casterId, interruptionType, reason)
            .withMetadata("spellName", spellName)
            .withMetadata("isDispel", isDispel);
        
        return processInterruptionWithImmunities(request);
    }
    
    /**
     * Обрабатывает массовое прерывание от AoE эффектов
     */
    public Map<UUID, InterruptionResult> processAoEInterruption(UUID sourceId, 
                                                              Collection<UUID> targets,
                                                              String effectName,
                                                              InterruptionType aoeType) {
        Map<UUID, InterruptionResult> results = new HashMap<>();
        
        for (UUID targetId : targets) {
            // Проверяем дистанцию и препятствия (можно добавить позже)
            
            InterruptionRequest request = new InterruptionRequest(targetId, sourceId, aoeType, 
                "Area effect: " + effectName)
                .withMetadata("effectName", effectName)
                .withMetadata("sourceId", sourceId);
                
            InterruptionResult result = processInterruptionWithImmunities(request);
            results.put(targetId, result);
        }
        
        return results;
    }
    
    /**
     * Специальная обработка для страха и контроля сознания
     */
    public InterruptionResult processFearEffect(UUID sourceId, UUID targetId, long durationMs) {
        // Страх имеет особую логику - может быть заблокирован высокой выносливостью
        PlayerStateMachine targetStateMachine = PlayerStateMachine.getInstance(targetId);
        if (targetStateMachine != null) {
            // Проверяем ресурсы цели
            float currentStamina = targetStateMachine.getResourceManager().getCurrentStamina(targetId);
            if (currentStamina > 80.0f) { // Высокая выносливость дает сопротивление страху
                return new InterruptionResult(false, null, InterruptionType.FEAR_EFFECT,
                    "Target resisted fear due to high stamina",
                    targetStateMachine.getCurrentState());
            }
        }
        
        InterruptionRequest request = new InterruptionRequest(targetId, sourceId, 
            InterruptionType.FEAR_EFFECT, "Fear effect")
            .withMetadata("duration", durationMs)
            .withMetadata("canResist", true);
            
        return processInterruptionWithImmunities(request);
    }
    
    /**
     * Принудительно очищает все действия игрока при прерывании
     */
    private void forceCleanupPlayerActions(UUID playerId, InterruptionType type) {
        // Прерываем атаки ближнего боя
        if (attackSystem != null) {
            attackSystem.forceInterruptAttack(playerId, "Interrupted by " + type.getDescription());
        }
        
        // Отменяем защитные действия (кроме случаев, когда они дают иммунитет)
        if (defenseManager != null && !defenseManager.canBlockInterruption(playerId, type)) {
            defenseManager.deactivateDefense(playerId);
        }
        
        // Дополнительная очистка может быть добавлена для других систем
    }
    
    /**
     * Расширенное применение прерывания с очисткой действий
     */
    private InterruptionResult applyInterruptionWithCleanup(InterruptionRequest request, PlayerState currentState) {
        UUID targetId = request.getTargetPlayerId();
        
        // Принудительно очищаем действия перед переходом состояния
        forceCleanupPlayerActions(targetId, request.getType());
        
        // Получаем state machine для конкретного игрока
        PlayerStateMachine targetStateMachine = PlayerStateMachine.getInstance(targetId);
        if (targetStateMachine == null) {
            return new InterruptionResult(false, null, request.getType(),
                "No state machine found for player", currentState);
        }
        
        // Сохраняем активное прерывание
        activeInterruptions.put(targetId, request);
        
        // Определяем состояние прерывания в зависимости от типа
        PlayerState interruptedState = switch (request.getType()) {
            case FEAR_EFFECT -> PlayerState.INTERRUPTED; // Можно добавить FEARED состояние
            case SILENCE_EFFECT -> PlayerState.INTERRUPTED; // Можно добавить SILENCED состояние  
            case STUNNING_BLOW -> PlayerState.INTERRUPTED; // Можно добавить STUNNED состояние
            default -> PlayerState.INTERRUPTED;
        };
        
        // Переводим игрока в состояние прерывания
        PlayerStateMachine.StateTransitionResult result = targetStateMachine.transitionTo(interruptedState, 
            "Interrupted by " + request.getType().getDescription(), 0);
        boolean stateChanged = result.isSuccess();
        
        if (stateChanged) {
            return new InterruptionResult(true, request.getType(), null,
                request.getType().getDescription(), interruptedState);
        } else {
            // Не удалось изменить состояние
            activeInterruptions.remove(targetId);
            return new InterruptionResult(false, null, request.getType(),
                "Failed to change state to " + interruptedState, currentState);
        }
    }
    
    // Фабричные методы для новых типов прерываний
    
    public static InterruptionRequest createEnvironmentalHazard(UUID targetId, String hazardType) {
        return new InterruptionRequest(targetId, null, InterruptionType.ENVIRONMENTAL_HAZARD,
            "Environmental hazard: " + hazardType)
            .withMetadata("hazardType", hazardType);
    }
    
    public static InterruptionRequest createChargedAttackHit(UUID targetId, UUID attackerId, 
                                                           DirectionalAttackSystem.AttackDirection direction) {
        return new InterruptionRequest(targetId, attackerId, InterruptionType.HEAVY_PHYSICAL_HIT,
            "Heavy charged " + direction.name() + " attack")
            .withMetadata("attackDirection", direction)
            .withMetadata("chargedAttack", true);
    }
    
    public static InterruptionRequest createDispelEffect(UUID targetId, UUID casterId, String spellName) {
        return new InterruptionRequest(targetId, casterId, InterruptionType.DISPEL_EFFECT,
            "Dispelled by " + spellName)
            .withMetadata("spellName", spellName)
            .withMetadata("canRemoveEffects", true);
    }
}