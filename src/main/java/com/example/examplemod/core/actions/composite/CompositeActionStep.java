package com.example.examplemod.core.actions.composite;

import java.util.List;
import java.util.Map;

/**
 * Шаг в Composite Action
 */
public class CompositeActionStep {
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
    
    // Getters
    public String getActionType() { return actionType; }
    public Map<String, Object> getParameters() { return parameters; }
    public boolean isRequired() { return required; }
    public boolean isBreakOnSuccess() { return breakOnSuccess; }
    public List<CompositeCondition> getStepConditions() { return stepConditions; }
    
    @Override
    public String toString() {
        return String.format("CompositeActionStep{type='%s', required=%s, breakOnSuccess=%s}", 
                           actionType, required, breakOnSuccess);
    }
}