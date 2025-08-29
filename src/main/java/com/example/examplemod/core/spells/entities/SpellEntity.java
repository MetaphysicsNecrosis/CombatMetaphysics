package com.example.examplemod.core.spells.entities;

import com.example.examplemod.core.spells.forms.SpellFormType;
import com.example.examplemod.core.spells.computation.SpellComputationTaskResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ЕДИНАЯ сущность для ВСЕХ форм заклинаний
 * Корень системы - все параметры применяются к этой сущности
 * 
 * Формы: PROJECTILE, BEAM, BARRIER, AREA, WAVE, TOUCH, WEAPON_ENCHANT, INSTANT_POINT, CHAIN
 */
public class SpellEntity extends Entity {
    
    // === DATA ACCESSORS для синхронизации с клиентом ===
    private static final EntityDataAccessor<String> FORM_TYPE = 
        SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> SPELL_DAMAGE = 
        SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPELL_SIZE = 
        SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOVEMENT_SPEED = 
        SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> PERSISTENCE_TYPE = 
        SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.STRING);
    
    // === СОСТОЯНИЕ ЗАКЛИНАНИЯ ===
    private UUID spellInstanceId;
    private Player caster;
    private SpellFormType formType;
    private int ticksExisted = 0;
    
    // === ВЫЧИСЛЕННЫЕ ПАРАМЕТРЫ (результат математической модели) ===
    private final Map<String, Object> appliedParameters = new ConcurrentHashMap<>();
    
    // === ПОВЕДЕНЧЕСКИЕ ПАРАМЕТРЫ ===
    private boolean ignoreBlocks = false;
    private boolean ignoreEntities = false;
    private int bounceCount = 0;
    private float bounceSpeedRetention = 0.8f;
    private float maxRange = 50.0f;
    private Vec3 startPosition;
    
    // === ВРЕМЕННЫЕ ПАРАМЕТРЫ ===
    private int maxLifetime = 200; // 10 секунд по умолчанию
    private boolean shouldDespawn = false;
    
    public SpellEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.startPosition = this.position();
    }
    
    public SpellEntity(EntityType<?> entityType, Level level, Player caster, SpellFormType formType, UUID spellId) {
        this(entityType, level);
        this.caster = caster;
        this.formType = formType;
        this.spellInstanceId = spellId;
        this.startPosition = this.position();
        
        // Устанавливаем базовые значения
        setFormType(formType.name());
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FORM_TYPE, "PROJECTILE");
        builder.define(SPELL_DAMAGE, 0.0f);
        builder.define(SPELL_SIZE, 1.0f);
        builder.define(MOVEMENT_SPEED, 1.0f);
        builder.define(PERSISTENCE_TYPE, "PHYSICAL");
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksExisted++;
        
        // === ЕДИНАЯ СИСТЕМА ЭФФЕКТОВ ===
        com.example.examplemod.core.spells.effects.UnifiedSpellEffectSystem.applyAllEffects(this, level());
        
        // === ПРОВЕРКА ВРЕМЕНИ ЖИЗНИ ===
        if (ticksExisted > maxLifetime || shouldDespawn) {
            this.discard();
        }
        
        // === ПРОВЕРКА ДАЛЬНОСТИ ===
        if (startPosition != null && position().distanceTo(startPosition) > maxRange) {
            this.discard();
        }
    }
    
    // === МЕТОДЫ ДЛЯ ПРИМЕНЕНИЯ РЕЗУЛЬТАТОВ ВЫЧИСЛЕНИЙ ===
    
    /**
     * ЕДИНАЯ точка применения всех параметров
     * Вызывается из SpellCoreModule после завершения вычислений
     * 
     * Интегрирует результаты ВСЕХ параметров модуля:
     * - Базовые: Damage, Healing, Duration, Range, Radius, Speed и др.
     * - Элементальные: Fire, Water, Earth и др. с взаимодействиями
     * - Поведенческие: CastMode, PersistenceType (системные контроллеры)
     */
    public void applyComputationResults(SpellComputationTaskResult result) {
        
        // Сохраняем все вычисленные значения
        appliedParameters.putAll(result.getAggregatedValues());
        
        // === БАЗОВЫЕ ПАРАМЕТРЫ ===
        
        // Урон (DamageParameter)
        if (result.hasAggregatedValue("total_damage")) {
            setSpellDamage(result.getAggregatedFloat("total_damage", 0.0f));
        }
        
        // Лечение (HealingParameter)
        if (result.hasAggregatedValue("healing_power")) {
            // Лечение обрабатывается в UnifiedSpellEffectSystem
        }
        
        // Размер/Радиус (RadiusParameter)
        if (result.hasAggregatedValue("spell_size")) {
            setSpellSize(result.getAggregatedFloat("spell_size", 1.0f));
        }
        
        // Скорость (SpeedParameter)
        if (result.hasAggregatedValue("movement_speed")) {
            float speed = result.getAggregatedFloat("movement_speed", 1.0f);
            setMovementSpeed(speed);
            applySpeedToMovement(speed);
        }
        
        // Дальность (RangeParameter)
        if (result.hasAggregatedValue("max_range")) {
            this.maxRange = result.getAggregatedFloat("max_range", 50.0f);
        }
        
        // Время жизни (DurationParameter)
        if (result.hasAggregatedValue("max_lifetime_ticks")) {
            this.maxLifetime = (int)result.getAggregatedFloat("max_lifetime_ticks", 200.0f);
        }
        
        // Отскоки (BounceCountParameter)
        if (result.hasAggregatedValue("bounce_count")) {
            this.bounceCount = (int)result.getAggregatedFloat("bounce_count", 0.0f);
            this.bounceSpeedRetention = result.getAggregatedFloat("bounce_speed_retention", 0.8f);
        }
        
        // Пробитие (PierceCountParameter)
        if (result.hasAggregatedValue("pierce_count")) {
            // Обрабатывается в коллизионной системе
        }
        
        // Самонаведение (HomingStrengthParameter)
        if (result.hasAggregatedValue("homing_strength")) {
            // Обрабатывается в системе движения
        }
        
        // Прочность (DurabilityParameter)
        if (result.hasAggregatedValue("spell_hp")) {
            // Для барьеров и защитных заклинаний
        }
        
        // Проникновение (PenetrationParameter)
        if (result.hasAggregatedValue("penetration_power")) {
            // Обрабатывается в системе урона
        }
        
        // Рост (GrowthRateParameter)
        if (result.hasAggregatedValue("growth_rate")) {
            // Обрабатывается в системе роста волн/зон
        }
        
        // Частота тиков (TickRateParameter)
        if (result.hasAggregatedValue("effect_tick_rate")) {
            // Обрабатывается в системе эффектов
        }
        
        // === ЭЛЕМЕНТАЛЬНЫЕ ПАРАМЕТРЫ ===
        
        // Огонь (FireElementParameter)
        if (result.hasAggregatedValue("fire_intensity")) {
            // Все элементальные эффекты обрабатываются в UnifiedSpellEffectSystem
        }
        
        // Другие элементы аналогично...
        
        // === ПОВЕДЕНЧЕСКИЕ ПАРАМЕТРЫ (СИСТЕМНЫЕ) ===
        
        // Режим каста (CastModeParameter) - КРИТИЧЕСКИ ВАЖНО!
        if (result.hasAggregatedValue("cast_mode_type")) {
            String castMode = result.getAggregatedString("cast_mode_type", "INSTANT_CAST");
            applyCastModeConfiguration(castMode, result);
        }
        
        // Тип проходимости (PersistenceTypeParameter) - КРИТИЧЕСКИ ВАЖНО!
        if (result.hasAggregatedValue("persistence_type")) {
            String persistenceType = result.getAggregatedString("persistence_type", "PHYSICAL");
            setPersistenceType(persistenceType);
            applyPersistenceType(persistenceType);
        }
    }
    
    /**
     * Применить скорость к движению сущности
     */
    private void applySpeedToMovement(float speed) {
        Vec3 currentMotion = getDeltaMovement();
        if (currentMotion.length() > 0) {
            Vec3 direction = currentMotion.normalize();
            setDeltaMovement(direction.scale(speed * 0.05)); // Масштабируем для Minecraft
        }
    }
    
    /**
     * Применить конфигурацию режима каста (СИСТЕМНЫЙ ПАРАМЕТР)
     * Настраивает поведение SpellEntity в зависимости от CastMode
     */
    private void applyCastModeConfiguration(String castMode, SpellComputationTaskResult result) {
        
        switch (castMode) {
            case "INSTANT_CAST" -> {
                // "После создания заклинание существует автономно"
                // "Не требует внимания заклинателя после каста"
                // Уже настроено по умолчанию
            }
            case "MANA_SUSTAINED" -> {
                // "Мана Усиления расходуется постоянно во время поддержания"
                // "Игрок может отменить заклинание в любой момент"
                // "При Silence заклинание продолжает работать, пока не кончится мана"
                // "Durability барьеров восстанавливается за счет текущего расхода маны"
                
                // TODO: Интеграция с системой маны и QTE
                // Пока сохраняем флаги для других систем
            }
            case "QTE_SUSTAINED" -> {
                // "Непрерывная QTE-игра во время всего существования заклинания"
                // "Мана Усиления расходуется динамически в зависимости от успешности QTE"
                // "Эффективность заклинания меняется в реальном времени от качества QTE"
                // "Durability восстанавливается пропорционально успешности QTE"
                
                // TODO: Интеграция с системой QTE
                // Пока сохраняем флаги для других систем
            }
        }
        
        // Сохраняем режим для других систем
        appliedParameters.put("cast_mode", castMode);
    }
    
    /**
     * Применить тип проходимости (СИСТЕМНЫЙ ПАРАМЕТР)
     * Настраивает коллизии SpellEntity в зависимости от PersistenceType
     */
    private void applyPersistenceType(String persistenceType) {
        
        switch (persistenceType.toUpperCase()) {
            case "GHOST" -> {
                // "проходит через физические препятствия, взаимодействует только с живыми"
                this.ignoreBlocks = true;
                this.ignoreEntities = false;
                this.setNoCollisionDetection(true);
                
                // Дополнительные флаги из PersistenceTypeParameter
                appliedParameters.put("collides_with_blocks", 0.0f);
                appliedParameters.put("collides_with_entities", 1.0f);
                appliedParameters.put("affects_only_living", 1.0f);
                appliedParameters.put("blocked_by_magic_resistance", 1.0f);
            }
            case "PHANTOM" -> {
                // "проходит через живые сущности, взаимодействует с неживыми"
                this.ignoreBlocks = false;
                this.ignoreEntities = true;
                this.setNoCollisionDetection(false);
                
                appliedParameters.put("collides_with_blocks", 1.0f);
                appliedParameters.put("collides_with_entities", 0.0f);
                appliedParameters.put("ignores_living_entities", 1.0f);
                appliedParameters.put("can_break_blocks", 1.0f);
            }
            case "PHYSICAL" -> {
                // "полная физическая коллизия со всеми объектами"
                this.ignoreBlocks = false;
                this.ignoreEntities = false;
                this.setNoCollisionDetection(false);
                
                appliedParameters.put("collides_with_blocks", 1.0f);
                appliedParameters.put("collides_with_entities", 1.0f);
                appliedParameters.put("can_be_blocked", 1.0f);
                appliedParameters.put("uses_ghost_vs_magic", 1.0f); // Особенность Physical из Concept.txt
            }
        }
        
        // Сохраняем тип для коллизионной системы
        appliedParameters.put("persistence_type", persistenceType);
    }
    
    // === ЛОГИКА ДЛЯ РАЗНЫХ ФОРМ ===
    
    private void tickProjectileForm() {
        // Движение по прямой с возможными отскоками
        // Коллизии с сущностями и блоками
    }
    
    private void tickBeamForm() {
        // Статичная позиция, raycast для поражения
        // Непрерывный урон по линии
    }
    
    private void tickBarrierForm() {
        // Статичная позиция, блокирует движение/урон
        // Может регенерировать прочность
    }
    
    private void tickAreaForm() {
        // Статичная позиция, постоянный эффект в радиусе
        // Tick-based применение эффектов
    }
    
    private void tickWaveForm() {
        // Расширяющаяся область поражения
        // Увеличение размера со временем
        float currentSize = getSpellSize();
        float growthRate = getAppliedFloat("growth_rate", 0.0f);
        if (growthRate > 0) {
            setSpellSize(currentSize + growthRate);
        }
    }
    
    private void tickTouchForm() {
        // Привязка к кастеру, активация при атаке
        if (caster != null) {
            this.setPos(caster.getX(), caster.getY(), caster.getZ());
        }
    }
    
    private void tickWeaponEnchantForm() {
        // Модификация оружия кастера
        // TODO: Интеграция с ItemStack энчантами
    }
    
    private void tickInstantPointForm() {
        // Мгновенный эффект в точке, затем исчезает
        this.shouldDespawn = true;
    }
    
    private void tickChainForm() {
        // Поиск следующих целей для цепной реакции
        // TODO: Система цепных переходов
    }
    
    // === GETTERS/SETTERS ===
    
    public String getFormType() { return this.entityData.get(FORM_TYPE); }
    public void setFormType(String formType) { this.entityData.set(FORM_TYPE, formType); }
    
    public float getSpellDamage() { return this.entityData.get(SPELL_DAMAGE); }
    public void setSpellDamage(float damage) { this.entityData.set(SPELL_DAMAGE, damage); }
    
    public float getSpellSize() { return this.entityData.get(SPELL_SIZE); }
    public void setSpellSize(float size) { this.entityData.set(SPELL_SIZE, size); }
    
    public float getMovementSpeed() { return this.entityData.get(MOVEMENT_SPEED); }
    public void setMovementSpeed(float speed) { this.entityData.set(MOVEMENT_SPEED, speed); }
    
    public String getPersistenceType() { return this.entityData.get(PERSISTENCE_TYPE); }
    public void setPersistenceType(String type) { this.entityData.set(PERSISTENCE_TYPE, type); }
    
    public Player getCaster() { return caster; }
    public UUID getSpellInstanceId() { return spellInstanceId; }
    
    public float getAppliedFloat(String key, float defaultValue) {
        Object value = appliedParameters.get(key);
        return value instanceof Number num ? num.floatValue() : defaultValue;
    }
    
    public boolean hasAppliedParameter(String key) { return appliedParameters.containsKey(key); }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("spell_id_most") && tag.contains("spell_id_least")) {
            long mostBits = tag.getLongOr("spell_id_most", 0L);
            long leastBits = tag.getLongOr("spell_id_least", 0L);
            spellInstanceId = new UUID(mostBits, leastBits);
        } else {
            spellInstanceId = UUID.randomUUID();
        }
        ticksExisted = tag.getIntOr("ticks_existed", 0);
        maxLifetime = tag.getIntOr("max_lifetime", 200);
        maxRange = tag.getFloatOr("max_range", 50.0f);
        shouldDespawn = tag.getBooleanOr("should_despawn", false);
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (spellInstanceId != null) {
            tag.putLong("spell_id_most", spellInstanceId.getMostSignificantBits());
            tag.putLong("spell_id_least", spellInstanceId.getLeastSignificantBits());
        }
        tag.putInt("ticks_existed", ticksExisted);
        tag.putInt("max_lifetime", maxLifetime);
        tag.putFloat("max_range", maxRange);
        tag.putBoolean("should_despawn", shouldDespawn);
    }
    
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource damageSource, float damage) {
        this.discard();
        return true;
    }
    
    public void setNoCollisionDetection(boolean value) {
        this.noPhysics = value;
    }
    
    // Поведенческие свойства
    public boolean isIgnoringBlocks() { return ignoreBlocks; }
    public void setIgnoreBlocks(boolean ignore) { this.ignoreBlocks = ignore; }
    
    public boolean isIgnoringEntities() { return ignoreEntities; }
    public void setIgnoreEntities(boolean ignore) { this.ignoreEntities = ignore; }
    
    public int getBounceCount() { return bounceCount; }
    public void setBounceCount(int count) { this.bounceCount = count; }
    
    public float getBounceSpeedRetention() { return bounceSpeedRetention; }
    public void setBounceSpeedRetention(float retention) { this.bounceSpeedRetention = retention; }
    
}