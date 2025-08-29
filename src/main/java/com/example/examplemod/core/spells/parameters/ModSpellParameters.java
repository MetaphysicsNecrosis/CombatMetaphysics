package com.example.examplemod.core.spells.parameters;

import com.example.examplemod.core.spells.parameters.types.*;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

/**
 * Регистрация всех параметров заклинаний мода
 */
public class ModSpellParameters {
    
    // Регистрируем базовые параметры
    public static final Supplier<DamageParameter> DAMAGE = SpellParameterRegistry.register(
        "damage", DamageParameter::new);
    
    public static final Supplier<SizeParameter> GEOMETRY_SIZE = SpellParameterRegistry.register(
        "geometry_size", SizeParameter::new);
        
    public static final Supplier<PersistenceParameter> PERSISTENCE_TYPE = SpellParameterRegistry.register(
        "persistence_type", PersistenceParameter::new);
    
    // Можно добавить другие параметры
    // public static final Supplier<HealingParameter> HEALING = SpellParameterRegistry.register(
    //     "healing", HealingParameter::new);
    
    // public static final Supplier<DurationParameter> DURATION = SpellParameterRegistry.register(
    //     "duration", DurationParameter::new);
    
    // public static final Supplier<RangeParameter> RANGE = SpellParameterRegistry.register(
    //     "range", RangeParameter::new);
    
    /**
     * Инициализация системы параметров
     * Вызывается из главного класса мода
     */
    public static void init(IEventBus eventBus) {
        SpellParameterRegistry.SPELL_PARAMETERS.register(eventBus);
        
        // Подписываемся на события для построения кеша после регистрации
        eventBus.addListener(ModSpellParameters::onParametersRegistered);
    }
    
    /**
     * Обработка события завершения регистрации параметров
     */
    private static void onParametersRegistered(net.neoforged.neoforge.registries.RegisterEvent event) {
        if (event.getRegistryKey().equals(SpellParameterRegistry.SPELL_PARAMETERS_REGISTRY_KEY)) {
            SpellParameterRegistry.buildCache();
            System.out.println("Spell parameters cache built with " + 
                             SpellParameterRegistry.getRegistry().size() + " parameters");
        }
    }
}