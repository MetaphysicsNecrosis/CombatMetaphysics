package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.SpellGeometry;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Поведение луча (BEAM) - поддерживаемый луч
 * Режим каста: MANA_SUSTAINED или QTE_SUSTAINED
 * Требует постоянной поддержки маны или QTE
 */
public class BeamBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private SpellGeometry geometry;
    private int ticksAlive = 0;
    private boolean shouldDestroy = false;
    private float currentIntensity = 1.0f;
    private float accumulatedDamage = 0.0f;
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = geometry;
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        ticksAlive++;
        
        // Обновляем геометрию луча от заклинателя к цели
        updateBeamGeometry();
        
        // Проверяем расход маны для MANA_SUSTAINED
        if (!checkManaConsumption()) {
            shouldDestroy = true;
            return;
        }
        
        // Обновляем интенсивность на основе QTE (если активно)
        updateIntensityFromQTE();
        
        // Проверяем прерывания
        if (isInterrupted()) {
            shouldDestroy = true;
        }
    }
    
    private void updateBeamGeometry() {
        Vec3 origin = context.caster().getEyePosition();
        Vec3 direction = context.caster().getViewVector(1.0f);
        float range = context.getParameter(SpellParameters.RANGE, 20.0f);
        
        // Обновляем геометрию луча
        geometry.update(origin, direction.scale(range));
    }
    
    private boolean checkManaConsumption() {
        float channelCostPerSecond = context.getParameter("channel_cost_per_second", 2.0f);
        float costPerTick = channelCostPerSecond * 0.05f; // 20 тиков в секунду
        
        // Здесь должна быть проверка и списание маны у игрока
        // return manaPool.consumeMana(costPerTick);
        return true; // Заглушка
    }
    
    private void updateIntensityFromQTE() {
        boolean qteActive = context.getParameter("qte_active", 0.0f) > 0;
        if (qteActive) {
            float qteAccuracy = context.getParameter("qte_accuracy", 0.85f);
            // QTE позволяет динамически изменять интенсивность
            currentIntensity = Math.min(2.0f, qteAccuracy * 1.5f);
        } else {
            // Без QTE интенсивность медленно падает
            currentIntensity = Math.max(0.3f, currentIntensity * 0.998f);
        }
    }
    
    private boolean isInterrupted() {
        // Проверяем прерывания (Interrupt или Silence)
        return context.getParameter("interrupted", 0.0f) > 0 ||
               !context.caster().isAlive() ||
               context.getParameter("silenced", 0.0f) > 0;
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        float damagePerSecond = context.getParameter(SpellParameters.DAMAGE, 10.0f);
        float tickRate = context.getParameter(SpellParameters.TICK_RATE, 1.0f);
        float actualDamage = damagePerSecond * currentIntensity * (0.05f / tickRate);
        
        accumulatedDamage += actualDamage;
        
        // Применяем урон каждые N тиков согласно tickRate
        if (accumulatedDamage >= 1.0f) {
            // entity.hurt(..., (float)Math.floor(accumulatedDamage))
            accumulatedDamage -= Math.floor(accumulatedDamage);
        }
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        float penetration = context.getParameter(SpellParameters.PENETRATION, 0.0f);
        
        // Лучи могут пробивать блоки с достаточной пробивной силой
        if (penetration > 0) {
            // Уменьшаем интенсивность при прохождении
            currentIntensity *= (1.0f - 1.0f/penetration);
            if (currentIntensity < 0.1f) {
                shouldDestroy = true;
            }
        } else {
            // Останавливаемся о непробиваемый блок
            shouldDestroy = true;
        }
    }
    
    @Override
    public boolean shouldContinue() {
        // Лучи существуют пока:
        // 1. Заклинатель жив и не прерван
        // 2. Есть мана или активно QTE
        // 3. Интенсивность > минимального порога
        return !shouldDestroy && 
               context.caster().isAlive() && 
               currentIntensity > 0.05f &&
               ticksAlive < context.getParameter(SpellParameters.DURATION, 1200.0f);
    }
    
    @Override
    public void cleanup() {
        // Эффекты при отключении луча
        currentIntensity = 0.0f;
        accumulatedDamage = 0.0f;
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
    
    /**
     * Получить текущую интенсивность луча
     */
    public float getCurrentIntensity() {
        return currentIntensity;
    }
}