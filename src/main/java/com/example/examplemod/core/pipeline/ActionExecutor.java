package com.example.examplemod.core.pipeline;

/**
 * Исполнитель - выполняет само действие
 */
public interface ActionExecutor {
    ExecutionResult execute(ActionContext context);
    String getActionType();
    boolean canExecute(ActionContext context);
}