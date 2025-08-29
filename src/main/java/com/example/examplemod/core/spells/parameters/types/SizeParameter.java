package com.example.examplemod.core.spells.parameters.types;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

/**
 * Параметр размера - влияет на геометрию заклинания
 */
public class SizeParameter extends AbstractSpellParameter<Float> {
    
    public SizeParameter() {
        super("size", Float.class);
    }
    
    @Override
    public SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        float size = parseFloat(inputValue, 1.0f);
        if (size <= 0) return emptyResult();
        
        // Большие заклинания медленнее
        float speedMultiplier = 1.0f / (1.0f + size * 0.1f);
        
        // Размер влияет на стоимость маны
        float manaCostMultiplier = 1.0f + size * size * 0.2f;
        
        // Точки коллизии
        int collisionPoints = (int) Math.max(4, size * 8);
        
        // Область поиска целей
        float targetSearchRadius = size * 2.0f;
        
        // Влияние на другие параметры
        float rangeMultiplier = 1.0f + size * 0.1f;
        float damageMultiplier = 1.0f + size * 0.05f;
        
        return buildResult()
            .putValue("size", size)
            .putValue("speed_multiplier", speedMultiplier)
            .putValue("mana_cost_multiplier", manaCostMultiplier)
            .putValue("collision_points", (float) collisionPoints)
            .putValue("target_search_radius", targetSearchRadius)
            .putValue("range_multiplier", rangeMultiplier)
            .putValue("damage_multiplier", damageMultiplier)
            .putValue("geometry_size", size)
            .build();
    }
}