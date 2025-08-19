package com.example.examplemod.core.pipeline;

import java.util.List;

/**
 * Результат выполнения действия
 */
public class ExecutionResult {
    private final boolean success;
    private final Object result;
    private final List<StateMutation> stateMutations;
    private final boolean suspended;
    private final SuspendedAction suspendedAction;
    private final String errorMessage;
    
    private ExecutionResult(Builder builder) {
        this.success = builder.success;
        this.result = builder.result;
        this.stateMutations = builder.stateMutations;
        this.suspended = builder.suspended;
        this.suspendedAction = builder.suspendedAction;
        this.errorMessage = builder.errorMessage;
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public Object getResult() { return result; }
    public List<StateMutation> getStateMutations() { return stateMutations; }
    public boolean isSuspended() { return suspended; }
    public SuspendedAction getSuspendedAction() { return suspendedAction; }
    public String getErrorMessage() { return errorMessage; }
    
    // Builder
    public static class Builder {
        private boolean success = false;
        private Object result;
        private List<StateMutation> stateMutations = List.of();
        private boolean suspended = false;
        private SuspendedAction suspendedAction;
        private String errorMessage = "";
        
        public Builder success(Object result) {
            this.success = true;
            this.result = result;
            return this;
        }
        
        public Builder failure(String error) {
            this.success = false;
            this.errorMessage = error;
            return this;
        }
        
        public Builder stateMutations(List<StateMutation> mutations) {
            this.stateMutations = mutations;
            return this;
        }
        
        public Builder suspend(SuspendedAction action) {
            this.suspended = true;
            this.suspendedAction = action;
            return this;
        }
        
        public ExecutionResult build() {
            return new ExecutionResult(this);
        }
    }
    
    public static ExecutionResult success(Object result) {
        return new Builder().success(result).build();
    }
    
    public static ExecutionResult failure(String error) {
        return new Builder().failure(error).build();
    }
    
    public static ExecutionResult suspended(SuspendedAction action) {
        return new Builder().suspend(action).build();
    }
}