package com.example.examplemod.util;

import com.example.examplemod.CombatMetaphysics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tick-based Block Processor по рекомендациям GPT
 * 
 * ПРИНЦИПЫ:
 * 1. Никогда не меняй все блоки за один тик
 * 2. Queue<BlockChange> + ServerTickEvent
 * 3. Фиксированное количество блоков за тик (BATCH_SIZE = 1000)
 * 4. Автоматическая подписка/отписка от событий
 * 5. Проверка chunk loading
 * 6. Прогресс для больших операций
 */
public class TickBasedBlockProcessor {
    
    // Параметры производительности (рекомендации GPT)
    private static final int BATCH_SIZE = 1000; // блоков за тик
    private static final int MAX_OPERATIONS_PER_SECOND = 50000; // лимит для предотвращения лагов
    
    // Singleton instance
    private static TickBasedBlockProcessor instance;
    
    // Очередь изменений (thread-safe для async access)
    private final Queue<BlockChange> blockQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ItemEntity> dropQueue = new ConcurrentLinkedQueue<>();
    private final Set<ChunkPos> lightingUpdateQueue = new HashSet<>();
    
    // Статистика операции
    private long totalBlocks = 0;
    private long processedBlocks = 0;
    private long startTime = 0;
    private boolean isRegistered = false;
    private Level currentWorld = null; // Сохраняем мир для правильного обращения
    
    // Undo support (опциональная функция)
    private final Map<BlockPos, BlockState> undoMap = new HashMap<>();
    private boolean enableUndo = false;
    
    private TickBasedBlockProcessor() {}
    
    public static TickBasedBlockProcessor getInstance() {
        if (instance == null) {
            instance = new TickBasedBlockProcessor();
        }
        return instance;
    }
    
    /**
     * Добавляет блоки в очередь для обработки с поддержкой пользовательских дропов
     * Автоматически регистрирует tick handler
     */
    public void queueMassDestruction(Level world, List<BlockPos> positions, boolean spawnDrops, boolean enableUndo) {
        queueMassDestructionWithSpellCenter(world, positions, spawnDrops, enableUndo, null);
    }
    
    /**
     * Добавляет блоки в очередь для обработки с кастомными дропами
     * @param spellCenter эпицентр заклинания для расчёта дропов (null = стандартные дропы)
     */
    public void queueMassDestructionWithSpellCenter(Level world, List<BlockPos> positions, boolean spawnDrops, boolean enableUndo, BlockPos spellCenter) {
        if (positions.isEmpty()) {
            return;
        }
        
        CombatMetaphysics.LOGGER.info("TickBasedProcessor: Queuing {} blocks for destruction", positions.size());
        
        this.enableUndo = enableUndo;
        this.totalBlocks = positions.size();
        this.processedBlocks = 0;
        this.startTime = System.currentTimeMillis();
        this.currentWorld = world; // Сохраняем ссылку на мир
        
        // Проверяем лимит операций в секунду
        if (totalBlocks > MAX_OPERATIONS_PER_SECOND) {
            CombatMetaphysics.LOGGER.warn("Large operation detected: {} blocks. This may take {} seconds", 
                totalBlocks, totalBlocks / MAX_OPERATIONS_PER_SECOND);
        }
        
        // Добавляем блоки в очередь
        for (BlockPos pos : positions) {
            BlockState currentState = world.getBlockState(pos);
            
            if (!currentState.isAir()) {
                // Сохраняем для undo если нужно
                if (enableUndo) {
                    undoMap.put(pos.immutable(), currentState);
                }
                
                // Добавляем в очередь
                blockQueue.offer(new BlockChange(pos.immutable(), currentState, Blocks.AIR.defaultBlockState()));
                
                // Планируем дропы (кастомные или стандартные)
                if (spawnDrops && world instanceof ServerLevel serverLevel) {
                    if (spellCenter != null) {
                        // КАСТОМНЫЕ ДРОПЫ: сундуки + автоплавка руд с модификатором расстояния
                        List<ItemEntity> customDrops = DistanceBasedDropManager.generateCustomDrops(world, pos, currentState, spellCenter);
                        dropQueue.addAll(customDrops);
                    } else {
                        // СТАНДАРТНЫЕ ДРОПЫ: используем vanilla механизм
                        List<ItemStack> drops = Block.getDrops(currentState, serverLevel, pos, null);
                        drops.forEach(stack -> {
                            ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                            dropQueue.offer(entity);
                        });
                    }
                }
                
                // Планируем lighting update для чанка
                lightingUpdateQueue.add(new ChunkPos(pos));
            }
        }
        
        // Регистрируем tick handler если ещё не зарегистрирован
        if (!isRegistered && !blockQueue.isEmpty()) {
            NeoForge.EVENT_BUS.register(this);
            isRegistered = true;
            CombatMetaphysics.LOGGER.info("TickBasedProcessor: Registered for ServerTickEvent");
        }
    }
    
    /**
     * Обработчик тиков сервера
     * Обрабатывает BATCH_SIZE блоков за тик
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (blockQueue.isEmpty() && dropQueue.isEmpty()) {
            // Завершаем обработку
            finishOperation();
            return;
        }
        
        long tickStart = System.currentTimeMillis();
        int blocksThisTick = 0;
        
        // ОБРАБОТКА БЛОКОВ (BATCH_SIZE за тик)
        while (!blockQueue.isEmpty() && blocksThisTick < BATCH_SIZE) {
            BlockChange change = blockQueue.poll();
            if (change == null) break;
            
            // Проверяем что чанк загружен (критично!)
            if (!isChunkLoaded(change.pos)) {
                // Пропускаем блок если чанк не загружен
                CombatMetaphysics.LOGGER.debug("Skipping block at {} - chunk not loaded", change.pos);
                continue;
            }
            
            try {
                // Используем сохранённый мир вместо предположения о dimension
                if (currentWorld instanceof ServerLevel serverLevel) {
                    // БЕЗОПАСНОЕ изменение блока через стандартный API (исправлено!)
                    serverLevel.setBlock(change.pos, change.newState, 
                        Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS); // Правильные флаги
                    
                    processedBlocks++;
                    blocksThisTick++;
                }
                
            } catch (Exception e) {
                CombatMetaphysics.LOGGER.warn("Failed to process block at {}: {}", change.pos, e.getMessage());
            }
        }
        
        // ОБРАБОТКА ДРОПОВ (меньшими батчами)
        int dropsThisTick = 0;
        final int DROP_BATCH_SIZE = 50;
        
        while (!dropQueue.isEmpty() && dropsThisTick < DROP_BATCH_SIZE) {
            ItemEntity drop = dropQueue.poll();
            if (drop == null) break;
            
            try {
                // Используем сохранённый мир для дропов тоже
                if (currentWorld instanceof ServerLevel serverLevel) {
                    serverLevel.addFreshEntity(drop);
                    dropsThisTick++;
                }
            } catch (Exception e) {
                CombatMetaphysics.LOGGER.warn("Failed to spawn drop: {}", e.getMessage());
            }
        }
        
        // Логируем прогресс для больших операций
        if (totalBlocks > 5000 && processedBlocks % 5000 == 0) {
            double progress = (double) processedBlocks / totalBlocks * 100;
            long elapsed = System.currentTimeMillis() - startTime;
            CombatMetaphysics.LOGGER.info("Block processing progress: {:.1f}% ({}/{}) - {}ms elapsed", 
                progress, processedBlocks, totalBlocks, elapsed);
        }
        
        long tickDuration = System.currentTimeMillis() - tickStart;
        if (tickDuration > 50) { // Warn если тик занял больше 50ms
            CombatMetaphysics.LOGGER.warn("Slow tick detected: {}ms for {} blocks", tickDuration, blocksThisTick);
        }
    }
    
    /**
     * Проверяет загружен ли чанк (критично для предотвращения крашей)
     */
    private boolean isChunkLoaded(BlockPos pos) {
        if (currentWorld == null) {
            return false;
        }
        
        try {
            // Проверяем загружен ли чанк
            ChunkPos chunkPos = new ChunkPos(pos);
            return currentWorld.isLoaded(pos) && currentWorld.getChunkSource().hasChunk(chunkPos.x, chunkPos.z);
        } catch (Exception e) {
            CombatMetaphysics.LOGGER.warn("Failed to check chunk loading for {}: {}", pos, e.getMessage());
            return false;
        }
    }
    
    /**
     * Завершает операцию и отписывается от событий
     */
    private void finishOperation() {
        if (isRegistered) {
            NeoForge.EVENT_BUS.unregister(this);
            isRegistered = false;
            
            long totalTime = System.currentTimeMillis() - startTime;
            double blocksPerSecond = processedBlocks * 1000.0 / Math.max(totalTime, 1);
            
            CombatMetaphysics.LOGGER.info("TickBasedProcessor: Operation completed - {} blocks in {}ms ({:.1f} blocks/sec)", 
                processedBlocks, totalTime, blocksPerSecond);
            
            // Планируем deferred lighting updates
            if (!lightingUpdateQueue.isEmpty()) {
                scheduleLightingUpdates();
            }
            
            // Сбрасываем статистику
            totalBlocks = 0;
            processedBlocks = 0;
            lightingUpdateQueue.clear();
        }
    }
    
    /**
     * Выполняет deferred lighting updates для затронутых чанков
     */
    private void scheduleLightingUpdates() {
        // Выполняем в отдельном потоке через небольшую задержку
        new Thread(() -> {
            try {
                Thread.sleep(100); // Даём время блокам обновиться
                
                // TODO: Реализовать правильные lighting updates
                CombatMetaphysics.LOGGER.debug("Would perform lighting updates for {} chunks", 
                    lightingUpdateQueue.size());
                    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Undo последней операции (если включено)
     */
    public void undo() {
        if (!enableUndo || undoMap.isEmpty()) {
            CombatMetaphysics.LOGGER.warn("Undo not available - either disabled or no operations to undo");
            return;
        }
        
        CombatMetaphysics.LOGGER.info("Starting undo operation for {} blocks", undoMap.size());
        
        List<BlockPos> undoPositions = new ArrayList<>(undoMap.keySet());
        
        // Создаём обратные изменения
        for (Map.Entry<BlockPos, BlockState> entry : undoMap.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState originalState = entry.getValue();
            
            blockQueue.offer(new BlockChange(pos, Blocks.AIR.defaultBlockState(), originalState));
        }
        
        // Регистрируем для обработки
        if (!isRegistered) {
            NeoForge.EVENT_BUS.register(this);
            isRegistered = true;
        }
        
        undoMap.clear();
    }
    
    /**
     * Получить текущий прогресс операции
     */
    public ProgressInfo getProgress() {
        if (totalBlocks == 0) {
            return new ProgressInfo(0, 0, 0, 0);
        }
        
        double percentage = (double) processedBlocks / totalBlocks * 100;
        long elapsed = System.currentTimeMillis() - startTime;
        long remaining = processedBlocks > 0 ? (elapsed * (totalBlocks - processedBlocks)) / processedBlocks : 0;
        
        return new ProgressInfo(processedBlocks, totalBlocks, percentage, remaining);
    }
    
    /**
     * Представляет одно изменение блока
     */
    private record BlockChange(BlockPos pos, BlockState oldState, BlockState newState) {}
    
    /**
     * Информация о прогрессе операции
     */
    public record ProgressInfo(long processed, long total, double percentage, long estimatedRemainingMs) {}
}