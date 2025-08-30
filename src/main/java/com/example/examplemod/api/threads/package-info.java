/**
 * MainThreadSynchronizer API - система многопоточной синхронизации для Minecraft модов
 * 
 * === ОБЗОР ===
 * 
 * Этот пакет предоставляет thread-safe способ выполнения задач в Main Thread Minecraft
 * из любых worker threads. Использует Weighted Round-Robin планировщик для справедливого
 * распределения ресурсов между модулями.
 * 
 * === ОСНОВНЫЕ КОМПОНЕНТЫ ===
 * 
 * • {@link com.example.examplemod.api.threads.IMainThreadSynchronizer} - Основной интерфейс API
 * • {@link com.example.examplemod.api.threads.MainThreadSynchronizerAPI} - Статический доступ
 * • {@link com.example.examplemod.api.threads.MainThreadSynchronizerAdapter} - Реализация с расширенными функциями
 * • {@link com.example.examplemod.api.threads.ThreadMonitoringService} - Мониторинг производительности
 * 
 * === БЫСТРЫЙ СТАРТ ===
 * 
 * <pre>{@code
 * // 1. Простое выполнение задачи
 * MainThreadSynchronizerAPI.execute("MyMod_UpdateBlocks", () -> {
 *     level.setBlock(pos, Blocks.STONE.defaultBlockState());
 * });
 * 
 * // 2. Задача с результатом
 * CompletableFuture<String> result = MainThreadSynchronizerAPI.submit("MyMod_GetData", () -> {
 *     return player.getName().getString();
 * });
 * 
 * // 3. Приоритезированные задачи
 * MainThreadSynchronizerAPI.executeHighPriority("Critical_SaveData", criticalTask);
 * MainThreadSynchronizerAPI.executeLowPriority("Background_Cleanup", cleanupTask);
 * 
 * // 4. Регистрация модуля для детальной статистики
 * IMainThreadSynchronizer.ModuleInfo info = new IMainThreadSynchronizer.ModuleInfo(
 *     "MyModule", "1.0.0", "Описание модуля", 
 *     IMainThreadSynchronizer.ModuleWeights.balanced()
 * );
 * IMainThreadSynchronizer.IModuleHandle handle = 
 *     MainThreadSynchronizerAPI.registerModule("MyModule", info);
 * }</pre>
 * 
 * === ПРИОРИТЕТЫ ЗАДАЧ ===
 * 
 * • **HIGH** - Критические операции (сохранение данных, ресурсы, безопасность)
 * • **NORMAL** - Обычные игровые операции (блоки, заклинания, взаимодействия)
 * • **LOW** - Фоновые задачи (статистика, очистка, визуальные эффекты)
 * 
 * === ПЛАНИРОВЩИК WEIGHTED ROUND-ROBIN ===
 * 
 * Использует кредитную систему для справедливого распределения:
 * • HIGH: 5 кредитов за раунд
 * • NORMAL: 3 кредита за раунд  
 * • LOW: 1 кредит за раунд
 * 
 * Неиспользованные кредиты перераспределяются между доступными приоритетами,
 * что предотвращает простой системы и обеспечивает максимальную пропускную способность.
 * 
 * === РАСШИРЕННЫЕ ВОЗМОЖНОСТИ ===
 * 
 * • **Timeout** - Автоматическая отмена задач по времени
 * • **Retry** - Повторные попытки при ошибках
 * • **Delayed** - Отложенное выполнение
 * • **Repeating** - Периодические задачи
 * • **Batch** - Пакетные операции
 * 
 * <pre>{@code
 * // Задача с таймаутом
 * api.executeWithTimeout("LongTask", task, 5000, TaskPriority.NORMAL);
 * 
 * // Задача с повторами
 * api.executeWithRetry("UnstableTask", task, 3, TaskPriority.HIGH);
 * 
 * // Отложенная задача
 * api.scheduleDelayed("DelayedTask", task, 2000, TaskPriority.LOW);
 * 
 * // Повторяющаяся задача
 * IScheduledTask repeating = api.scheduleRepeating("PeriodicTask", task, 1000, TaskPriority.LOW);
 * // ... позже
 * repeating.cancel();
 * }</pre>
 * 
 * === МОНИТОРИНГ И ДИАГНОСТИКА ===
 * 
 * Система предоставляет подробную статистику:
 * 
 * <pre>{@code
 * // Общая статистика системы
 * SynchronizerStats stats = MainThreadSynchronizerAPI.getStats();
 * 
 * // Детальный отчет о здоровье
 * SystemHealthReport health = MainThreadSynchronizerAPI.getInstance().getSystemHealthReport();
 * 
 * // Проблемные модули
 * List<String> problematic = MainThreadSynchronizerAPI.getInstance().getProblematicModules();
 * 
 * // Статистика по приоритетам
 * Map<TaskPriority, PriorityStats> priorityStats = 
 *     MainThreadSynchronizerAPI.getInstance().getPriorityStats();
 * }</pre>
 * 
 * === УПРАВЛЕНИЕ МОДУЛЯМИ ===
 * 
 * <pre>{@code
 * // Регистрация модуля
 * IModuleHandle handle = MainThreadSynchronizerAPI.registerModule(name, info);
 * 
 * // Изменение весов
 * handle.updateWeights(ModuleWeights.highPriorityFocused());
 * 
 * // Получение статистики модуля
 * ModuleStats moduleStats = handle.getStats();
 * 
 * // Временное отключение
 * handle.setEnabled(false);
 * 
 * // Отмена регистрации
 * handle.unregister();
 * }</pre>
 * 
 * === ПРИМЕРЫ ИСПОЛЬЗОВАНИЯ ===
 * 
 * Смотрите примеры в пакете examples:
 * • {@link com.example.examplemod.api.threads.examples.BasicModuleExample} - Базовое использование
 * • {@link com.example.examplemod.api.threads.examples.AdvancedModuleExample} - Расширенные возможности
 * • {@link com.example.examplemod.api.threads.examples.GameplayModuleExample} - Игровые сценарии
 * 
 * === АРХИТЕКТУРНЫЕ ПРИНЦИПЫ ===
 * 
 * 1. **Thread Safety** - Все операции thread-safe
 * 2. **Non-blocking** - Планирование не блокирует вызывающий поток
 * 3. **Fair Scheduling** - Справедливое распределение ресурсов
 * 4. **Graceful Degradation** - Система работает даже при ошибках модулей
 * 5. **Monitoring** - Подробная диагностика для отладки
 * 
 * === ИНТЕГРАЦИЯ С ДРУГИМИ МОДУЛЯМИ ===
 * 
 * Для интеграции MainThreadSynchronizer с другими модулями:
 * 
 * <pre>{@code
 * // В вашем модуле
 * public class YourModule {
 *     private IMainThreadSynchronizer.IModuleHandle handle;
 *     
 *     public void initialize() {
 *         ModuleInfo info = new ModuleInfo("YourModule", "1.0.0", 
 *                                         "Your module description",
 *                                         ModuleWeights.balanced());
 *         handle = MainThreadSynchronizerAPI.registerModule("YourModule", info);
 *     }
 *     
 *     public void doWork() {
 *         MainThreadSynchronizerAPI.execute("YourModule_Work", () -> {
 *             // Ваш код для Main Thread
 *         });
 *     }
 *     
 *     public void shutdown() {
 *         if (handle != null) {
 *             handle.unregister();
 *         }
 *     }
 * }
 * }</pre>
 * 
 * === ПРОИЗВОДИТЕЛЬНОСТЬ ===
 * 
 * • Минимальные накладные расходы на планирование (~1-2 микросекунды)
 * • Efficient credit redistribution предотвращает простой
 * • Lock-free статистика для высокой пропускной способности
 * • Circular buffers для минимизации GC pressure
 * 
 * === СОВМЕСТИМОСТЬ ===
 * 
 * • Minecraft 1.21.5+
 * • NeoForge
 * • Java 21+
 * • Thread-safe для использования из любых потоков
 * 
 * @since 1.0.0
 * @author CombatMetaphysics Development Team
 */
package com.example.examplemod.api.threads;