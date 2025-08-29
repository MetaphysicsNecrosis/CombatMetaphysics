package com.example.examplemod.core.spells.effects;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;

/**
 * Абстрактный интерфейс для эффектов заклинаний
 * ОТДЕЛЁН от математической модели параметров
 * 
 * Мат. модель вычисляет -> ISpellEffect применяет -> Minecraft реализует
 */
public interface ISpellEffect {
    
    /**
     * Применить эффект к целям в мире
     * 
     * @param context Контекст применения эффекта
     * @param level Мир Minecraft
     */
    void apply(SpellEffectContext context, Level level);
    
    /**
     * Получить тип эффекта
     */
    String getEffectType();
    
    /**
     * Проверить, может ли эффект быть применён к данной цели
     */
    boolean canApplyTo(Entity target, SpellEffectContext context);
    
    /**
     * Получить приоритет применения эффекта
     */
    default int getPriority() {
        return 100;
    }
}