package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.SpellGeometry;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Поведение касания (TOUCH) - контактное воздействие
 * Накладывает способность на руку заклинателя
 * Активируется при ударе рукой или инструментом
 */
public class TouchBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private SpellGeometry geometry;
    private int ticksAlive = 0;
    private boolean shouldDestroy = false;
    private int chargesRemaining;
    private boolean isActive = true;
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = geometry;
        this.chargesRemaining = (int)context.getParameter("touch_charges", 5.0f);
        
        // Touch заклинания привязываются к заклинателю
        // Геометрия следует за рукой игрока
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        ticksAlive++;
        
        // Обновляем позицию touch-эффекта (следует за рукой игрока)
        updateTouchPosition();
        
        // Проверяем время жизни
        float duration = context.getParameter(SpellParameters.ENCHANTMENT_DURATION, 600.0f);
        if (ticksAlive > duration) {
            shouldDestroy = true;
        }
        
        // Проверяем количество оставшихся зарядов
        if (chargesRemaining <= 0) {
            shouldDestroy = true;
        }
    }
    
    private void updateTouchPosition() {
        // Обновляем геометрию возле руки игрока
        Vec3 handPosition = context.caster().getEyePosition()
            .add(context.caster().getViewVector(1.0f).scale(0.8f));
        
        geometry.update(handPosition, context.caster().getViewVector(1.0f));
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        if (!isActive || chargesRemaining <= 0) return;
        
        // Применяем touch-эффект
        applyTouchEffect(entity);
        
        // Тратим заряд
        chargesRemaining--;
        
        // Кулдаун между активациями
        isActive = false;
        // Через несколько тиков снова активируемся
    }
    
    private void applyTouchEffect(Entity entity) {
        float damage = context.getParameter(SpellParameters.DAMAGE, 15.0f);
        float healing = context.getParameter(SpellParameters.HEALING, 0.0f);
        
        // Усиленный урон от контакта
        float touchMultiplier = context.getParameter("touch_multiplier", 1.5f);
        float actualDamage = damage * touchMultiplier;
        
        if (actualDamage > 0) {
            // entity.hurt(..., actualDamage)
        } else if (healing > 0) {
            // entity.heal(healing * touchMultiplier)
        }
        
        // Специальные touch-эффекты
        applySpecialTouchEffects(entity);
        
        // Элементальные эффекты концентрированные
        applyConcentratedElementalEffects(entity);
    }
    
    private void applySpecialTouchEffects(Entity entity) {
        // Особые эффекты касания
        boolean hasLifeDrain = context.getParameter("life_drain", 0.0f) > 0;
        boolean hasManaBurn = context.getParameter("mana_burn", 0.0f) > 0;
        boolean hasParalyze = context.getParameter("paralyze", 0.0f) > 0;
        
        if (hasLifeDrain) {
            float drainAmount = context.getParameter("life_drain_amount", 5.0f);
            // Поглощаем здоровье и передаём заклинателю
            // entity.hurt(..., drainAmount)
            // context.caster().heal(drainAmount * 0.5f)
        }
        
        if (hasManaBurn) {
            // Сжигаем ману у цели (если это игрок)
            float burnAmount = context.getParameter("mana_burn_amount", 10.0f);
        }
        
        if (hasParalyze) {
            // Парализуем цель на короткое время
            int paralyzeDuration = (int)context.getParameter("paralyze_duration", 40.0f);
            // entity.addStatusEffect(paralysis, paralyzeDuration)
        }
    }
    
    private void applyConcentratedElementalEffects(Entity entity) {
        // Touch-заклинания имеют усиленные элементальные эффекты
        float elementMultiplier = 2.0f;
        
        float fireElement = context.getElement(SpellParameters.FIRE, 0.0f) * elementMultiplier;
        float iceElement = context.getElement(SpellParameters.ICE, 0.0f) * elementMultiplier;
        float lightningElement = context.getElement(SpellParameters.LIGHTNING, 0.0f) * elementMultiplier;
        
        if (fireElement > 0) {
            // entity.setSecondsOnFire((int)fireElement)
            // Дополнительный огненный урон
        }
        if (iceElement > 0) {
            // Мгновенная заморозка
        }
        if (lightningElement > 0) {
            // Электрический разряд с оглушением
        }
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        // Touch может разрушать блоки при касании
        float blockDamage = context.getParameter("block_damage", 0.0f);
        if (blockDamage > 0 && chargesRemaining > 0) {
            // level.destroyBlock(blockPos, true)
            chargesRemaining--;
        }
    }
    
    @Override
    public boolean shouldContinue() {
        return !shouldDestroy && 
               chargesRemaining > 0 && 
               context.caster().isAlive();
    }
    
    @Override
    public void cleanup() {
        // Эффекты при исчерпании зарядов
        isActive = false;
        
        // Визуальное уведомление об окончании enchant'а
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
    
    /**
     * Получить оставшиеся заряды
     */
    public int getChargesRemaining() {
        return chargesRemaining;
    }
    
    /**
     * Активировать touch-эффект (после кулдауна)
     */
    public void reactivate() {
        isActive = true;
    }
    
    /**
     * Проверить, активен ли touch-эффект
     */
    public boolean isActive() {
        return isActive && chargesRemaining > 0;
    }
}