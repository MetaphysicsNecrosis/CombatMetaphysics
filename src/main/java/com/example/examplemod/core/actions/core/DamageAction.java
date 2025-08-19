package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.actions.CoreActionExecutor;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Core Action: Нанесение урона сущности
 * Базовый примитив для всех урон-действий
 */
public class DamageAction extends CoreActionExecutor {
    
    public DamageAction() {
        super("damage");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        Entity target = context.getEvent().getEntityParameter("target");
        if (target == null || !(target instanceof LivingEntity livingTarget)) {
            return ExecutionResult.failure("Invalid or missing target entity");
        }
        
        // Получаем модифицированный урон из context (может быть изменен модификаторами)
        float damage = context.getModifiedDamage();
        if (damage <= 0) {
            return ExecutionResult.failure("Invalid damage amount: " + damage);
        }
        
        // Получаем источник урона
        String damageType = context.getEvent().getStringParameter("damageType");
        DamageSource damageSource = createDamageSource(context, damageType);
        
        // Применяем урон
        livingTarget.hurt(damageSource, damage);
        boolean success = true; // В Minecraft 1.21.1 hurt() возвращает void
        
        if (success) {
            // Сохраняем результат для Effects
            context.setPipelineData("actualDamage", damage);
            context.setPipelineData("targetHealth", livingTarget.getHealth());
            context.setPipelineData("targetMaxHealth", livingTarget.getMaxHealth());
            
            return ExecutionResult.success(new DamageResult(damage, success, livingTarget.getHealth()));
        } else {
            return ExecutionResult.failure("Damage was blocked or prevented");
        }
    }
    
    /**
     * Создает источник урона на основе типа
     */
    private DamageSource createDamageSource(ActionContext context, String damageType) {
        // TODO: Implement proper damage source creation based on type
        // Используем magic damage для магических заклинаний
        if (damageType != null && damageType.contains("magical")) {
            return context.getWorld().damageSources().magic();
        }
        // Пока используем базовый generic damage
        return context.getWorld().damageSources().generic();
    }
    
    /**
     * Результат нанесения урона
     */
    public record DamageResult(float damageDealt, boolean wasSuccessful, float remainingHealth) {}
}