package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ActionExecutor;
import com.example.examplemod.core.pipeline.ExecutionResult;

/**
 * Базовый класс для Core Actions (уровень 1)
 * Оптимизированные Java примитивы с максимальной производительностью
 */
public abstract class CoreActionExecutor implements ActionExecutor {
    
    protected final String actionType;
    
    protected CoreActionExecutor(String actionType) {
        this.actionType = actionType;
    }
    
    @Override
    public String getActionType() {
        return actionType;
    }
    
    @Override
    public boolean canExecute(ActionContext context) {
        // Базовые проверки для всех Core Actions
        return context.getPlayer() != null && 
               context.getWorld() != null && 
               !context.isCancelled();
    }
    
    @Override
    public ExecutionResult execute(ActionContext context) {
        // Проверка возможности выполнения
        if (!canExecute(context)) {
            return ExecutionResult.failure("Cannot execute " + actionType + ": invalid context");
        }
        
        try {
            // Выполняем конкретное действие
            return executeCore(context);
        } catch (Exception e) {
            return ExecutionResult.failure("Core action failed: " + e.getMessage());
        }
    }
    
    /**
     * Конкретная реализация Core Action
     * Переопределяется в наследниках
     */
    protected abstract ExecutionResult executeCore(ActionContext context);
    
    /**
     * Получение приоритета для Core Actions (всегда максимальный)
     */
    public int getPriority() {
        return 1000; // Высший приоритет для Core Actions
    }
    
    /**
     * Проверка на критичность действия
     */
    public boolean isCritical() {
        return false; // По умолчанию не критично
    }
}