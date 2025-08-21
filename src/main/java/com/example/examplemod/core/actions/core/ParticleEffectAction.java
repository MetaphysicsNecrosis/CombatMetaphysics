package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.actions.CoreActionExecutor;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import com.example.examplemod.CombatMetaphysics;
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
        
        // Создаем частицы в зависимости от типа эффекта (ОПТИМИЗИРОВАНО)
        // Ограничиваем количество частиц для предотвращения лагов
        int baseParticleCount = Math.min((int)(20 * scale), 100); // Макс 100 частиц
        double spread = 2.0 * scale;
        
        switch (effectType) {
            case "meteor_warning" -> {
                // Красные частицы предупреждения (BATCH отправка)
                serverWorld.sendParticles(ParticleTypes.FLAME, 
                        position.x, position.y, position.z,
                        baseParticleCount, // Отправляем все сразу 
                        spread * 0.5, spread * 0.25, spread * 0.5, // spread X,Y,Z
                        0.1); // speed
            }
            case "meteor_explosion" -> {
                // Взрывные частицы (BATCH отправка для оптимизации)
                int explosionParticles = Math.min(baseParticleCount, 50); // Ещё больше ограничиваем
                
                // Explosion particles
                serverWorld.sendParticles(ParticleTypes.EXPLOSION, 
                        position.x, position.y, position.z,
                        explosionParticles,
                        spread, spread * 0.5, spread,
                        0.2);
                
                // Lava particles (меньше)
                serverWorld.sendParticles(ParticleTypes.LAVA, 
                        position.x, position.y, position.z,
                        explosionParticles / 2, // Вдвое меньше лавы
                        spread * 0.8, spread * 0.3, spread * 0.8,
                        0.1);
            }
            default -> {
                // Базовые частицы (BATCH отправка)
                serverWorld.sendParticles(ParticleTypes.FIREWORK, 
                        position.x, position.y, position.z,
                        baseParticleCount,
                        spread * 0.5, spread * 0.25, spread * 0.5,
                        0.1);
            }
        }
        
        // Сохраняем информацию об эффекте
        context.setPipelineData("particleEffectType", effectType);
        context.setPipelineData("particleCount", baseParticleCount);
        context.setPipelineData("particlePosition", position);
        
        CombatMetaphysics.LOGGER.debug("Created {} {} particles at {}", baseParticleCount, effectType, position);
        return ExecutionResult.success(new ParticleEffectResult(effectType, baseParticleCount, position, scale));
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