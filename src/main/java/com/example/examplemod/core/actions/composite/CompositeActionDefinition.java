package com.example.examplemod.core.actions.composite;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;

import java.util.List;
import java.util.Map;

/**
 * Определение Composite Action из JSON
 * Описывает последовательность Core Actions для выполнения
 */
public class CompositeActionDefinition {
    private final String actionType;
    private final String description;
    private final List<CompositeActionStep> steps;
    private final List<CompositeCondition> conditions;
    private final Map<String, Object> defaultParameters;
    
    public CompositeActionDefinition(String actionType, 
                                   String description, 
                                   List<CompositeActionStep> steps, 
                                   List<CompositeCondition> conditions,
                                   Map<String, Object> defaultParameters) {
        this.actionType = actionType;
        this.description = description;
        this.steps = steps;
        this.conditions = conditions;
        this.defaultParameters = defaultParameters;
    }
    
    // Getters
    public String getActionType() { return actionType; }
    public String getDescription() { return description; }
    public List<CompositeActionStep> getSteps() { return steps; }
    public List<CompositeCondition> getConditions() { return conditions; }
    public Map<String, Object> getDefaultParameters() { return defaultParameters; }
}

// CompositeActionStep теперь в отдельном файле

/**
 * Контекст выполнения Composite Action
 */
class CompositeExecutionContext {
    private final ActionContext originalContext;
    private final List<ExecutionResult> stepResults = new java.util.ArrayList<>();
    
    public CompositeExecutionContext(ActionContext originalContext) {
        this.originalContext = originalContext;
    }
    
    public void addStepResult(int stepIndex, ExecutionResult result) {
        // Расширяем список если нужно
        while (stepResults.size() <= stepIndex) {
            stepResults.add(null);
        }
        stepResults.set(stepIndex, result);
    }
    
    public ExecutionResult getStepResult(int stepIndex) {
        return stepIndex < stepResults.size() ? stepResults.get(stepIndex) : null;
    }
    
    public List<ExecutionResult> getAllStepResults() {
        return new java.util.ArrayList<>(stepResults);
    }
    
    public ActionContext getOriginalContext() {
        return originalContext;
    }
}