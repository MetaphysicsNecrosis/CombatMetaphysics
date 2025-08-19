package com.example.examplemod.core.actions;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ActionExecutor;
import com.example.examplemod.core.pipeline.ExecutionResult;

/**
 * Базовый класс для всех Core Actions (уровень 1)
 * Оптимизированные Java примитивы с type safety
 */
public abstract class CoreActionExecutor implements ActionExecutor {
    private final String actionType;
    
    protected CoreActionExecutor(String actionType) {
        this.actionType = actionType;
    }
    
    @Override
    public final String getActionType() {
        return actionType;
    }
    
    @Override
    public boolean canExecute(ActionContext context) {
        return true; // По умолчанию все Core Actions могут выполняться
    }
    
    @Override
    public final ExecutionResult execute(ActionContext context) {
        try {
            return executeCore(context);
        } catch (Exception e) {
            return ExecutionResult.failure("Core action failed: " + e.getMessage());
        }
    }
    
    /**
     * Основная логика выполнения действия
     */
    protected abstract ExecutionResult executeCore(ActionContext context);
    
    /**
     * Проверка обязательных параметров
     */
    protected boolean hasRequiredParameter(ActionContext context, String paramName) {
        return context.getEvent().getParameters().containsKey(paramName);
    }
    
    /**
     * Получение параметра с дефолтным значением
     */
    protected <T> T getParameter(ActionContext context, String paramName, T defaultValue) {
        var value = context.getEvent().getParameters().get(paramName);
        if (value == null) {
            return defaultValue;
        }
        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}