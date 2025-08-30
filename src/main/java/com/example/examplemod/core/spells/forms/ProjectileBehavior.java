package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.SpellGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Поведение снарядов (PROJECTILE)
 * Линейное движение с возможностью рикошета и пробивания
 */
public class ProjectileBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private SpellGeometry geometry;
    private Vec3 velocity;
    private int ticksAlive = 0;
    private int pierceCount = 0;
    private int bounceCount = 0;
    private boolean shouldDestroy = false;
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = geometry;
        
        float speed = context.getParameter("speed", 1.0f);
        this.velocity = context.direction().scale(speed);
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        ticksAlive++;
        
        // Проверяем дальность
        float range = context.getParameter("range", 20.0f);
        if (geometry.getCenter().distanceTo(context.origin()) > range) {
            shouldDestroy = true;
            return;
        }
        
        // Применяем самонаведение
        applyHoming();
        
        // Двигаемся
        Vec3 currentPos = geometry.getCenter();
        Vec3 newPos = currentPos.add(velocity.scale(0.05)); // 1 tick = 0.05 секунды
        geometry.update(newPos, velocity.normalize());
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        // Наносим урон
        float damage = context.getParameter("damage", 10.0f);
        // entity.hurt(...) - реализация зависит от системы урона
        
        // Проверяем пробивание
        float maxPierce = context.getParameter("pierce_count", 0.0f);
        if (pierceCount < maxPierce) {
            pierceCount++;
            // Продолжаем движение с уменьшенным уроном
            velocity = velocity.scale(0.8f);
        } else {
            shouldDestroy = true;
        }
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        float maxBounce = context.getParameter("bounce_count", 0.0f);
        
        if (bounceCount < maxBounce) {
            bounceCount++;
            // Отражаем скорость (упрощенная физика)
            velocity = velocity.scale(-0.7f);
        } else {
            shouldDestroy = true;
        }
    }
    
    private void applyHoming() {
        float homingStrength = context.getParameter("homing_strength", 0.0f);
        if (homingStrength <= 0 || context.target() == null) return;
        
        Vec3 currentPos = geometry.getCenter();
        Vec3 targetPos = context.target().position();
        Vec3 toTarget = targetPos.subtract(currentPos).normalize();
        
        // Интерполируем направление к цели
        velocity = velocity.normalize().lerp(toTarget, homingStrength * 0.1f).scale(velocity.length());
    }
    
    @Override
    public boolean shouldContinue() {
        return !shouldDestroy && ticksAlive < 1200; // 60 секунд максимум
    }
    
    @Override
    public void cleanup() {
        // Создаем эффекты при уничтожении если нужно
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
}