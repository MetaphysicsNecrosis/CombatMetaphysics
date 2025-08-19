package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Core Action: Исцеление сущности
 */
public class HealAction extends CoreActionExecutor {
    
    public HealAction() {
        super("heal");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        Entity target = context.getEvent().getEntityParameter("target");
        if (target == null || !(target instanceof LivingEntity livingTarget)) {
            return ExecutionResult.failure("Invalid or missing target entity");
        }
        
        Float healAmount = context.getEvent().getFloatParameter("healAmount");
        if (healAmount == null || healAmount <= 0) {
            return ExecutionResult.failure("Invalid heal amount");
        }
        
        float oldHealth = livingTarget.getHealth();
        livingTarget.heal(healAmount);
        float newHealth = livingTarget.getHealth();
        float actualHealing = newHealth - oldHealth;
        
        context.setPipelineData("actualHealing", actualHealing);
        context.setPipelineData("targetHealthBefore", oldHealth);
        context.setPipelineData("targetHealthAfter", newHealth);
        
        return ExecutionResult.success(new HealResult(actualHealing, oldHealth, newHealth));
    }
    
    public record HealResult(float actualHealing, float oldHealth, float newHealth) {}
}