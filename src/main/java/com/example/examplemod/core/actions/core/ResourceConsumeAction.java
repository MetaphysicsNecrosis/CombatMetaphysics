package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;

/**
 * Core Action: Потребление ресурсов (мана, выносливость)
 */
public class ResourceConsumeAction extends CoreActionExecutor {
    
    public ResourceConsumeAction() {
        super("resource_consume");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        String resourceType = context.getEvent().getStringParameter("resourceType");
        if (resourceType == null) {
            resourceType = "mana"; // По умолчанию мана
        }
        
        Float amount = context.getEvent().getFloatParameter("amount");
        if (amount == null || amount <= 0) {
            return ExecutionResult.failure("Invalid resource amount: " + amount);
        }
        
        boolean consumed = switch (resourceType) {
            case "mana" -> consumeMana(context, amount);
            case "stamina" -> consumeStamina(context, amount);
            default -> {
                yield false; // Неизвестный тип ресурса
            }
        };
        
        if (!consumed) {
            return ExecutionResult.failure("Failed to consume " + amount + " " + resourceType);
        }
        
        context.setPipelineData("consumedResourceType", resourceType);
        context.setPipelineData("consumedResourceAmount", amount);
        
        return ExecutionResult.success(new ResourceConsumeResult(resourceType, amount));
    }
    
    private boolean consumeMana(ActionContext context, float amount) {
        // TODO: Интеграция с ResourceManager
        // Пока просто возвращаем true для тестирования
        return true;
    }
    
    private boolean consumeStamina(ActionContext context, float amount) {
        // TODO: Интеграция с ResourceManager
        // Пока просто возвращаем true для тестирования
        return true;
    }
    
    public record ResourceConsumeResult(String resourceType, float amount) {}
}