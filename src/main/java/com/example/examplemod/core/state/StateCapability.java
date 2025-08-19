package com.example.examplemod.core.state;

/**
 * Базовая способность состояния в Event-Driven State Machine
 * Состояние игрока = композиция активных StateCapability
 * 
 * Примеры: CASTING, DEFENDING, MOVING, CHANNELING, etc.
 * Можно комбинировать: CASTING + DEFENDING одновременно
 */
public enum StateCapability {
    // Базовые способности
    IDLE("idle", 0, true),
    
    // Магические способности  
    CASTING("casting", 100, false),
    CHANNELING("channeling", 90, false),
    QTE_ACTIVE("qte_active", 200, false),
    
    // Боевые способности
    ATTACKING("attacking", 120, false), 
    DEFENDING("defending", 80, true),
    DODGING("dodging", 110, false),
    
    // Движение и позиционирование
    MOVING("moving", 10, true),
    SPRINTING("sprinting", 20, true),
    JUMPING("jumping", 30, true),
    
    // Взаимодействие с миром
    BUILDING("building", 5, true),
    MINING("mining", 15, true),
    
    // Особые состояния
    STUNNED("stunned", 300, false),
    INTERRUPTED("interrupted", 250, false),
    INVULNERABLE("invulnerable", 400, true);
    
    private final String id;
    private final int priority; // Чем выше, тем важнее для разрешения конфликтов
    private final boolean interruptible; // Можно ли прервать эту способность
    
    StateCapability(String id, int priority, boolean interruptible) {
        this.id = id;
        this.priority = priority;
        this.interruptible = interruptible;
    }
    
    public String getId() { return id; }
    public int getPriority() { return priority; }
    public boolean isInterruptible() { return interruptible; }
    
    /**
     * Может ли эта способность сосуществовать с другой
     */
    public boolean canCoexistWith(StateCapability other) {
        // Некоторые способности взаимоисключающие
        return switch (this) {
            case ATTACKING -> other != DEFENDING && other != DODGING && other != CASTING;
            case CASTING -> other != ATTACKING && other != DODGING;
            case DEFENDING -> other != ATTACKING && other != DODGING;
            case DODGING -> other != ATTACKING && other != DEFENDING && other != CASTING;
            case STUNNED, INTERRUPTED -> other == IDLE || other == MOVING; // Только базовые
            case INVULNERABLE -> true; // Может сочетаться с любым
            default -> true; // Остальные совместимы
        };
    }
    
    /**
     * Может ли эта способность прервать другую
     */
    public boolean canInterrupt(StateCapability other) {
        return other.isInterruptible() && this.priority > other.priority;
    }
    
    /**
     * Разрешения для различных действий
     */
    public boolean allowsAction(ActionType actionType) {
        return switch (this) {
            case STUNNED, INTERRUPTED -> false; // Ничего нельзя
            case CASTING, CHANNELING -> switch (actionType) {
                case MOVE, LOOK -> true;
                case ATTACK, BLOCK_PLACE, BLOCK_BREAK -> false;
                default -> false;
            };
            case ATTACKING -> switch (actionType) {
                case MOVE, LOOK, ATTACK -> true;
                case BLOCK_PLACE, BLOCK_BREAK -> false;
                default -> false;
            };
            case DEFENDING -> switch (actionType) {
                case MOVE, LOOK -> true;
                case ATTACK, BLOCK_PLACE, BLOCK_BREAK -> false;
                default -> true;
            };
            default -> true; // По умолчанию разрешаем все
        };
    }
    
    public enum ActionType {
        MOVE, LOOK, ATTACK, BLOCK_PLACE, BLOCK_BREAK, INTERACT, USE_ITEM
    }
}