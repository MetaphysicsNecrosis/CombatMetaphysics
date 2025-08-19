package com.example.examplemod.core.pipeline;

/**
 * Валидатор - проверяет возможность выполнения действия
 */
public interface ActionValidator {
    ValidationResult validate(ActionContext context);
    int getPriority(); // Порядок выполнения валидаторов
    String getName();
}