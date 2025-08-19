package com.example.examplemod.core.actions.composite;

import com.example.examplemod.core.pipeline.ActionContext;

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

/**
 * Шаг в Composite Action
 */
class CompositeActionStep {
    private final String actionType;
    private final Map<String, Object> parameters;
    private final boolean required;
    private final boolean breakOnSuccess;
    private final List<CompositeCondition> stepConditions;
    
    public CompositeActionStep(String actionType, 
                              Map<String, Object> parameters, 
                              boolean required,
                              boolean breakOnSuccess,
                              List<CompositeCondition> stepConditions) {
        this.actionType = actionType;
        this.parameters = parameters;
        this.required = required;
        this.breakOnSuccess = breakOnSuccess;
        this.stepConditions = stepConditions;
    }
    
    public String getActionType() { return actionType; }
    public Map<String, Object> getParameters() { return parameters; }
    public boolean isRequired() { return required; }
    public boolean shouldBreakOnSuccess() { return breakOnSuccess; }
    public List<CompositeCondition> getStepConditions() { return stepConditions; }
}

/**
 * Условие для выполнения Composite Action или шага
 */
interface CompositeCondition {
    boolean evaluate(ActionContext context);
    String getDescription();
}

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