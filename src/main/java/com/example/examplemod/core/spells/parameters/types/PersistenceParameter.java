package com.example.examplemod.core.spells.parameters.types;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

/**
 * Параметр проходимости - определяет как заклинание взаимодействует с физическим миром
 * Ghost/Phantom/Physical из концепта
 */
public class PersistenceParameter extends AbstractSpellParameter<String> {
    
    public PersistenceParameter() {
        super("persistence_type", String.class);
    }
    
    @Override
    public SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        String persistenceType = inputValue != null ? inputValue.toString().toLowerCase() : "physical";
        
        // Нормализация типов
        persistenceType = switch (persistenceType) {
            case "ghost", "ghostly", "ethereal" -> "ghost";
            case "phantom", "phasing", "incorporeal" -> "phantom";
            case "physical", "solid", "material" -> "physical";
            default -> "physical";
        };
        
        // Вычисление свойств на основе типа проходимости
        boolean ignoreBlocks = persistenceType.equals("ghost");
        boolean ignoreEntities = persistenceType.equals("phantom");
        boolean canAffectMagic = !persistenceType.equals("physical");
        boolean canBreakBlocks = persistenceType.equals("phantom");
        boolean phaseThrough = persistenceType.equals("ghost");
        
        // Влияние на другие параметры
        float manaCostMultiplier = switch (persistenceType) {
            case "ghost" -> 1.3f; // Призрачность требует больше маны
            case "phantom" -> 1.2f; // Призрачность для живых сущностей
            case "physical" -> 1.0f; // Базовая стоимость
            default -> 1.0f;
        };
        
        float stabilityMultiplier = switch (persistenceType) {
            case "ghost" -> 0.8f; // Менее стабильно
            case "phantom" -> 0.9f; // Умеренно стабильно
            case "physical" -> 1.0f; // Полностью стабильно
            default -> 1.0f;
        };
        
        return buildResult()
            .putValue("persistence_type", persistenceType)
            .putValue("ignore_blocks", ignoreBlocks ? 1.0f : 0.0f)
            .putValue("ignore_entities", ignoreEntities ? 1.0f : 0.0f)
            .putValue("can_affect_magic", canAffectMagic ? 1.0f : 0.0f)
            .putValue("can_break_blocks", canBreakBlocks ? 1.0f : 0.0f)
            .putValue("phase_through", phaseThrough ? 1.0f : 0.0f)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("stability_multiplier", stabilityMultiplier)
            .putValue("persistence_type_value", persistenceType) // Основное значение
            .build();
    }
}