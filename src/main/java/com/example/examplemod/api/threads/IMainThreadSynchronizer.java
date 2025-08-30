package com.example.examplemod.api.threads;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Main Thread Synchronizer API - Публичный интерфейс для других модулей
 * 
 * Этот интерфейс предоставляет thread-safe способ выполнения задач в Main Thread
 * из любых worker threads. Использует Weighted Round-Robin планировщик для
 * справедливого распределения ресурсов между модулями.
 * 
 * === ИСПОЛЬЗОВАНИЕ ===
 * 
 * ```java
 * // Получить экземпляр
 * IMainThreadSynchronizer sync = MainThreadSynchronizerAPI.getInstance();
 * 
 * // Запланировать простую задачу
 * sync.execute("MyModule_DoSomething", () -> {
 *     // Код выполнится в Main Thread
 *     level.setBlock(pos, newState);
 * });
 * 
 * // Запланировать задачу с результатом
 * CompletableFuture<String> result = sync.submit("MyModule_Calculate", () -> {
 *     return player.getName().getString();
 * });
 * 
 * // Использовать разные приоритеты
 * sync.executeHighPriority("Critical_Task", criticalTask);
 * sync.executeLowPriority("Cleanup_Task", cleanupTask);
 * ```
 */
public interface IMainThreadSynchronizer {
    
    // === ОСНОВНЫЕ МЕТОДЫ ПЛАНИРОВАНИЯ ===
    
    /**
     * Выполнить задачу в Main Thread с NORMAL приоритетом
     * 
     * @param taskName Имя задачи для отладки и статистики
     * @param task Задача для выполнения в Main Thread
     */
    void execute(String taskName, Runnable task);
    
    /**
     * Выполнить задачу в Main Thread с возвратом результата
     * 
     * @param taskName Имя задачи для отладки и статистики
     * @param task Задача с возвращаемым значением
     * @return CompletableFuture с результатом выполнения
     */
    <T> CompletableFuture<T> submit(String taskName, Supplier<T> task);
    
    /**
     * Выполнить задачу в Main Thread с ВЫСОКИМ приоритетом
     * Используется для критических операций (ресурсы, безопасность)
     */
    void executeHighPriority(String taskName, Runnable task);
    
    /**
     * Выполнить задачу в Main Thread с НИЗКИМ приоритетом
     * Используется для фоновых задач (статистика, очистка)
     */
    void executeLowPriority(String taskName, Runnable task);
    
    /**
     * Выполнить задачу с автоматическим определением приоритета
     * 
     * @param taskName Имя задачи
     * @param task Задача для выполнения
     * @param priority Приоритет выполнения
     */
    void execute(String taskName, Runnable task, TaskPriority priority);
    
    // === РАСШИРЕННЫЕ МЕТОДЫ ===
    
    /**
     * Выполнить задачу с таймаутом
     * Если задача не выполнится за указанное время, будет отменена
     */
    CompletableFuture<Void> executeWithTimeout(String taskName, Runnable task, 
                                               long timeoutMs, TaskPriority priority);
    
    /**
     * Выполнить задачу с повторными попытками при ошибке
     */
    CompletableFuture<Void> executeWithRetry(String taskName, Runnable task,
                                             int maxRetries, TaskPriority priority);
    
    /**
     * Запланировать задачу на выполнение через определенное время
     * (выполнится в одном из следующих тиков)
     */
    void scheduleDelayed(String taskName, Runnable task, long delayMs, TaskPriority priority);
    
    /**
     * Запланировать повторяющуюся задачу
     * Будет выполняться каждый N-й тик
     */
    IScheduledTask scheduleRepeating(String taskName, Runnable task, 
                                    long intervalMs, TaskPriority priority);
    
    // === ПАКЕТНЫЕ ОПЕРАЦИИ ===
    
    /**
     * Выполнить группу связанных задач как единое целое
     * Полезно для операций которые должны быть выполнены вместе
     */
    CompletableFuture<Void> executeBatch(String batchName, TaskBatch batch, TaskPriority priority);
    
    // === КОНФИГУРИРОВАНИЕ И УПРАВЛЕНИЕ ===
    
    /**
     * Настроить веса планировщика для данного модуля
     * Позволяет модулю запросить больше ресурсов определенного приоритета
     */
    void configureModuleWeights(String moduleName, ModuleWeights weights);
    
    /**
     * Зарегистрировать модуль в синхронизаторе
     * Рекомендуется для получения детальной статистики
     */
    IModuleHandle registerModule(String moduleName, ModuleInfo moduleInfo);
    
    // === МОНИТОРИНГ И СТАТИСТИКА ===
    
    /**
     * Получить общую статистику синхронизатора
     */
    SynchronizerStats getStats();
    
    /**
     * Получить статистику по конкретному модулю
     */
    ModuleStats getModuleStats(String moduleName);
    
    /**
     * Получить количество ожидающих задач для модуля
     */
    int getPendingTaskCount(String moduleName);
    
    /**
     * Проверить здоровье синхронизатора
     * Возвращает предупреждения о возможных проблемах
     */
    HealthStatus getHealthStatus();
    
    // === РАСШИРЕННЫЙ МОНИТОРИНГ ===
    
    /**
     * Получить детальный отчет о здоровье системы
     */
    default SystemHealthReport getSystemHealthReport() {
        var health = ThreadMonitoringService.getInstance().getSystemHealth();
        return new SystemHealthReport(
            health.status(),
            health.uptimeMs(),
            health.totalTasksExecuted(),
            health.totalTasksFailed(),
            health.failureRate(),
            health.averageExecutionTimeMs(),
            health.activeModules(),
            health.currentThroughput()
        );
    }
    
    /**
     * Получить детальную статистику модуля
     */
    default DetailedModuleStats getDetailedModuleStats(String moduleName) {
        var stats = ThreadMonitoringService.getInstance().getDetailedModuleStats(moduleName);
        return new DetailedModuleStats(
            stats.moduleName(),
            stats.tasksExecuted(),
            stats.tasksFailed(),
            stats.failureRate(),
            stats.averageExecutionTimeMs(),
            stats.lastActivityTime(),
            stats.errorsByType(),
            stats.recentErrorCount()
        );
    }
    
    /**
     * Получить список проблемных модулей
     */
    default java.util.List<String> getProblematicModules() {
        return ThreadMonitoringService.getInstance().getProblematicModules();
    }
    
    /**
     * Получить статистику по приоритетам
     */
    default java.util.Map<TaskPriority, PriorityStats> getPriorityStats() {
        var serviceStats = ThreadMonitoringService.getInstance().getPriorityStats();
        java.util.Map<TaskPriority, PriorityStats> result = new java.util.HashMap<>();
        for (var entry : serviceStats.entrySet()) {
            var stats = entry.getValue();
            result.put(entry.getKey(), new PriorityStats(
                stats.tasksExecuted(),
                stats.tasksFailed(),
                stats.failureRate(),
                stats.averageExecutionTimeMs()
            ));
        }
        return result;
    }
    
    // === ENUMS И ВСПОМОГАТЕЛЬНЫЕ КЛАССЫ ===
    
    enum TaskPriority {
        HIGH,    // Критические операции, ресурсы
        NORMAL,  // Обычные задачи модулей
        LOW      // Фоновые задачи, статистика
    }
    
    /**
     * Handle для управления запланированной задачей
     */
    interface IScheduledTask {
        void cancel();
        boolean isCancelled();
        boolean isRunning();
        long getNextExecutionTime();
        int getExecutionCount();
    }
    
    /**
     * Группа связанных задач
     */
    interface TaskBatch {
        void execute() throws Exception;
        String getDescription();
        int getEstimatedTaskCount();
    }
    
    /**
     * Handle для зарегистрированного модуля
     */
    interface IModuleHandle {
        void unregister();
        void updateWeights(ModuleWeights weights);
        ModuleStats getStats();
        void setEnabled(boolean enabled);
    }
    
    // === DATA CLASSES ===
    
    record ModuleWeights(
        int highPriorityWeight,
        int normalPriorityWeight,
        int lowPriorityWeight
    ) {
        public static ModuleWeights balanced() {
            return new ModuleWeights(1, 1, 1);
        }
        
        public static ModuleWeights highPriorityFocused() {
            return new ModuleWeights(3, 1, 1);
        }
        
        public static ModuleWeights backgroundFocused() {
            return new ModuleWeights(1, 1, 3);
        }
    }
    
    record ModuleInfo(
        String name,
        String version,
        String description,
        ModuleWeights defaultWeights
    ) {}
    
    record SynchronizerStats(
        int totalPendingTasks,
        int highPriorityPending,
        int normalPriorityPending,
        int lowPriorityPending,
        long totalTasksProcessed,
        int tasksProcessedThisTick,
        int registeredModules,
        long uptimeMs,
        WRRWeights currentWeights
    ) {}
    
    record ModuleStats(
        String moduleName,
        long totalTasksSubmitted,
        long totalTasksCompleted,
        long totalTasksFailed,
        int currentPendingTasks,
        double averageExecutionTimeMs,
        long lastActivityTime,
        boolean isEnabled
    ) {}
    
    record WRRWeights(
        int highWeight,
        int normalWeight,
        int lowWeight
    ) {}
    
    enum HealthStatus {
        HEALTHY,
        WARNING,
        CRITICAL,
        SHUTDOWN
    }
    
    // === РАСШИРЕННЫЕ DATA CLASSES ===
    
    record SystemHealthReport(
        HealthStatus status,
        long uptimeMs,
        long totalTasksExecuted,
        long totalTasksFailed,
        double failureRate,
        double averageExecutionTimeMs,
        int activeModules,
        double currentThroughput
    ) {}
    
    record DetailedModuleStats(
        String moduleName,
        long tasksExecuted,
        long tasksFailed,
        double failureRate,
        double averageExecutionTimeMs,
        long lastActivityTime,
        java.util.Map<String, java.util.concurrent.atomic.AtomicLong> errorsByType,
        int recentErrorCount
    ) {}
    
    record PriorityStats(
        long tasksExecuted,
        long tasksFailed,
        double failureRate,
        double averageExecutionTimeMs
    ) {}
}