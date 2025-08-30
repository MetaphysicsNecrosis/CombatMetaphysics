package com.example.examplemod.api.threads;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Сервис мониторинга системы многопоточности
 * 
 * Отслеживает производительность, здоровье системы и предоставляет
 * детальную статистику для диагностики проблем
 */
public class ThreadMonitoringService {
    
    private static ThreadMonitoringService INSTANCE;
    
    // === Глобальные метрики ===
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicLong totalExecutionTimeNs = new AtomicLong(0);
    private final AtomicLong totalTasksFailed = new AtomicLong(0);
    private final AtomicLong systemStartTime = new AtomicLong(System.currentTimeMillis());
    
    // === Метрики по модулям ===
    private final Map<String, ModuleMetrics> moduleMetrics = new ConcurrentHashMap<>();
    
    // === Метрики производительности ===
    private final Map<IMainThreadSynchronizer.TaskPriority, PriorityMetrics> priorityMetrics = new ConcurrentHashMap<>();
    
    // === История производительности (последние 100 записей) ===
    private final CircularBuffer<PerformanceSample> performanceHistory = new CircularBuffer<>(100);
    
    private ThreadMonitoringService() {
        // Инициализация метрик приоритетов
        for (IMainThreadSynchronizer.TaskPriority priority : IMainThreadSynchronizer.TaskPriority.values()) {
            priorityMetrics.put(priority, new PriorityMetrics());
        }
    }
    
    public static ThreadMonitoringService getInstance() {
        if (INSTANCE == null) {
            synchronized (ThreadMonitoringService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ThreadMonitoringService();
                }
            }
        }
        return INSTANCE;
    }
    
    // === Регистрация событий ===
    
    /**
     * Записать выполнение задачи
     */
    public void recordTaskExecution(String moduleName, String taskName, 
                                  IMainThreadSynchronizer.TaskPriority priority,
                                  long executionTimeNs, boolean success) {
        // Глобальные метрики
        totalTasksExecuted.incrementAndGet();
        totalExecutionTimeNs.addAndGet(executionTimeNs);
        
        if (!success) {
            totalTasksFailed.incrementAndGet();
        }
        
        // Метрики модуля
        moduleMetrics.computeIfAbsent(moduleName, k -> new ModuleMetrics())
                    .recordExecution(executionTimeNs, success);
        
        // Метрики приоритета
        priorityMetrics.get(priority).recordExecution(executionTimeNs, success);
        
        // Обновляем историю производительности
        updatePerformanceHistory();
    }
    
    /**
     * Записать ошибку модуля
     */
    public void recordModuleError(String moduleName, String errorType, String message) {
        moduleMetrics.computeIfAbsent(moduleName, k -> new ModuleMetrics())
                    .recordError(errorType, message);
        
        System.err.println("[ThreadMonitoring] Module " + moduleName + " error (" + errorType + "): " + message);
    }
    
    // === Получение статистики ===
    
    /**
     * Получить общую статистику системы
     */
    public SystemHealthReport getSystemHealth() {
        long uptime = System.currentTimeMillis() - systemStartTime.get();
        long totalTasks = totalTasksExecuted.get();
        long failedTasks = totalTasksFailed.get();
        
        double failureRate = totalTasks > 0 ? (double) failedTasks / totalTasks : 0.0;
        double avgExecutionTimeMs = totalTasks > 0 ? 
            totalExecutionTimeNs.get() / 1_000_000.0 / totalTasks : 0.0;
        
        IMainThreadSynchronizer.HealthStatus health = determineHealthStatus(failureRate, avgExecutionTimeMs);
        
        return new SystemHealthReport(
            health,
            uptime,
            totalTasks,
            failedTasks,
            failureRate,
            avgExecutionTimeMs,
            getActiveModulesCount(),
            getCurrentThroughput()
        );
    }
    
    /**
     * Получить детальную статистику модуля
     */
    public DetailedModuleStats getDetailedModuleStats(String moduleName) {
        ModuleMetrics metrics = moduleMetrics.get(moduleName);
        if (metrics == null) {
            return null;
        }
        
        return metrics.generateDetailedStats(moduleName);
    }
    
    /**
     * Получить статистику по приоритетам
     */
    public Map<IMainThreadSynchronizer.TaskPriority, PriorityStats> getPriorityStats() {
        Map<IMainThreadSynchronizer.TaskPriority, PriorityStats> stats = new ConcurrentHashMap<>();
        
        for (Map.Entry<IMainThreadSynchronizer.TaskPriority, PriorityMetrics> entry : priorityMetrics.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().getStats());
        }
        
        return stats;
    }
    
    /**
     * Получить список проблемных модулей
     */
    public List<String> getProblematicModules() {
        List<String> problematic = new ArrayList<>();
        
        for (Map.Entry<String, ModuleMetrics> entry : moduleMetrics.entrySet()) {
            ModuleMetrics metrics = entry.getValue();
            
            // Модуль считается проблемным, если:
            // 1. Высокий процент ошибок (>5%)
            // 2. Слишком медленное выполнение (>100ms среднее время)
            // 3. Много недавних ошибок
            
            if (metrics.getFailureRate() > 0.05 ||
                metrics.getAverageExecutionTimeMs() > 100.0 ||
                metrics.getRecentErrorCount() > 10) {
                problematic.add(entry.getKey());
            }
        }
        
        return problematic;
    }
    
    // === Внутренние методы ===
    
    private IMainThreadSynchronizer.HealthStatus determineHealthStatus(double failureRate, double avgExecutionTimeMs) {
        if (failureRate > 0.1 || avgExecutionTimeMs > 200) {
            return IMainThreadSynchronizer.HealthStatus.CRITICAL;
        } else if (failureRate > 0.05 || avgExecutionTimeMs > 100) {
            return IMainThreadSynchronizer.HealthStatus.WARNING;
        }
        return IMainThreadSynchronizer.HealthStatus.HEALTHY;
    }
    
    private int getActiveModulesCount() {
        return moduleMetrics.size();
    }
    
    private double getCurrentThroughput() {
        // Вычисляем throughput за последние 10 секунд
        long currentTime = System.nanoTime();
        long tenSecondsAgo = currentTime - 10_000_000_000L; // 10 секунд в наносекундах
        
        return performanceHistory.getStream()
            .filter(sample -> sample.timestamp > tenSecondsAgo)
            .mapToDouble(sample -> sample.tasksPerSecond)
            .average()
            .orElse(0.0);
    }
    
    private void updatePerformanceHistory() {
        long currentTime = System.nanoTime();
        
        // Добавляем новый sample каждые 100ms
        PerformanceSample lastSample = performanceHistory.getLatest();
        if (lastSample == null || (currentTime - lastSample.timestamp) > 100_000_000L) {
            
            double tasksPerSecond = calculateCurrentTasksPerSecond();
            double avgResponseTimeMs = calculateCurrentAvgResponseTime();
            
            performanceHistory.add(new PerformanceSample(currentTime, tasksPerSecond, avgResponseTimeMs));
        }
    }
    
    private double calculateCurrentTasksPerSecond() {
        // Простое приближение - можно улучшить
        return totalTasksExecuted.get() / ((System.currentTimeMillis() - systemStartTime.get()) / 1000.0);
    }
    
    private double calculateCurrentAvgResponseTime() {
        long totalTasks = totalTasksExecuted.get();
        return totalTasks > 0 ? totalExecutionTimeNs.get() / 1_000_000.0 / totalTasks : 0.0;
    }
    
    // === Внутренние классы ===
    
    /**
     * Метрики модуля
     */
    private static class ModuleMetrics {
        private final AtomicLong tasksExecuted = new AtomicLong(0);
        private final AtomicLong totalExecutionTimeNs = new AtomicLong(0);
        private final AtomicLong tasksFailed = new AtomicLong(0);
        private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();
        private final CircularBuffer<Long> recentErrors = new CircularBuffer<>(50);
        private volatile long lastActivity = System.currentTimeMillis();
        
        void recordExecution(long executionTimeNs, boolean success) {
            tasksExecuted.incrementAndGet();
            totalExecutionTimeNs.addAndGet(executionTimeNs);
            lastActivity = System.currentTimeMillis();
            
            if (!success) {
                tasksFailed.incrementAndGet();
                recentErrors.add(System.currentTimeMillis());
            }
        }
        
        void recordError(String errorType, String message) {
            errorsByType.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
            recentErrors.add(System.currentTimeMillis());
        }
        
        double getFailureRate() {
            long total = tasksExecuted.get();
            return total > 0 ? (double) tasksFailed.get() / total : 0.0;
        }
        
        double getAverageExecutionTimeMs() {
            long total = tasksExecuted.get();
            return total > 0 ? totalExecutionTimeNs.get() / 1_000_000.0 / total : 0.0;
        }
        
        int getRecentErrorCount() {
            long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000L;
            return (int) recentErrors.getStream().filter(timestamp -> timestamp > fiveMinutesAgo).count();
        }
        
        DetailedModuleStats generateDetailedStats(String moduleName) {
            return new DetailedModuleStats(
                moduleName,
                tasksExecuted.get(),
                tasksFailed.get(),
                getFailureRate(),
                getAverageExecutionTimeMs(),
                lastActivity,
                new ConcurrentHashMap<>(errorsByType),
                getRecentErrorCount()
            );
        }
    }
    
    /**
     * Метрики приоритета
     */
    private static class PriorityMetrics {
        private final AtomicLong tasksExecuted = new AtomicLong(0);
        private final AtomicLong totalExecutionTimeNs = new AtomicLong(0);
        private final AtomicLong tasksFailed = new AtomicLong(0);
        
        void recordExecution(long executionTimeNs, boolean success) {
            tasksExecuted.incrementAndGet();
            totalExecutionTimeNs.addAndGet(executionTimeNs);
            
            if (!success) {
                tasksFailed.incrementAndGet();
            }
        }
        
        PriorityStats getStats() {
            long total = tasksExecuted.get();
            return new PriorityStats(
                total,
                tasksFailed.get(),
                total > 0 ? (double) tasksFailed.get() / total : 0.0,
                total > 0 ? totalExecutionTimeNs.get() / 1_000_000.0 / total : 0.0
            );
        }
    }
    
    // === Data Classes ===
    
    public record SystemHealthReport(
        IMainThreadSynchronizer.HealthStatus status,
        long uptimeMs,
        long totalTasksExecuted,
        long totalTasksFailed,
        double failureRate,
        double averageExecutionTimeMs,
        int activeModules,
        double currentThroughput
    ) {}
    
    public record DetailedModuleStats(
        String moduleName,
        long tasksExecuted,
        long tasksFailed,
        double failureRate,
        double averageExecutionTimeMs,
        long lastActivityTime,
        Map<String, AtomicLong> errorsByType,
        int recentErrorCount
    ) {}
    
    public record PriorityStats(
        long tasksExecuted,
        long tasksFailed,
        double failureRate,
        double averageExecutionTimeMs
    ) {}
    
    private record PerformanceSample(
        long timestamp,
        double tasksPerSecond,
        double avgResponseTimeMs
    ) {}
    
    // === Утилитный класс для кольцевого буфера ===
    
    private static class CircularBuffer<T> {
        private final Object[] buffer;
        private final int capacity;
        private volatile int head = 0;
        private volatile int size = 0;
        
        CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new Object[capacity];
        }
        
        synchronized void add(T item) {
            buffer[head] = item;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++;
            }
        }
        
        @SuppressWarnings("unchecked")
        T getLatest() {
            if (size == 0) return null;
            int latestIndex = (head - 1 + capacity) % capacity;
            return (T) buffer[latestIndex];
        }
        
        @SuppressWarnings("unchecked")
        java.util.stream.Stream<T> getStream() {
            List<T> items = new ArrayList<>();
            synchronized (this) {
                for (int i = 0; i < size; i++) {
                    int index = (head - size + i + capacity) % capacity;
                    items.add((T) buffer[index]);
                }
            }
            return items.stream();
        }
    }
}