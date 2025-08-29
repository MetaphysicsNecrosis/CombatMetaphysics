package com.example.examplemod.core.spells.parameters;

import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationResult;

/**
 * Thread-safe интерфейс параметров заклинаний
 * Соответствует многопоточной архитектуре из MultiThread.txt
 * 
 * Pipeline: Main Thread -> Spell Computation Pool -> Collision Thread -> Aggregation -> Main Thread
 */
public interface ISpellParameter {
    
    /**
     * Вычислить влияние параметра на заклинание (WORKER THREAD)
     * Этот метод вызывается в Spell Computation Pool - НЕ содержит Minecraft объектов!
     * 
     * @param context Thread-safe контекст вычисления (без Level/Player)
     * @param value Значение параметра
     * @return Результат вычисления для последующего применения в main thread
     */
    SpellComputationResult compute(SpellComputationContext context, Object value);
    
    /**
     * Получить ключ параметра
     */
    String getKey();
    
    /**
     * Получить тип значения
     */
    Class<?> getValueType();
    
    /**
     * Приоритет вычисления (меньше = раньше в pipeline)
     * Критично для многопоточной архитектуры
     */
    default int getComputationPriority() {
        return 100;
    }
    
    /**
     * Является ли параметр thread-safe для параллельного вычисления
     */
    default boolean isThreadSafe() {
        return true;
    }
    
    /**
     * Требует ли параметр данные из main thread (Player/Level)
     * Если true - будет обработан отдельно
     */
    default boolean requiresMainThreadData() {
        return false;
    }
}