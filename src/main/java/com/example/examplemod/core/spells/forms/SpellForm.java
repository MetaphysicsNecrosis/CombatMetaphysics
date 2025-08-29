package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.parameters.ISpellParameter;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.instances.SpellInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Базовый интерфейс для всех форм заклинаний
 * Определяет геометрическо-пространственное проявление заклинания
 */
public interface SpellForm {
    
    /**
     * Получить тип формы заклинания
     */
    SpellFormType getType();
    
    /**
     * Проверить совместимость с определённым параметром
     */
    boolean isCompatibleWith(String parameterType);
    
    /**
     * Получить базовую стоимость маны для инициации этой формы
     */
    float getBaseManaInitiationCost();
    
    /**
     * Получить базовую стоимость маны для усиления этой формы
     */
    float getBaseManaAmplificationCost();
    
    /**
     * Получить время чтения заклинания для этой формы
     */
    int getBaseCastTime();
    
    /**
     * Проверить, требует ли эта форма обязательного QTE
     */
    boolean requiresQTE();
    
    /**
     * Получить тип проходимости по умолчанию для этой формы
     */
    PersistenceType getDefaultPersistenceType();
    
    /**
     * Принять применение параметра к этой форме
     * Использует паттерн Visitor для типобезопасного применения параметров
     * 
     * @param parameter Параметр для применения
     * @param value Значение параметра
     * @param allParameters Все параметры заклинания
     * @param instance Экземпляр заклинания
     * @param level Мир
     * @param caster Кастующий игрок
     */
    default void acceptParameter(ISpellParameter parameter, 
                                 Object value, 
                                 SpellParameters allParameters,
                                 SpellInstance instance,
                                 Level level, 
                                 Player caster) {
        // TODO: Применить параметр к форме заклинания
        // Упрощенная реализация без дженериков
        // parameter.apply(this, value, allParameters, instance, level, caster);
    }
}