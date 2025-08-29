package com.example.examplemod.core.spells;

import com.example.examplemod.core.spells.computation.SpellComputationContext;
import com.example.examplemod.core.spells.computation.SpellComputationTask;
import com.example.examplemod.core.spells.computation.SpellComputationTaskResult;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.instances.SpellInstance;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
        
        // Инициализация компонентов
        this.definitionRegistry = new SpellDefinitionRegistry();
        this.spellFactory = new SpellFactory(definitionRegistry);
        this.lifecycleManager = new SpellLifecycleManager();
        this.stateCache = new SpellStateCache();
        
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
        
        // Настраиваем pipeline: Computation -> Collision -> Aggregation -> Main Thread
        return computationFuture
            .thenComposeAsync(this::processCollisions, collisionThread)      // Collision Thread
            .thenComposeAsync(this::aggregateResults, aggregationThread)     // Aggregation Thread
            .whenCompleteAsync(this::applyToMainThread);                     // Back to Main Thread
    }
    
    /**
     * Создать thread-safe контекст вычисления (Main Thread)
     */
    private SpellComputationContext createSafeContext(SpellDefinition definition, Level level, Player caster) {
        UUID spellId = UUID.randomUUID();
        
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
            level.dimension().hashCode()
        );
    }
    
    /**
     * Обработка коллизий (Collision Thread)
     */
    private CompletableFuture<SpellComputationTaskResult> processCollisions(SpellComputationTaskResult computationResult) {
        return CompletableFuture.supplyAsync(() -> {
            if (computationResult.needsCollisionUpdate()) {
                // TODO: Интеграция с GeometryCore Module
                // Здесь будет обработка коллизий в выделенном потоке
                System.out.println("Processing collisions for spell: " + computationResult.getSpellInstanceId());
            }
            return computationResult;
        }, collisionThread);
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
     * Применение к игровому миру (Main Thread)
     */
    private void applyToMainThread(SpellComputationTaskResult result, Throwable error) {
        if (error != null) {
            System.err.println("Spell computation failed: " + error.getMessage());
            return;
        }
        
        if (result.needsMainThreadApplication()) {
            // Здесь безопасно работать с Minecraft API
            // Применяем изменения к игровому миру
            System.out.println("Applying spell to game world: " + result.getSpellInstanceId());
            
            // TODO: Применить form modifications к заклинанию
            // TODO: Обновить визуальные эффекты
            // TODO: Воспроизвести звуки
        }
        
        // Очищаем отслеживание
        computationFutures.remove(result.getSpellInstanceId());
    }
    
    // === Lifecycle Management ===
    
    public void shutdown() {
        System.out.println("Shutting down SpellCoreModule...");
        
        // Останавливаем все активные вычисления
        for (CompletableFuture<?> future : computationFutures.values()) {
            future.cancel(true);
        }
        
        // Останавливаем thread pools
        spellComputationPool.shutdown();
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