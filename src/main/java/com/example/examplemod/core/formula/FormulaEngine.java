package com.example.examplemod.core.formula;

import com.example.examplemod.core.spells.parameters.SpellParameters;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FormulaEngine {
    
    private static FormulaEngine instance;
    private final FormulaCache cache;
    private final ParameterResolver resolver;
    private final Map<String, String> defaultFormulas;
    
    private FormulaEngine() {
        this.cache = new FormulaCache(2000);
        this.resolver = new ParameterResolver();
        this.defaultFormulas = new ConcurrentHashMap<>();
        initializeDefaultFormulas();
    }
    
    public static FormulaEngine getInstance() {
        if (instance == null) {
            synchronized (FormulaEngine.class) {
                if (instance == null) {
                    instance = new FormulaEngine();
                }
            }
        }
        return instance;
    }
    
    private void initializeDefaultFormulas() {
        defaultFormulas.put("mana_cost", "{base_damage} * 2 + {base_range} * 0.5 + {base_radius} * 1.5");
        defaultFormulas.put("channel_cost", "{mana_cost} * 0.1");
        defaultFormulas.put("cast_time", "20 + {mana_cost} * 0.05 / {cast_speed}");
        defaultFormulas.put("cooldown", "{cast_time} * 0.8");
        defaultFormulas.put("speed", "{base_speed} * {spell_power}");
        defaultFormulas.put("damage", "{base_damage} * {spell_power} * {global_magic_level}");
        defaultFormulas.put("healing", "{base_healing} * {spell_power} * {global_magic_level}");
        defaultFormulas.put("range", "{base_range} * {spell_power}");
        defaultFormulas.put("radius", "{base_radius} * {spell_power}");
        defaultFormulas.put("duration", "{base_duration} * {mana_efficiency}");
        defaultFormulas.put("elemental_damage", "{base_damage} * 0.5 * ({fire_element} + {ice_element} + {lightning_element})");
    }
    
    public double calculate(String formulaKey, SpellParameters spellParams, Player caster) {
        String formulaExpression = defaultFormulas.get(formulaKey);
        if (formulaExpression == null) {
            throw new IllegalArgumentException("Неизвестная формула: " + formulaKey);
        }
        
        CompiledFormula formula = cache.getOrCompile(formulaExpression);
        Map<String, Double> parameters = resolver.resolveParameters(spellParams, caster);
        
        return formula.evaluate(parameters);
    }
    
    public void registerFormula(String key, String expression) {
        cache.precompile(expression);
        defaultFormulas.put(key, expression);
    }
    
    public void precompileAllFormulas() {
        for (String expression : defaultFormulas.values()) {
            cache.precompile(expression);
        }
    }
    
    public boolean hasFormula(String key) {
        return defaultFormulas.containsKey(key);
    }
    
    public String getFormula(String key) {
        return defaultFormulas.get(key);
    }
    
    public void clearCache() {
        cache.clear();
    }
    
    public int getCacheSize() {
        return cache.size();
    }
}