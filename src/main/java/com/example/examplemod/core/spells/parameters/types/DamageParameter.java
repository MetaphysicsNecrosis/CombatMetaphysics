package com.example.examplemod.core.spells.parameters.types;

import com.example.examplemod.core.spells.parameters.ISpellParameter;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

/**
 * Thread-safe параметр урона
 * Соответствует архитектуре: Main Thread -> Spell Computation Pool -> Collision Thread
 */
public class DamageParameter implements ISpellParameter {
    
    @Override
    public SpellComputationResult compute(SpellComputationContext context, Object value) {
        float baseDamage = ((Number) value).floatValue();
        SpellComputationResult result = new SpellComputationResult(context.getSpellInstanceId(), getKey());
        
        // === THREAD-SAFE ВЫЧИСЛЕНИЯ (Worker Thread) ===
        
        // Базовый урон
        float finalDamage = baseDamage;
        
        // Взаимодействие с критом из параметров
        float critChance = context.getParameterFloat(SpellParameters.CRIT_CHANCE, 0.0f);
        float critMultiplier = 1.0f;
        if (critChance > 0) {
            critMultiplier = 1.0f + (critChance * 1.5f); // Крит увеличивает урон на 150%
            result.putComputedValue("crit_chance", critChance);
            result.putComputedValue("crit_multiplier", critMultiplier);
        }
        
        // Элементальные модификаторы (если есть)
        float elementalBonus = calculateElementalBonus(context);
        finalDamage *= (1.0f + elementalBonus);
        
        // Модификатор от состояния мира
        float weatherMultiplier = context.isRaining() ? 1.1f : 1.0f; // Дождь +10% урона
        finalDamage *= weatherMultiplier;
        
        // Модификатор времени (ночь/день)
        float timeMultiplier = (context.getWorldTime() % 24000 > 12000) ? 1.05f : 1.0f; // Ночь +5%
        finalDamage *= timeMultiplier;
        
        // === РЕЗУЛЬТАТЫ ДЛЯ РАЗНЫХ ПОТОКОВ ===
        
        // Для Main Thread (применение к форме)
        result.addFormModification("damage", finalDamage);
        result.addFormModification("collision_force", finalDamage * 0.1f);
        
        // Для Collision Thread (модификация коллизий)
        result.addCollisionModification("penetration_power", String.valueOf(finalDamage / 10.0f));
        
        if (finalDamage > 50.0f) {
            result.addCollisionModification("can_break_blocks", "true");
            result.addCollisionModification("max_block_hardness", String.valueOf(finalDamage / 25.0f));
        }
        
        // Для визуальных эффектов
        if (finalDamage > 30.0f) {
            result.markNeedsVisualsUpdate();
            result.putComputedValue("visual_intensity", Math.min(finalDamage / 100.0f, 1.0f));
        }
        
        // Сохраняем финальные значения
        result.putComputedValue("final_damage", finalDamage);
        result.putComputedValue("base_damage", baseDamage);
        result.putComputedValue("damage_multiplier", finalDamage / baseDamage);
        
        return result;
    }
    
    /**
     * Вычислить элементальный бонус урона (thread-safe)
     */
    private float calculateElementalBonus(SpellComputationContext context) {
        float bonus = 0.0f;
        
        // Проверяем элементальные параметры
        if (context.hasParameter("elemental_fire")) {
            float fireIntensity = context.getParameterFloat("elemental_fire", 0.0f);
            bonus += fireIntensity * 0.2f; // Огонь +20% за полную интенсивность
        }
        
        if (context.hasParameter("elemental_lightning")) {
            float lightningIntensity = context.getParameterFloat("elemental_lightning", 0.0f);
            bonus += lightningIntensity * 0.15f; // Молния +15%
        }
        
        return bonus;
    }
    
    @Override
    public String getKey() {
        return SpellParameters.DAMAGE;
    }
    
    @Override
    public Class<?> getValueType() {
        return Float.class;
    }
    
    @Override
    public int getComputationPriority() {
        return 10; // Высокий приоритет - урон базовый параметр
    }
    
    @Override
    public boolean isThreadSafe() {
        return true; // Все вычисления thread-safe
    }
    
    @Override
    public boolean requiresMainThreadData() {
        return false; // Не нужны Player/Level объекты
    }
}