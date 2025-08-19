package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.actions.CoreActionExecutor;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Core Action: Воспроизведение звуковых эффектов
 */
public class SoundEffectAction extends CoreActionExecutor {
    
    public SoundEffectAction() {
        super("sound_effect");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        String soundId = context.getEvent().getStringParameter("soundId");
        if (soundId == null) {
            return ExecutionResult.failure("Missing soundId parameter");
        }
        
        Level world = context.getWorld();
        Vec3 position = getSoundPosition(context);
        float volume = getFloatParameter(context, "volume", 1.0f);
        float pitch = getFloatParameter(context, "pitch", 1.0f);
        
        // Воспроизводим звук в зависимости от типа
        switch (soundId) {
            case "meteor_incoming" -> {
                world.playSound(null, position.x, position.y, position.z,
                        SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE,
                        volume, pitch * 0.5f);
            }
            case "meteor_explosion" -> {
                world.playSound(null, position.x, position.y, position.z,
                        SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                        volume * 2.0f, pitch);
                
                // Дополнительный звук грома для эпичности
                world.playSound(null, position.x, position.y, position.z,
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                        volume, pitch * 0.8f);
            }
            case "spell_cast" -> {
                world.playSound(null, position.x, position.y, position.z,
                        SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS,
                        volume, pitch);
            }
            case "spell_prepare" -> {
                world.playSound(null, position.x, position.y, position.z,
                        SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS,
                        volume, pitch);
            }
            default -> {
                // Базовый магический звук
                world.playSound(null, position.x, position.y, position.z,
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                        volume, pitch);
            }
        }
        
        // Сохраняем информацию о звуке
        context.setPipelineData("soundId", soundId);
        context.setPipelineData("soundPosition", position);
        context.setPipelineData("soundVolume", volume);
        context.setPipelineData("soundPitch", pitch);
        
        return ExecutionResult.success(new SoundEffectResult(soundId, position, volume, pitch));
    }
    
    private Vec3 getSoundPosition(ActionContext context) {
        // Проверяем сохраненную позицию сканирования
        Vec3 scanCenter = context.getPipelineData("scanCenter", Vec3.class);
        if (scanCenter != null) {
            return scanCenter;
        }
        
        // Параметры события
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
    
    public record SoundEffectResult(String soundId, Vec3 position, float volume, float pitch) {}
}