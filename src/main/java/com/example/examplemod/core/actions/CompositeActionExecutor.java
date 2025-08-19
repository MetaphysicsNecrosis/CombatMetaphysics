package com.example.examplemod.core.actions;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ActionExecutor;
import com.example.examplemod.core.pipeline.ActionEvent;
import com.example.examplemod.core.pipeline.ExecutionResult;
import com.example.examplemod.core.actions.composite.CompositeActionDefinition;
import com.example.examplemod.core.actions.composite.CompositeActionStep;
import com.example.examplemod.CombatMetaphysics;

import java.util.Map;

/**
 * Исполнитель Composite Actions (уровень 2)
 * Выполняет последовательность действий согласно JSON конфигурации
 */
public class CompositeActionExecutor implements ActionExecutor {
    private final CompositeActionDefinition definition;
    
    public CompositeActionExecutor(CompositeActionDefinition definition) {
        this.definition = definition;
    }
    
    @Override
    public String getActionType() {
        return definition.getActionType();
    }
    
    @Override
    public boolean canExecute(ActionContext context) {
        return true; // Проверка зависимостей будет позже
    }
    
    @Override
    public ExecutionResult execute(ActionContext context) {
        CombatMetaphysics.LOGGER.info("Executing composite action: {}", definition.getActionType());
        
        try {
            // Выполняем каждый шаг последовательно
            for (CompositeActionStep step : definition.getSteps()) {
                CombatMetaphysics.LOGGER.info("Executing step: {}", step.getActionType());
                ExecutionResult stepResult = executeStep(step, context);
                
                CombatMetaphysics.LOGGER.info("Step {} result: success={}, error={}", 
                    step.getActionType(), stepResult.isSuccess(), stepResult.getErrorMessage());
                
                // Если шаг критичен и провалился
                if (step.isRequired() && !stepResult.isSuccess()) {
                    String error = "Required step failed: " + step.getActionType() + 
                                  " - " + stepResult.getErrorMessage();
                    CombatMetaphysics.LOGGER.error(error);
                    return ExecutionResult.failure(error);
                }
                
                // Если шаг успешен и нужно прервать выполнение
                if (step.isBreakOnSuccess() && stepResult.isSuccess()) {
                    CombatMetaphysics.LOGGER.info("Breaking on success for step: {}", step.getActionType());
                    break;
                }
                
                // Если шаг приостановлен (QTE), возвращаем suspended
                if (stepResult.isSuspended()) {
                    CombatMetaphysics.LOGGER.info("Step suspended: {}", step.getActionType());
                    return stepResult;
                }
            }
            
            CombatMetaphysics.LOGGER.info("Composite action completed successfully: {}", definition.getActionType());
            return ExecutionResult.success("Composite action completed: " + definition.getActionType());
            
        } catch (Exception e) {
            CombatMetaphysics.LOGGER.error("Composite action error: {}", definition.getActionType(), e);
            return ExecutionResult.failure("Composite action error: " + e.getMessage());
        }
    }
    
    /**
     * Выполняет один шаг композитного действия
     */
    private ExecutionResult executeStep(CompositeActionStep step, ActionContext context) {
        try {
            // Получаем исполнитель для шага
            ActionExecutor stepExecutor = com.example.examplemod.core.actions.ActionRegistry
                    .getInstance().getExecutor(step.getActionType());
                    
            if (stepExecutor == null) {
                return ExecutionResult.failure("No executor found for step: " + step.getActionType());
            }
            
            // Создаём новый контекст для шага с объединёнными параметрами
            ActionEvent stepEvent = createStepEvent(step, context);
            ActionContext stepContext = new ActionContext(stepEvent, context.getState(), context.getPlayer());
            
            // Копируем pipeline данные из родительского контекста
            copyPipelineData(context, stepContext);
            
            // Выполняем шаг
            ExecutionResult result = stepExecutor.execute(stepContext);
            
            // Копируем результаты обратно в родительский контекст
            copyPipelineDataBack(stepContext, context);
            
            return result;
            
        } catch (Exception e) {
            return ExecutionResult.failure("Step execution error: " + e.getMessage());
        }
    }
    
    /**
     * Создаёт событие для выполнения шага
     */
    private ActionEvent createStepEvent(CompositeActionStep step, ActionContext parentContext) {
        ActionEvent.Builder builder = new ActionEvent.Builder(step.getActionType(), 
                                                              parentContext.getPlayer());
        
        // Копируем параметры из родительского события
        for (Map.Entry<String, Object> entry : parentContext.getEvent().getParameters().entrySet()) {
            builder.parameter(entry.getKey(), entry.getValue());
        }
        
        // Добавляем параметры шага (они перезаписывают родительские)
        for (Map.Entry<String, Object> entry : step.getParameters().entrySet()) {
            Object value = entry.getValue();
            
            // Обрабатываем шаблонные значения типа "${damage}"
            if (value instanceof String && ((String) value).startsWith("${") && ((String) value).endsWith("}")) {
                String paramName = ((String) value).substring(2, ((String) value).length() - 1);
                Object resolvedValue = parentContext.getEvent().getParameters().get(paramName);
                if (resolvedValue != null) {
                    value = resolvedValue;
                }
            }
            
            builder.parameter(entry.getKey(), value);
        }
        
        builder.source(parentContext.getEvent().getSource());
        return builder.build();
    }
    
    /**
     * Копирует pipeline данные между контекстами
     */
    private void copyPipelineData(ActionContext from, ActionContext to) {
        // Копируем основные данные
        if (from.hasPipelineData("scannedEntities")) {
            to.setPipelineData("scannedEntities", from.getPipelineData("scannedEntities", Object.class));
        }
        if (from.hasPipelineData("scannedBlocks")) {
            to.setPipelineData("scannedBlocks", from.getPipelineData("scannedBlocks", Object.class));
        }
    }
    
    /**
     * Копирует pipeline данные обратно
     */
    private void copyPipelineDataBack(ActionContext from, ActionContext to) {
        // Копируем результаты обратно
        if (from.hasPipelineData("scannedEntities")) {
            to.setPipelineData("scannedEntities", from.getPipelineData("scannedEntities", Object.class));
        }
        if (from.hasPipelineData("scannedBlocks")) {
            to.setPipelineData("scannedBlocks", from.getPipelineData("scannedBlocks", Object.class));
        }
        if (from.hasPipelineData("actualDamage")) {
            to.setPipelineData("actualDamage", from.getPipelineData("actualDamage", Object.class));
        }
        if (from.hasPipelineData("blocksDestroyed")) {
            to.setPipelineData("blocksDestroyed", from.getPipelineData("blocksDestroyed", Object.class));
        }
    }
    
    public CompositeActionDefinition getDefinition() {
        return definition;
    }
}