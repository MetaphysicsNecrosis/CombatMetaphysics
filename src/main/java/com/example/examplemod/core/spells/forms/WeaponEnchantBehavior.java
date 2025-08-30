package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.SpellGeometry;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Поведение зачарования оружия (WEAPON_ENCHANT) - наложение на оружие/конечности
 * Действует пока не исчерпается прочность или время
 * Активируется при каждом ударе оружием
 */
public class WeaponEnchantBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private SpellGeometry geometry;
    private int ticksAlive = 0;
    private boolean shouldDestroy = false;
    private float currentDurability;
    private int chargesUsed = 0;
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = geometry;
        this.currentDurability = context.getParameter(SpellParameters.DURABILITY, 100.0f);
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        ticksAlive++;
        
        // Проверяем время действия зачарования
        float duration = context.getParameter(SpellParameters.ENCHANTMENT_DURATION, 1200.0f);
        if (ticksAlive > duration) {
            shouldDestroy = true;
        }
        
        // Медленная деградация зачарования
        float degradationRate = context.getParameter("degradation_rate", 0.1f);
        currentDurability -= degradationRate * 0.05f; // За тик
        
        if (currentDurability <= 0) {
            shouldDestroy = true;
        }
        
        // Обновляем позицию зачарования (следует за оружием)
        updateEnchantPosition();
    }
    
    private void updateEnchantPosition() {
        // Геометрия следует за рукой/оружием игрока
        Vec3 weaponPosition = context.caster().getEyePosition()
            .add(context.caster().getViewVector(1.0f).scale(1.2f));
        
        geometry.update(weaponPosition, context.caster().getViewVector(1.0f));
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        if (currentDurability <= 0) return;
        
        // Зачарование активируется при ударе оружием
        applyEnchantmentEffect(entity);
        
        // Снижаем прочность зачарования при использовании
        float usageCost = context.getParameter("usage_durability_cost", 5.0f);
        currentDurability -= usageCost;
        chargesUsed++;
        
        // Эффект становится слабее с использованием
        float efficiencyLoss = context.getParameter("efficiency_loss", 0.02f);
        float newEfficiency = Math.max(0.1f, 1.0f - chargesUsed * efficiencyLoss);
        context.setParameter("current_efficiency", newEfficiency);
    }
    
    private void applyEnchantmentEffect(Entity entity) {
        float efficiency = context.getParameter("current_efficiency", 1.0f);
        float damage = context.getParameter(SpellParameters.DAMAGE, 10.0f) * efficiency;
        float healing = context.getParameter(SpellParameters.HEALING, 0.0f) * efficiency;
        
        if (damage > 0) {
            // Дополнительный магический урон к обычному урону оружия
            // entity.hurt(..., damage)
        }
        
        if (healing > 0) {
            // Исцеление владельца при ударе (вампиризм)
            // context.caster().heal(healing)
        }
        
        // Применяем элементальные эффекты зачарования
        applyEnchantmentElementalEffects(entity, efficiency);
        
        // Специальные эффекты зачарования
        applySpecialEnchantmentEffects(entity, efficiency);
    }
    
    private void applyEnchantmentElementalEffects(Entity entity, float efficiency) {
        float fireElement = context.getElement(SpellParameters.FIRE, 0.0f) * efficiency;
        float iceElement = context.getElement(SpellParameters.ICE, 0.0f) * efficiency;
        float lightningElement = context.getElement(SpellParameters.LIGHTNING, 0.0f) * efficiency;
        
        if (fireElement > 0) {
            // entity.setSecondsOnFire((int)(fireElement * 2))
            // Огненное оружие
        }
        if (iceElement > 0) {
            // Замораживающие удары
        }
        if (lightningElement > 0) {
            // Электрические разряды при ударе
        }
    }
    
    private void applySpecialEnchantmentEffects(Entity entity, float efficiency) {
        boolean hasLifeSteal = context.getParameter("life_steal", 0.0f) > 0;
        boolean hasManaSteal = context.getParameter("mana_steal", 0.0f) > 0;
        boolean hasArmorPierce = context.getParameter("armor_pierce", 0.0f) > 0;
        
        if (hasLifeSteal) {
            float stealAmount = context.getParameter("life_steal_amount", 3.0f) * efficiency;
            // context.caster().heal(stealAmount)
        }
        
        if (hasManaSteal) {
            float manaSteal = context.getParameter("mana_steal_amount", 5.0f) * efficiency;
            // Восстанавливаем ману заклинателю
        }
        
        if (hasArmorPierce) {
            // Игнорируем часть брони цели
            float piercePercent = context.getParameter("armor_pierce_percent", 0.3f);
        }
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        // Зачарование может воздействовать на блоки при ударе
        float blockDamage = context.getParameter("block_damage", 0.0f);
        if (blockDamage > 0) {
            // Ускоренная добыча или разрушение блоков
            currentDurability -= 1.0f;
        }
    }
    
    @Override
    public boolean shouldContinue() {
        return !shouldDestroy && 
               currentDurability > 0 && 
               context.caster().isAlive();
    }
    
    @Override
    public void cleanup() {
        // Эффекты при исчезновении зачарования
        // Визуальное уведомление игроку
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
    
    /**
     * Получить оставшуюся прочность зачарования
     */
    public float getCurrentDurability() {
        return currentDurability;
    }
    
    /**
     * Получить количество использований
     */
    public int getChargesUsed() {
        return chargesUsed;
    }
    
    /**
     * Получить текущую эффективность
     */
    public float getCurrentEfficiency() {
        return context.getParameter("current_efficiency", 1.0f);
    }
}