package com.example.examplemod.core.spells.effects.impl;

import com.example.examplemod.core.spells.effects.ISpellEffect;
import com.example.examplemod.core.spells.effects.SpellEffectContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;

import java.util.List;

/**
 * Эффект урона - ФИЗИЧЕСКАЯ РЕАЛИЗАЦИЯ
 * Получает ГОТОВЫЕ вычисленные значения и применяет их к миру Minecraft
 * 
 * НЕ содержит математики параметров - только логику применения урона
 */
public class DamageEffect implements ISpellEffect {
    
    @Override
    public void apply(SpellEffectContext context, Level level) {
        
        // === ПОЛУЧАЕМ ГОТОВЫЕ ЗНАЧЕНИЯ (из мат. модели) ===
        float damage = context.getFloat("total_damage", 0.0f);
        float effectRadius = context.getFloat("effect_radius", 2.0f);
        float critChance = context.getFloat("crit_chance", 0.0f);
        float critMultiplier = context.getFloat("crit_multiplier", 1.0f);
        boolean ignoreArmor = context.getBoolean("ignore_armor", false);
        
        if (damage <= 0) return; // Нет урона для применения
        
        // === ПОИСК ЦЕЛЕЙ В ОБЛАСТИ ===
        Vec3 effectPos = context.getEffectPosition();
        List<Entity> targets = findTargetsInRadius(level, effectPos, effectRadius, context);
        
        // === ПРИМЕНЕНИЕ УРОНА К КАЖДОЙ ЦЕЛИ ===
        for (Entity target : targets) {
            if (target instanceof LivingEntity livingTarget) {
                applyDamageToTarget(livingTarget, damage, critChance, critMultiplier, 
                                  ignoreArmor, context, level);
            }
        }
    }
    
    /**
     * Найти цели в радиусе поражения
     */
    private List<Entity> findTargetsInRadius(Level level, Vec3 center, float radius, SpellEffectContext context) {
        
        return level.getEntitiesOfClass(Entity.class, 
            new net.minecraft.world.phys.AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
            ),
            entity -> canApplyTo(entity, context)
        );
    }
    
    /**
     * Применить урон к конкретной цели
     */
    private void applyDamageToTarget(LivingEntity target, float baseDamage, 
                                   float critChance, float critMultiplier,
                                   boolean ignoreArmor, SpellEffectContext context, Level level) {
        
        // === РАССЧИТЫВАЕМ ФИНАЛЬНЫЙ УРОН ===
        float finalDamage = baseDamage;
        
        // Урон по расстоянию
        double distance = target.position().distanceTo(context.getEffectPosition());
        float maxRadius = context.getFloat("effect_radius", 2.0f);
        finalDamage *= calculateDistanceFalloff(distance, maxRadius);
        
        // Критический удар
        boolean isCritical = level.getRandom().nextFloat() < critChance;
        if (isCritical) {
            finalDamage *= critMultiplier;
            // TODO: Визуальный эффект критического урона
        }
        
        // === СОЗДАЁМ ИСТОЧНИК УРОНА ===
        DamageSource damageSource = createMagicDamageSource(level, context, ignoreArmor);
        
        // === ПРИМЕНЯЕМ УРОН ===
        target.hurt(damageSource, finalDamage);
        boolean wasHurt = true;
        
        if (wasHurt) {
            // Пост-эффекты после успешного урона
            applyPostDamageEffects(target, context, level, isCritical);
        }
    }
    
    /**
     * Расчёт ослабления урона по расстоянию
     */
    private float calculateDistanceFalloff(double distance, float maxRadius) {
        if (distance >= maxRadius) return 0.0f;
        
        // Линейное ослабление по расстоянию
        return 1.0f - (float)(distance / maxRadius);
    }
    
    /**
     * Создать источник магического урона
     */
    private DamageSource createMagicDamageSource(Level level, SpellEffectContext context, boolean ignoreArmor) {
        
        DamageSource baseSource = level.damageSources().magic();
        
        // TODO: Создать кастомный DamageSource для заклинаний
        // с поддержкой игнорирования брони, элементального типа и т.д.
        
        return baseSource;
    }
    
    /**
     * Применить дополнительные эффекты после урона
     */
    private void applyPostDamageEffects(LivingEntity target, SpellEffectContext context, 
                                       Level level, boolean wasCritical) {
        
        // Отброс
        float knockback = context.getFloat("knockback_force", 0.0f);
        if (knockback > 0) {
            Vec3 direction = target.position().subtract(context.getEffectPosition()).normalize();
            Vec3 knockbackVelocity = direction.scale(knockback * 0.1); // Масштабируем для Minecraft
            target.setDeltaMovement(target.getDeltaMovement().add(knockbackVelocity));
        }
        
        // Поджог от огненной магии
        if (context.hasValue("fire_duration")) {
            int fireDuration = (int) context.getFloat("fire_duration", 0.0f);
            if (fireDuration > 0) {
                target.setRemainingFireTicks(fireDuration * 20);
            }
        }
        
        // Замедление от ледяной магии
        if (context.hasValue("slowness_duration")) {
            int slownessDuration = (int) context.getFloat("slowness_duration", 0.0f);
            int slownessAmplifier = (int) context.getFloat("slowness_amplifier", 0.0f);
            
            if (slownessDuration > 0) {
                // TODO: Применить эффект замедления через PotionEffect
            }
        }
        
        // Визуальные эффекты
        if (wasCritical) {
            // TODO: Частицы критического урона
        }
        
        // TODO: Звуки урона в зависимости от типа магии
    }
    
    @Override
    public String getEffectType() {
        return "damage";
    }
    
    @Override
    public boolean canApplyTo(Entity target, SpellEffectContext context) {
        // Не наносим урон кастеру или неживым сущностям
        return target instanceof LivingEntity && 
               target != context.getCaster() && 
               target != context.getSpellEntity();
    }
    
    @Override
    public int getPriority() {
        return 50; // Средний приоритет - после перемещений, но до визуальных эффектов
    }
}