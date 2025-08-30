package com.example.examplemod.api.threads;

import com.example.examplemod.core.threads.MainThreadSynchronizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Адаптер между API интерфейсом и реальной реализацией MainThreadSynchronizer
 * 
 * Этот класс преобразует вызовы API в вызовы реального MainThreadSynchronizer,
 * добавляя дополнительную функциональность:
 * - Регистрация модулей и статистика по модулям
 * - Расширенные методы планирования
 * - Мониторинг здоровья системы
 * - Timeout и retry механизмы
 */
class MainThreadSynchronizerAdapter implements IMainThreadSynchronizer {
    
    private final MainThreadSynchronizer underlying;
    private final Map<String, RegisteredModule> registeredModules = new ConcurrentHashMap<>();
    private final AtomicLong taskIdGenerator = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    private final ThreadMonitoringService monitoring = ThreadMonitoringService.getInstance();
    
    // Статистика
    private final AtomicLong totalTasksSubmitted = new AtomicLong(0);
    private final AtomicLong totalTasksCompleted = new AtomicLong(0);
    private final AtomicLong totalTasksFailed = new AtomicLong(0);
    
    public MainThreadSynchronizerAdapter(MainThreadSynchronizer underlying) {
        this.underlying = underlying;
    }
    
    // === ОСНОВНЫЕ МЕТОДЫ ПЛАНИРОВАНИЯ ===
    
    @Override
    public void execute(String taskName, Runnable task) {
        execute(taskName, task, TaskPriority.NORMAL);
    }
    
    @Override
    public <T> CompletableFuture<T> submit(String taskName, Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        String moduleName = extractModuleName(taskName);
        
        execute(taskName, () -> {
            try {
                T result = task.get();
                future.complete(result);
                recordTaskCompletion(moduleName, true);
            } catch (Exception e) {
                future.completeExceptionally(e);
                recordTaskCompletion(moduleName, false);
            }
        }, TaskPriority.NORMAL);
        
        return future;
    }
    
    @Override
    public void executeHighPriority(String taskName, Runnable task) {
        execute(taskName, task, TaskPriority.HIGH);
    }
    
    @Override
    public void executeLowPriority(String taskName, Runnable task) {
        execute(taskName, task, TaskPriority.LOW);
    }
    
    @Override
    public void execute(String taskName, Runnable task, TaskPriority priority) {
        String moduleName = extractModuleName(taskName);
        long taskId = taskIdGenerator.incrementAndGet();
        
        // Обертываем задачу для сбора статистики
        Runnable wrappedTask = () -> {
            long startTimeNs = System.nanoTime();
            boolean success = false;
            try {
                task.run();
                success = true;
                recordTaskCompletion(moduleName, true);
            } catch (Exception e) {
                System.err.println("Task '" + taskName + "' failed: " + e.getMessage());
                monitoring.recordModuleError(moduleName, "task_execution", e.getMessage());
                recordTaskCompletion(moduleName, false);
                throw e;
            } finally {
                long durationNs = System.nanoTime() - startTimeNs;
                recordTaskDuration(moduleName, durationNs / 1_000_000); // to milliseconds for legacy
                
                // Записываем в систему мониторинга
                monitoring.recordTaskExecution(moduleName, taskName, priority, durationNs, success);
            }
        };
        
        // Планируем в основном синхронизаторе
        switch (priority) {
            case HIGH -> underlying.scheduleHighPriority(taskName, wrappedTask);
            case NORMAL -> underlying.scheduleNormal(taskName, wrappedTask);
            case LOW -> underlying.scheduleLowPriority(taskName, wrappedTask);
        }
        
        // Обновляем статистику
        totalTasksSubmitted.incrementAndGet();
        updateModuleStats(moduleName, taskName);
    }
    
    // === РАСШИРЕННЫЕ МЕТОДЫ ===
    
    @Override
    public CompletableFuture<Void> executeWithTimeout(String taskName, Runnable task, 
                                                     long timeoutMs, TaskPriority priority) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        // Планируем задачу
        execute(taskName + "_WithTimeout", () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, priority);
        
        // Устанавливаем таймаут
        CompletableFuture.delayedExecutor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .execute(() -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new java.util.concurrent.TimeoutException(
                        "Task '" + taskName + "' timed out after " + timeoutMs + "ms"));
                }
            });
        
        return future;
    }
    
    @Override
    public CompletableFuture<Void> executeWithRetry(String taskName, Runnable task, 
                                                   int maxRetries, TaskPriority priority) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        executeWithRetryInternal(taskName, task, maxRetries, 0, priority, future);
        
        return future;
    }
    
    private void executeWithRetryInternal(String taskName, Runnable task, int maxRetries, 
                                        int currentAttempt, TaskPriority priority, 
                                        CompletableFuture<Void> future) {
        execute(taskName + "_Attempt" + (currentAttempt + 1), () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                if (currentAttempt < maxRetries) {
                    System.out.println("Task '" + taskName + "' failed on attempt " + (currentAttempt + 1) + 
                                     ", retrying... (" + (maxRetries - currentAttempt) + " attempts left)");
                    executeWithRetryInternal(taskName, task, maxRetries, currentAttempt + 1, priority, future);
                } else {
                    future.completeExceptionally(new RuntimeException(
                        "Task '" + taskName + "' failed after " + (maxRetries + 1) + " attempts", e));
                }
            }
        }, priority);
    }
    
    @Override
    public void scheduleDelayed(String taskName, Runnable task, long delayMs, TaskPriority priority) {
        // Простая реализация - планируем через CompletableFuture
        CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .execute(() -> execute(taskName + "_Delayed", task, priority));
    }
    
    @Override
    public IScheduledTask scheduleRepeating(String taskName, Runnable task, 
                                          long intervalMs, TaskPriority priority) {
        return new ScheduledTaskImpl(taskName, task, intervalMs, priority);
    }
    
    // === ПАКЕТНЫЕ ОПЕРАЦИИ ===
    
    @Override
    public CompletableFuture<Void> executeBatch(String batchName, TaskBatch batch, TaskPriority priority) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        execute(batchName, () -> {
            try {
                batch.execute();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, priority);
        
        return future;
    }
    
    // === УПРАВЛЕНИЕ МОДУЛЯМИ ===
    
    @Override
    public void configureModuleWeights(String moduleName, ModuleWeights weights) {
        RegisteredModule module = registeredModules.get(moduleName);
        if (module != null) {
            module.weights = weights;
            System.out.println("Updated weights for module " + moduleName + ": " + weights);
        }
    }
    
    @Override
    public IModuleHandle registerModule(String moduleName, ModuleInfo moduleInfo) {
        RegisteredModule module = new RegisteredModule(moduleInfo);
        registeredModules.put(moduleName, module);
        
        System.out.println("Registered module: " + moduleName + " v" + moduleInfo.version());
        
        return new ModuleHandleImpl(moduleName, module);
    }
    
    // === СТАТИСТИКА ===
    
    @Override
    public SynchronizerStats getStats() {
        var underlyingStats = underlying.getStats();
        var weights = underlying.getCurrentWeights();
        
        return new SynchronizerStats(
            underlyingStats.totalPending(),
            underlyingStats.highPriorityPending(),
            underlyingStats.normalPriorityPending(),
            underlyingStats.lowPriorityPending(),
            totalTasksCompleted.get(),
            underlyingStats.tasksPerTick(),
            registeredModules.size(),
            System.currentTimeMillis() - startTime,
            new WRRWeights(weights.highWeight(), weights.normalWeight(), weights.lowWeight())
        );
    }
    
    @Override
    public ModuleStats getModuleStats(String moduleName) {
        RegisteredModule module = registeredModules.get(moduleName);
        if (module == null) {
            return null;
        }
        
        return new ModuleStats(
            moduleName,
            module.totalSubmitted.get(),
            module.totalCompleted.get(),
            module.totalFailed.get(),
            getPendingTaskCount(moduleName),
            module.getAverageExecutionTime(),
            module.lastActivityTime,
            module.enabled
        );
    }
    
    @Override
    public int getPendingTaskCount(String moduleName) {
        // Приближенная оценка - в реальной реализации может быть более точной
        return 0;
    }
    
    @Override
    public HealthStatus getHealthStatus() {
        var stats = underlying.getStats();
        
        // Простая логика определения здоровья
        if (stats.totalPending() > 1000) {
            return HealthStatus.CRITICAL;
        } else if (stats.totalPending() > 500) {
            return HealthStatus.WARNING;
        } else {
            return HealthStatus.HEALTHY;
        }
    }
    
    // === ВНУТРЕННИЕ КЛАССЫ И УТИЛИТЫ ===
    
    /**
     * Извлечь имя модуля из имени задачи (предполагается формат "ModuleName_TaskName")
     */
    private String extractModuleName(String taskName) {
        int underscoreIndex = taskName.indexOf('_');
        return underscoreIndex > 0 ? taskName.substring(0, underscoreIndex) : "Unknown";
    }
    
    /**
     * Записать завершение задачи
     */
    private void recordTaskCompletion(String moduleName, boolean success) {
        if (success) {
            totalTasksCompleted.incrementAndGet();
        } else {
            totalTasksFailed.incrementAndGet();
        }
        
        RegisteredModule module = registeredModules.get(moduleName);
        if (module != null) {
            if (success) {
                module.totalCompleted.incrementAndGet();
            } else {
                module.totalFailed.incrementAndGet();
            }
            module.lastActivityTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Записать время выполнения задачи
     */
    private void recordTaskDuration(String moduleName, long duration) {
        RegisteredModule module = registeredModules.get(moduleName);
        if (module != null) {
            module.addExecutionTime(duration);
        }
    }
    
    /**
     * Обновить статистику модуля
     */
    private void updateModuleStats(String moduleName, String taskName) {
        RegisteredModule module = registeredModules.get(moduleName);
        if (module != null) {
            module.totalSubmitted.incrementAndGet();
            module.lastActivityTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Получить базовый синхронизатор (для внутреннего использования)
     */
    public MainThreadSynchronizer getUnderlyingSynchronizer() {
        return underlying;
    }
    
    // === ВНУТРЕННИЕ КЛАССЫ ===
    
    private static class RegisteredModule {
        final ModuleInfo info;
        volatile ModuleWeights weights;
        volatile boolean enabled = true;
        volatile long lastActivityTime = System.currentTimeMillis();
        
        final AtomicLong totalSubmitted = new AtomicLong(0);
        final AtomicLong totalCompleted = new AtomicLong(0);
        final AtomicLong totalFailed = new AtomicLong(0);
        final AtomicLong totalExecutionTime = new AtomicLong(0);
        final AtomicLong executionCount = new AtomicLong(0);
        
        RegisteredModule(ModuleInfo info) {
            this.info = info;
            this.weights = info.defaultWeights();
        }
        
        void addExecutionTime(long duration) {
            totalExecutionTime.addAndGet(duration);
            executionCount.incrementAndGet();
        }
        
        double getAverageExecutionTime() {
            long count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
        }
    }
    
    private class ModuleHandleImpl implements IModuleHandle {
        private final String moduleName;
        private final RegisteredModule module;
        
        ModuleHandleImpl(String moduleName, RegisteredModule module) {
            this.moduleName = moduleName;
            this.module = module;
        }
        
        @Override
        public void unregister() {
            registeredModules.remove(moduleName);
            System.out.println("Unregistered module: " + moduleName);
        }
        
        @Override
        public void updateWeights(ModuleWeights weights) {
            module.weights = weights;
        }
        
        @Override
        public ModuleStats getStats() {
            return MainThreadSynchronizerAdapter.this.getModuleStats(moduleName);
        }
        
        @Override
        public void setEnabled(boolean enabled) {
            module.enabled = enabled;
        }
    }
    
    private class ScheduledTaskImpl implements IScheduledTask {
        private final String taskName;
        private final Runnable task;
        private final long intervalMs;
        private final TaskPriority priority;
        private volatile boolean cancelled = false;
        private volatile boolean running = false;
        private volatile long nextExecutionTime;
        private final AtomicLong executionCount = new AtomicLong(0);
        
        ScheduledTaskImpl(String taskName, Runnable task, long intervalMs, TaskPriority priority) {
            this.taskName = taskName;
            this.task = task;
            this.intervalMs = intervalMs;
            this.priority = priority;
            this.nextExecutionTime = System.currentTimeMillis() + intervalMs;
            
            scheduleNext();
        }
        
        private void scheduleNext() {
            if (!cancelled) {
                scheduleDelayed(taskName + "_Repeating_" + executionCount.get(), () -> {
                    if (!cancelled) {
                        running = true;
                        try {
                            task.run();
                            executionCount.incrementAndGet();
                        } finally {
                            running = false;
                            nextExecutionTime = System.currentTimeMillis() + intervalMs;
                            scheduleNext();
                        }
                    }
                }, intervalMs, priority);
            }
        }
        
        @Override
        public void cancel() {
            cancelled = true;
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled;
        }
        
        @Override
        public boolean isRunning() {
            return running;
        }
        
        @Override
        public long getNextExecutionTime() {
            return nextExecutionTime;
        }
        
        @Override
        public int getExecutionCount() {
            return (int) executionCount.get();
        }
    }
}