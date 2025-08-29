package com.example.examplemod.core.spells.instances;

/**
 * Состояния жизненного цикла заклинания
 */
public enum SpellState {
    
    /**
     * Заклинание создано, но ещё не начато
     */
    CREATED("created"),
    
    /**
     * Идёт процесс чтения заклинания (cast time)
     */
    CASTING("casting"),
    
    /**
     * Заклинание активно и работает
     */
    ACTIVE("active"),
    
    /**
     * Заклинание поддерживается маной (channeling)
     */
    CHANNELING("channeling"),
    
    /**
     * Заклинание на паузе (прерывание или силенс)
     */
    PAUSED("paused"),
    
    /**
     * Заклинание успешно завершено
     */
    COMPLETED("completed"),
    
    /**
     * Заклинание отменено пользователем
     */
    CANCELLED("cancelled"),
    
    /**
     * Заклинание провалено (нехватка маны, QTE fail, etc)
     */
    FAILED("failed");

    private final String id;

    SpellState(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED;
    }

    public boolean canTransitionTo(SpellState newState) {
        return switch (this) {
            case CREATED -> newState == CASTING || newState == CANCELLED || newState == FAILED;
            case CASTING -> newState == ACTIVE || newState == CHANNELING || newState == PAUSED || 
                           newState == CANCELLED || newState == FAILED;
            case ACTIVE -> newState == COMPLETED || newState == PAUSED || 
                          newState == CANCELLED || newState == FAILED;
            case CHANNELING -> newState == ACTIVE || newState == PAUSED || 
                              newState == COMPLETED || newState == CANCELLED || newState == FAILED;
            case PAUSED -> newState == CASTING || newState == ACTIVE || newState == CHANNELING ||
                          newState == CANCELLED || newState == FAILED;
            case COMPLETED, CANCELLED, FAILED -> false; // Terminal states
        };
    }
}