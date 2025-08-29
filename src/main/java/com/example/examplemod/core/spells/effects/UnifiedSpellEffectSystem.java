package com.example.examplemod.core.spells.effects;

import com.example.examplemod.core.spells.entities.SpellEntity;
import com.example.examplemod.core.spells.computation.SpellComputationTaskResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;

import java.util.List;

/**
 * ЕДИНАЯ система применения всех эффектов заклинаний
 * Все параметры работают через одну SpellEntity
 * 
 * Принцип: "Один корень - все эффекты"
 */
public class UnifiedSpellEffectSystem {
    
    /**
     * ЕДИНАЯ точка применения ВСЕХ эффектов заклинания
     * Вызывается каждый тик для SpellEntity
     */
    public static void applyAllEffects(SpellEntity spellEntity, Level level) {
        
        // === ПРОВЕРЯЕМ КАКИЕ ЭФФЕКТЫ НУЖНО ПРИМЕНИТЬ ===
        
        // Урон - если есть и форма поражает цели
        if (spellEntity.getSpellDamage() > 0 && canDealDamage(spellEntity)) {
            applyDamageEffect(spellEntity, level);
        }
        
        // Движение - если есть скорость и форма движется
        if (spellEntity.getMovementSpeed() > 0 && canMove(spellEntity)) {
            applyMovementEffect(spellEntity, level);
        }
        
        // Разрушение блоков - если включено
        if (spellEntity.hasAppliedParameter("can_break_blocks")) {
            applyBlockBreaking(spellEntity, level);
        }
        
        // Лечение - если есть параметр лечения
        if (spellEntity.hasAppliedParameter("healing_power")) {
            applyHealingEffect(spellEntity, level);
        }
        
        // Статус эффекты - если есть элементальные параметры
        applyElementalEffects(spellEntity, level);
        
        // Специальные эффекты форм
        applyFormSpecificEffects(spellEntity, level);
    }
    
    // === УРОН (ЕДИНАЯ РЕАЛИЗАЦИЯ ДЛЯ ВСЕХ ФОРМ) ===
    
    private static void applyDamageEffect(SpellEntity spellEntity, Level level) {
        float damage = spellEntity.getSpellDamage();
        float effectRadius = spellEntity.getSpellSize() * 2.0f; // Размер определяет радиус
        
        // Находим цели в зависимости от формы
        List<LivingEntity> targets = findTargetsForForm(spellEntity, level, effectRadius);
        
        for (LivingEntity target : targets) {
            if (target == spellEntity.getCaster()) continue; // Не вредим кастеру
            
            // Рассчитываем финальный урон
            float finalDamage = calculateFinalDamage(damage, spellEntity, target);
            
            // Создаём источник урона
            DamageSource damageSource = level.damageSources().magic(); // TODO: кастомный источник
            
            // Наносим урон
            target.hurt(damageSource, finalDamage);
            boolean wasHurt = true;
            
            if (wasHurt) {
                // Дополнительные эффекты после урона
                applyPostDamageEffects(spellEntity, target, level);
            }
        }
    }
    
    // === ДВИЖЕНИЕ (ЕДИНАЯ РЕАЛИЗАЦИЯ ДЛЯ ВСЕХ ДВИЖУЩИХСЯ ФОРМ) ===
    
    private static void applyMovementEffect(SpellEntity spellEntity, Level level) {
        String formType = spellEntity.getFormType();
        
        switch (formType) {
            case "PROJECTILE" -> {
                // Прямолинейное движение с возможными отскоками
                applyProjectileMovement(spellEntity, level);
            }
            case "WAVE" -> {
                // Расширение волны
                applyWaveExpansion(spellEntity, level);
            }
            case "CHAIN" -> {
                // Перемещение к следующей цели
                applyChainMovement(spellEntity, level);
            }
            // Другие формы не двигаются
        }
    }
    
    private static void applyProjectileMovement(SpellEntity spellEntity, Level level) {
        Vec3 currentMotion = spellEntity.getDeltaMovement();
        float speed = spellEntity.getMovementSpeed();
        
        // Самонаведение
        if (spellEntity.hasAppliedParameter("homing_strength")) {
            float homingStrength = spellEntity.getAppliedFloat("homing_strength", 0.0f);
            currentMotion = applyHoming(spellEntity, level, currentMotion, homingStrength);
        }
        
        // Применяем движение
        Vec3 newMotion = currentMotion.normalize().scale(speed * 0.05);
        spellEntity.setDeltaMovement(newMotion);
    }
    
    private static void applyWaveExpansion(SpellEntity spellEntity, Level level) {
        // Волна расширяется, а не движется
        float currentSize = spellEntity.getSpellSize();
        float growthRate = spellEntity.getAppliedFloat("growth_rate", 0.1f);
        float maxSize = spellEntity.getAppliedFloat("max_size", 10.0f);
        
        if (currentSize < maxSize) {
            spellEntity.setSpellSize(Math.min(currentSize + growthRate, maxSize));
        }
    }
    
    // === РАЗРУШЕНИЕ БЛОКОВ (ЕДИНАЯ РЕАЛИЗАЦИЯ) ===
    
    private static void applyBlockBreaking(SpellEntity spellEntity, Level level) {
        float maxHardness = spellEntity.getAppliedFloat("max_block_hardness", 1.0f);
        float breakRadius = spellEntity.getSpellSize();
        
        Vec3 pos = spellEntity.position();
        int radius = (int) Math.ceil(breakRadius);
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    
                    Vec3 blockVec = pos.add(x, y, z);
                    if (pos.distanceTo(blockVec) > breakRadius) continue;
                    
                    BlockPos blockPos = BlockPos.containing(blockVec);
                    BlockState blockState = level.getBlockState(blockPos);
                    
                    float hardness = blockState.getDestroySpeed(level, blockPos);
                    if (hardness >= 0 && hardness <= maxHardness) {
                        level.destroyBlock(blockPos, true); // Дропает итемы
                    }
                }
            }
        }
    }
    
    // === ЭЛЕМЕНТАЛЬНЫЕ ЭФФЕКТЫ (ЕДИНАЯ РЕАЛИЗАЦИЯ) ===
    
    private static void applyElementalEffects(SpellEntity spellEntity, Level level) {
        
        // Огонь - поджог
        if (spellEntity.hasAppliedParameter("fire_intensity")) {
            float fireIntensity = spellEntity.getAppliedFloat("fire_intensity", 0.0f);
            if (fireIntensity > 0) {
                applyFireEffect(spellEntity, level, fireIntensity);
            }
        }
        
        // Лед - замедление
        if (spellEntity.hasAppliedParameter("ice_intensity")) {
            float iceIntensity = spellEntity.getAppliedFloat("ice_intensity", 0.0f);
            if (iceIntensity > 0) {
                applyIceEffect(spellEntity, level, iceIntensity);
            }
        }
        
        // Молния - цепная реакция
        if (spellEntity.hasAppliedParameter("lightning_intensity")) {
            float lightningIntensity = spellEntity.getAppliedFloat("lightning_intensity", 0.0f);
            if (lightningIntensity > 0) {
                applyLightningEffect(spellEntity, level, lightningIntensity);
            }
        }
    }
    
    // === СПЕЦИАЛЬНЫЕ ЭФФЕКТЫ ФОРМ ===
    
    private static void applyFormSpecificEffects(SpellEntity spellEntity, Level level) {
        String formType = spellEntity.getFormType();
        
        switch (formType) {
            case "BEAM" -> applyBeamEffect(spellEntity, level);
            case "BARRIER" -> applyBarrierEffect(spellEntity, level);
            case "AREA" -> applyAreaEffect(spellEntity, level);
            case "TOUCH" -> applyTouchEffect(spellEntity, level);
            case "WEAPON_ENCHANT" -> applyWeaponEnchantEffect(spellEntity, level);
            case "INSTANT_POINT" -> com.example.examplemod.core.spells.forms.instant.InstantPointFormManager.processInstantPointForm(spellEntity, level);
            case "CHAIN" -> com.example.examplemod.core.spells.forms.chain.ChainFormManager.processChainForm(spellEntity, level);
        }
    }
    
    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===
    
    private static boolean canDealDamage(SpellEntity spellEntity) {
        String form = spellEntity.getFormType();
        return !form.equals("WEAPON_ENCHANT"); // Все формы кроме энчанта могут наносить урон
    }
    
    private static boolean canMove(SpellEntity spellEntity) {
        String form = spellEntity.getFormType();
        return form.equals("PROJECTILE") || form.equals("CHAIN") || form.equals("WAVE");
    }
    
    private static List<LivingEntity> findTargetsForForm(SpellEntity spellEntity, Level level, float radius) {
        AABB searchBox = new AABB(
            spellEntity.getX() - radius, spellEntity.getY() - radius, spellEntity.getZ() - radius,
            spellEntity.getX() + radius, spellEntity.getY() + radius, spellEntity.getZ() + radius
        );
        
        return level.getEntitiesOfClass(LivingEntity.class, searchBox,
            entity -> entity != spellEntity.getCaster());
    }
    
    private static float calculateFinalDamage(float baseDamage, SpellEntity spellEntity, LivingEntity target) {
        float finalDamage = baseDamage;
        
        // Расстояние от центра заклинания
        double distance = spellEntity.distanceTo(target);
        float maxRadius = spellEntity.getSpellSize() * 2.0f;
        
        if (distance > 0 && distance < maxRadius) {
            // Урон уменьшается с расстоянием
            float distanceFactor = 1.0f - (float)(distance / maxRadius);
            finalDamage *= distanceFactor;
        }
        
        // Критический удар
        if (spellEntity.hasAppliedParameter("crit_chance")) {
            float critChance = spellEntity.getAppliedFloat("crit_chance", 0.0f);
            float critMultiplier = spellEntity.getAppliedFloat("crit_multiplier", 1.5f);
            
            if (spellEntity.level().getRandom().nextFloat() < critChance) {
                finalDamage *= critMultiplier;
            }
        }
        
        return finalDamage;
    }
    
    // Заглушки для специфических эффектов - будут реализованы позже
    private static void applyPostDamageEffects(SpellEntity spellEntity, LivingEntity target, Level level) {
        // Отметить, что цель обработана для цепных заклинаний
        if ("CHAIN".equals(spellEntity.getFormType())) {
            com.example.examplemod.core.spells.forms.chain.ChainFormManager.markTargetProcessed(spellEntity);
        }
    }
    private static Vec3 applyHoming(SpellEntity spellEntity, Level level, Vec3 currentMotion, float strength) { return currentMotion; }
    private static void applyChainMovement(SpellEntity spellEntity, Level level) {}
    private static void applyFireEffect(SpellEntity spellEntity, Level level, float intensity) {}
    private static void applyIceEffect(SpellEntity spellEntity, Level level, float intensity) {}
    private static void applyLightningEffect(SpellEntity spellEntity, Level level, float intensity) {}
    private static void applyBeamEffect(SpellEntity spellEntity, Level level) {}
    private static void applyBarrierEffect(SpellEntity spellEntity, Level level) {}
    private static void applyAreaEffect(SpellEntity spellEntity, Level level) {}
    private static void applyTouchEffect(SpellEntity spellEntity, Level level) {}
    private static void applyWeaponEnchantEffect(SpellEntity spellEntity, Level level) {}
    private static void applyHealingEffect(SpellEntity spellEntity, Level level) {}
}