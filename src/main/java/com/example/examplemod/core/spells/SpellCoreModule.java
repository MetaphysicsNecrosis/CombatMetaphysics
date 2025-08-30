package com.example.examplemod.core.spells;

import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationTask;
import com.example.examplemod.core.spells.computation.SpellComputationTaskResult;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.instances.SpellInstance;
import com.example.examplemod.core.spells.collision.CollisionSnapshot;
import com.example.examplemod.core.geometry.CollisionDetector;
import com.example.examplemod.core.geometry.SpellShape;
import com.example.examplemod.core.threads.MainThreadSynchronizer;
import com.example.examplemod.api.threads.MainThreadSynchronizerAPI;
import com.example.examplemod.api.threads.IMainThreadSynchronizer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
// import net.neoforged.neoforge.common.util.TriState; // TODO: Fix import

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SpellCore Module - ядро системы заклинаний
 * 
 * Реализует многопоточную архитектуру из MultiThread.txt:
 * Main Thread -> Spell Computation Pool -> Collision Thread -> Aggregation Thread -> Main Thread
 * 
 * Компоненты:
 * - SpellDefinitionRegistry: Хранилище определений заклинаний
 * - SpellFactory: Создание экземпляров заклинаний  
 * - SpellLifecycleManager: Управление жизненным циклом
 * - SpellStateCache: Кэш состояний для быстрого доступа
 */
public class SpellCoreModule {
    
    private static SpellCoreModule INSTANCE;
    
    // === Thread Pools (согласно MultiThread.txt) ===
    private final ForkJoinPool spellComputationPool;
    private final ExecutorService collisionThread;
    private final ExecutorService aggregationThread;
    private final ExecutorService resourceThread;
    
    // Main Thread Synchronizer для безопасного возврата к Main Thread
    private final MainThreadSynchronizer mainThreadSynchronizer;
    private final IMainThreadSynchronizer.IModuleHandle moduleHandle;
    
    // === Core Components ===
    private final SpellDefinitionRegistry definitionRegistry;
    private final SpellFactory spellFactory;
    private final SpellLifecycleManager lifecycleManager;
    private final SpellStateCache stateCache;
    
    // === Active Spells Tracking ===
    private final Map<UUID, SpellInstance> activeSpells = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<SpellComputationTaskResult>> computationFutures = new ConcurrentHashMap<>();
    
    private SpellCoreModule() {
        // Инициализация thread pools согласно MultiThread.txt
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        // Spell Computation Pool: 4-8 потоков для вычислений
        this.spellComputationPool = new ForkJoinPool(Math.min(8, Math.max(4, cpuCores)));
        
        // Выделенный поток для коллизий
        this.collisionThread = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SpellCollisionThread");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1); // Высокий приоритет
            return t;
        });
        
        // Поток для агрегации результатов
        this.aggregationThread = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SpellAggregationThread");
            t.setDaemon(true);
            return t;
        });
        
        // Resource Management Thread (согласно MultiThread.txt)
        this.resourceThread = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SpellResourceThread");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1); // Высокий приоритет для ресурсов
            return t;
        });
        
        // Инициализация компонентов
        this.definitionRegistry = new SpellDefinitionRegistry();
        this.spellFactory = new SpellFactory(definitionRegistry);
        this.lifecycleManager = new SpellLifecycleManager();
        this.stateCache = new SpellStateCache();
        this.mainThreadSynchronizer = MainThreadSynchronizer.getInstance();
        
        // Регистрируем модуль в MainThreadSynchronizer через API
        IMainThreadSynchronizer.ModuleInfo moduleInfo = new IMainThreadSynchronizer.ModuleInfo(
            "SpellCore",
            "1.0.0",
            "Ядро системы заклинаний с многопоточными вычислениями",
            IMainThreadSynchronizer.ModuleWeights.highPriorityFocused()  // Заклинания требуют высокого приоритета
        );
        this.moduleHandle = MainThreadSynchronizerAPI.registerModule("SpellCore", moduleInfo);
        
        System.out.println("SpellCoreModule initialized with " + spellComputationPool.getParallelism() + " computation workers");
    }
    
    public static SpellCoreModule getInstance() {
        if (INSTANCE == null) {
            synchronized (SpellCoreModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SpellCoreModule();
                }
            }
        }
        return INSTANCE;
    }
    
    // === Main Thread API (NeoForge-safe) ===
    
    /**
     * Начать кастование заклинания (вызывается из Main Thread)
     * Создаёт thread-safe контекст и отправляет в Spell Computation Pool
     */
    public CompletableFuture<SpellComputationTaskResult> castSpell(SpellDefinition definition, 
                                                                  SpellParameters parameters,
                                                                  Level level, Player caster) {
        // Создаём thread-safe контекст (снепшоты данных, без ссылок на Minecraft объекты)
        SpellComputationContext context = createSafeContext(definition, level, caster);
        
        // Создаём задачу для Spell Computation Pool
        SpellComputationTask task = SpellComputationTask.create(context, parameters);
        
        // Отправляем в computation pool
        CompletableFuture<SpellComputationTaskResult> computationFuture = 
            CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    return SpellComputationTaskResult.error(context.getSpellInstanceId(), e, System.nanoTime());
                }
            }, spellComputationPool);
        
        // Регистрируем future для отслеживания
        computationFutures.put(context.getSpellInstanceId(), computationFuture);
        
        // Настраиваем pipeline: Computation -> Resource -> Collision -> Aggregation -> Main Thread Queue
        return computationFuture
            .thenComposeAsync(this::processResources, resourceThread)        // Resource Thread
            .thenComposeAsync(this::processCollisions, collisionThread)      // Collision Thread
            .thenComposeAsync(this::aggregateResults, aggregationThread)     // Aggregation Thread
            .whenComplete(this::scheduleMainThreadApplication);              // Schedule for Main Thread
    }
    
    /**
     * Создать thread-safe контекст вычисления (Main Thread)
     */
    private SpellComputationContext createSafeContext(SpellDefinition definition, Level level, Player caster) {
        UUID spellId = UUID.randomUUID();
        
        // Создаём снепшот коллизий для thread-safe обработки
        net.minecraft.world.phys.AABB searchArea = new net.minecraft.world.phys.AABB(
            caster.getX() - 16, caster.getY() - 8, caster.getZ() - 16,
            caster.getX() + 16, caster.getY() + 8, caster.getZ() + 16
        );
        CollisionSnapshot collisionSnapshot = new CollisionSnapshot(level, searchArea);
        
        // Создаём снепшоты данных без ссылок на Minecraft объекты
        return new SpellComputationContext(
            spellId,
            definition.type(),
            definition.baseParameters(),
            (float) level.getGameTime(),
            caster.getX(), caster.getY(), caster.getZ(),
            caster.getYRot(), caster.getXRot(),
            100.0f, // TODO: получить реальную ману кастера
            90.0f,  // TODO: доступная мана инициации
            80.0f,  // TODO: доступная мана усиления
            level.isRaining(),
            level.getDayTime(),
            level.dimension().hashCode(),
            collisionSnapshot
        );
    }
    
    /**
     * Обработка ресурсов (Resource Management Thread)
     */
    private CompletableFuture<SpellComputationTaskResult> processResources(SpellComputationTaskResult computationResult) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Проверяем финальную стоимость маны на основе вычисленных параметров
                double totalManaCost = computationResult.getParameterValue("final_mana_cost", 0.0);
                double amplificationCost = computationResult.getParameterValue("amplification_cost", 0.0);
                
                // TODO: Интеграция с реальными ManaPool игрока
                // Пока что просто логируем затраты ресурсов
                System.out.println("Resource processing - Mana cost: " + totalManaCost + 
                                 ", Amplification: " + amplificationCost + 
                                 " for spell: " + computationResult.getSpellInstanceId());
                
                // Добавляем информацию о затратах ресурсов в результат
                computationResult.getAggregatedValues().put("resource_validated", true);
                computationResult.getAggregatedValues().put("final_mana_cost", totalManaCost);
                computationResult.getAggregatedValues().put("final_amplification_cost", amplificationCost);
                
            } catch (Exception e) {
                System.err.println("Error processing resources: " + e.getMessage());
                computationResult.addError("resource_processing", e.getMessage());
            }
            
            return computationResult;
        }, resourceThread);
    }
    
    /**
     * Обработка коллизий (Collision Thread)
     */
    private CompletableFuture<SpellComputationTaskResult> processCollisions(SpellComputationTaskResult computationResult) {
        return CompletableFuture.supplyAsync(() -> {
            if (computationResult.needsCollisionUpdate()) {
                try {
                    // Получаем thread-safe снепшот из результата вычислений
                    CollisionSnapshot snapshot = computationResult.getCollisionSnapshot();
                    if (snapshot != null) {
                        // TODO: Получить реальную SpellShape из результата вычислений
                        // Пока создаем простую форму для тестирования
                        SpellShape tempShape = createTempShape(computationResult);
                        
                        // Выполняем thread-safe детекцию коллизий
                        CollisionDetector.CollisionResult collisionResult = 
                            CollisionDetector.detectCollisions(snapshot, tempShape, 
                                com.example.examplemod.core.spells.forms.PersistenceType.PHYSICAL);
                        
                        // Добавляем результаты коллизий в computation result
                        computationResult.addCollisionData(collisionResult);
                        
                        System.out.println("Processed " + collisionResult.getEntityCount() + 
                                         " collision entities for spell: " + computationResult.getSpellInstanceId());
                    }
                } catch (Exception e) {
                    System.err.println("Error processing collisions: " + e.getMessage());
                    computationResult.addError("collision_processing", e.getMessage());
                }
            }
            return computationResult;
        }, collisionThread);
    }
    
    /**
     * Создать временную форму заклинания для тестирования
     * TODO: Заменить на реальное получение формы из результата вычислений
     */
    private SpellShape createTempShape(SpellComputationTaskResult computationResult) {
        // Создаем простую сферическую форму на основе параметров
        double radius = computationResult.getParameterValue("radius", 2.0);
        // TODO: Implement proper SpellShape creation based on spell form type
        return new com.example.examplemod.core.geometry.shapes.SphereShape(
            new net.minecraft.world.phys.Vec3(0, 0, 0), radius);
    }
    
    /**
     * Агрегация результатов (Aggregation Thread)
     */
    private CompletableFuture<SpellComputationTaskResult> aggregateResults(SpellComputationTaskResult result) {
        return CompletableFuture.supplyAsync(() -> {
            // Финальная агрегация всех результатов
            // Подготовка данных для применения в Main Thread
            System.out.println("Aggregating results for spell: " + result.getSpellInstanceId());
            return result;
        }, aggregationThread);
    }
    
    /**
     * Запланировать применение в Main Thread (НЕ выполняется в Main Thread!)
     */
    private void scheduleMainThreadApplication(SpellComputationTaskResult result, Throwable error) {
        // Используем API для планирования в Main Thread
        String taskName = "SpellCore_ApplySpell_" + result.getSpellInstanceId().toString().substring(0, 8);
        
        // Используем высокий приоритет для применения заклинаний - это критично для геймплея
        MainThreadSynchronizerAPI.executeHighPriority(taskName, () -> applyToMainThread(result, error));
    }
    
    /**
     * Применение к игровому миру - ТОЛЬКО в Main Thread!
     * Вызывается из processMainThreadTasks()
     */
    private void applyToMainThread(SpellComputationTaskResult result, Throwable error) {
        // ВНИМАНИЕ: Этот метод должен вызываться ТОЛЬКО из Main Thread!
        
        if (error != null) {
            System.err.println("Spell computation failed: " + error.getMessage());
            computationFutures.remove(result.getSpellInstanceId());
            return;
        }
        
        if (result.needsMainThreadApplication()) {
            // Здесь БЕЗОПАСНО работать с Minecraft API - мы в Main Thread
            System.out.println("Applying spell to game world: " + result.getSpellInstanceId());
            
            // TODO: Получить реальный Level и применить изменения
            // TODO: Применить form modifications к заклинанию
            // TODO: Обновить визуальные эффекты
            // TODO: Воспроизвести звуки
        }
        
        // Очищаем отслеживание
        computationFutures.remove(result.getSpellInstanceId());
    }
    
    /**
     * Получить статистику Main Thread Synchronizer
     */
    public IMainThreadSynchronizer.SynchronizerStats getMainThreadStats() {
        return MainThreadSynchronizerAPI.getStats();
    }
    
    /**
     * Получить статистику модуля SpellCore
     */
    public IMainThreadSynchronizer.ModuleStats getSpellCoreModuleStats() {
        return moduleHandle.getStats();
    }
    
    // === Lifecycle Management ===
    
    public void shutdown() {
        System.out.println("Shutting down SpellCoreModule...");
        
        // Останавливаем все активные вычисления
        for (CompletableFuture<?> future : computationFutures.values()) {
            future.cancel(true);
        }
        
        // Отменяем регистрацию модуля
        if (moduleHandle != null) {
            moduleHandle.unregister();
        }
        
        // Останавливаем thread pools
        spellComputationPool.shutdown();
        resourceThread.shutdown();
        collisionThread.shutdown();
        aggregationThread.shutdown();
        
        System.out.println("SpellCoreModule shutdown complete");
    }
    
    // === Getters для компонентов ===
    
    public SpellDefinitionRegistry getDefinitionRegistry() { return definitionRegistry; }
    public SpellFactory getSpellFactory() { return spellFactory; }
    public SpellLifecycleManager getLifecycleManager() { return lifecycleManager; }
    public SpellStateCache getStateCache() { return stateCache; }
    
    // === Статистика ===
    
    public int getActiveComputations() { return computationFutures.size(); }
    public int getComputationPoolSize() { return spellComputationPool.getParallelism(); }
    public boolean isShutdown() { return spellComputationPool.isShutdown(); }
}