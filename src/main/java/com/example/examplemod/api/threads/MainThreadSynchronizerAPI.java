package com.example.examplemod.api.threads;

import com.example.examplemod.core.threads.MainThreadSynchronizer;

/**
 * Публичный API для MainThreadSynchronizer
 * 
 * Этот класс предоставляет статический доступ к MainThreadSynchronizer
 * для других модулей системы. Скрывает детали реализации за чистым API.
 * 
 * === БЕЗОПАСНОСТЬ ===
 * - Thread-safe: все методы могут вызываться из любых потоков
 * - Non-blocking: планирование задач не блокирует вызывающий поток
 * - Graceful degradation: если синхронизатор не инициализирован, задачи выполняются синхронно
 * 
 * === ПРОИЗВОДИТЕЛЬНОСТЬ ===
 * - Минимальные накладные расходы на планирование
 * - Weighted Round-Robin обеспечивает справедливое распределение ресурсов
 * - Автоматическая оптимизация очередей в фоновом режиме
 * 
 * === ПРИМЕР ИСПОЛЬЗОВАНИЯ ===
 * 
 * ```java
 * // Простая задача
 * MainThreadSynchronizerAPI.execute("MyMod_UpdateBlocks", () -> {
 *     level.setBlock(pos, Blocks.STONE.defaultBlockState());
 * });
 * 
 * // Критическая задача
 * MainThreadSynchronizerAPI.executeHighPriority("ResourceManager_SaveData", () -> {
 *     savePlayerData(player);
 * });
 * 
 * // Задача с результатом
 * CompletableFuture<Integer> result = MainThreadSynchronizerAPI.submit("Calculator_GetValue", () -> {
 *     return calculateSomething();
 * });
 * 
 * result.thenAccept(value -> {
 *     System.out.println("Calculation result: " + value);
 * });
 * ```
 */
public final class MainThreadSynchronizerAPI {
    
    private static volatile IMainThreadSynchronizer instance;
    private static final Object LOCK = new Object();
    
    private MainThreadSynchronizerAPI() {
        // Утилитный класс
    }
    
    /**
     * Получить экземпляр MainThreadSynchronizer API
     * 
     * @return IMainThreadSynchronizer экземпляр
     * @throws IllegalStateException если синхронизатор не был инициализирован
     */
    public static IMainThreadSynchronizer getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    // Создаем адаптер для MainThreadSynchronizer
                    instance = new MainThreadSynchronizerAdapter(MainThreadSynchronizer.getInstance());
                }
            }
        }
        return instance;
    }
    
    /**
     * Проверить инициализирован ли синхронизатор
     */
    public static boolean isInitialized() {
        return instance != null;
    }
    
    /**
     * Принудительно инициализировать синхронизатор
     * Обычно не требуется - инициализируется автоматически при первом обращении
     */
    public static void initialize() {
        getInstance(); // Принудительная инициализация
    }
    
    // === СТАТИЧЕСКИЕ МЕТОДЫ ДЛЯ УДОБСТВА ===
    
    /**
     * Выполнить задачу в Main Thread с NORMAL приоритетом
     */
    public static void execute(String taskName, Runnable task) {
        getInstance().execute(taskName, task);
    }
    
    /**
     * Выполнить задачу с возвратом результата
     */
    public static <T> java.util.concurrent.CompletableFuture<T> submit(String taskName, java.util.function.Supplier<T> task) {
        return getInstance().submit(taskName, task);
    }
    
    /**
     * Выполнить высокоприоритетную задачу
     */
    public static void executeHighPriority(String taskName, Runnable task) {
        getInstance().executeHighPriority(taskName, task);
    }
    
    /**
     * Выполнить низкоприоритетную задачу
     */
    public static void executeLowPriority(String taskName, Runnable task) {
        getInstance().executeLowPriority(taskName, task);
    }
    
    /**
     * Выполнить задачу с указанным приоритетом
     */
    public static void execute(String taskName, Runnable task, IMainThreadSynchronizer.TaskPriority priority) {
        getInstance().execute(taskName, task, priority);
    }
    
    /**
     * Получить статистику работы синхронизатора
     */
    public static IMainThreadSynchronizer.SynchronizerStats getStats() {
        return getInstance().getStats();
    }
    
    /**
     * Зарегистрировать модуль в системе
     */
    public static IMainThreadSynchronizer.IModuleHandle registerModule(String moduleName, 
                                                                       IMainThreadSynchronizer.ModuleInfo moduleInfo) {
        return getInstance().registerModule(moduleName, moduleInfo);
    }
    
    /**
     * Настроить веса планировщика глобально
     */
    public static void configureGlobalWeights(int highWeight, int normalWeight, int lowWeight) {
        if (instance != null && instance instanceof MainThreadSynchronizerAdapter adapter) {
            adapter.getUnderlyingSynchronizer().configureWeights(highWeight, normalWeight, lowWeight);
        }
    }
    
    /**
     * Получить статус здоровья системы
     */
    public static IMainThreadSynchronizer.HealthStatus getHealthStatus() {
        if (!isInitialized()) {
            return IMainThreadSynchronizer.HealthStatus.SHUTDOWN;
        }
        return getInstance().getHealthStatus();
    }
    
    // === ВНУТРЕННИЕ УТИЛИТЫ ===
    
    /**
     * Метод для тестирования - позволяет установить mock экземпляр
     */
    static void setInstanceForTesting(IMainThreadSynchronizer testInstance) {
        synchronized (LOCK) {
            instance = testInstance;
        }
    }
    
    /**
     * Очистить экземпляр (для shutdown)
     */
    static void reset() {
        synchronized (LOCK) {
            instance = null;
        }
    }
    
    // === ИНФОРМАЦИЯ О ВЕРСИИ ===
    
    public static final String API_VERSION = "1.0.0";
    public static final String COMPATIBLE_MOD_VERSION = "0.0.0.0.0.40a+";
    
    /**
     * Проверить совместимость версии API
     */
    public static boolean isVersionCompatible(String modVersion) {
        // Простая проверка совместимости
        return modVersion.contains("0.0.0.0.0.40a") || modVersion.compareTo(COMPATIBLE_MOD_VERSION) >= 0;
    }
    
    /**
     * Получить информацию о версии API
     */
    public static String getVersionInfo() {
        return String.format("MainThreadSynchronizer API v%s (Compatible with mod v%s+)", 
                           API_VERSION, COMPATIBLE_MOD_VERSION);
    }
}