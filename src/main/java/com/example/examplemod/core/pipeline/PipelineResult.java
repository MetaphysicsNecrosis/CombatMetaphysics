package com.example.examplemod.core.pipeline;

/**
 * Результат обработки через pipeline
 */
public class PipelineResult {
    private final boolean success;
    private final Object result;
    private final boolean suspended;
    private final SuspendedAction suspendedAction;
    private final String errorMessage;
    
    private PipelineResult(boolean success, Object result, boolean suspended, 
                          SuspendedAction suspendedAction, String errorMessage) {
        this.success = success;
        this.result = result;
        this.suspended = suspended;
        this.suspendedAction = suspendedAction;
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() { return success; }
    public Object getResult() { return result; }
    public boolean isSuspended() { return suspended; }
    public SuspendedAction getSuspendedAction() { return suspendedAction; }
    public String getErrorMessage() { return errorMessage; }
    
    public static PipelineResult success(Object result) {
        return new PipelineResult(true, result, false, null, "");
    }
    
    public static PipelineResult failure(String error) {
        return new PipelineResult(false, null, false, null, error);
    }
    
    public static PipelineResult suspended(SuspendedAction action) {
        return new PipelineResult(false, null, true, action, "");
    }
}