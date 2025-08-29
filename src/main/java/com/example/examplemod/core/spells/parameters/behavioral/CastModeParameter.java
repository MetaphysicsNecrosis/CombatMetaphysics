package com.example.examplemod.core.spells.parameters.behavioral;

import com.example.examplemod.core.spells.parameters.AbstractSpellParameter;
import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

import java.util.Map;

/**
 * СИСТЕМНЫЙ КОНТРОЛЛЕР режимов применения заклинаний
 * 
 * Это НЕ обычный параметр - это архитектурное решение, которое:
 * 1. Определяет ЖИЗНЕННЫЙ ЦИКЛ заклинания
 * 2. Контролирует расход маны (инициации vs усиления)
 * 3. Управляет QTE механиками
 * 4. Влияет на поведение при Silence/Interrupt
 * 5. Определяет durability восстановление барьеров
 * 6. Фиксирует/динамически меняет параметры заклинания
 * 
 * 3 режима из Concept.txt:
 * INSTANT_CAST: выстрел и забыл, автономное существование
 * MANA_SUSTAINED: постоянный drain маны, можно отменить
 * QTE_SUSTAINED: непрерывное QTE, динамический эффект
 */
public class CastModeParameter extends AbstractSpellParameter<String> {
    
    // Режимы каста из Concept.txt
    public static final String INSTANT_CAST = "INSTANT_CAST";
    public static final String MANA_SUSTAINED = "MANA_SUSTAINED";
    public static final String QTE_SUSTAINED = "QTE_SUSTAINED";
    
    public CastModeParameter() {
        super("cast_mode", String.class);
    }
    
    @Override
    public SpellComputationResult compute(SpellComputationContext context, Object inputValue) {
        
        String requestedMode = inputValue != null ? inputValue.toString() : INSTANT_CAST;
        
        // === АВТОМАТИЧЕСКОЕ ОПРЕДЕЛЕНИЕ РЕЖИМА ===
        
        String determinedMode = determineCastMode(context, requestedMode);
        
        // === ПРИМЕНЕНИЕ РЕЖИМА ===
        
        return switch (determinedMode) {
            case INSTANT_CAST -> SpellComputationResult.builder()
                    .putValue("mana_initiation_cost", calculateInitiationCost(context))
                    .putValue("cast_mode", "INSTANT_CAST")
                    .build();
            case MANA_SUSTAINED -> SpellComputationResult.builder()
                    .putValue("mana_initiation_cost", calculateInitiationCost(context))
                    .putValue("cast_mode", "MANA_SUSTAINED")
                    .build();
            case QTE_SUSTAINED -> SpellComputationResult.builder()
                    .putValue("mana_initiation_cost", calculateInitiationCost(context))
                    .putValue("cast_mode", "QTE_SUSTAINED")
                    .build();
            default -> SpellComputationResult.builder()
                    .putValue("mana_initiation_cost", calculateInitiationCost(context))
                    .putValue("cast_mode", "INSTANT_CAST")
                    .build();
        };
    }
    
    /**
     * КРИТИЧЕСКИ ВАЖНАЯ ЛОГИКА: Определение режима каста
     * Реализует правила из Concept.txt:
     * 1. Форма имеет предпочтительные режимы
     * 2. Параметры могут ФОРСИРОВАТЬ режим
     * 3. Выбор игрока учитывается если возможен
     * 4. Баланс: мощные комбинации = обязательное QTE
     */
    private String determineCastMode(SpellComputationContext context, String requestedMode) {
        
        String formType = context.getFormType();
        
        // === 1. ФОРМА ОПРЕДЕЛЯЕТ ПРЕДПОЧТЕНИЯ ===
        
        String formPreference = getFormPreference(formType);
        
        // === 2. ПАРАМЕТРЫ МОГУТ ФОРСИРОВАТЬ РЕЖИМ ===
        
        String forcedMode = checkForcedMode(context);
        if (forcedMode != null) {
            return forcedMode; // Принудительно
        }
        
        // === 3. БАЛАНС: МОЩНЫЕ КОМБИНАЦИИ = QTE ===
        
        if (requiresQTEForBalance(context)) {
            return QTE_SUSTAINED;
        }
        
        // === 4. ВЫБОР ИГРОКА (если форма позволяет) ===
        
        if (isValidModeForForm(formType, requestedMode)) {
            return requestedMode;
        }
        
        // === 5. FALLBACK НА ПРЕДПОЧТЕНИЕ ФОРМЫ ===
        return formPreference;
    }
    
    /**
     * Предпочтительный режим для каждой формы (из Concept.txt)
     */
    private String getFormPreference(String formType) {
        return switch (formType) {
            case "PROJECTILE", "INSTANT_POINT" -> INSTANT_CAST; // "выстрел и забыл"
            case "CHAIN" -> INSTANT_CAST; // Автономные переходы
            case "BEAM" -> MANA_SUSTAINED; // "BEAM тяготеет к поддержанию"
            case "AREA" -> MANA_SUSTAINED; // Постоянный эффект
            case "BARRIER" -> MANA_SUSTAINED; // Восстанавливающийся барьер
            case "WAVE" -> INSTANT_CAST; // Волна расширяется автономно
            case "TOUCH", "WEAPON_ENCHANT" -> MANA_SUSTAINED; // Длительные эффекты
            default -> INSTANT_CAST;
        };
    }
    
    /**
     * Проверка принудительных режимов из параметров
     */
    private String checkForcedMode(SpellComputationContext context) {
        
        // "параметр regeneration требует поддержания" (Concept.txt)
        if (context.hasParameter("regeneration") || 
            context.hasParameter("durability_regen_rate")) {
            return MANA_SUSTAINED;
        }
        
        // Барьеры с восстановлением ДОЛЖНЫ быть поддерживаемыми
        if ("BARRIER".equals(context.getFormType()) && 
            context.getFloat("durability", 0) > 0) {
            return MANA_SUSTAINED;
        }
        
        // Составные заклинания (PROJECTILE->AREA) остаются мгновенными
        if (context.hasParameter("composite_transition")) {
            return INSTANT_CAST;
        }
        
        return null; // Нет принуждения
    }
    
    /**
     * Проверка необходимости QTE для баланса
     */
    private boolean requiresQTEForBalance(SpellComputationContext context) {
        
        // Расчет "могущества" заклинания
        float spellPower = calculateSpellPower(context);
        
        // Мощные комбинации требуют QTE (Concept.txt)
        if (spellPower >= 150.0f) return true;
        
        // Сложные элементальные комбинации
        int elementCount = context.getElementalMix().size();
        if (elementCount >= 3) return true;
        
        // Высокие параметры урона + радиус
        float damage = context.getFloat("damage", 0);
        float radius = context.getFloat("radius", 0);
        if (damage > 100 && radius > 8) return true;
        
        return false;
    }
    
    /**
     * Проверка совместимости режима с формой
     */
    private boolean isValidModeForForm(String formType, String mode) {
        
        // Некоторые комбинации невозможны
        if ("PROJECTILE".equals(formType) && QTE_SUSTAINED.equals(mode)) {
            return false; // Снаряд не может требовать непрерывное QTE
        }
        
        if ("INSTANT_POINT".equals(formType) && !INSTANT_CAST.equals(mode)) {
            return false; // Мгновенные эффекты не могут поддерживаться
        }
        
        return true;
    }
    
    /**
     * МГНОВЕННЫЙ КАСТ - из Concept.txt
     */
    
    /**
     * ПОДДЕРЖИВАЕМЫЙ МАНОЙ - из Concept.txt
     */
    
    /**
     * ПОДДЕРЖИВАЕМЫЙ С QTE - из Concept.txt
     */
    
    /**
     * Расчет стоимости маны инициации
     */
    private float calculateInitiationCost(SpellComputationContext context) {
        
        float baseCost = 10.0f; // Базовая стоимость
        
        // Зависит от формы
        String formType = context.getFormType();
        float formMultiplier = switch (formType) {
            case "INSTANT_POINT" -> 1.5f;
            case "PROJECTILE" -> 1.0f;
            case "BEAM" -> 1.3f;
            case "AREA" -> 1.4f;
            case "BARRIER" -> 1.6f;
            case "WAVE" -> 1.2f;
            case "CHAIN" -> 1.1f;
            case "TOUCH", "WEAPON_ENCHANT" -> 0.8f;
            default -> 1.0f;
        };
        
        // Зависит от параметров
        float damageMultiplier = 1.0f + context.getFloat("damage", 0) * 0.01f;
        float sizeMultiplier = 1.0f + context.getFloat("radius", 1) * 0.1f;
        
        return baseCost * formMultiplier * damageMultiplier * sizeMultiplier;
    }
    
    /**
     * Расчет стоимости маны усиления
     */
    private float calculateAmplificationCost(SpellComputationContext context) {
        return calculateInitiationCost(context) * 0.6f;
    }
    
    /**
     * Расчет стоимости поддержания за тик
     */
    private float calculateUpkeepCost(SpellComputationContext context) {
        return calculateInitiationCost(context) * 0.05f; // 5% от инициации за тик
    }
    
    /**
     * Расчет "могущества" заклинания для определения QTE требования
     */
    private float calculateSpellPower(SpellComputationContext context) {
        
        float damage = context.getFloat("damage", 0);
        float radius = context.getFloat("radius", 1);
        float duration = context.getFloat("duration", 1);
        float range = context.getFloat("range", 1);
        
        // Формула могущества из Concept.txt
        float basePower = damage * 1.0f +                    // Урон важнее всего
                         (radius * radius) * 3.0f +         // Площадь воздействия
                         duration * 2.0f +                  // Время существования
                         range * 0.5f;                     // Дальность
        
        // Элементальные комбинации увеличивают сложность
        float elementalComplexity = context.getElementalMix().size() * 25.0f;
        
        // Специальные параметры
        float specialBonus = 0.0f;
        if (context.getFloat("pierce_count", 0) > 3) specialBonus += 20.0f;
        if (context.getFloat("bounce_count", 0) > 2) specialBonus += 15.0f;
        if (context.getFloat("homing_strength", 0) > 0.5f) specialBonus += 10.0f;
        
        return basePower + elementalComplexity + specialBonus;
    }
}