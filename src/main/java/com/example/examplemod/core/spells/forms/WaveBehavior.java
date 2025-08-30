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
 * Поведение волны (WAVE) - расширяющаяся волна
 * Отличается от PROJECTILE широкой структурой с изгибом
 * Постепенно расширяется от точки происхождения
 */
public class WaveBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private SpellGeometry geometry;
    private int ticksAlive = 0;
    private boolean shouldDestroy = false;
    private float currentRadius = 1.0f;
    private float maxRadius;
    private float waveSpeed;
    private final Set<Entity> hitEntities = new HashSet<>();
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = geometry;
        this.maxRadius = context.getParameter("max_wave_radius", 20.0f);
        this.waveSpeed = context.getParameter(SpellParameters.SPEED, 5.0f);
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        ticksAlive++;
        
        // Расширяем волну
        expandWave();
        
        // Проверяем коллизии с сущностями
        checkWaveCollisions(level);
        
        // Проверяем достижение максимального радиуса
        if (currentRadius >= maxRadius) {
            shouldDestroy = true;
        }
        
        // Проверяем время жизни
        float duration = context.getParameter(SpellParameters.DURATION, 100.0f);
        if (duration > 0 && ticksAlive > duration) {
            shouldDestroy = true;
        }
    }
    
    private void expandWave() {
        float expansionPerTick = waveSpeed * 0.05f; // За тик
        currentRadius += expansionPerTick;
        
        // Применяем изгиб волны
        float waveBend = context.getParameter(SpellParameters.WAVE_BEND, 0.0f);
        if (waveBend > 0) {
            // Изгиб влияет на форму волны - создаёт неровности
            float bendOffset = (float)Math.sin(ticksAlive * 0.1f) * waveBend;
            context.setParameter("current_wave_radius", currentRadius + bendOffset);
        } else {
            context.setParameter("current_wave_radius", currentRadius);
        }
        
        // Обновляем геометрию
        geometry.update(geometry.getCenter(), Vec3.ZERO);
    }
    
    private void checkWaveCollisions(Level level) {
        Vec3 center = geometry.getCenter();
        float thickness = context.getParameter("wave_thickness", 0.5f);
        
        // Находим сущности в кольце волны (между currentRadius и currentRadius-thickness)
        Set<Entity> entitiesInWave = findEntitiesInWaveRing(level, center, currentRadius, thickness);
        
        for (Entity entity : entitiesInWave) {
            // Не бьём одну сущность дважды
            if (!hitEntities.contains(entity)) {
                hitEntities.add(entity);
                applyWaveEffect(entity);
            }
        }
    }
    
    private Set<Entity> findEntitiesInWaveRing(Level level, Vec3 center, float radius, float thickness) {
        Set<Entity> entities = new HashSet<>();
        
        // Поиск сущностей в кольце (заглушка)
        // entities = level.getEntitiesOfClass(Entity.class, AABB.ofSize(center, radius * 2))
        //     .stream().filter(e -> {
        //         double dist = e.position().distanceTo(center);
        //         return dist <= radius && dist > (radius - thickness);
        //     }).collect(Collectors.toSet());
        
        return entities;
    }
    
    private void applyWaveEffect(Entity entity) {
        float damage = context.getParameter(SpellParameters.DAMAGE, 10.0f);
        
        // Урон уменьшается с расстоянием от центра
        Vec3 center = geometry.getCenter();
        double distance = entity.position().distanceTo(center);
        float damageMultiplier = Math.max(0.1f, 1.0f - (float)(distance / maxRadius));
        float actualDamage = damage * damageMultiplier;
        
        // entity.hurt(..., actualDamage)
        
        // Отталкивание от центра волны
        float knockback = context.getParameter("knockback", 1.0f);
        if (knockback > 0) {
            Vec3 pushDirection = entity.position().subtract(center).normalize();
            float pushForce = knockback * damageMultiplier;
            
            // entity.setDeltaMovement(entity.getDeltaMovement().add(pushDirection.scale(pushForce)))
        }
        
        // Элементальные эффекты
        applyElementalEffects(entity);
    }
    
    private void applyElementalEffects(Entity entity) {
        float fireElement = context.getElement(SpellParameters.FIRE, 0.0f);
        float iceElement = context.getElement(SpellParameters.ICE, 0.0f);
        float lightningElement = context.getElement(SpellParameters.LIGHTNING, 0.0f);
        
        if (fireElement > 0) {
            // entity.setSecondsOnFire((int)fireElement)
        }
        if (iceElement > 0) {
            // Замедление от ледяной волны
        }
        if (lightningElement > 0) {
            // Электрический разряд
        }
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        // Обрабатывается в checkWaveCollisions
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        // Волны могут проходить через блоки или отражаться
        float penetration = context.getParameter(SpellParameters.PENETRATION, 1.0f);
        if (penetration <= 0) {
            // Волна останавливается о препятствие
            shouldDestroy = true;
        } else {
            // Волна ослабевает при прохождении через блоки
            waveSpeed *= 0.9f;
        }
    }
    
    @Override
    public boolean shouldContinue() {
        return !shouldDestroy && 
               currentRadius < maxRadius && 
               waveSpeed > 0.1f;
    }
    
    @Override
    public void cleanup() {
        hitEntities.clear();
        
        // Финальный эффект при достижении максимального радиуса
        boolean hasFinaleEffect = context.getParameter("finale_explosion", 0.0f) > 0;
        if (hasFinaleEffect && currentRadius >= maxRadius) {
            // Взрыв в конце волны
        }
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
    
    /**
     * Получить текущий радиус волны
     */
    public float getCurrentRadius() {
        return currentRadius;
    }
    
    /**
     * Получить количество задетых сущностей
     */
    public int getHitEntitiesCount() {
        return hitEntities.size();
    }
}