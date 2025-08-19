package com.example.examplemod.core.pipeline;

/**
 * Эффект - визуальные/звуковые эффекты действия
 */
public interface ActionEffect {
    void apply(ActionContext context, ExecutionResult result);
    boolean shouldApply(ActionContext context, ExecutionResult result);
    String getName();
}