package com.example.examplemod.core.spells.effects;

import com.example.examplemod.core.spells.computation.SpellComputationTaskResult;
import com.example.examplemod.core.spells.entities.SpellEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.ArrayList;

/**
 * Система применения результатов вычисления параметров к реальному миру Minecraft
 * Выполняется в MAIN THREAD после завершения всех вычислений
 * 
 * Мост: SpellComputationTaskResult -> Minecraft World Effects
 */
public class SpellEffectApplicator {
    
    /**
     * Применить все эффекты заклинания к миру (MAIN THREAD ONLY)
     */
    public static void applyEffectsToWorld(SpellComputationTaskResult result, 
                                          SpellEntity spellEntity, 
                                          Level level, 
                                          Player caster) {
        
        // === ПРИМЕНЕНИЕ УРОНА ===
        if (result.hasAggregatedValue("total_damage")) {
            float totalDamage = result.getAggregatedFloat("total_damage", 0.0f);
            applyDamageEffects(totalDamage, result, spellEntity, level, caster);
        }
        
        // === ПРИМЕНЕНИЕ ДВИЖЕНИЯ ===
        if (result.hasAggregatedValue("movement_speed")) {
            float speed = result.getAggregatedFloat("movement_speed", 0.0f);
            applyMovementEffects(speed, result, spellEntity, level);
        }
        
        // === ПРИМЕНЕНИЕ ПРОХОДИМОСТИ ===
        if (result.hasAggregatedValue("persistence_type")) {
            String persistenceType = result.getAggregatedString("persistence_type", "PHYSICAL");
            applyPersistenceEffects(persistenceType, result, spellEntity, level);
        }
        
        // === РАЗРУШЕНИЕ БЛОКОВ ===
        if (result.getCollisionModifications().containsKey("can_break_blocks")) {
            boolean canBreakBlocks = Boolean.parseBoolean(result.getCollisionModifications().get("can_break_blocks"));
            if (canBreakBlocks) {
                float hardness = Float.parseFloat(result.getCollisionModifications().getOrDefault("max_block_hardness", "1.0"));
                applyBlockBreaking(spellEntity, level, hardness);
            }
        }
    }
    
    /**
     * Применить урон к сущностям в области действия заклинания
     */
    private static void applyDamageEffects(float damage, SpellComputationTaskResult result, 
                                          SpellEntity spellEntity, Level level, Player caster) {
        
        // Получаем область поражения
        float effectRadius = result.getAggregatedFloat("effect_radius", 2.0f);
        Vec3 spellPos = spellEntity.position();
        
        // Ищем сущности в радиусе
        List<Entity> nearbyEntities = level.getEntitiesOfClass(Entity.class, 
            spellEntity.getBoundingBox().inflate(effectRadius),
            entity -> entity != caster && entity != spellEntity);
        
        // Создаём источник урона
        DamageSource spellDamage = createSpellDamageSource(level.damageSources(), caster, spellEntity);
        
        for (Entity target : nearbyEntities) {
            if (target instanceof LivingEntity livingTarget) {
                
                // Вычисляем финальный урон с учётом расстояния
                double distance = target.position().distanceTo(spellPos);
                float finalDamage = calculateDistanceBasedDamage(damage, distance, effectRadius);
                
                // Применяем критический удар если есть
                if (result.hasAggregatedValue("crit_chance")) {
                    float critChance = result.getAggregatedFloat("crit_chance", 0.0f);
                    float critMultiplier = result.getAggregatedFloat("crit_multiplier", 1.0f);
                    
                    if (level.getRandom().nextFloat() < critChance) {
                        finalDamage *= critMultiplier;
                        // TODO: Визуальный эффект критического удара
                    }
                }
                
                // Наносим урон
                livingTarget.hurt(spellDamage, finalDamage);
                boolean damaged = true;
                
                if (damaged) {
                    // Дополнительные эффекты после успешного урона
                    applyPostDamageEffects(result, livingTarget, spellEntity);
                }
            }
        }
    }
    
    /**
     * Применить эффекты движения к заклинанию
     */
    private static void applyMovementEffects(float speed, SpellComputationTaskResult result, 
                                           SpellEntity spellEntity, Level level) {
        
        // Устанавливаем скорость движения
        Vec3 currentMotion = spellEntity.getDeltaMovement();
        Vec3 direction = currentMotion.normalize();
        
        // Новая скорость
        Vec3 newMotion = direction.scale(speed * 0.05); // Масштабируем для Minecraft
        spellEntity.setDeltaMovement(newMotion);
        
        // Самонаведение если есть
        if (result.hasAggregatedValue("homing_strength")) {
            float homingStrength = result.getAggregatedFloat("homing_strength", 0.0f);
            if (homingStrength > 0) {
                applyHomingMovement(spellEntity, level, homingStrength);
            }
        }
        
        // Отскоки если есть
        if (result.hasAggregatedValue("bounce_count")) {
            int bounceCount = (int) result.getAggregatedFloat("bounce_count", 0.0f);
            if (bounceCount > 0) {
                spellEntity.setBounceCount(bounceCount);
                spellEntity.setBounceSpeedRetention(result.getAggregatedFloat("bounce_speed_retention", 0.8f));
            }
        }
    }
    
    /**
     * Применить эффекты проходимости (Ghost/Phantom/Physical)
     */
    private static void applyPersistenceEffects(String persistenceType, SpellComputationTaskResult result,
                                               SpellEntity spellEntity, Level level) {
        
        switch (persistenceType.toUpperCase()) {
            case "GHOST" -> {
                // Проходит через блоки, взаимодействует с живыми
                spellEntity.setIgnoreBlocks(true);
                spellEntity.setIgnoreEntities(false);
                spellEntity.setNoCollisionDetection(true);
            }
            case "PHANTOM" -> {
                // Проходит через живых, взаимодействует с блоками
                spellEntity.setIgnoreBlocks(false); 
                spellEntity.setIgnoreEntities(true);
                spellEntity.setNoCollisionDetection(false);
            }
            case "PHYSICAL" -> {
                // Полная физическая коллизия
                spellEntity.setIgnoreBlocks(false);
                spellEntity.setIgnoreEntities(false);
                spellEntity.setNoCollisionDetection(false);
            }
        }
    }
    
    /**
     * Разрушение блоков заклинанием
     */
    private static void applyBlockBreaking(SpellEntity spellEntity, Level level, float maxHardness) {
        Vec3 pos = spellEntity.position();
        BlockPos blockPos = BlockPos.containing(pos);
        
        // Проверяем блоки в области воздействия
        int radius = 2; // TODO: получить из параметров
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = blockPos.offset(x, y, z);
                    BlockState blockState = level.getBlockState(checkPos);
                    Block block = blockState.getBlock();
                    
                    // Проверяем твёрдость блока
                    float blockHardness = blockState.getDestroySpeed(level, checkPos);
                    
                    if (blockHardness >= 0 && blockHardness <= maxHardness) {
                        // Разрушаем блок
                        level.destroyBlock(checkPos, true); // true = дропает айтемы
                        
                        // TODO: Частицы разрушения
                        // TODO: Звук разрушения
                    }
                }
            }
        }
    }
    
    // === Вспомогательные методы ===
    
    private static DamageSource createSpellDamageSource(DamageSources damageSources, Player caster, SpellEntity spell) {
        // TODO: Создать кастомный DamageSource для магического урона
        return damageSources.magic(); // Временно используем магический урон
    }
    
    private static float calculateDistanceBasedDamage(float baseDamage, double distance, float maxRadius) {
        if (distance >= maxRadius) return 0.0f;
        
        // Урон уменьшается с расстоянием
        float distanceFactor = 1.0f - (float)(distance / maxRadius);
        return baseDamage * distanceFactor;
    }
    
    private static void applyPostDamageEffects(SpellComputationTaskResult result, LivingEntity target, SpellEntity spell) {
        // Отброс
        if (result.hasAggregatedValue("knockback_force")) {
            float knockback = result.getAggregatedFloat("knockback_force", 0.0f);
            if (knockback > 0) {
                Vec3 direction = target.position().subtract(spell.position()).normalize();
                target.setDeltaMovement(target.getDeltaMovement().add(direction.scale(knockback)));
            }
        }
        
        // Статус эффекты
        // TODO: Применить статус эффекты на основе результатов
    }
    
    private static void applyHomingMovement(SpellEntity spellEntity, Level level, float homingStrength) {
        // Поиск ближайшей цели
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
            spellEntity.getBoundingBox().inflate(16.0), // 16 блоков радиус поиска
            entity -> entity != spellEntity.getCaster());
        
        if (!targets.isEmpty()) {
            LivingEntity closestTarget = targets.get(0);
            double closestDistance = spellEntity.distanceTo(closestTarget);
            
            // Находим самую близкую цель
            for (LivingEntity target : targets) {
                double distance = spellEntity.distanceTo(target);
                if (distance < closestDistance) {
                    closestTarget = target;
                    closestDistance = distance;
                }
            }
            
            // Направление к цели
            Vec3 toTarget = closestTarget.position().subtract(spellEntity.position()).normalize();
            Vec3 currentMotion = spellEntity.getDeltaMovement();
            
            // Смешиваем текущее направление с направлением к цели
            Vec3 newMotion = currentMotion.scale(1.0f - homingStrength).add(toTarget.scale(homingStrength));
            spellEntity.setDeltaMovement(newMotion.normalize().scale(currentMotion.length()));
        }
    }
}