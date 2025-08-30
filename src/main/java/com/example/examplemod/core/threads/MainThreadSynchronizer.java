package com.example.examplemod.core.threads;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * MainThreadSynchronizer Module - согласно MultiThread.txt
 * 
 * Единственный модуль который синхронизируется с Main Thread (Game Thread)
 * Все остальные потоки отправляют задачи сюда через thread-safe очереди
 * 
 * === WEIGHTED ROUND-ROBIN ПЛАНИРОВЩИК ===
 * 
 * Логика: Многоуровневая очередь с взвешенной обработкой
 * Вместо простых приоритетов используется система "кредитов":
 * 
 * 1. В начале каждого тика пополняем кредиты:
 *    - HIGH Priority: 5 кредитов (может обработать 5 задач)
 *    - NORMAL Priority: 3 кредита (может обработать 3 задачи)  
 *    - LOW Priority: 1 кредит (может обработать 1 задачу)
 * 
 * 2. Обрабатываем задачи в Round-Robin порядке:
 *    - Проверяем HIGH -> NORMAL -> LOW -> HIGH -> NORMAL -> LOW ...
 *    - Обрабатываем по 1 задаче за раз, тратя кредиты
 *    - Гарантированно обрабатываем LOW задачи даже при высокой нагрузке
 * 
 * 3. Пропорция обработки: HIGH:NORMAL:LOW = 5:3:1
 *    Это означает из каждых 9 обработанных задач:
 *    - 5 будут HIGH priority (55.6%)
 *    - 3 будут NORMAL priority (33.3%)
 *    - 1 будет LOW priority (11.1%)
 * 
 * Архитектура:
 * Worker Threads -> [MainThreadSynchronizer] -> Main Thread (Game Thread)
 */
public class MainThreadSynchronizer {
    
    private static MainThreadSynchronizer INSTANCE;
    
    // === THREAD-SAFE ОЧЕРЕДИ ===
    
    // Высокоприоритетные задачи (ресурсы, критические операции)
    private final ConcurrentLinkedQueue<MainThreadTask> highPriorityTasks = new ConcurrentLinkedQueue<>();
    
    // Обычные задачи (применение заклинаний, визуальные эффекты)
    private final ConcurrentLinkedQueue<MainThreadTask> normalPriorityTasks = new ConcurrentLinkedQueue<>();
    
    // Низкоприоритетные задачи (статистика, очистка)
    private final ConcurrentLinkedQueue<MainThreadTask> lowPriorityTasks = new ConcurrentLinkedQueue<>();
    
    // === СИНХРОНИЗИРУЮЩИЙ ПОТОК ===
    
    // Выделенный поток для подготовки задач к выполнению в Main Thread
    private final ExecutorService synchronizerThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    
    // === СТАТИСТИКА ===
    
    private final AtomicInteger totalTasksProcessed = new AtomicInteger(0);
    private final AtomicInteger tasksPerTick = new AtomicInteger(0);
    private volatile long lastTickTime = System.currentTimeMillis();
    
    // === WEIGHTED ROUND-ROBIN ПЛАНИРОВЩИК ===
    
    // Конфигурируемые веса для планировщика (сколько задач обрабатываем за цикл)
    private volatile int highPriorityWeight = 5;   // HIGH: 5 задач по умолчанию
    private volatile int normalPriorityWeight = 3; // NORMAL: 3 задачи по умолчанию
    private volatile int lowPriorityWeight = 1;    // LOW: 1 задача по умолчанию
    
    // Текущие кредиты для каждого уровня приоритета
    private volatile int highPriorityCredits = 0;
    private volatile int normalPriorityCredits = 0;
    private volatile int lowPriorityCredits = 0;
    
    // Счётчик циклов для отладки
    private volatile int roundRobinCycle = 0;
    
    // === КОНФИГУРАЦИЯ ===
    
    private static final int MAX_HIGH_PRIORITY_TASKS_PER_TICK = 5;
    private static final int MAX_NORMAL_TASKS_PER_TICK = 10;
    private static final int MAX_LOW_PRIORITY_TASKS_PER_TICK = 3;
    
    // Anti-starvation параметры
    private static final int MAX_TICKS_WITHOUT_LOW_PRIORITY = 20;
    private static final long MAX_TASK_AGE_MS = 5000;
    
    // Отслеживание для anti-starvation
    private volatile int ticksSinceLastLowPriorityExecution = 0;
    private volatile long oldestLowPriorityTaskTime = Long.MAX_VALUE;
    
    private MainThreadSynchronizer() {
        // Создаём поток-синхронизатор
        this.synchronizerThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MainThreadSynchronizer");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 2); // Максимальный приоритет
            return t;
        });
        
        // Запускаем фоновую обработку
        startBackgroundProcessing();
        
        System.out.println("MainThreadSynchronizer initialized");
    }
    
    public static MainThreadSynchronizer getInstance() {
        if (INSTANCE == null) {
            synchronized (MainThreadSynchronizer.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MainThreadSynchronizer();
                }
            }
        }
        return INSTANCE;
    }
    
    // === ПЛАНИРОВАНИЕ ЗАДАЧ ===
    
    /**
     * Запланировать высокоприоритетную задачу (ресурсы, критические операции)
     */
    public void scheduleHighPriority(String taskName, Runnable task) {
        MainThreadTask wrappedTask = new MainThreadTask(taskName, task, TaskPriority.HIGH);
        highPriorityTasks.offer(wrappedTask);
        
        System.out.println("Scheduled HIGH priority task: " + taskName);
    }
    
    /**
     * Запланировать обычную задачу (применение заклинаний)
     */
    public void scheduleNormal(String taskName, Runnable task) {
        MainThreadTask wrappedTask = new MainThreadTask(taskName, task, TaskPriority.NORMAL);
        normalPriorityTasks.offer(wrappedTask);
    }
    
    /**
     * Запланировать низкоприоритетную задачу (статистика, очистка)
     */
    public void scheduleLowPriority(String taskName, Runnable task) {
        MainThreadTask wrappedTask = new MainThreadTask(taskName, task, TaskPriority.LOW);
        lowPriorityTasks.offer(wrappedTask);
    }
    
    /**
     * Запланировать задачу с автоопределением приоритета
     */
    public void schedule(String taskName, Runnable task, TaskPriority priority) {
        switch (priority) {
            case HIGH -> scheduleHighPriority(taskName, task);
            case NORMAL -> scheduleNormal(taskName, task);
            case LOW -> scheduleLowPriority(taskName, task);
        }
    }
    
    // === ОБРАБОТКА В MAIN THREAD ===
    
    /**
     * Обработать все задачи в Main Thread
     * ВЫЗЫВАЕТСЯ ТОЛЬКО ИЗ Main Thread через ServerTickEvent!
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
        if (!isRunning.get()) return;
        
        long currentTime = System.currentTimeMillis();
        int processedThisTick = 0;
        
        try {
            // WEIGHTED ROUND-ROBIN: Пополняем кредиты в начале каждого тика
            replenishCredits();
            
            // Обрабатываем задачи в Round-Robin порядке пока есть кредиты
            int totalCredits = highPriorityCredits + normalPriorityCredits + lowPriorityCredits;
            int iterations = 0;
            final int MAX_ITERATIONS = 20; // Защита от бесконечного цикла
            
            while (totalCredits > 0 && iterations < MAX_ITERATIONS) {
                boolean taskProcessed = false;
                
                // Обрабатываем HIGH priority если есть кредиты и задачи
                if (highPriorityCredits > 0 && !highPriorityTasks.isEmpty()) {
                    int processed = processTasks(highPriorityTasks, 1, "HIGH");
                    if (processed > 0) {
                        highPriorityCredits--;
                        processedThisTick++;
                        taskProcessed = true;
                    }
                }
                
                // Обрабатываем NORMAL priority если есть кредиты и задачи  
                if (normalPriorityCredits > 0 && !normalPriorityTasks.isEmpty()) {
                    int processed = processTasks(normalPriorityTasks, 1, "NORMAL");
                    if (processed > 0) {
                        normalPriorityCredits--;
                        processedThisTick++;
                        taskProcessed = true;
                    }
                }
                
                // Обрабатываем LOW priority если есть кредиты и задачи
                if (lowPriorityCredits > 0 && !lowPriorityTasks.isEmpty()) {
                    int processed = processTasks(lowPriorityTasks, 1, "LOW");
                    if (processed > 0) {
                        lowPriorityCredits--;
                        processedThisTick++;
                        taskProcessed = true;
                    }
                }
                
                // Пересчитываем общие кредиты
                totalCredits = highPriorityCredits + normalPriorityCredits + lowPriorityCredits;
                
                // Если ни одной задачи не обработано, выходим (все очереди пусты)
                if (!taskProcessed) {
                    break;
                }
                
                iterations++;
            }
            
            // CREDIT REDISTRIBUTION: Перераспределяем неиспользованные кредиты
            redistributeUnusedCredits();
            
            if (processedThisTick > 0) {
                System.out.println("WRR Cycle " + roundRobinCycle + ": processed " + processedThisTick + 
                                 " tasks (HIGH:" + (highPriorityWeight - highPriorityCredits) + 
                                 ", NORMAL:" + (normalPriorityWeight - normalPriorityCredits) + 
                                 ", LOW:" + (lowPriorityWeight - lowPriorityCredits) + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Error in MainThreadSynchronizer tick processing: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Обновляем статистику
        totalTasksProcessed.addAndGet(processedThisTick);
        tasksPerTick.set(processedThisTick);
        lastTickTime = currentTime;
    }
    
    /**
     * Обработать задачи из конкретной очереди
     */
    private int processTasks(ConcurrentLinkedQueue<MainThreadTask> queue, int maxTasks, String priorityName) {
        int processed = 0;
        MainThreadTask task;
        
        while ((task = queue.poll()) != null && processed < maxTasks) {
            try {
                long startTime = System.nanoTime();
                
                // ВЫПОЛНЯЕМ В MAIN THREAD - здесь безопасно использовать Minecraft API
                task.getTask().run();
                
                long duration = System.nanoTime() - startTime;
                processed++;
                
                // Логируем только медленные задачи
                if (duration > 1_000_000) { // > 1ms
                    System.out.println("Executed " + priorityName + " task '" + task.getName() + 
                                     "' in " + (duration / 1_000_000.0) + "ms");
                }
                
            } catch (Exception e) {
                System.err.println("Error executing " + priorityName + " task '" + task.getName() + "': " + e.getMessage());
            }
        }
        
        return processed;
    }
    
    // === ФОНОВАЯ ОБРАБОТКА ===
    
    /**
     * Запуск фонового потока для предобработки задач
     */
    private void startBackgroundProcessing() {
        synchronizerThread.submit(() -> {
            while (isRunning.get()) {
                try {
                    // Фоновая обработка: группировка задач, оптимизация очередей и т.д.
                    optimizeTaskQueues();
                    
                    // Спим 50ms между итерациями
                    Thread.sleep(50);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error in MainThreadSynchronizer background processing: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Оптимизация очередей задач (фоновый поток)
     */
    private void optimizeTaskQueues() {
        // TODO: Группировка похожих задач
        // TODO: Удаление устаревших задач
        // TODO: Балансировка приоритетов
        
        // Пока что простая проверка переполнения
        if (getTotalPendingTasks() > 1000) {
            System.err.println("WARNING: MainThreadSynchronizer queue overflow: " + getTotalPendingTasks() + " tasks pending!");
        }
    }
    
    // === СТАТИСТИКА И МОНИТОРИНГ ===
    
    public int getHighPriorityPendingTasks() { return highPriorityTasks.size(); }
    public int getNormalPriorityPendingTasks() { return normalPriorityTasks.size(); }
    public int getLowPriorityPendingTasks() { return lowPriorityTasks.size(); }
    
    public int getTotalPendingTasks() {
        return getHighPriorityPendingTasks() + getNormalPriorityPendingTasks() + getLowPriorityPendingTasks();
    }
    
    public int getTotalTasksProcessed() { return totalTasksProcessed.get(); }
    public int getTasksPerTick() { return tasksPerTick.get(); }
    
    public SynchronizerStats getStats() {
        return new SynchronizerStats(
            getTotalPendingTasks(),
            getHighPriorityPendingTasks(),
            getNormalPriorityPendingTasks(), 
            getLowPriorityPendingTasks(),
            getTotalTasksProcessed(),
            getTasksPerTick()
        );
    }
    
    // === SHUTDOWN ===
    
    public void shutdown() {
        System.out.println("Shutting down MainThreadSynchronizer...");
        
        isRunning.set(false);
        synchronizerThread.shutdown();
        
        // Очищаем очереди
        int clearedTasks = highPriorityTasks.size() + normalPriorityTasks.size() + lowPriorityTasks.size();
        highPriorityTasks.clear();
        normalPriorityTasks.clear();
        lowPriorityTasks.clear();
        
        System.out.println("MainThreadSynchronizer shutdown complete. Cleared " + clearedTasks + " pending tasks.");
    }
    
    // === WEIGHTED ROUND-ROBIN МЕТОДЫ ===
    
    /**
     * Пополнить кредиты для всех уровней приоритета
     * Вызывается в начале каждого тика
     */
    private void replenishCredits() {
        highPriorityCredits = highPriorityWeight;
        normalPriorityCredits = normalPriorityWeight; 
        lowPriorityCredits = lowPriorityWeight;
        
        roundRobinCycle++;
        
        // Отладочный вывод каждые 100 циклов
        if (roundRobinCycle % 100 == 0) {
            System.out.println("WRR: Starting cycle " + roundRobinCycle + 
                             " with credits HIGH:" + highPriorityCredits + 
                             ", NORMAL:" + normalPriorityCredits + 
                             ", LOW:" + lowPriorityCredits);
        }
    }
    
    /**
     * Перераспределить неиспользованные кредиты между доступными очередями
     * Вызывается после основного Round-Robin цикла
     */
    private void redistributeUnusedCredits() {
        // Собираем неиспользованные кредиты
        int unusedCredits = highPriorityCredits + normalPriorityCredits + lowPriorityCredits;
        if (unusedCredits == 0) {
            return; // Нет неиспользованных кредитов
        }
        
        // Определяем какие очереди имеют задачи
        boolean hasHighTasks = !highPriorityTasks.isEmpty();
        boolean hasNormalTasks = !normalPriorityTasks.isEmpty();
        boolean hasLowTasks = !lowPriorityTasks.isEmpty();
        
        int availableQueues = (hasHighTasks ? 1 : 0) + (hasNormalTasks ? 1 : 0) + (hasLowTasks ? 1 : 0);
        
        if (availableQueues == 0) {
            return; // Нет задач для обработки
        }
        
        System.out.println("CREDIT REDISTRIBUTION: " + unusedCredits + " unused credits, " + 
                         availableQueues + " queues with tasks");
        
        int processedInRedistribution = 0;
        
        // Перераспределяем кредиты поровну между доступными очередями
        int creditsPerQueue = unusedCredits / availableQueues;
        int remainingCredits = unusedCredits % availableQueues;
        
        // HIGH priority получает дополнительные кредиты если есть задачи
        if (hasHighTasks && creditsPerQueue > 0) {
            int extraHigh = creditsPerQueue + (remainingCredits > 0 ? 1 : 0);
            if (remainingCredits > 0) remainingCredits--;
            
            int processed = processTasks(highPriorityTasks, extraHigh, "HIGH (redistributed)");
            processedInRedistribution += processed;
            System.out.println("HIGH: used " + processed + "/" + extraHigh + " redistributed credits");
        }
        
        // NORMAL priority получает дополнительные кредиты если есть задачи
        if (hasNormalTasks && creditsPerQueue > 0) {
            int extraNormal = creditsPerQueue + (remainingCredits > 0 ? 1 : 0);
            if (remainingCredits > 0) remainingCredits--;
            
            int processed = processTasks(normalPriorityTasks, extraNormal, "NORMAL (redistributed)");
            processedInRedistribution += processed;
            System.out.println("NORMAL: used " + processed + "/" + extraNormal + " redistributed credits");
        }
        
        // LOW priority получает дополнительные кредиты если есть задачи
        if (hasLowTasks && creditsPerQueue > 0) {
            int extraLow = creditsPerQueue + (remainingCredits > 0 ? 1 : 0);
            
            int processed = processTasks(lowPriorityTasks, extraLow, "LOW (redistributed)");
            processedInRedistribution += processed;
            System.out.println("LOW: used " + processed + "/" + extraLow + " redistributed credits");
        }
        
        // Обнуляем все кредиты после перераспределения
        highPriorityCredits = 0;
        normalPriorityCredits = 0;
        lowPriorityCredits = 0;
        
        if (processedInRedistribution > 0) {
            System.out.println("CREDIT REDISTRIBUTION: processed " + processedInRedistribution + 
                             " additional tasks using unused credits");
        }
    }
    
    // === КОНФИГУРИРОВАНИЕ ВЕСОВ ===
    
    /**
     * Установить веса для Weighted Round-Robin планировщика
     * @param high Количество HIGH priority задач за тик (1-10)
     * @param normal Количество NORMAL priority задач за тик (1-8)  
     * @param low Количество LOW priority задач за тик (1-5)
     */
    public void configureWeights(int high, int normal, int low) {
        // Валидация входных параметров
        high = Math.max(1, Math.min(10, high));
        normal = Math.max(1, Math.min(8, normal));
        low = Math.max(1, Math.min(5, low));
        
        this.highPriorityWeight = high;
        this.normalPriorityWeight = normal;
        this.lowPriorityWeight = low;
        
        System.out.println("WRR weights configured: HIGH=" + high + ", NORMAL=" + normal + ", LOW=" + low);
        System.out.println("New proportions: HIGH=" + String.format("%.1f", (float)high/(high+normal+low)*100) + 
                         "%, NORMAL=" + String.format("%.1f", (float)normal/(high+normal+low)*100) + 
                         "%, LOW=" + String.format("%.1f", (float)low/(high+normal+low)*100) + "%");
    }
    
    /**
     * Сбросить веса к значениям по умолчанию (5:3:1)
     */
    public void resetWeightsToDefault() {
        configureWeights(5, 3, 1);
    }
    
    /**
     * Получить текущие веса
     */
    public WRRWeights getCurrentWeights() {
        return new WRRWeights(highPriorityWeight, normalPriorityWeight, lowPriorityWeight);
    }
    
    // === УСТАРЕВШИЕ ANTI-STARVATION МЕТОДЫ (для совместимости) ===
    
    /**
     * Проверить нужно ли принудительно выполнить LOW задачи
     */
    private boolean shouldForceLowPriorityExecution() {
        // Условие 1: Прошло слишком много тиков без LOW задач
        if (ticksSinceLastLowPriorityExecution >= MAX_TICKS_WITHOUT_LOW_PRIORITY) {
            System.out.println("ANTI-STARVATION: Forcing LOW priority execution - " + 
                             ticksSinceLastLowPriorityExecution + " ticks without LOW tasks");
            return true;
        }
        
        // Условие 2: Самая старая LOW задача слишком старая
        long currentTime = System.currentTimeMillis();
        if (oldestLowPriorityTaskTime != Long.MAX_VALUE && 
            currentTime - oldestLowPriorityTaskTime > MAX_TASK_AGE_MS) {
            System.out.println("ANTI-STARVATION: Forcing LOW priority execution - oldest task age: " + 
                             (currentTime - oldestLowPriorityTaskTime) + "ms");
            return true;
        }
        
        // Условие 3: Слишком много LOW задач накопилось (> 50)
        if (getLowPriorityPendingTasks() > 50) {
            System.out.println("ANTI-STARVATION: Forcing LOW priority execution - " + 
                             getLowPriorityPendingTasks() + " LOW tasks queued");
            return true;
        }
        
        return false;
    }
    
    /**
     * Обновить время самой старой LOW задачи
     */
    private void updateOldestLowPriorityTime() {
        MainThreadTask oldestTask = null;
        long oldestTime = Long.MAX_VALUE;
        
        // Находим самую старую задачу без итерации (approx)
        if (!lowPriorityTasks.isEmpty()) {
            // Берем первую задачу как приближение (очередь FIFO)
            oldestTask = lowPriorityTasks.peek();
            if (oldestTask != null) {
                oldestTime = oldestTask.getCreationTime();
            }
        }
        
        oldestLowPriorityTaskTime = oldestTime;
    }
    
    // === ВНУТРЕННИЕ КЛАССЫ ===
    
    public enum TaskPriority {
        HIGH,    // Ресурсы, критические операции
        NORMAL,  // Применение заклинаний, визуальные эффекты  
        LOW      // Статистика, очистка
    }
    
    private static class MainThreadTask {
        private final String name;
        private final Runnable task;
        private final TaskPriority priority;
        private final long creationTime;
        
        public MainThreadTask(String name, Runnable task, TaskPriority priority) {
            this.name = name;
            this.task = task;
            this.priority = priority;
            this.creationTime = System.currentTimeMillis();
        }
        
        public String getName() { return name; }
        public Runnable getTask() { return task; }
        public TaskPriority getPriority() { return priority; }
        public long getCreationTime() { return creationTime; }
        public long getAge() { return System.currentTimeMillis() - creationTime; }
    }
    
    public record SynchronizerStats(
        int totalPending,
        int highPriorityPending,
        int normalPriorityPending,
        int lowPriorityPending,
        int totalProcessed,
        int tasksPerTick
    ) {}
    
    /**
     * Record для хранения конфигурации весов WRR
     */
    public record WRRWeights(
        int highWeight,
        int normalWeight, 
        int lowWeight
    ) {
        public int totalWeight() {
            return highWeight + normalWeight + lowWeight;
        }
        
        public float highPercentage() {
            return (float) highWeight / totalWeight() * 100;
        }
        
        public float normalPercentage() {
            return (float) normalWeight / totalWeight() * 100;
        }
        
        public float lowPercentage() {
            return (float) lowWeight / totalWeight() * 100;
        }
        
        @Override
        public String toString() {
            return String.format("WRR[%d:%d:%d] (%.1f%%:%.1f%%:%.1f%%)", 
                    highWeight, normalWeight, lowWeight,
                    highPercentage(), normalPercentage(), lowPercentage());
        }
    }
}