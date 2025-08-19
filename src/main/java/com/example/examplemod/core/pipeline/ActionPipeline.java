package com.example.examplemod.core.pipeline;

import com.example.examplemod.core.state.PlayerStateComposition;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Action Pipeline: Event → Validators → Modifiers → Executors → Effects → State Mutation
 * Основа реактивной архитектуры по рекомендации Opus
 */
public class ActionPipeline {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionPipeline.class);
    
    private final List<ActionValidator> validators = new ArrayList<>();
    private final List<ActionModifier> modifiers = new ArrayList<>();
    private final Map<String, ActionExecutor> executors = new HashMap<>();
    private final List<ActionEffect> effects = new ArrayList<>();
    
    // Для suspended actions (QTE и прочих)
    private final Queue<SuspendedAction> suspendedActions = new ConcurrentLinkedQueue<>();
    private final Map<UUID, ActionContext> activeContexts = new HashMap<>();
    
    /**
     * Основной метод обработки действий
     */
    public PipelineResult process(ActionEvent event, PlayerStateComposition state, Player player) {
        ActionContext context = new ActionContext(event, state, player);
        activeContexts.put(context.getId(), context);
        
        try {
            return processInternal(context);
        } finally {
            // Очистка контекста после обработки
            activeContexts.remove(context.getId());
            context.cleanup();
        }
    }
    
    private PipelineResult processInternal(ActionContext context) {
        LOGGER.debug("Processing action: {} for player: {}", 
                context.getEvent().getActionType(), context.getPlayer().getName().getString());
        
        // 1. Validation Phase
        ValidationResult validation = runValidators(context);
        if (!validation.isValid()) {
            LOGGER.debug("Action validation failed: {}", validation.getReason());
            return PipelineResult.failure(validation.getReason());
        }
        
        // 2. Modification Phase  
        runModifiers(context);
        
        // 3. Execution Phase
        ActionExecutor executor = getExecutor(context.getEvent().getActionType());
        if (executor == null) {
            LOGGER.error("No executor found for action type: {}", context.getEvent().getActionType());
            return PipelineResult.failure("No executor found");
        }
        
        ExecutionResult execution;
        try {
            execution = executor.execute(context);
        } catch (Exception e) {
            LOGGER.error("Execution failed for action: {}", context.getEvent().getActionType(), e);
            return PipelineResult.failure("Execution error: " + e.getMessage());
        }
        
        // Проверяем на suspended action (QTE и подобное)
        if (execution.isSuspended()) {
            suspendAction(context, execution.getSuspendedAction());
            return PipelineResult.suspended(execution.getSuspendedAction());
        }
        
        // 4. Effects Phase
        runEffects(context, execution);
        
        // 5. State Mutation Phase
        applyStateMutations(context, execution);
        
        LOGGER.debug("Action completed successfully: {}", context.getEvent().getActionType());
        return PipelineResult.success(execution.getResult());
    }
    
    /**
     * Фаза валидации - проверка возможности выполнения действия
     */
    private ValidationResult runValidators(ActionContext context) {
        for (ActionValidator validator : validators) {
            ValidationResult result = validator.validate(context);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.valid();
    }
    
    /**
     * Фаза модификации - изменение параметров действия
     */
    private void runModifiers(ActionContext context) {
        for (ActionModifier modifier : modifiers) {
            modifier.modify(context);
        }
    }
    
    /**
     * Фаза эффектов - визуалка, звуки, частицы
     */
    private void runEffects(ActionContext context, ExecutionResult execution) {
        for (ActionEffect effect : effects) {
            if (effect.shouldApply(context, execution)) {
                try {
                    effect.apply(context, execution);
                } catch (Exception e) {
                    LOGGER.error("Effect failed: {}", effect.getClass().getSimpleName(), e);
                }
            }
        }
    }
    
    /**
     * Фаза мутации состояния - изменение состояния игрока
     */
    private void applyStateMutations(ActionContext context, ExecutionResult execution) {
        if (!execution.getStateMutations().isEmpty()) {
            PlayerStateComposition state = context.getState();
            for (StateMutation mutation : execution.getStateMutations()) {
                mutation.apply(state);
            }
        }
    }
    
    /**
     * Приостанавливает действие для асинхронной обработки (QTE, каналирование)
     */
    private void suspendAction(ActionContext context, SuspendedAction suspendedAction) {
        suspendedActions.offer(suspendedAction);
        LOGGER.debug("Action suspended: {} for player: {}", 
                suspendedAction.getType(), context.getPlayer().getName().getString());
    }
    
    /**
     * Возобновляет приостановленное действие с результатом
     */
    public void resumeSuspendedAction(UUID actionId, Object result) {
        SuspendedAction action = suspendedActions.stream()
                .filter(sa -> sa.getId().equals(actionId))
                .findFirst()
                .orElse(null);
        
        if (action != null) {
            suspendedActions.remove(action);
            action.resume(result);
            LOGGER.debug("Resumed suspended action: {} with result: {}", action.getType(), result);
        }
    }
    
    /**
     * Обработка tick'ов для suspended actions
     */
    public void tick() {
        Iterator<SuspendedAction> iterator = suspendedActions.iterator();
        while (iterator.hasNext()) {
            SuspendedAction action = iterator.next();
            action.tick();
            
            // Проверяем таймаут
            if (action.isTimedOut()) {
                iterator.remove();
                action.timeout();
                LOGGER.debug("Suspended action timed out: {}", action.getType());
            }
        }
    }
    
    /**
     * Регистрация компонентов pipeline
     */
    public void registerValidator(ActionValidator validator) {
        validators.add(validator);
        validators.sort(Comparator.comparingInt(ActionValidator::getPriority));
    }
    
    public void registerModifier(ActionModifier modifier) {
        modifiers.add(modifier);
        modifiers.sort(Comparator.comparingInt(ActionModifier::getPriority));
    }
    
    public void registerExecutor(String actionType, ActionExecutor executor) {
        executors.put(actionType, executor);
    }
    
    public void registerEffect(ActionEffect effect) {
        effects.add(effect);
    }
    
    private ActionExecutor getExecutor(String actionType) {
        // Сначала проверяем локальные исполнители
        ActionExecutor local = executors.get(actionType);
        if (local != null) {
            return local;
        }
        
        // Потом проверяем ActionRegistry
        return com.example.examplemod.core.actions.ActionRegistry.getInstance().getExecutor(actionType);
    }
    
    /**
     * Debug информация
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("validatorCount", validators.size());
        info.put("modifierCount", modifiers.size());
        info.put("executorCount", executors.size());
        info.put("effectCount", effects.size());
        info.put("suspendedActionCount", suspendedActions.size());
        info.put("activeContextCount", activeContexts.size());
        
        List<String> suspendedTypes = suspendedActions.stream()
                .map(SuspendedAction::getType)
                .toList();
        info.put("suspendedActionTypes", suspendedTypes);
        
        return info;
    }
    
    /**
     * Очистка всех suspended actions (при отключении игрока)
     */
    public void cleanup(Player player) {
        suspendedActions.removeIf(action -> 
                action.getPlayerId().equals(player.getUUID()));
        
        activeContexts.values().removeIf(context -> 
                context.getPlayer().getUUID().equals(player.getUUID()));
    }
}