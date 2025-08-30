package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.SpellGeometry;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Поведение барьера (BARRIER) - защитные структуры
 * Может быть физическим (блоки) или магическим (entity)
 * Поддерживает регенерацию через MANA_SUSTAINED
 */
public class BarrierBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private SpellGeometry geometry;
    private int ticksAlive = 0;
    private boolean shouldDestroy = false;
    private float currentDurability;
    private float maxDurability;
    private boolean isPhysicalBarrier;
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = geometry;
        this.maxDurability = context.getParameter(SpellParameters.DURABILITY, 100.0f);
        this.currentDurability = maxDurability;
        this.isPhysicalBarrier = context.getParameter("physical_barrier", 1.0f) > 0;
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        ticksAlive++;
        
        // Проверяем автоматическую регенерацию
        handleRegeneration();
        
        // Проверяем время жизни
        float duration = context.getParameter(SpellParameters.DURATION, 300.0f);
        if (duration > 0 && ticksAlive > duration) {
            shouldDestroy = true;
        }
        
        // Проверяем разрушение
        if (currentDurability <= 0) {
            shouldDestroy = true;
        }
    }
    
    private void handleRegeneration() {
        boolean hasRegeneration = context.getParameter("regeneration", 0.0f) > 0;
        if (!hasRegeneration) return;
        
        // Регенерация за счет маны заклинателя
        float regenPerSecond = context.getParameter("regeneration_rate", 5.0f);
        float regenPerTick = regenPerSecond * 0.05f;
        
        // Проверяем мана-каст для регенерации
        float regenManaCost = context.getParameter("regen_mana_cost", 1.0f) * 0.05f;
        // Здесь должна быть проверка маны: if (manaPool.canConsume(regenManaCost))
        
        currentDurability = Math.min(maxDurability, currentDurability + regenPerTick);
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        // Барьеры могут блокировать движение или отталкивать
        boolean blocksMovement = context.getParameter("blocks_movement", 1.0f) > 0;
        boolean damagesEntities = context.getParameter("damages_entities", 0.0f) > 0;
        
        if (damagesEntities) {
            float damage = context.getParameter("barrier_damage", 2.0f);
            // entity.hurt(..., damage)
        }
        
        if (blocksMovement) {
            // Отталкиваем сущность от барьера
            Vec3 barrierCenter = geometry.getCenter();
            Vec3 entityPos = entity.position();
            Vec3 pushDirection = entityPos.subtract(barrierCenter).normalize();
            float pushForce = context.getParameter("push_force", 0.5f);
            
            // entity.setDeltaMovement(entity.getDeltaMovement().add(pushDirection.scale(pushForce)))
        }
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        // Барьеры обычно не взаимодействуют с блоками,
        // кроме случаев интеграции в ландшафт
    }
    
    /**
     * Получить урон барьеру
     */
    public void takeDamage(float damage) {
        float actualDamage = damage;
        
        // Применяем сопротивления
        float physicalResist = context.getParameter("physical_resistance", 0.0f);
        float magicalResist = context.getParameter("magical_resistance", 0.0f);
        
        // Уменьшаем урон на основе типа атаки (заглушка)
        actualDamage *= (1.0f - physicalResist * 0.01f);
        
        currentDurability -= actualDamage;
        
        // Визуальные эффекты повреждения
        if (currentDurability < maxDurability * 0.5f) {
            // Эффекты трещин на барьере
        }
    }
    
    /**
     * Восстановить прочность барьера
     */
    public void heal(float amount) {
        currentDurability = Math.min(maxDurability, currentDurability + amount);
    }
    
    @Override
    public boolean shouldContinue() {
        return !shouldDestroy && 
               currentDurability > 0 &&
               context.caster().isAlive();
    }
    
    @Override
    public void cleanup() {
        // Если физический барьер - убираем блоки
        if (isPhysicalBarrier) {
            // Логика удаления блоков барьера
        }
        
        // Эффекты разрушения
        if (currentDurability <= 0) {
            // Эффект взрыва/разрушения
        }
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
    
    /**
     * Получить текущую прочность
     */
    public float getCurrentDurability() {
        return currentDurability;
    }
    
    /**
     * Получить процент прочности
     */
    public float getDurabilityPercent() {
        return maxDurability > 0 ? currentDurability / maxDurability : 0.0f;
    }
}