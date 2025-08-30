package com.example.examplemod.core.spells.computation;

import com.example.examplemod.core.spells.parameters.ISpellParameter;
import com.example.examplemod.core.spells.parameters.SpellParameterRegistry;
import com.example.examplemod.core.spells.parameters.SpellParameters;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinTask;
import java.util.Map;
import java.util.UUID;

/**
 * Task для выполнения в Spell Computation Pool
 * Реализует многопоточную архитектуру из MultiThread.txt:
 * 
 * Main Thread -> [Spell Computation Pool] -> Collision Thread -> Aggregation Thread -> Main Thread
 *                       ^-- МЫ ЗДЕСЬ --^
 */
public class SpellComputationTask implements Callable<SpellComputationTaskResult> {
    
    private final SpellComputationContext context;
    private final SpellParameters parameters;
    
    public SpellComputationTask(SpellComputationContext context, SpellParameters parameters) {
        this.context = context;
        this.parameters = parameters;
    }
    
    @Override
    public SpellComputationTaskResult call() throws Exception {
        UUID spellId = context.getSpellInstanceId();
        long startTime = System.nanoTime();
        
        try {
            // === ФАЗА 1: Получение всех параметров ===
            List<ParameterComputation> computations = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : parameters.getAllParameters().entrySet()) {
                ISpellParameter parameter = SpellParameterRegistry.getParameterByKey(entry.getKey());
                
                if (parameter != null && parameter.isThreadSafe()) {
                    computations.add(new ParameterComputation(parameter, entry.getValue()));
                }
            }
            
            // === ФАЗА 2: Сортировка по приоритету ===
            computations.sort((a, b) -> Integer.compare(
                a.parameter.getComputationPriority(), 
                b.parameter.getComputationPriority()
            ));
            
            // === ФАЗА 3: Параллельное выполнение (Fork-Join) ===
            List<SpellComputationResult> results = new ArrayList<>();
            
            if (computations.size() <= 2) {
                // Мало параметров - выполняем последовательно
                for (ParameterComputation comp : computations) {
                    SpellComputationResult result = comp.parameter.compute(context, comp.value);
                    results.add(result);
                }
            } else {
                // Много параметров - используем Fork-Join
                results = computeParametersParallel(computations);
            }
            
            // === ФАЗА 4: Агрегация результатов ===
            SpellComputationTaskResult taskResult = aggregateResults(spellId, results, startTime);
            
            // Передаем снепшот коллизий для Collision Thread
            if (context.hasCollisionSnapshot()) {
                taskResult.setCollisionSnapshot(context.getCollisionSnapshot());
            }
            
            return taskResult;
            
        } catch (Exception e) {
            // Обработка ошибок в worker thread
            long duration = System.nanoTime() - startTime;
            return SpellComputationTaskResult.error(spellId, e, duration);
        }
    }
    
    /**
     * Параллельное вычисление параметров с использованием Fork-Join
     */
    private List<SpellComputationResult> computeParametersParallel(List<ParameterComputation> computations) {
        List<CompletableFuture<SpellComputationResult>> futures = new ArrayList<>();
        
        // Создаём Future для каждого параметра
        for (ParameterComputation comp : computations) {
            CompletableFuture<SpellComputationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return comp.parameter.compute(context, comp.value);
                } catch (Exception e) {
                    // Создаём результат с ошибкой
                    SpellComputationResult errorResult = new SpellComputationResult(
                        context.getSpellInstanceId(), comp.parameter.getKey());
                    errorResult.putComputedValue("error", e.getMessage());
                    return errorResult;
                }
            });
            
            futures.add(future);
        }
        
        // Ждём завершения всех вычислений
        List<SpellComputationResult> results = new ArrayList<>();
        for (CompletableFuture<SpellComputationResult> future : futures) {
            try {
                SpellComputationResult result = future.get(); // Blocking call
                results.add(result);
            } catch (Exception e) {
                // Future failed - создаём пустой результат
                results.add(new SpellComputationResult(context.getSpellInstanceId(), "unknown"));
            }
        }
        
        return results;
    }
    
    /**
     * Агрегация результатов вычислений
     */
    private SpellComputationTaskResult aggregateResults(UUID spellId, 
                                                       List<SpellComputationResult> results,
                                                       long startTime) {
        long computationDuration = System.nanoTime() - startTime;
        
        SpellComputationTaskResult taskResult = new SpellComputationTaskResult(spellId, computationDuration);
        
        // Собираем все результаты
        for (SpellComputationResult result : results) {
            taskResult.addParameterResult(result);
        }
        
        // Вычисляем общую статистику
        taskResult.computeAggregateStats();
        
        return taskResult;
    }
    
    /**
     * Внутренний класс для хранения параметра и его значения
     */
    private static class ParameterComputation {
        final ISpellParameter parameter;
        final Object value;
        
        ParameterComputation(ISpellParameter parameter, Object value) {
            this.parameter = parameter;
            this.value = value;
        }
    }
    
    // === Статические методы для создания тасков ===
    
    /**
     * Создать задачу вычисления заклинания
     */
    public static SpellComputationTask create(SpellComputationContext context, SpellParameters parameters) {
        return new SpellComputationTask(context, parameters);
    }
    
    /**
     * Создать и сразу отправить в пул воркеров
     */
    public static CompletableFuture<SpellComputationTaskResult> submitAsync(
            SpellComputationContext context, SpellParameters parameters) {
        
        SpellComputationTask task = new SpellComputationTask(context, parameters);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                return SpellComputationTaskResult.error(
                    context.getSpellInstanceId(), e, System.nanoTime());
            }
        });
    }
}