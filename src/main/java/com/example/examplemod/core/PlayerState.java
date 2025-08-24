package com.example.examplemod.core;

public enum PlayerState {
    // Базовые состояния (Gothic-style)
    PEACEFUL,           // Мирное состояние - оружие убрано, полная регенерация
    COMBAT_STANCE,      // Боевая стойка - оружие готово, замедленная регенерация
    
    // Атака - трехфазная система Gothic
    ATTACK_WINDUP,      // Замах (0.2-0.4s) - телеграфирование, уязвимость
    ATTACK_ACTIVE,      // Активная фаза (0.15-0.3s) - коллайдер урона активен
    ATTACK_RECOVERY,    // Восстановление (0.3-0.5s) - возврат в позицию
    
    // Блокирование и парирование
    BLOCKING,           // Активный блок - расход выносливости
    PARRYING,           // Окно парирования 100-200ms
    STUNNED,            // Оглушение после сломанного блока
    
    // Уклонение
    DODGING,            // Уклонение с i-frames (300ms неуязвимость)
    
    // Магические состояния (интегрированы с боевой системой)
    MAGIC_PREPARING,    // Подготовка заклинания
    MAGIC_CASTING,      // Процесс чтения
    QTE_TRANSITION,     // OSU-style QTE между заклинаниями
    
    // Состояния восстановления
    ATTACK_COOLDOWN,    // Короткий кулдаун после атаки
    COMBO_WINDOW,       // Окно для продолжения комбо
    EXHAUSTED,          // Истощение выносливости
    INTERRUPTED,
    
    // === ЗАГЛУШКИ ДЛЯ СОВМЕСТИМОСТИ СО СТАРЫМ КОДОМ ===
    // (будут удалены после полного перехода на Gothic систему)
    @Deprecated IDLE,
    @Deprecated MELEE_PREPARING, 
    @Deprecated MELEE_CHARGING,
    @Deprecated MELEE_ATTACKING,
    @Deprecated MELEE_RECOVERY,
    @Deprecated DEFENSIVE_ACTION,
    @Deprecated DEFENSIVE_RECOVERY,
    @Deprecated MAGIC_COOLDOWN,
    @Deprecated COOLDOWN;

    public boolean canTransitionTo(PlayerState newState) {
        return switch (this) {
            // Мирное состояние - переход в боевую стойку при угрозе
            case PEACEFUL -> newState == COMBAT_STANCE || newState == MAGIC_PREPARING || newState == INTERRUPTED;
            
            // Боевая стойка - центральное состояние для всех боевых действий
            case COMBAT_STANCE -> newState == ATTACK_WINDUP || newState == BLOCKING || newState == PARRYING || 
                                newState == DODGING || newState == MAGIC_PREPARING || newState == PEACEFUL || newState == INTERRUPTED;
            
            // Трехфазная атака Gothic (последовательная, не прерывается игроком)
            case ATTACK_WINDUP -> newState == ATTACK_ACTIVE || newState == INTERRUPTED || newState == STUNNED;
            case ATTACK_ACTIVE -> newState == ATTACK_RECOVERY || newState == INTERRUPTED;
            case ATTACK_RECOVERY -> newState == COMBO_WINDOW || newState == ATTACK_COOLDOWN;
            
            // Окно комбо - возможность продолжить атаку
            case COMBO_WINDOW -> newState == ATTACK_WINDUP || newState == ATTACK_COOLDOWN || newState == COMBAT_STANCE;
            
            // Защитные действия
            case BLOCKING -> newState == PARRYING || newState == STUNNED || newState == COMBAT_STANCE;
            case PARRYING -> newState == COMBAT_STANCE || newState == STUNNED || newState == ATTACK_WINDUP; // Контратака после парирования
            case DODGING -> newState == COMBAT_STANCE;
            case STUNNED -> newState == COMBAT_STANCE || newState == EXHAUSTED;
            
            // Магические состояния
            case MAGIC_PREPARING -> newState == MAGIC_CASTING || newState == INTERRUPTED;
            case MAGIC_CASTING -> newState == QTE_TRANSITION || newState == COMBAT_STANCE || newState == INTERRUPTED;
            case QTE_TRANSITION -> newState == MAGIC_CASTING || newState == COMBAT_STANCE || newState == INTERRUPTED;
            
            // Восстановление
            case ATTACK_COOLDOWN -> newState == COMBAT_STANCE;
            case EXHAUSTED -> newState == PEACEFUL || newState == COMBAT_STANCE;
            case INTERRUPTED -> newState == COMBAT_STANCE || newState == PEACEFUL;
            
            // === СОВМЕСТИМОСТЬ СО СТАРЫМ КОДОМ ===
            case IDLE -> true; // Старое состояние может переходить в любое
            case MELEE_PREPARING, MELEE_CHARGING, MELEE_ATTACKING, MELEE_RECOVERY -> true;
            case DEFENSIVE_ACTION, DEFENSIVE_RECOVERY -> true;
            case MAGIC_COOLDOWN, COOLDOWN -> true;
        };
    }

    public boolean isActive() {
        return this != PEACEFUL && this != INTERRUPTED;
    }

    public boolean canBeInterrupted() {
        // Фазы атаки НЕ могут быть прерваны игроком, но могут быть прерваны системой (урон)
        // Защитные действия НЕ прерываются вообще (кроме оглушения)
        return this == MAGIC_PREPARING || this == MAGIC_CASTING || this == QTE_TRANSITION ||
               this == ATTACK_WINDUP || this == COMBAT_STANCE;
    }
    
    public boolean isMagicState() {
        return this == MAGIC_PREPARING || this == MAGIC_CASTING || this == QTE_TRANSITION;
    }
    
    public boolean isAttackState() {
        return this == ATTACK_WINDUP || this == ATTACK_ACTIVE || this == ATTACK_RECOVERY;
    }
    
    public boolean isDefensiveState() {
        return this == BLOCKING || this == PARRYING || this == DODGING;
    }
    
    // Совместимость со старым кодом
    public boolean isMeleeState() {
        return isAttackState();
    }
    
    public boolean isCombatState() {
        return this == COMBAT_STANCE || isAttackState() || isDefensiveState() || isMagicState();
    }
    
    public boolean isVulnerable() {
        // Игрок уязвим во время замаха, особенно уязвим
        return this == ATTACK_WINDUP || this == ATTACK_RECOVERY || this == STUNNED || this == EXHAUSTED;
    }
    
    public boolean canRegenerateStamina() {
        // Полная регенерация только в мирном состоянии
        // Замедленная в боевой стойке
        // Остановка во время активных действий
        return this == PEACEFUL || this == COMBAT_STANCE;
    }
    
    public boolean canMove() {
        // Движение блокируется во время фаз атаки и некоторых магических состояний
        return this != ATTACK_WINDUP && this != ATTACK_ACTIVE && this != ATTACK_RECOVERY &&
               this != MAGIC_CASTING && this != STUNNED && this != EXHAUSTED;
    }
    
    public float getMovementSpeedMultiplier() {
        return switch (this) {
            case PEACEFUL -> 1.0f;
            case COMBAT_STANCE -> 0.7f;  // Замедленное движение в боевой стойке
            case BLOCKING -> 0.3f;       // Медленное движение при блоке
            case EXHAUSTED -> 0.5f;      // Истощение замедляет
            case ATTACK_WINDUP, ATTACK_ACTIVE, ATTACK_RECOVERY, MAGIC_CASTING, STUNNED -> 0.0f; // Нет движения
            default -> 0.8f;
        };
    }
    
    public float getStaminaRegenRate() {
        return switch (this) {
            case PEACEFUL -> 1.0f;           // Полная регенерация
            case COMBAT_STANCE -> 0.5f;      // Замедленная регенерация
            case BLOCKING -> -0.2f;          // Расход выносливости
            case ATTACK_WINDUP, ATTACK_ACTIVE, ATTACK_RECOVERY -> -0.3f; // Расход на атаку
            case DODGING -> -0.4f;           // Большой расход на уклонение
            case EXHAUSTED -> 0.3f;          // Медленное восстановление
            default -> 0.0f;                 // Остановка регенерации
        };
    }
    
    public long getTypicalDuration() {
        // Типичная продолжительность состояния в миллисекундах
        return switch (this) {
            case ATTACK_WINDUP -> 300;       // 0.2-0.4s
            case ATTACK_ACTIVE -> 200;       // 0.15-0.3s
            case ATTACK_RECOVERY -> 400;     // 0.3-0.5s
            case COMBO_WINDOW -> 600;        // 0.6s окно для комбо
            case PARRYING -> 150;            // 100-200ms окно парирования
            case DODGING -> 300;             // 300ms i-frames
            case BLOCKING -> 1500;           // 1.5s активного блока
            case ATTACK_COOLDOWN -> 500;     // Короткий кулдаун
            case MAGIC_CASTING -> 2000;      // 2s каст заклинания
            case QTE_TRANSITION -> 1200;     // 1.2s QTE окно
            case STUNNED -> 1000;            // 1s оглушение
            default -> Long.MAX_VALUE;       // Бессрочные состояния
        };
    }
    
    public CombatType getCombatType() {
        if (isMagicState()) return CombatType.MAGIC;
        if (isAttackState()) return CombatType.MELEE;
        if (isDefensiveState()) return CombatType.DEFENSIVE;
        return CombatType.NONE;
    }
    
    public enum CombatType {
        NONE, MAGIC, MELEE, DEFENSIVE
    }
}