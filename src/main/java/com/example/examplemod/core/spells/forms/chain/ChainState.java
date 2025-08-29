package com.example.examplemod.core.spells.forms.chain;

import net.minecraft.world.entity.LivingEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Состояние цепного заклинания
 * Thread-safe структура для отслеживания цепи
 */
public class ChainState {
    
    // === ТЕКУЩАЯ ЦЕЛЬ ===
    public volatile LivingEntity currentTarget;
    
    // === ПАРАМЕТРЫ ЦЕПИ ===
    public volatile int remainingJumps;        // Сколько переходов осталось
    public volatile float maxRange;            // Максимальная дальность перехода
    public volatile float damageDecay;         // Коэффициент уменьшения урона
    public volatile int jumpCooldown = 0;      // Задержка между переходами
    
    // === СОСТОЯНИЕ ОБРАБОТКИ ===
    public volatile boolean hasProcessedCurrentTarget = false; // Обработали ли текущую цель
    
    // === ПОСЕЩЁННЫЕ ЦЕЛИ (thread-safe) ===
    public final Set<UUID> visitedTargets = ConcurrentHashMap.newKeySet();
    
    // === СТАТИСТИКА ===
    public volatile int totalJumps = 0;        // Общее количество выполненных переходов
    public volatile float totalDamageDealt = 0.0f; // Общий нанесённый урон
    
    /**
     * Проверить, можем ли мы ещё делать переходы
     */
    public boolean canContinue() {
        return remainingJumps > 0 && currentTarget != null;
    }
    
    /**
     * Записать выполненный переход
     */
    public void recordJump(float damageDealt) {
        totalJumps++;
        totalDamageDealt += damageDealt;
        hasProcessedCurrentTarget = false; // Сброс для новой цели
    }
    
    /**
     * Получить эффективность цепи (урон на переход)
     */
    public float getChainEfficiency() {
        return totalJumps > 0 ? totalDamageDealt / totalJumps : 0.0f;
    }
    
    /**
     * Проверить, была ли цель уже посещена
     */
    public boolean hasVisited(UUID targetId) {
        return visitedTargets.contains(targetId);
    }
    
    /**
     * Добавить цель в список посещённых
     */
    public void addVisitedTarget(UUID targetId) {
        visitedTargets.add(targetId);
    }
    
    /**
     * Получить прогресс цепи (0.0 = начало, 1.0 = конец)
     */
    public float getProgress() {
        if (totalJumps == 0) return 0.0f;
        
        int initialJumps = totalJumps + remainingJumps;
        return (float) totalJumps / initialJumps;
    }
}