package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.actions.CoreActionExecutor;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import com.example.examplemod.CombatMetaphysics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
        // Поддержка BOTH single target И multiple targets из scannedEntities
        List<Entity> targets = extractTargets(context);
        
        if (targets.isEmpty()) {
            CombatMetaphysics.LOGGER.error("DamageAction: No valid targets found");
            return ExecutionResult.failure("No valid targets found");
        }
        
        CombatMetaphysics.LOGGER.info("DamageAction: Processing {} targets", targets.size());
        
        float baseDamage = context.getModifiedDamage();
        if (baseDamage <= 0) {
            return ExecutionResult.failure("Invalid damage amount: " + baseDamage);
        }
        
        // Получение epicenter для area damage calculations
        Vec3 epicenter = getEpicenter(context);
        float radius = getRadius(context);
        
        // Предрасчёт damage falloff для каждой цели
        Float[] damageValues = new Float[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            Entity target = targets.get(i);
            
            if (radius > 0) {
                // Area damage с falloff
                double distance = target.position().distanceTo(epicenter);
                float falloff = (float) Math.max(0, 1 - Math.pow(distance / radius, 2));
                damageValues[i] = baseDamage * falloff;
            } else {
                // Прямой урон без falloff
                damageValues[i] = baseDamage;
            }
        }
        
        // Создаем источник урона
        String damageType = context.getEvent().getStringParameter("damageType");
        DamageSource damageSource = createDamageSource(context, damageType);
        
        // Применяем урон ко всем целям (sequential для корректности Minecraft)
        List<DamageInstance> damageResults = new ArrayList<>();
        float totalDamage = 0f;
        int successfulHits = 0;
        
        for (int i = 0; i < targets.size(); i++) {
            Entity target = targets.get(i);
            if (!(target instanceof LivingEntity livingTarget)) {
                continue;
            }
            
            float damage = damageValues[i];
            if (damage <= 0) {
                continue;
            }
            
            float healthBefore = livingTarget.getHealth();
            livingTarget.hurt(damageSource, damage);
            float healthAfter = livingTarget.getHealth();
            float actualDamage = healthBefore - healthAfter;
            
            damageResults.add(new DamageInstance(livingTarget, damage, actualDamage, healthAfter));
            totalDamage += actualDamage;
            successfulHits++;
            
            CombatMetaphysics.LOGGER.debug("DamageAction: Hit {} for {} damage (actual: {})", 
                livingTarget.getName().getString(), damage, actualDamage);
        }
        
        // Сохраняем результаты для Effects и других action
        context.setPipelineData("damageInstances", damageResults);
        context.setPipelineData("totalDamage", totalDamage);
        context.setPipelineData("hitCount", successfulHits);
        
        AreaDamageResult result = new AreaDamageResult(
            totalDamage, 
            successfulHits, 
            targets.size(),
            damageResults
        );
        
        CombatMetaphysics.LOGGER.info("DamageAction: Completed - {} hits, {} total damage", 
            successfulHits, totalDamage);
        
        return ExecutionResult.success(result);
    }
    
    /**
     * Извлекает список целей из context (поддерживает single target И multiple targets)
     */
    private List<Entity> extractTargets(ActionContext context) {
        // Сначала проверяем множественные цели из area scan
        @SuppressWarnings("unchecked")
        List<Entity> scannedEntities = context.getPipelineData("scannedEntities", List.class);
        if (scannedEntities != null && !scannedEntities.isEmpty()) {
            return scannedEntities.stream()
                .filter(Entity.class::isInstance)
                .map(Entity.class::cast)
                .toList();
        }
        
        // Fallback на single target
        Entity singleTarget = context.getEvent().getEntityParameter("target");
        return singleTarget != null ? List.of(singleTarget) : List.of();
    }
    
    /**
     * Получает эпицентр для area damage calculations
     */
    private Vec3 getEpicenter(ActionContext context) {
        // Используем позицию из area scan если есть
        Vec3 scanCenter = context.getPipelineData("scanCenter", Vec3.class);
        if (scanCenter != null) {
            return scanCenter;
        }
        
        // Fallback на позицию игрока
        return context.getPlayer().position();
    }
    
    /**
     * Получает радиус для area damage falloff
     */
    private float getRadius(ActionContext context) {
        Float scanRange = context.getPipelineData("scanRange", Float.class);
        if (scanRange != null) {
            return scanRange;
        }
        
        // Fallback - нет area damage
        return 0f;
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
     * Результат нанесения урона одной цели
     */
    public record DamageInstance(LivingEntity target, float intendedDamage, float actualDamage, float remainingHealth) {}
    
    /**
     * Результат area damage действия
     */
    public record AreaDamageResult(float totalDamage, int successfulHits, int totalTargets, List<DamageInstance> instances) {}
    
    /**
     * Legacy результат для обратной совместимости
     */
    public record DamageResult(float damageDealt, boolean wasSuccessful, float remainingHealth) {}
}