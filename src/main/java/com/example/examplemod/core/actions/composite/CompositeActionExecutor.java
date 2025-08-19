package com.example.examplemod.core.actions.composite;

import com.example.examplemod.core.actions.ActionRegistry;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ActionExecutor;
import com.example.examplemod.core.pipeline.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composite Action Executor (уровень 2)
 * Выполняет последовательность Core Actions на основе JSON конфигурации
 * 
 * Пример: "meteor_strike" = area_scan + damage + block_destruction + particles
 */
public class CompositeActionExecutor implements ActionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeActionExecutor.class);
    
    private final CompositeActionDefinition definition;
    private final ActionRegistry actionRegistry;
    
    public CompositeActionExecutor(CompositeActionDefinition definition) {
        this.definition = definition;
        this.actionRegistry = ActionRegistry.getInstance();
    }
    
    @Override
    public String getActionType() {
        return definition.getActionType();
    }
    
    @Override
    public boolean canExecute(ActionContext context) {
        // Проверяем все условия из definition
        for (CompositeActionStep step : definition.getSteps()) {
            if (!actionRegistry.hasAction(step.getActionType())) {
                LOGGER.error("Missing required action: {} for composite action: {}", 
                        step.getActionType(), definition.getActionType());
                return false;
            }
        }
        
        // Проверяем условия выполнения
        return evaluateConditions(context, definition.getConditions());
    }
    
    @Override
    public ExecutionResult execute(ActionContext context) {
        if (!canExecute(context)) {
            return ExecutionResult.failure("Composite action cannot be executed");
        }
        
        List<Object> stepResults = new ArrayList<>();
        CompositeExecutionContext compositeContext = new CompositeExecutionContext(context);
        
        // Выполняем шаги последовательно
        for (int i = 0; i < definition.getSteps().size(); i++) {
            CompositeActionStep step = definition.getSteps().get(i);
            
            // Создаем контекст для шага
            ActionContext stepContext = createStepContext(context, step, compositeContext);
            
            // Выполняем шаг
            ActionExecutor executor = actionRegistry.getExecutor(step.getActionType());
            ExecutionResult stepResult = executor.execute(stepContext);
            
            if (!stepResult.isSuccess()) {
                // Обработка неудачного шага
                if (step.isRequired()) {
                    return ExecutionResult.failure("Required step failed: " + step.getActionType() + 
                            " - " + stepResult.getErrorMessage());
                } else {
                    LOGGER.debug("Optional step failed: {} - {}", step.getActionType(), 
                            stepResult.getErrorMessage());
                }
            }
            
            // Сохраняем результат шага
            stepResults.add(stepResult.getResult());
            compositeContext.addStepResult(i, stepResult);
            
            // Проверяем условие прерывания
            if (step.shouldBreakOnSuccess() && stepResult.isSuccess()) {
                break;
            }
        }
        
        // Создаем итоговый результат
        CompositeActionResult result = new CompositeActionResult(
                definition.getActionType(),
                stepResults,
                compositeContext.getAllStepResults()
        );
        
        return ExecutionResult.success(result);
    }
    
    /**
     * Создает контекст для выполнения отдельного шага
     */
    private ActionContext createStepContext(ActionContext originalContext, 
                                          CompositeActionStep step, 
                                          CompositeExecutionContext compositeContext) {
        // Копируем оригинальный контекст
        ActionContext stepContext = new ActionContext(originalContext.getEvent(), 
                                                     originalContext.getState(), 
                                                     originalContext.getPlayer());
        
        // Переносим pipeline данные
        originalContext.getPipelineData("modifiedDamage", Float.class);
        if (originalContext.hasPipelineData("modifiedDamage")) {
            stepContext.setPipelineData("modifiedDamage", 
                    originalContext.getPipelineData("modifiedDamage", Float.class));
        }
        
        // Применяем параметры шага
        for (Map.Entry<String, Object> param : step.getParameters().entrySet()) {
            stepContext.setPipelineData("step_" + param.getKey(), param.getValue());
        }
        
        // Добавляем результаты предыдущих шагов
        stepContext.setPipelineData("compositeContext", compositeContext);
        
        return stepContext;
    }
    
    /**
     * Проверка условий выполнения
     */
    private boolean evaluateConditions(ActionContext context, List<CompositeCondition> conditions) {
        for (CompositeCondition condition : conditions) {
            if (!condition.evaluate(context)) {
                return false;
            }
        }
        return true;
    }
    
    public CompositeActionDefinition getDefinition() {
        return definition;
    }
    
    /**
     * Результат выполнения Composite Action
     */
    public record CompositeActionResult(String actionType, 
                                       List<Object> stepResults, 
                                       List<ExecutionResult> allStepResults) {}
}