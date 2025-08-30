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
 * Поведение зоны воздействия (AREA) - двумерная область
 * Накладывается на пол/потолок с постоянным эффектом
 * Может расти со временем (GrowthRate)
 */
public class AreaBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private SpellGeometry geometry;
    private int ticksAlive = 0;
    private boolean shouldDestroy = false;
    private float currentRadius;
    private float maxRadius;
    private final Set<Entity> entitiesInside = new HashSet<>();
    private int ticksSinceLastEffect = 0;
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = geometry;
        this.currentRadius = context.getParameter(SpellParameters.RADIUS, 3.0f);
        this.maxRadius = context.getParameter("max_radius", currentRadius * 2.0f);
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        ticksAlive++;
        ticksSinceLastEffect++;
        
        // Рост зоны со временем
        handleAreaGrowth();
        
        // Применяем эффекты к сущностям в зоне
        applyAreaEffects(level);
        
        // Проверяем время жизни
        float duration = context.getParameter(SpellParameters.DURATION, 300.0f);
        if (duration > 0 && ticksAlive > duration) {
            shouldDestroy = true;
        }
    }
    
    private void handleAreaGrowth() {
        float growthRate = context.getParameter(SpellParameters.GROWTH_RATE, 0.0f);
        if (growthRate > 0 && currentRadius < maxRadius) {
            float growthPerTick = growthRate * 0.05f;
            currentRadius = Math.min(maxRadius, currentRadius + growthPerTick);
            
            // Обновляем геометрию с новым радиусом
            context.setParameter(SpellParameters.RADIUS, currentRadius);
            geometry.update(geometry.getCenter(), Vec3.ZERO);
        }
    }
    
    private void applyAreaEffects(Level level) {
        float tickRate = context.getParameter(SpellParameters.TICK_RATE, 1.0f);
        int tickInterval = Math.max(1, (int)(20.0f / tickRate)); // Интервал в тиках
        
        if (ticksSinceLastEffect >= tickInterval) {
            ticksSinceLastEffect = 0;
            
            // Находим все сущности в зоне
            Set<Entity> currentEntities = findEntitiesInArea(level);
            
            // Применяем эффекты к сущностям
            for (Entity entity : currentEntities) {
                applyEffectToEntity(entity);
            }
            
            // Обновляем список сущностей
            entitiesInside.clear();
            entitiesInside.addAll(currentEntities);
        }
    }
    
    private Set<Entity> findEntitiesInArea(Level level) {
        Set<Entity> entities = new HashSet<>();
        Vec3 center = geometry.getCenter();
        
        // Поиск сущностей в радиусе (заглушка)
        // entities = level.getEntitiesOfClass(Entity.class, AABB.ofSize(center, currentRadius * 2))
        //     .stream().filter(e -> e.position().distanceTo(center) <= currentRadius)
        //     .collect(Collectors.toSet());
        
        return entities;
    }
    
    private void applyEffectToEntity(Entity entity) {
        // Урон или исцеление
        float damage = context.getParameter(SpellParameters.DAMAGE, 0.0f);
        float healing = context.getParameter(SpellParameters.HEALING, 0.0f);
        
        if (damage > 0) {
            // entity.hurt(..., damage)
        } else if (healing > 0) {
            // entity.heal(healing)
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
            // Замедление или заморозка
        }
        if (lightningElement > 0) {
            // Электрический урон или паралич
        }
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        // Для AREA коллизия означает вход в зону
        entitiesInside.add(entity);
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        // Зоны воздействия не взаимодействуют с блоками
        // Они накладываются на поверхность
    }
    
    @Override
    public boolean shouldContinue() {
        return !shouldDestroy && 
               context.caster().isAlive();
    }
    
    @Override
    public void cleanup() {
        entitiesInside.clear();
        
        // Финальный эффект при исчезновении зоны
        boolean hasFinaleEffect = context.getParameter("finale_effect", 0.0f) > 0;
        if (hasFinaleEffect) {
            // Взрыв или другой эффект при окончании
        }
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
    
    /**
     * Получить текущий радиус зоны
     */
    public float getCurrentRadius() {
        return currentRadius;
    }
    
    /**
     * Получить количество сущностей в зоне
     */
    public int getEntitiesCount() {
        return entitiesInside.size();
    }
}