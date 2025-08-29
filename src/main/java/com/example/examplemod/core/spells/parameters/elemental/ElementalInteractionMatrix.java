package com.example.examplemod.core.spells.parameters.elemental;

import java.util.Map;
import java.util.HashMap;

/**
 * Матрица взаимодействий между элементами
 * Реализует систему из Concept.txt: "определение антагонистических модификаторов 
 * средством двух полносвязных вершин к каждому остальному элементу"
 * 
 * Пример: Вода -> Огонь = 10 (сильно подавляет), Огонь -> Вода = 2 (слабо подавляет)
 */
public class ElementalInteractionMatrix {
    
    // === БАЗОВЫЕ ЭЛЕМЕНТЫ ===
    public static final String FIRE = "fire";
    public static final String WATER = "water";
    public static final String EARTH = "earth";
    public static final String AIR = "air";
    public static final String ICE = "ice";
    public static final String LIGHTNING = "lightning";
    public static final String LIGHT = "light";
    public static final String SHADOW = "shadow";
    public static final String SPIRIT = "spirit";
    public static final String NATURE = "nature";
    
    // === МАТРИЦА ВЗАИМОДЕЙСТВИЙ ===
    // Ключ: "element1->element2", Значение: коэффициент влияния
    private static final Map<String, Float> INTERACTION_MATRIX = new HashMap<>();
    
    static {
        initializeInteractionMatrix();
    }
    
    /**
     * Инициализация матрицы взаимодействий
     */
    private static void initializeInteractionMatrix() {
        
        // === ОГОНЬ (FIRE) ===
        setInteraction(FIRE, WATER, 0.2f);    // Огонь слабо воздействует на воду
        setInteraction(FIRE, ICE, 8.0f);      // Огонь сильно плавит лед
        setInteraction(FIRE, EARTH, 3.0f);    // Огонь может плавить землю
        setInteraction(FIRE, AIR, 2.5f);      // Огонь создает горячий воздух
        setInteraction(FIRE, NATURE, 6.0f);   // Огонь сжигает природу
        setInteraction(FIRE, LIGHT, 1.8f);    // Огонь дает свет
        setInteraction(FIRE, SHADOW, 4.0f);   // Огонь рассеивает тьму
        setInteraction(FIRE, LIGHTNING, 1.0f); // Нейтральное взаимодействие
        setInteraction(FIRE, SPIRIT, 0.5f);   // Огонь слабо влияет на дух
        
        // === ВОДА (WATER) ===
        setInteraction(WATER, FIRE, 10.0f);   // Вода сильно тушит огонь
        setInteraction(WATER, ICE, 0.3f);     // Вода слабо воздействует на лед
        setInteraction(WATER, EARTH, 4.0f);   // Вода размывает землю
        setInteraction(WATER, AIR, 0.8f);     // Вода создает туман
        setInteraction(WATER, LIGHTNING, 7.0f); // Вода проводит молнию
        setInteraction(WATER, NATURE, 2.0f);  // Вода питает природу
        setInteraction(WATER, LIGHT, 0.6f);   // Вода преломляет свет
        setInteraction(WATER, SHADOW, 0.4f);  // Слабое воздействие
        setInteraction(WATER, SPIRIT, 1.2f);  // Вода очищает дух
        
        // === ЗЕМЛЯ (EARTH) ===
        setInteraction(EARTH, FIRE, 0.5f);    // Земля частично сопротивляется огню
        setInteraction(EARTH, WATER, 0.3f);   // Земля поглощает воду
        setInteraction(EARTH, AIR, 5.0f);     // Земля создает пыльные бури
        setInteraction(EARTH, ICE, 1.2f);     // Земля может покрыться льдом
        setInteraction(EARTH, LIGHTNING, 0.2f); // Земля заземляет молнию
        setInteraction(EARTH, NATURE, 3.0f);  // Земля питает природу
        setInteraction(EARTH, LIGHT, 0.1f);   // Земля поглощает свет
        setInteraction(EARTH, SHADOW, 2.5f);  // Земля создает тени
        setInteraction(EARTH, SPIRIT, 0.8f);  // Земля стабилизирует дух
        
        // === ВОЗДУХ (AIR) ===
        setInteraction(AIR, FIRE, 3.5f);      // Воздух раздувает огонь
        setInteraction(AIR, WATER, 2.0f);     // Воздух испаряет воду
        setInteraction(AIR, EARTH, 0.4f);     // Воздух слабо воздействует на землю
        setInteraction(AIR, ICE, 1.5f);       // Воздух может сдувать лед
        setInteraction(AIR, LIGHTNING, 4.0f); // Воздух проводит молнию
        setInteraction(AIR, NATURE, 1.8f);    // Воздух распространяет семена
        setInteraction(AIR, LIGHT, 1.2f);     // Воздух рассеивает свет
        setInteraction(AIR, SHADOW, 0.7f);    // Воздух развеивает тени
        setInteraction(AIR, SPIRIT, 2.5f);    // Воздух освобождает дух
        
        // === ЛЕД (ICE) ===
        setInteraction(ICE, FIRE, 0.1f);      // Лед очень слабо против огня
        setInteraction(ICE, WATER, 5.0f);     // Лед замораживает воду
        setInteraction(ICE, EARTH, 2.0f);     // Лед может заморозить землю
        setInteraction(ICE, AIR, 3.0f);       // Лед создает холодный воздух
        setInteraction(ICE, LIGHTNING, 0.6f); // Лед плохо проводит молнию
        setInteraction(ICE, NATURE, 4.0f);    // Лед замораживает природу
        setInteraction(ICE, LIGHT, 1.5f);     // Лед отражает свет
        setInteraction(ICE, SHADOW, 0.8f);    // Слабое воздействие
        setInteraction(ICE, SPIRIT, 1.0f);    // Лед замедляет дух
        
        // === МОЛНИЯ (LIGHTNING) ===
        setInteraction(LIGHTNING, FIRE, 1.5f);    // Молния может зажигать
        setInteraction(LIGHTNING, WATER, 9.0f);   // Молния сильно воздействует на воду
        setInteraction(LIGHTNING, EARTH, 0.1f);   // Молния заземляется
        setInteraction(LIGHTNING, AIR, 6.0f);     // Молния ионизирует воздух
        setInteraction(LIGHTNING, ICE, 3.0f);     // Молния разбивает лед
        setInteraction(LIGHTNING, NATURE, 5.0f);  // Молния может сжечь природу
        setInteraction(LIGHTNING, LIGHT, 4.0f);   // Молния создает вспышки света
        setInteraction(LIGHTNING, SHADOW, 7.0f);  // Молния рассеивает тьму
        setInteraction(LIGHTNING, SPIRIT, 2.0f);  // Молния возбуждает дух
        
        // === СВЕТ (LIGHT) ===
        setInteraction(LIGHT, FIRE, 2.0f);      // Свет усиливает огонь
        setInteraction(LIGHT, WATER, 1.2f);     // Свет может испарять воду
        setInteraction(LIGHT, EARTH, 0.8f);     // Свет освещает землю
        setInteraction(LIGHT, AIR, 0.9f);       // Свет рассеивается в воздухе
        setInteraction(LIGHT, ICE, 2.5f);       // Свет может растапливать лед
        setInteraction(LIGHT, LIGHTNING, 1.8f); // Свет синергирует с молнией
        setInteraction(LIGHT, NATURE, 3.0f);    // Свет питает природу
        setInteraction(LIGHT, SHADOW, 12.0f);   // Свет сильно рассеивает тьму
        setInteraction(LIGHT, SPIRIT, 4.0f);    // Свет очищает дух
        
        // === ТЬМА (SHADOW) ===
        setInteraction(SHADOW, FIRE, 0.3f);     // Тьма поглощает огонь
        setInteraction(SHADOW, WATER, 1.5f);    // Тьма может затемнять воду
        setInteraction(SHADOW, EARTH, 3.0f);    // Тьма проникает в землю
        setInteraction(SHADOW, AIR, 2.0f);      // Тьма наполняет воздух
        setInteraction(SHADOW, ICE, 2.2f);      // Тьма усиливает холод
        setInteraction(SHADOW, LIGHTNING, 0.2f); // Тьма поглощает молнию
        setInteraction(SHADOW, LIGHT, 0.1f);    // Тьма очень слабо против света
        setInteraction(SHADOW, NATURE, 4.0f);   // Тьма иссушает природу
        setInteraction(SHADOW, SPIRIT, 6.0f);   // Тьма подавляет дух
        
        // === ДУХ (SPIRIT) ===
        setInteraction(SPIRIT, FIRE, 1.8f);     // Дух может управлять огнем
        setInteraction(SPIRIT, WATER, 2.2f);    // Дух очищается водой
        setInteraction(SPIRIT, EARTH, 1.0f);    // Дух связан с землей
        setInteraction(SPIRIT, AIR, 3.0f);      // Дух свободен как воздух
        setInteraction(SPIRIT, ICE, 0.7f);      // Дух замедляется льдом
        setInteraction(SPIRIT, LIGHTNING, 2.5f); // Дух возбуждается молнией
        setInteraction(SPIRIT, LIGHT, 5.0f);    // Дух тянется к свету
        setInteraction(SPIRIT, SHADOW, 0.4f);   // Дух сопротивляется тьме
        setInteraction(SPIRIT, NATURE, 4.0f);   // Дух живет в природе
        
        // === ПРИРОДА (NATURE) ===
        setInteraction(NATURE, FIRE, 0.2f);     // Природа горит от огня
        setInteraction(NATURE, WATER, 4.0f);    // Природа растет от воды
        setInteraction(NATURE, EARTH, 3.5f);    // Природа растет из земли
        setInteraction(NATURE, AIR, 2.8f);      // Природа нуждается в воздухе
        setInteraction(NATURE, ICE, 0.3f);      // Природа замерзает от льда
        setInteraction(NATURE, LIGHTNING, 0.5f); // Природа страдает от молний
        setInteraction(NATURE, LIGHT, 5.0f);    // Природа нуждается в свете
        setInteraction(NATURE, SHADOW, 0.4f);   // Природа вянет в тени
        setInteraction(NATURE, SPIRIT, 6.0f);   // Природа полна духа
    }
    
    /**
     * Установить взаимодействие между элементами
     */
    private static void setInteraction(String from, String to, float coefficient) {
        INTERACTION_MATRIX.put(from + "->" + to, coefficient);
    }
    
    /**
     * Получить коэффициент воздействия одного элемента на другой
     */
    public static float getInteractionCoefficient(String fromElement, String toElement) {
        if (fromElement.equals(toElement)) {
            return 1.0f; // Элемент не воздействует на себя
        }
        
        String key = fromElement + "->" + toElement;
        return INTERACTION_MATRIX.getOrDefault(key, 1.0f); // 1.0f = нейтральное взаимодействие
    }
    
    /**
     * Получить взаимный коэффициент (среднее между A->B и B->A)
     */
    public static float getMutualInteractionCoefficient(String element1, String element2) {
        float coeff1 = getInteractionCoefficient(element1, element2);
        float coeff2 = getInteractionCoefficient(element2, element1);
        return (coeff1 + coeff2) / 2.0f;
    }
    
    /**
     * Проверить, является ли взаимодействие синергическим (оба элемента усиливают друг друга)
     */
    public static boolean isSynergistic(String element1, String element2) {
        float coeff1 = getInteractionCoefficient(element1, element2);
        float coeff2 = getInteractionCoefficient(element2, element1);
        return coeff1 > 1.5f && coeff2 > 1.5f;
    }
    
    /**
     * Проверить, является ли взаимодействие антагонистическим (элементы подавляют друг друга)
     */
    public static boolean isAntagonistic(String element1, String element2) {
        float coeff1 = getInteractionCoefficient(element1, element2);
        float coeff2 = getInteractionCoefficient(element2, element1);
        return (coeff1 < 0.5f && coeff2 > 2.0f) || (coeff1 > 2.0f && coeff2 < 0.5f);
    }
    
    /**
     * Получить доминирующий элемент во взаимодействии
     */
    public static String getDominantElement(String element1, String element2) {
        float coeff1to2 = getInteractionCoefficient(element1, element2);
        float coeff2to1 = getInteractionCoefficient(element2, element1);
        
        if (coeff1to2 > coeff2to1 * 1.5f) {
            return element1;
        } else if (coeff2to1 > coeff1to2 * 1.5f) {
            return element2;
        }
        return null; // Нет доминирующего элемента
    }
    
    /**
     * Рассчитать результирующую интенсивность элемента при взаимодействии
     */
    public static float calculateResultingIntensity(String targetElement, Map<String, Float> elementalMix) {
        float resultingIntensity = elementalMix.getOrDefault(targetElement, 0.0f);
        
        // Применяем влияние других элементов
        for (Map.Entry<String, Float> entry : elementalMix.entrySet()) {
            String sourceElement = entry.getKey();
            float sourceIntensity = entry.getValue();
            
            if (!sourceElement.equals(targetElement)) {
                float coefficient = getInteractionCoefficient(sourceElement, targetElement);
                float influence = sourceIntensity * (coefficient - 1.0f) * 0.1f; // 10% от влияния
                resultingIntensity += influence;
            }
        }
        
        return Math.max(0.0f, resultingIntensity);
    }
    
    /**
     * Получить все доступные элементы
     */
    public static String[] getAllElements() {
        return new String[]{
            FIRE, WATER, EARTH, AIR, ICE, 
            LIGHTNING, LIGHT, SHADOW, SPIRIT, NATURE
        };
    }
}