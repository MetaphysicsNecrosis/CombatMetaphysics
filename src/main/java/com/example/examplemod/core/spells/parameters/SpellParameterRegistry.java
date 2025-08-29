package com.example.examplemod.core.spells.parameters;

import com.example.examplemod.CombatMetaphysics;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Система регистрации параметров заклинаний для NeoForge
 * Использует кастомный реестр для типобезопасной регистрации параметров
 */
public class SpellParameterRegistry {
    
    // Создаём кастомный реестр для параметров заклинаний  
    public static final ResourceKey<Registry<ISpellParameter>> SPELL_PARAMETERS_REGISTRY_KEY = 
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(CombatMetaphysics.MODID, "spell_parameters"));
    
    // DeferredRegister для автоматической регистрации
    public static final DeferredRegister<ISpellParameter> SPELL_PARAMETERS = 
        DeferredRegister.create(SPELL_PARAMETERS_REGISTRY_KEY, CombatMetaphysics.MODID);
    
    // Локальный кеш для быстрого доступа по ключу
    private static final Map<String, ISpellParameter> parametersByKey = new ConcurrentHashMap<>();
    
    // Registry создается через makeRegistry
    public static final Registry<ISpellParameter> SPELL_PARAMETER_REGISTRY = 
        SPELL_PARAMETERS.makeRegistry(builder -> builder.sync(false));
    
    /**
     * Зарегистрировать новый параметр
     */
    public static <T extends ISpellParameter> Supplier<T> register(String name, Supplier<T> parameterSupplier) {
        return SPELL_PARAMETERS.register(name, parameterSupplier);
    }
    
    /**
     * Получить параметр по ключу (быстрый доступ через кеш)
     */
    public static ISpellParameter getParameterByKey(String key) {
        // Проверяем кеш
        ISpellParameter cached = parametersByKey.get(key);
        if (cached != null) {
            return cached;
        }
        
        // Ищем в реестре
        Registry<ISpellParameter> registry = SPELL_PARAMETER_REGISTRY;
        if (registry != null) {
            for (ISpellParameter parameter : registry) {
                if (parameter.getKey().equals(key)) {
                    parametersByKey.put(key, parameter); // Кешируем
                    return parameter;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Получить все зарегистрированные параметры
     */
    public static Registry<ISpellParameter> getRegistry() {
        return SPELL_PARAMETER_REGISTRY;
    }
    
    /**
     * Инициализация кеша после загрузки всех параметров
     * Вызывается из события регистрации
     */
    public static void buildCache() {
        parametersByKey.clear();
        Registry<ISpellParameter> registry = SPELL_PARAMETER_REGISTRY;
        if (registry != null) {
            for (ISpellParameter parameter : registry) {
                parametersByKey.put(parameter.getKey(), parameter);
            }
        }
    }
    
    /**
     * Проверить, существует ли параметр с данным ключом
     */
    public static boolean hasParameter(String key) {
        return getParameterByKey(key) != null;
    }
}