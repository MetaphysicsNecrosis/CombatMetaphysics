package com.example.examplemod.core.actions.composite;

import com.example.examplemod.core.pipeline.ActionContext;

/**
 * Условие для Composite Actions
 */
public class CompositeCondition {
    private final String type;
    private final Object value;
    
    public CompositeCondition(String type, Object value) {
        this.type = type;
        this.value = value;
    }
    
    public String getType() { return type; }
    public Object getValue() { return value; }
    
    /**
     * Проверяет выполнение условия
     */
    public boolean evaluate(ActionContext context) {
        // TODO: Implement condition evaluation
        return true; // Пока всегда true
    }
    
    /**
     * Описание условия для отладки
     */
    public String getDescription() {
        return type + ": " + value;
    }
}