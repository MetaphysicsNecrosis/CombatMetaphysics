package com.example.examplemod.api.threads.examples;

import com.example.examplemod.api.threads.MainThreadSynchronizerAPI;
import com.example.examplemod.api.threads.IMainThreadSynchronizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.Map;

/**
 * Продвинутый пример использования MainThreadSynchronizer API
 * 
 * Демонстрирует:
 * - Расширенное планирование (timeout, retry, delay, repeat)
 * - Пакетные операции
 * - Мониторинг и диагностика
 * - Управление приоритетами и весами
 * - Обработка ошибок
 */
public class AdvancedModuleExample {
    
    private final String moduleName = "AdvancedModule";
    private IMainThreadSynchronizer.IModuleHandle moduleHandle;
    private IMainThreadSynchronizer api;
    
    public void initialize() {
        // Регистрируем модуль с фокусом на высокий приоритет
        IMainThreadSynchronizer.ModuleInfo moduleInfo = new IMainThreadSynchronizer.ModuleInfo(
            moduleName,
            "2.0.0",
            "Продвинутый модуль с расширенными возможностями", 
            IMainThreadSynchronizer.ModuleWeights.highPriorityFocused()
        );
        
        moduleHandle = MainThreadSynchronizerAPI.registerModule(moduleName, moduleInfo);
        api = MainThreadSynchronizerAPI.getInstance();
        
        System.out.println("AdvancedModuleExample initialized");
    }
    
    /**
     * Задача с таймаутом
     */
    public void doTaskWithTimeout() {
        String taskName = moduleName + "_TimeoutTask";
        
        CompletableFuture<Void> future = api.executeWithTimeout(taskName, () -> {
            // Симуляция долгой операции
            try {
                Thread.sleep(2000);
                System.out.println("Долгая операция завершена");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1000, IMainThreadSynchronizer.TaskPriority.NORMAL); // Таймаут 1 секунда
        
        future.whenComplete((result, error) -> {
            if (error instanceof TimeoutException) {
                System.err.println("Задача превысила таймаут!");
            } else if (error != null) {
                System.err.println("Ошибка выполнения: " + error.getMessage());
            } else {
                System.out.println("Задача с таймаутом выполнена успешно");
            }
        });
    }
    
    /**
     * Задача с повторными попытками
     */
    public void doTaskWithRetry() {
        String taskName = moduleName + "_RetryTask";
        
        CompletableFuture<Void> future = api.executeWithRetry(taskName, () -> {
            // Симуляция нестабильной операции
            if (Math.random() < 0.7) {
                throw new RuntimeException("Случайная ошибка!");
            }
            System.out.println("Операция с retry успешно выполнена");
        }, 3, IMainThreadSynchronizer.TaskPriority.HIGH); // Максимум 3 попытки
        
        future.whenComplete((result, error) -> {
            if (error != null) {
                System.err.println("Все попытки исчерпаны: " + error.getMessage());
            } else {
                System.out.println("Задача с retry выполнена успешно");
            }
        });
    }
    
    /**
     * Отложенная задача
     */
    public void doDelayedTask() {
        String taskName = moduleName + "_DelayedTask";
        
        System.out.println("Планирую задачу с задержкой в 2 секунды...");
        
        api.scheduleDelayed(taskName, () -> {
            System.out.println("Отложенная задача выполнена! Время: " + System.currentTimeMillis());
        }, 2000, IMainThreadSynchronizer.TaskPriority.NORMAL);
    }
    
    /**
     * Повторяющаяся задача
     */
    public IMainThreadSynchronizer.IScheduledTask doRepeatingTask() {
        String taskName = moduleName + "_RepeatingTask";
        
        System.out.println("Запускаю повторяющуюся задачу каждые 500ms...");
        
        return api.scheduleRepeating(taskName, () -> {
            System.out.println("Повторяющаяся задача #" + System.currentTimeMillis());
            
            // Останавливаем после 5 выполнений
            // В реальном коде логика остановки будет другой
        }, 500, IMainThreadSynchronizer.TaskPriority.LOW);
    }
    
    /**
     * Пакетная операция
     */
    public void doBatchOperation() {
        String batchName = moduleName + "_BatchOperation";
        
        IMainThreadSynchronizer.TaskBatch batch = new IMainThreadSynchronizer.TaskBatch() {
            @Override
            public void execute() throws Exception {
                System.out.println("Выполняю пакетную операцию:");
                
                // Группа связанных задач
                for (int i = 1; i <= 5; i++) {
                    System.out.println("  Пакетная подзадача " + i);
                    Thread.sleep(50); // Симуляция работы
                }
                
                System.out.println("Пакетная операция завершена");
            }
            
            @Override
            public String getDescription() {
                return "Пример пакетной операции из 5 подзадач";
            }
            
            @Override
            public int getEstimatedTaskCount() {
                return 5;
            }
        };
        
        CompletableFuture<Void> future = api.executeBatch(batchName, batch, 
                                                         IMainThreadSynchronizer.TaskPriority.NORMAL);
        
        future.whenComplete((result, error) -> {
            if (error != null) {
                System.err.println("Ошибка пакетной операции: " + error.getMessage());
            } else {
                System.out.println("Пакетная операция успешно завершена");
            }
        });
    }
    
    /**
     * Изменение весов модуля во время выполнения
     */
    public void adjustModuleWeights() {
        System.out.println("Меняю веса модуля на backgroundFocused...");
        
        IMainThreadSynchronizer.ModuleWeights newWeights = 
            IMainThreadSynchronizer.ModuleWeights.backgroundFocused();
        
        moduleHandle.updateWeights(newWeights);
        
        // Или через API
        api.configureModuleWeights(moduleName, newWeights);
    }
    
    /**
     * Демонстрация мониторинга системы
     */
    public void demonstrateMonitoring() {
        System.out.println("\n=== СИСТЕМНЫЙ МОНИТОРИНГ ===");
        
        // Общая статистика системы
        IMainThreadSynchronizer.SynchronizerStats systemStats = api.getStats();
        System.out.println("Всего задач в очереди: " + systemStats.totalPendingTasks());
        System.out.println("Обработано за этот тик: " + systemStats.tasksProcessedThisTick());
        System.out.println("Зарегистрированных модулей: " + systemStats.registeredModules());
        System.out.println("Время работы: " + systemStats.uptimeMs() + " ms");
        
        // Детальный отчет о здоровье
        IMainThreadSynchronizer.SystemHealthReport health = api.getSystemHealthReport();
        System.out.println("\nЗдоровье системы: " + health.status());
        System.out.println("Общий процент ошибок: " + (health.failureRate() * 100) + "%");
        System.out.println("Среднее время выполнения: " + health.averageExecutionTimeMs() + " ms");
        System.out.println("Текущая пропускная способность: " + health.currentThroughput() + " tasks/sec");
        
        // Статистика по приоритетам
        System.out.println("\n--- Статистика по приоритетам ---");
        Map<IMainThreadSynchronizer.TaskPriority, IMainThreadSynchronizer.PriorityStats> priorityStats = 
            api.getPriorityStats();
        
        for (Map.Entry<IMainThreadSynchronizer.TaskPriority, IMainThreadSynchronizer.PriorityStats> entry : 
             priorityStats.entrySet()) {
            IMainThreadSynchronizer.PriorityStats stats = entry.getValue();
            System.out.println(entry.getKey() + ": " + stats.tasksExecuted() + 
                             " выполнено, " + (stats.failureRate() * 100) + "% ошибок");
        }
        
        // Проблемные модули
        List<String> problematic = api.getProblematicModules();
        if (!problematic.isEmpty()) {
            System.out.println("\n--- Проблемные модули ---");
            for (String module : problematic) {
                IMainThreadSynchronizer.DetailedModuleStats details = api.getDetailedModuleStats(module);
                if (details != null) {
                    System.out.println(module + ": " + details.recentErrorCount() + 
                                     " недавних ошибок, " + details.averageExecutionTimeMs() + "ms среднее время");
                }
            }
        }
    }
    
    /**
     * Управление включением/выключением модуля
     */
    public void toggleModuleState() {
        // Временно отключаем модуль
        System.out.println("Отключаю модуль...");
        moduleHandle.setEnabled(false);
        
        // Проверяем статус
        IMainThreadSynchronizer.ModuleStats stats = moduleHandle.getStats();
        System.out.println("Модуль активен: " + stats.isEnabled());
        
        // Включаем обратно
        System.out.println("Включаю модуль обратно...");
        moduleHandle.setEnabled(true);
    }
    
    public void shutdown() {
        if (moduleHandle != null) {
            moduleHandle.unregister();
            moduleHandle = null;
        }
        System.out.println("AdvancedModuleExample shutdown");
    }
    
    // === Демонстрационный main ===
    
    public static void main(String[] args) throws InterruptedException {
        AdvancedModuleExample example = new AdvancedModuleExample();
        
        example.initialize();
        
        // Демонстрируем различные возможности
        example.doTaskWithTimeout();
        Thread.sleep(500);
        
        example.doTaskWithRetry();
        Thread.sleep(500);
        
        example.doDelayedTask();
        
        IMainThreadSynchronizer.IScheduledTask repeatingTask = example.doRepeatingTask();
        Thread.sleep(3000); // Дать поработать
        repeatingTask.cancel(); // Останавливаем
        
        example.doBatchOperation();
        Thread.sleep(500);
        
        example.adjustModuleWeights();
        example.toggleModuleState();
        
        // Дать системе поработать
        Thread.sleep(2000);
        
        // Показать мониторинг
        example.demonstrateMonitoring();
        
        example.shutdown();
    }
}