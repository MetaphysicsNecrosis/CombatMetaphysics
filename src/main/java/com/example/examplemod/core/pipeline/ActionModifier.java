package com.example.examplemod.core.pipeline;

/**
 * Модификатор - изменяет параметры действия
 */
public interface ActionModifier {
    void modify(ActionContext context);
    int getPriority(); // Порядок применения модификаторов
    String getName();
}