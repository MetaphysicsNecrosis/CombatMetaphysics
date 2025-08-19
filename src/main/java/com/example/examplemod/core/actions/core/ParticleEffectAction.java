package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.actions.CoreActionExecutor;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Core Action: Создание частиц/эффектов
 */
public class ParticleEffectAction extends CoreActionExecutor {
    
    public ParticleEffectAction() {
        super("particle_effect");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        String effectType = context.getEvent().getStringParameter("effectType");
        if (effectType == null) {
            return ExecutionResult.failure("Missing effectType parameter");
        }
        
        Level world = context.getWorld();
        if (!(world instanceof ServerLevel serverWorld)) {
            return ExecutionResult.failure("Particle effects only work on server side");
        }
        
        Vec3 position = getEffectPosition(context);
        float scale = getFloatParameter(context, "scale", 1.0f);
        int duration = getIntParameter(context, "duration", 1000);
        
        // Создаем частицы в зависимости от типа эффекта
        int particleCount = (int)(50 * scale);
        double spread = 2.0 * scale;
        
        switch (effectType) {
            case "meteor_warning" -> {
                // Красные частицы предупреждения
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * spread;
                    double offsetY = world.random.nextDouble() * spread * 0.5;
                    double offsetZ = (world.random.nextDouble() - 0.5) * spread;
                    
                    serverWorld.sendParticles(ParticleTypes.FLAME, 
                            position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                            1, 0, 0.1, 0, 0.1);
                }
            }
            case "meteor_explosion" -> {
                // Взрывные частицы
                for (int i = 0; i < particleCount * 2; i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * spread * 2;
                    double offsetY = world.random.nextDouble() * spread;
                    double offsetZ = (world.random.nextDouble() - 0.5) * spread * 2;
                    
                    serverWorld.sendParticles(ParticleTypes.EXPLOSION, 
                            position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                            1, offsetX * 0.1, 0.2, offsetZ * 0.1, 0.1);
                    
                    serverWorld.sendParticles(ParticleTypes.LAVA, 
                            position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                            1, offsetX * 0.05, 0.1, offsetZ * 0.05, 0.05);
                }
            }
            default -> {
                // Базовые частицы
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * spread;
                    double offsetY = world.random.nextDouble() * spread * 0.5;
                    double offsetZ = (world.random.nextDouble() - 0.5) * spread;
                    
                    serverWorld.sendParticles(ParticleTypes.FIREWORK, 
                            position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                            1, 0, 0.1, 0, 0.1);
                }
            }
        }
        
        // Сохраняем информацию об эффекте
        context.setPipelineData("particleEffectType", effectType);
        context.setPipelineData("particleCount", particleCount);
        context.setPipelineData("particlePosition", position);
        
        return ExecutionResult.success(new ParticleEffectResult(effectType, particleCount, position, scale));
    }
    
    private Vec3 getEffectPosition(ActionContext context) {
        // Сначала проверяем сохраненную позицию сканирования
        Vec3 scanCenter = context.getPipelineData("scanCenter", Vec3.class);
        if (scanCenter != null) {
            return scanCenter;
        }
        
        // Потом проверяем параметры события
        if (context.getEvent().getBlockPosParameter("position") != null) {
            return Vec3.atCenterOf(context.getEvent().getBlockPosParameter("position"));
        }
        
        // По умолчанию - позиция игрока
        return context.getPlayer().position();
    }
    
    private float getFloatParameter(ActionContext context, String key, float defaultValue) {
        Float value = context.getEvent().getFloatParameter(key);
        return value != null ? value : defaultValue;
    }
    
    private int getIntParameter(ActionContext context, String key, int defaultValue) {
        Integer value = context.getEvent().getIntParameter(key);
        return value != null ? value : defaultValue;
    }
    
    public record ParticleEffectResult(String effectType, int particleCount, Vec3 position, float scale) {}
}