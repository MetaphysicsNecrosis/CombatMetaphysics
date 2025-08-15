package com.example.examplemod.client.qte;

public enum QTEType {
    SEQUENCE,     // Нажать последовательность клавиш
    TIMING,       // Нажать в нужный момент
    RAPID,        // Быстро нажимать клавишу
    PRECISION;    // Попасть в определенную зону
    
    public float getDifficultyMultiplier(int chainPosition) {
        // Сложность растет с позицией в цепочке
        float baseMultiplier = switch (this) {
            case SEQUENCE -> 1.0f;
            case TIMING -> 1.2f;
            case RAPID -> 0.8f;
            case PRECISION -> 1.5f;
        };
        
        return baseMultiplier * (1.0f + chainPosition * 0.2f);
    }
}