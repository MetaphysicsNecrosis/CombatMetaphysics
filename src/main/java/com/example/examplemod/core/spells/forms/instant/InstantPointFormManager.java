package com.example.examplemod.core.spells.forms.instant;

import com.example.examplemod.core.spells.entities.SpellEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ЕДИНЫЙ менеджер для мгновенных точечных заклинаний (INSTANT_POINT)
 * Управляет мгновенными эффектами через единый SpellEntity корень
 * 
 * Принцип: SpellEntity создаётся на один тик, применяет все эффекты и исчезает
 */
public class InstantPointFormManager {
    
    // === ГЛОБАЛЬНОЕ СОСТОЯНИЕ МГНОВЕННЫХ ЭФФЕКТОВ ===
    private static final Map<UUID, InstantPointState> activeInstants = new ConcurrentHashMap<>();
    
    /**
     * Инициализировать мгновенное заклинание
     */
    public static void initializeInstantPoint(SpellEntity spellEntity) {
        UUID spellId = spellEntity.getSpellInstanceId();
        
        InstantPointState instantState = new InstantPointState();
        instantState.effectPosition = spellEntity.position();
        instantState.shouldApplyEffects = true;
        instantState.effectsApplied = false;
        
        // Определяем тип мгновенного эффекта
        String instantType = getInstantType(spellEntity);
        instantState.instantType = instantType;
        
        // Подготавливаем последовательность эффектов
        prepareEffectSequence(spellEntity, instantState);
        
        activeInstants.put(spellId, instantState);
    }
    
    /**
     * ЕДИНАЯ точка обработки мгновенной формы - вызывается из SpellEntity.tick()
     */
    public static void processInstantPointForm(SpellEntity spellEntity, Level level) {
        UUID spellId = spellEntity.getSpellInstanceId();
        InstantPointState instantState = activeInstants.get(spellId);
        
        if (instantState == null) {
            initializeInstantPoint(spellEntity);
            return;
        }
        
        // === ПРИМЕНЯЕМ ВСЕ ЭФФЕКТЫ В ОДИН ТИК ===
        if (instantState.shouldApplyEffects && !instantState.effectsApplied) {
            applyAllInstantEffects(spellEntity, level, instantState);
            instantState.effectsApplied = true;
            instantState.shouldApplyEffects = false;
        }
        
        // === ОБРАБОТКА ПОСЛЕДОВАТЕЛЬНЫХ ЭФФЕКТОВ ===
        if (!instantState.effectSequence.isEmpty()) {
            processEffectSequence(spellEntity, level, instantState);
        }
        
        // === ПРОВЕРКА ЗАВЕРШЕНИЯ ===
        if (isInstantComplete(instantState)) {
            completeInstantPoint(spellEntity);
        }
    }
    
    /**
     * Применить все мгновенные эффекты
     */
    private static void applyAllInstantEffects(SpellEntity spellEntity, Level level, InstantPointState instantState) {
        
        switch (instantState.instantType) {
            case "EXPLOSION" -> applyExplosionEffect(spellEntity, level, instantState);
            case "HEAL_BURST" -> applyHealBurstEffect(spellEntity, level, instantState);
            case "TELEPORT" -> applyTeleportEffect(spellEntity, level, instantState);
            case "SUMMON" -> applySummonEffect(spellEntity, level, instantState);
            case "TERRAIN_MODIFICATION" -> applyTerrainEffect(spellEntity, level, instantState);
            case "STATUS_AREA" -> applyStatusAreaEffect(spellEntity, level, instantState);
            case "RESOURCE_DRAIN" -> applyResourceDrainEffect(spellEntity, level, instantState);
            default -> applyGenericInstantEffect(spellEntity, level, instantState);
        }
        
        // Визуальные эффекты
        createInstantVisualEffect(spellEntity, level, instantState);
    }
    
    /**
     * Взрывной эффект - мгновенный урон в радиусе
     */
    private static void applyExplosionEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {
        float explosionRadius = spellEntity.getAppliedFloat("explosion_radius", 5.0f);
        float explosionDamage = spellEntity.getSpellDamage();
        
        // Находим всех в радиусе взрыва
        Vec3 center = instantState.effectPosition;
        var targets = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
            new net.minecraft.world.phys.AABB(
                center.x - explosionRadius, center.y - explosionRadius, center.z - explosionRadius,
                center.x + explosionRadius, center.y + explosionRadius, center.z + explosionRadius
            ),
            entity -> entity != spellEntity.getCaster() && center.distanceTo(entity.position()) <= explosionRadius
        );
        
        // Применяем урон с учётом расстояния
        for (var target : targets) {
            double distance = center.distanceTo(target.position());
            float damageMultiplier = 1.0f - (float)(distance / explosionRadius);
            float finalDamage = explosionDamage * damageMultiplier;
            
            var damageSource = level.damageSources().explosion(null, spellEntity.getCaster());
            target.hurt(damageSource, finalDamage);
        }
        
        // Разрушение блоков
        if (spellEntity.hasAppliedParameter("can_break_blocks")) {
            destroyBlocksInRadius(level, center, explosionRadius * 0.7f, spellEntity);
        }
    }
    
    /**
     * Лечащий всплеск - мгновенное лечение союзников
     */
    private static void applyHealBurstEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {
        float healRadius = spellEntity.getAppliedFloat("heal_radius", 8.0f);
        float healAmount = spellEntity.getAppliedFloat("heal_amount", 10.0f);
        
        Vec3 center = instantState.effectPosition;
        var targets = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
            new net.minecraft.world.phys.AABB(
                center.x - healRadius, center.y - healRadius, center.z - healRadius,
                center.x + healRadius, center.y + healRadius, center.z + healRadius
            ),
            entity -> center.distanceTo(entity.position()) <= healRadius && isAlly(entity, spellEntity)
        );
        
        for (var target : targets) {
            target.heal(healAmount);
        }
    }
    
    /**
     * Телепортация кастера
     */
    private static void applyTeleportEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {
        if (spellEntity.getCaster() != null) {
            Vec3 teleportPos = instantState.effectPosition;
            spellEntity.getCaster().teleportTo(teleportPos.x, teleportPos.y, teleportPos.z);
        }
    }
    
    /**
     * Обработка последовательных эффектов
     */
    private static void processEffectSequence(SpellEntity spellEntity, Level level, InstantPointState instantState) {
        if (instantState.sequenceTimer > 0) {
            instantState.sequenceTimer--;
            return;
        }
        
        if (!instantState.effectSequence.isEmpty()) {
            InstantEffect nextEffect = instantState.effectSequence.poll();
            applySequenceEffect(spellEntity, level, nextEffect);
            
            // Устанавливаем задержку до следующего эффекта
            instantState.sequenceTimer = nextEffect.delay;
        }
    }
    
    /**
     * Подготовить последовательность эффектов
     */
    private static void prepareEffectSequence(SpellEntity spellEntity, InstantPointState instantState) {
        
        // Проверяем наличие задержанных эффектов
        if (spellEntity.hasAppliedParameter("delayed_effects")) {
            
            // Огонь через 1 секунду
            if (spellEntity.hasAppliedParameter("fire_delay")) {
                int delay = (int)(spellEntity.getAppliedFloat("fire_delay", 1.0f) * 20);
                instantState.effectSequence.offer(new InstantEffect("FIRE", delay));
            }
            
            // Лёд через 2 секунды
            if (spellEntity.hasAppliedParameter("ice_delay")) {
                int delay = (int)(spellEntity.getAppliedFloat("ice_delay", 2.0f) * 20);
                instantState.effectSequence.offer(new InstantEffect("ICE", delay));
            }
            
            // Дополнительный урон через 0.5 секунд
            if (spellEntity.hasAppliedParameter("damage_delay")) {
                int delay = (int)(spellEntity.getAppliedFloat("damage_delay", 0.5f) * 20);
                instantState.effectSequence.offer(new InstantEffect("DAMAGE", delay));
            }
        }
    }
    
    /**
     * Определить тип мгновенного эффекта
     */
    private static String getInstantType(SpellEntity spellEntity) {
        
        // Проверяем параметры для определения типа
        if (spellEntity.hasAppliedParameter("explosion_radius")) return "EXPLOSION";
        if (spellEntity.hasAppliedParameter("heal_amount")) return "HEAL_BURST";
        if (spellEntity.hasAppliedParameter("teleport_target")) return "TELEPORT";
        if (spellEntity.hasAppliedParameter("summon_type")) return "SUMMON";
        if (spellEntity.hasAppliedParameter("terrain_type")) return "TERRAIN_MODIFICATION";
        if (spellEntity.hasAppliedParameter("status_effects")) return "STATUS_AREA";
        if (spellEntity.hasAppliedParameter("drain_type")) return "RESOURCE_DRAIN";
        
        return "GENERIC";
    }
    
    /**
     * Завершить мгновенное заклинание
     */
    private static void completeInstantPoint(SpellEntity spellEntity) {
        UUID spellId = spellEntity.getSpellInstanceId();
        activeInstants.remove(spellId);
        
        // Создаём финальный эффект
        createInstantCompleteEffect(spellEntity);
        
        // Уничтожаем SpellEntity
        spellEntity.discard();
    }
    
    /**
     * Проверить, завершено ли мгновенное заклинание
     */
    private static boolean isInstantComplete(InstantPointState instantState) {
        return instantState.effectsApplied && instantState.effectSequence.isEmpty();
    }
    
    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===
    
    private static boolean isAlly(net.minecraft.world.entity.LivingEntity entity, SpellEntity spellEntity) {
        // Простая проверка - можно расширить логикой команд/фракций
        return entity instanceof net.minecraft.world.entity.player.Player;
    }
    
    private static void destroyBlocksInRadius(Level level, Vec3 center, float radius, SpellEntity spellEntity) {
        int r = (int) Math.ceil(radius);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Vec3 blockPos = center.add(x, y, z);
                    if (center.distanceTo(blockPos) <= radius) {
                        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(blockPos);
                        var blockState = level.getBlockState(pos);
                        
                        float hardness = blockState.getDestroySpeed(level, pos);
                        float maxHardness = spellEntity.getAppliedFloat("max_block_hardness", 3.0f);
                        
                        if (hardness >= 0 && hardness <= maxHardness) {
                            level.destroyBlock(pos, true);
                        }
                    }
                }
            }
        }
    }
    
    private static void applySequenceEffect(SpellEntity spellEntity, Level level, InstantEffect effect) {
        // Применить конкретный эффект из последовательности
        switch (effect.type) {
            case "FIRE" -> {
                // Поджечь всех в радиусе
                float fireRadius = spellEntity.getAppliedFloat("fire_radius", 3.0f);
                int fireDuration = (int) spellEntity.getAppliedFloat("fire_duration", 5.0f);
                applyFireInRadius(level, spellEntity.position(), fireRadius, fireDuration);
            }
            case "ICE" -> {
                // Заморозить всех в радиусе
                float iceRadius = spellEntity.getAppliedFloat("ice_radius", 4.0f);
                applySlownessInRadius(level, spellEntity.position(), iceRadius, spellEntity);
            }
            case "DAMAGE" -> {
                // Дополнительный урон
                float extraDamage = spellEntity.getAppliedFloat("extra_damage", 5.0f);
                applyDamageInRadius(level, spellEntity.position(), 3.0f, extraDamage, spellEntity);
            }
        }
    }
    
    // Заглушки для конкретных эффектов
    private static void applyGenericInstantEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {}
    private static void applySummonEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {}
    private static void applyTerrainEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {}
    private static void applyStatusAreaEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {}
    private static void applyResourceDrainEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {}
    private static void createInstantVisualEffect(SpellEntity spellEntity, Level level, InstantPointState instantState) {}
    private static void createInstantCompleteEffect(SpellEntity spellEntity) {}
    private static void applyFireInRadius(Level level, Vec3 center, float radius, int duration) {}
    private static void applySlownessInRadius(Level level, Vec3 center, float radius, SpellEntity spellEntity) {}
    private static void applyDamageInRadius(Level level, Vec3 center, float radius, float damage, SpellEntity spellEntity) {}
    
    /**
     * Получить состояние мгновенного эффекта
     */
    public static InstantPointState getInstantState(UUID spellId) {
        return activeInstants.get(spellId);
    }
    
    /**
     * Очистить все мгновенные эффекты
     */
    public static void clearAllInstants() {
        activeInstants.clear();
    }
}