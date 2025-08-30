package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.SpellGeometry;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Поведение мгновенной точки (INSTANT_POINT) - мгновенное проявление в точке
 * Задержка перед активацией, затем мгновенный эффект
 * Используется для телепортации, взрывов, исцеления в точке
 */
public class InstantPointBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private SpellGeometry geometry;
    private int delayTicks = 0;
    private boolean hasTriggered = false;
    private boolean shouldDestroy = false;
    private Vec3 targetPoint;
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = geometry;
        this.targetPoint = context.getTargetPosition();
        
        // Задержка до активации
        float instantDelay = context.getParameter(SpellParameters.INSTANT_DELAY, 1.0f);
        this.delayTicks = (int)(instantDelay * 20); // конвертируем в тики
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        if (delayTicks > 0) {
            delayTicks--;
            // Визуальные эффекты зарядки
            createChargingEffects();
            return;
        }
        
        if (!hasTriggered) {
            triggerInstantEffect(level);
            hasTriggered = true;
            shouldDestroy = true;
        }
    }
    
    private void createChargingEffects() {
        // Визуальные эффекты во время зарядки
        // Партиклы, свечение, звуки предупреждения
        float chargeProgress = 1.0f - (delayTicks / (context.getParameter(SpellParameters.INSTANT_DELAY, 1.0f) * 20));
        
        // Увеличиваем интенсивность эффектов по мере приближения к активации
        if (chargeProgress > 0.8f) {
            // Интенсивные предупредительные эффекты
        }
    }
    
    private void triggerInstantEffect(Level level) {
        float damage = context.getParameter(SpellParameters.DAMAGE, 30.0f);
        float healing = context.getParameter(SpellParameters.HEALING, 0.0f);
        float radius = context.getParameter(SpellParameters.RADIUS, 3.0f);
        
        // Находим все сущности в радиусе воздействия
        Set<Entity> affectedEntities = findEntitiesInRadius(level, targetPoint, radius);
        
        for (Entity entity : affectedEntities) {
            // Урон уменьшается с расстоянием от центра
            double distance = entity.position().distanceTo(targetPoint);
            float damageMultiplier = Math.max(0.1f, 1.0f - (float)(distance / radius));
            
            float actualDamage = damage * damageMultiplier;
            float actualHealing = healing * damageMultiplier;
            
            if (actualDamage > 0) {
                // entity.hurt(..., actualDamage)
            } else if (actualHealing > 0) {
                // entity.heal(actualHealing)
            }
            
            // Применяем специальные эффекты
            applySpecialInstantEffects(entity, damageMultiplier);
        }
        
        // Воздействие на блоки
        handleBlockEffects(level, targetPoint, radius);
        
        // Элементальные эффекты области
        applyAreaElementalEffects(level, targetPoint, radius);
    }
    
    private Set<Entity> findEntitiesInRadius(Level level, Vec3 center, float radius) {
        Set<Entity> entities = new HashSet<>();
        
        // Поиск сущностей в радиусе (заглушка)
        // entities = level.getEntitiesOfClass(Entity.class, AABB.ofSize(center, radius * 2))
        //     .stream().filter(e -> e.position().distanceTo(center) <= radius)
        //     .collect(Collectors.toSet());
        
        return entities;
    }
    
    private void applySpecialInstantEffects(Entity entity, float multiplier) {
        boolean hasTeleport = context.getParameter("teleport_effect", 0.0f) > 0;
        boolean hasDispel = context.getParameter("dispel_effect", 0.0f) > 0;
        boolean hasStun = context.getParameter("stun_effect", 0.0f) > 0;
        
        if (hasTeleport) {
            // Телепортация цели в случайную точку
            float teleportRange = context.getParameter("teleport_range", 10.0f) * multiplier;
        }
        
        if (hasDispel) {
            // Снятие всех магических эффектов с цели
            // entity.removeAllEffects()
        }
        
        if (hasStun) {
            // Оглушение цели
            int stunDuration = (int)(context.getParameter("stun_duration", 60.0f) * multiplier);
            // entity.addStatusEffect(stun, stunDuration)
        }
    }
    
    private void handleBlockEffects(Level level, Vec3 center, float radius) {
        float blockDamage = context.getParameter("block_damage", 0.0f);
        boolean breaksBlocks = context.getParameter("breaks_blocks", 0.0f) > 0;
        
        if (breaksBlocks && blockDamage > 0) {
            // Разрушение блоков в радиусе
            // for (BlockPos pos : BlockPos.betweenClosed(...)) {
            //     if (pos.distToCenterSqr(center.x, center.y, center.z) <= radius * radius) {
            //         level.destroyBlock(pos, true)
            //     }
            // }
        }
    }
    
    private void applyAreaElementalEffects(Level level, Vec3 center, float radius) {
        float fireElement = context.getElement(SpellParameters.FIRE, 0.0f);
        float iceElement = context.getElement(SpellParameters.ICE, 0.0f);
        float lightningElement = context.getElement(SpellParameters.LIGHTNING, 0.0f);
        
        if (fireElement > 0) {
            // Поджигаем область
            // level.setBlock(..., Blocks.FIRE.defaultBlockState())
        }
        if (iceElement > 0) {
            // Замораживаем область (лёд, снег)
        }
        if (lightningElement > 0) {
            // Электрические разряды по области
        }
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        // Обрабатывается в triggerInstantEffect
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        // Обрабатывается в triggerInstantEffect
    }
    
    @Override
    public boolean shouldContinue() {
        return !shouldDestroy;
    }
    
    @Override
    public void cleanup() {
        // Финальные эффекты после мгновенного воздействия
        // Кратер, огонь, лёд и т.д. в зависимости от элементов
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
    
    /**
     * Получить оставшееся время до активации
     */
    public int getRemainingDelay() {
        return Math.max(0, delayTicks);
    }
    
    /**
     * Проверить, сработало ли заклинание
     */
    public boolean hasTriggered() {
        return hasTriggered;
    }
}