package com.example.examplemod.api.threads.examples;

import com.example.examplemod.api.threads.MainThreadSynchronizerAPI;
import com.example.examplemod.api.threads.IMainThreadSynchronizer;
import java.util.concurrent.CompletableFuture;

/**
 * Пример базового модуля, использующего MainThreadSynchronizer API
 * 
 * Демонстрирует:
 * - Регистрацию модуля
 * - Простое планирование задач
 * - Получение статистики
 * - Правильное освобождение ресурсов
 */
public class BasicModuleExample {
    
    private final String moduleName = "ExampleModule";
    private IMainThreadSynchronizer.IModuleHandle moduleHandle;
    
    /**
     * Инициализация модуля
     */
    public void initialize() {
        // Регистрируем модуль с базовыми весами
        IMainThreadSynchronizer.ModuleInfo moduleInfo = new IMainThreadSynchronizer.ModuleInfo(
            moduleName,
            "1.0.0", 
            "Пример базового модуля",
            IMainThreadSynchronizer.ModuleWeights.balanced()
        );
        
        moduleHandle = MainThreadSynchronizerAPI.registerModule(moduleName, moduleInfo);
        
        System.out.println("BasicModuleExample initialized");
    }
    
    /**
     * Простая задача в Main Thread
     */
    public void doSimpleTask() {
        MainThreadSynchronizerAPI.execute(moduleName + "_SimpleTask", () -> {
            // Код, который должен выполняться в Main Thread
            System.out.println("Выполняю простую задачу в Main Thread");
            
            // Симуляция работы
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        });
    }
    
    /**
     * Критическая задача с высоким приоритетом
     */
    public void doCriticalTask() {
        MainThreadSynchronizerAPI.executeHighPriority(moduleName + "_CriticalTask", () -> {
            System.out.println("Выполняю критическую задачу");
            // Критический код
        });
    }
    
    /**
     * Фоновая задача с низким приоритетом
     */
    public void doBackgroundTask() {
        MainThreadSynchronizerAPI.executeLowPriority(moduleName + "_BackgroundTask", () -> {
            System.out.println("Выполняю фоновую задачу");
            // Фоновая очистка, статистика и т.п.
        });
    }
    
    /**
     * Задача с возвращаемым значением
     */
    public CompletableFuture<String> doTaskWithResult() {
        return MainThreadSynchronizerAPI.submit(moduleName + "_TaskWithResult", () -> {
            // Вычисления в Main Thread
            return "Результат вычислений: " + System.currentTimeMillis();
        });
    }
    
    /**
     * Пример обработки результата
     */
    public void processResultExample() {
        doTaskWithResult()
            .thenAccept(result -> {
                System.out.println("Получен результат: " + result);
            })
            .exceptionally(throwable -> {
                System.err.println("Ошибка выполнения задачи: " + throwable.getMessage());
                return null;
            });
    }
    
    /**
     * Получить статистику модуля
     */
    public void printModuleStats() {
        if (moduleHandle != null) {
            IMainThreadSynchronizer.ModuleStats stats = moduleHandle.getStats();
            System.out.println("=== Статистика модуля " + moduleName + " ===");
            System.out.println("Всего отправлено: " + stats.totalTasksSubmitted());
            System.out.println("Выполнено: " + stats.totalTasksCompleted());
            System.out.println("Не удалось: " + stats.totalTasksFailed());
            System.out.println("В очереди: " + stats.currentPendingTasks());
            System.out.println("Среднее время: " + stats.averageExecutionTimeMs() + " ms");
            System.out.println("Активен: " + stats.isEnabled());
        }
    }
    
    /**
     * Завершение работы модуля
     */
    public void shutdown() {
        if (moduleHandle != null) {
            printModuleStats(); // Финальная статистика
            moduleHandle.unregister();
            moduleHandle = null;
        }
        System.out.println("BasicModuleExample shutdown");
    }
    
    // === Демонстрационный main метод ===
    
    public static void main(String[] args) throws InterruptedException {
        BasicModuleExample example = new BasicModuleExample();
        
        // Инициализация
        example.initialize();
        
        // Выполняем разные типы задач
        example.doSimpleTask();
        example.doCriticalTask();
        example.doBackgroundTask();
        example.processResultExample();
        
        // Ждем завершения задач
        Thread.sleep(1000);
        
        // Показываем статистику
        example.printModuleStats();
        
        // Завершение
        example.shutdown();
    }
}