package com.example.examplemod.core.resources;

import com.example.examplemod.core.spells.SpellDefinition;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.forms.SpellForm;
import com.example.examplemod.core.spells.forms.SpellFormType;
import net.minecraft.world.entity.player.Player;

/**
 * Вычисление расхода ресурсов для заклинаний
 * Учитывает форму, параметры, характеристики игрока и синергии
 */
public class ResourceCalculator {
    
    // Базовые множители для различных форм
    private static final float PROJECTILE_BASE_COST = 1.0f;
    private static final float BEAM_BASE_COST = 1.5f;
    private static final float BARRIER_BASE_COST = 2.0f;
    private static final float AREA_BASE_COST = 1.2f;
    private static final float WAVE_BASE_COST = 1.3f;
    private static final float TOUCH_BASE_COST = 0.8f;
    private static final float WEAPON_ENCHANT_BASE_COST = 1.1f;
    private static final float INSTANT_POINT_BASE_COST = 0.9f;
    private static final float CHAIN_BASE_COST = 1.4f;

    /**
     * Рассчитать стоимость маны инициации
     */
    public static float calculateInitiationCost(SpellDefinition definition, Player caster) {
        SpellFormType form = definition.form();
        SpellParameters params = definition.parameters();
        
        // Базовая стоимость формы
        float baseCost = getBaseManaInitiationCost(form);
        
        // Модификаторы от параметров
        float damage = params.getFloat(SpellParameters.DAMAGE, 0f);
        float healing = params.getFloat(SpellParameters.HEALING, 0f);
        float range = params.getFloat(SpellParameters.RANGE, 1f);
        float radius = params.getFloat(SpellParameters.RADIUS, 1f);
        float duration = params.getFloat(SpellParameters.DURATION, 1f);
        
        // Формула расчёта (базируется на концептуальных требованиях)
        float parameterCost = (damage + healing) * 0.1f + 
                             range * 0.05f + 
                             radius * radius * 0.02f + 
                             duration * 0.03f;
        
        float totalCost = baseCost + parameterCost;
        
        // Модификаторы от характеристик игрока
        totalCost *= getPlayerManaEfficiency(caster);
        
        return Math.max(totalCost, 1f); // Минимум 1 мана
    }

    /**
     * Рассчитать стоимость маны усиления
     */
    public static float calculateAmplificationCost(SpellDefinition definition, Player caster, float qteAccuracy) {
        SpellFormType form = definition.form();
        SpellParameters params = definition.parameters();
        
        float baseCost = getBaseManaAmplificationCost(form);
        
        // QTE accuracy влияет на эффективность использования маны усиления
        float qteMultiplier = 0.5f + (qteAccuracy * 0.5f); // От 0.5x до 1.0x эффективности
        
        // Параметры влияющие на стоимость усиления
        float amplifyFactor = params.getFloat(SpellParameters.AMPLIFY_FACTOR, 1f);
        float pierceCount = params.getFloat(SpellParameters.PIERCE_COUNT, 0f);
        float bounceCount = params.getFloat(SpellParameters.BOUNCE_COUNT, 0f);
        
        float enhancementCost = amplifyFactor * 2f + 
                               pierceCount * 1.5f + 
                               bounceCount * 1.2f;
        
        float totalCost = (baseCost + enhancementCost) * qteMultiplier;
        
        // Модификаторы от игрока
        totalCost *= getPlayerManaEfficiency(caster);
        
        return totalCost;
    }

    /**
     * Рассчитать стоимость поддержания маны в секунду
     */
    public static float calculateChannelCost(SpellDefinition definition, Player caster) {
        SpellParameters params = definition.parameters();
        
        float baseCost = params.getFloat(SpellParameters.CHANNEL_COST_PER_SECOND, 5f);
        
        // Размер и длительность влияют на стоимость поддержания
        float radius = params.getFloat(SpellParameters.RADIUS, 1f);
        float durability = params.getFloat(SpellParameters.DURABILITY, 1f);
        
        float maintainCost = baseCost + 
                           radius * radius * 0.1f + 
                           durability * 0.05f;
        
        return maintainCost * getPlayerManaEfficiency(caster);
    }
    
    private static float getBaseManaInitiationCost(SpellFormType formType) {
        return switch (formType) {
            case PROJECTILE -> 10.0f;
            case BEAM -> 15.0f;
            case BARRIER -> 20.0f;
            case AREA -> 12.0f;
            case WAVE -> 18.0f;
            case TOUCH -> 5.0f;
            case WEAPON_ENCHANT -> 8.0f;
            case INSTANT_POINT -> 25.0f;
            case CHAIN -> 15.0f;
        };
    }
    
    private static float getBaseManaAmplificationCost(SpellFormType formType) {
        return switch (formType) {
            case PROJECTILE -> 5.0f;
            case BEAM -> 8.0f;
            case BARRIER -> 10.0f;
            case AREA -> 6.0f;
            case WAVE -> 9.0f;
            case TOUCH -> 3.0f;
            case WEAPON_ENCHANT -> 4.0f;
            case INSTANT_POINT -> 12.0f;
            case CHAIN -> 7.0f;
        };
    }

    /**
     * Проверить доступность ресурсов для заклинания
     */
    public static ResourceAvailability checkAvailability(SpellDefinition definition, Player caster, ManaPool manaPool) {
        float initiationCost = calculateInitiationCost(definition, caster);
        float amplificationCost = calculateAmplificationCost(definition, caster, 1.0f); // Максимальная стоимость
        
        boolean hasInitiation = manaPool.hasInitiation(initiationCost);
        boolean hasAmplification = manaPool.hasAmplification(amplificationCost);
        
        return new ResourceAvailability(
            hasInitiation,
            hasAmplification,
            initiationCost,
            amplificationCost,
            hasInitiation && hasAmplification
        );
    }

    /**
     * Получить эффективность использования маны игроком
     * В будущем будет зависеть от характеристик/навыков игрока
     */
    private static float getPlayerManaEfficiency(Player player) {
        // Пока возвращаем базовое значение
        // В будущем здесь будут учитываться:
        // - Уровень магических навыков
        // - Экипировка
        // - Активные баффы/дебаффы
        // - Усталость персонажа
        return 1.0f;
    }

    /**
     * Результат проверки доступности ресурсов
     */
    public record ResourceAvailability(
        boolean hasInitiation,
        boolean hasAmplification,
        float requiredInitiation,
        float requiredAmplification,
        boolean canCast
    ) {}
}