package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.ArrayList;

/**
 * Core Action: Наложение статусных эффектов
 */
public class StatusEffectAction extends CoreActionExecutor {
    
    public StatusEffectAction() {
        super("status_effect");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        String effectType = context.getEvent().getStringParameter("effectType");
        if (effectType == null) {
            return ExecutionResult.failure("Missing effectType parameter");
        }
        
        Integer duration = context.getEvent().getIntParameter("duration");
        if (duration == null || duration <= 0) {
            duration = 200; // 10 секунд по умолчанию
        }
        
        Integer amplifier = context.getEvent().getIntParameter("amplifier");
        if (amplifier == null) {
            amplifier = 0;
        }
        
        MobEffect effect = getEffectByType(effectType);
        if (effect == null) {
            return ExecutionResult.failure("Unknown effect type: " + effectType);
        }
        
        // Получаем цели
        List<LivingEntity> targets = getTargets(context);
        if (targets.isEmpty()) {
            return ExecutionResult.failure("No valid targets for status effect");
        }
        
        List<LivingEntity> affectedEntities = new ArrayList<>();
        MobEffectInstance effectInstance = new MobEffectInstance(effect, duration, amplifier);
        
        for (LivingEntity target : targets) {
            target.addEffect(effectInstance);
            affectedEntities.add(target);
        }
        
        context.setPipelineData("statusEffectType", effectType);
        context.setPipelineData("affectedEntities", affectedEntities);
        context.setPipelineData("effectDuration", duration);
        context.setPipelineData("effectAmplifier", amplifier);
        
        return ExecutionResult.success(new StatusEffectResult(effectType, affectedEntities.size(), duration, amplifier));
    }
    
    private MobEffect getEffectByType(String effectType) {
        return switch (effectType) {
            case "poison" -> MobEffects.POISON;
            case "weakness" -> MobEffects.WEAKNESS;
            case "slowness" -> MobEffects.MOVEMENT_SLOWDOWN;
            case "blindness" -> MobEffects.BLINDNESS;
            case "nausea" -> MobEffects.CONFUSION;
            case "wither" -> MobEffects.WITHER;
            case "levitation" -> MobEffects.LEVITATION;
            case "glowing" -> MobEffects.GLOWING;
            case "regeneration" -> MobEffects.REGENERATION;
            case "strength" -> MobEffects.DAMAGE_BOOST;
            case "speed" -> MobEffects.MOVEMENT_SPEED;
            case "resistance" -> MobEffects.DAMAGE_RESISTANCE;
            default -> null;
        };
    }
    
    @SuppressWarnings("unchecked")
    private List<LivingEntity> getTargets(ActionContext context) {
        List<LivingEntity> targets = new ArrayList<>();
        
        // Проверяем конкретную цель
        Entity target = context.getEvent().getEntityParameter("target");
        if (target instanceof LivingEntity livingTarget) {
            targets.add(livingTarget);
        }
        
        // Проверяем найденных сущностей
        List<Entity> scannedEntities = context.getPipelineData("scannedEntities", List.class);
        if (scannedEntities != null) {
            for (Entity entity : scannedEntities) {
                if (entity instanceof LivingEntity livingEntity) {
                    targets.add(livingEntity);
                }
            }
        }
        
        return targets;
    }
    
    public record StatusEffectResult(String effectType, int affectedCount, int duration, int amplifier) {}
}