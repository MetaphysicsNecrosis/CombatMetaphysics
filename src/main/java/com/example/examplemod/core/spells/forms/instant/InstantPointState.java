package com.example.examplemod.core.spells.forms.instant;

import net.minecraft.world.phys.Vec3;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Состояние мгновенного точечного заклинания
 * Thread-safe структура для отслеживания мгновенных эффектов
 */
public class InstantPointState {
    
    // === ПОЗИЦИЯ ЭФФЕКТА ===
    public volatile Vec3 effectPosition;
    
    // === СОСТОЯНИЕ ПРИМЕНЕНИЯ ===
    public volatile boolean shouldApplyEffects = false;  // Нужно ли применять эффекты
    public volatile boolean effectsApplied = false;      // Применены ли основные эффекты
    
    // === ТИП МГНОВЕННОГО ЭФФЕКТА ===
    public volatile String instantType = "GENERIC";     // Тип эффекта (EXPLOSION, HEAL_BURST, etc.)
    
    // === ПОСЛЕДОВАТЕЛЬНЫЕ ЭФФЕКТЫ ===
    public final Queue<InstantEffect> effectSequence = new ConcurrentLinkedQueue<>();
    public volatile int sequenceTimer = 0;              // Таймер для задержек между эффектами
    
    // === СТАТИСТИКА ===
    public volatile int effectsCount = 0;               // Количество применённых эффектов
    public volatile float totalDamageDealt = 0.0f;      // Общий нанесённый урон
    public volatile float totalHealingDone = 0.0f;      // Общее лечение
    public volatile int entitiesAffected = 0;           // Количество затронутых сущностей
    
    // === ВРЕМЕННЫЕ ПАРАМЕТРЫ ===
    public volatile long creationTime = System.currentTimeMillis();
    public volatile int maxDuration = 100; // Максимальная продолжительность в тиках
    public volatile int currentTick = 0;
    
    /**
     * Проверить, завершён ли эффект
     */
    public boolean isComplete() {
        return effectsApplied && effectSequence.isEmpty();
    }
    
    /**
     * Проверить, истекло ли время действия
     */
    public boolean isExpired() {
        return currentTick >= maxDuration;
    }
    
    /**
     * Обновить тик
     */
    public void tick() {
        currentTick++;
    }
    
    /**
     * Добавить эффект в очередь
     */
    public void addDelayedEffect(InstantEffect effect) {
        effectSequence.offer(effect);
    }
    
    /**
     * Записать применённый эффект
     */
    public void recordEffect(float damage, float healing, int entities) {
        effectsCount++;
        totalDamageDealt += damage;
        totalHealingDone += healing;
        entitiesAffected += entities;
    }
    
    /**
     * Получить время существования в миллисекундах
     */
    public long getAge() {
        return System.currentTimeMillis() - creationTime;
    }
    
    /**
     * Получить прогресс выполнения (0.0 = начало, 1.0 = конец)
     */
    public float getProgress() {
        if (maxDuration <= 0) return 1.0f;
        return Math.min(1.0f, (float) currentTick / maxDuration);
    }
    
    /**
     * Получить эффективность воздействия
     */
    public float getEfficiency() {
        if (entitiesAffected == 0) return 0.0f;
        return (totalDamageDealt + totalHealingDone) / entitiesAffected;
    }
}