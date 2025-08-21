package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.actions.CoreActionExecutor;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.util.BlockChangeBuffer;
import com.example.examplemod.util.WorldEditStyleBlockBuffer;
import com.example.examplemod.util.CombatSideEffects;
import com.example.examplemod.util.TickBasedBlockProcessor;
import com.example.examplemod.util.BlockProtectionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Core Action: Разрушение блоков
 */
public class BlockDestructionAction extends CoreActionExecutor {
    
    public BlockDestructionAction() {
        super("block_destruction");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        CombatMetaphysics.LOGGER.info("BlockDestructionAction: Starting WorldEdit-style mass destruction");
        
        Float power = context.getEvent().getFloatParameter("power");
        if (power == null || power <= 0) {
            CombatMetaphysics.LOGGER.error("BlockDestructionAction: Missing or invalid power parameter: {}", power);
            return ExecutionResult.failure("Missing or invalid power parameter");
        }
        
        // Получаем список блоков для разрушения из предыдущих шагов
        @SuppressWarnings("unchecked")
        List<BlockPos> targetBlocks = context.getPipelineData("scannedBlocks", List.class);
        
        if (targetBlocks == null || targetBlocks.isEmpty()) {
            CombatMetaphysics.LOGGER.error("BlockDestructionAction: No blocks found for destruction");
            return ExecutionResult.failure("No blocks found for destruction");
        }
        
        CombatMetaphysics.LOGGER.info("BlockDestructionAction: Processing {} blocks with WorldEdit-style architecture", targetBlocks.size());
        
        Level world = context.getWorld();
        
        // ОПРЕДЕЛЯЕМ СТРАТЕГИЮ в зависимости от количества блоков
        if (targetBlocks.size() >= 1000) {
            // НОВАЯ СТРАТЕГИЯ: Tick-based processing для очень больших операций
            return executeTickBasedDestruction(targetBlocks, power, context, world);
        } else if (targetBlocks.size() >= 50) {
            // WorldEdit-style для средних операций (50-999 блоков)  
            return executeMassDestruction(targetBlocks, power, context, world);
        } else {
            // Обычное разрушение для небольшого количества (<50 блоков)
            return executeNormalDestruction(targetBlocks, power, context, world);
        }
    }
    
    /**
     * НОВЕЙШИЙ МЕТОД: Tick-based processing для очень больших операций (1000+ блоков)
     * Использует GPT рекомендации: Queue + ServerTickEvent + BATCH_SIZE за тик
     */
    private ExecutionResult executeTickBasedDestruction(List<BlockPos> targetBlocks, Float power, 
                                                      ActionContext context, Level world) {
        CombatMetaphysics.LOGGER.info("Using tick-based destruction for {} blocks (large operation)", targetBlocks.size());
        
        // Фильтруем защищённые блоки
        List<BlockPos> protectedBlocks = new ArrayList<>();
        List<BlockPos> destroyableBlocks = new ArrayList<>();
        
        for (BlockPos pos : targetBlocks) {
            BlockState currentState = world.getBlockState(pos);
            
            if (isBlockProtected(currentState, power, context)) {
                protectedBlocks.add(pos);
            } else if (!currentState.isAir()) {
                destroyableBlocks.add(pos);
            }
        }
        
        CombatMetaphysics.LOGGER.info("Tick-based queue: {} destroyable blocks, {} protected", 
            destroyableBlocks.size(), protectedBlocks.size());
        
        // Получаем эпицентр заклинания для кастомных дропов
        BlockPos spellCenter = context.getEvent().getBlockPosParameter("center");
        if (spellCenter == null) {
            // Fallback: берём позицию игрока или первый блок
            spellCenter = destroyableBlocks.isEmpty() ? BlockPos.ZERO : destroyableBlocks.get(0);
        }
        
        // Запускаем tick-based обработку с кастомными дропами
        TickBasedBlockProcessor processor = TickBasedBlockProcessor.getInstance();
        processor.queueMassDestructionWithSpellCenter(world, destroyableBlocks, true, false, spellCenter);
        
        // Сохраняем результаты (приблизительные, так как обработка асинхронная)
        context.setPipelineData("destroyedBlocks", destroyableBlocks);
        context.setPipelineData("protectedBlocks", protectedBlocks);
        context.setPipelineData("destructionPower", power);
        
        BlockDestructionResult result = new BlockDestructionResult(
            destroyableBlocks.size(), // Приблизительное количество (будет обработано асинхронно)
            protectedBlocks.size(),
            destroyableBlocks,
            protectedBlocks,
            power
        );
        
        CombatMetaphysics.LOGGER.info("Tick-based destruction queued: {} blocks will be processed over time", 
            destroyableBlocks.size());
        
        return ExecutionResult.success(result);
    }
    
    /**
     * WorldEdit-style массовое разрушение для средних операций (50-999 блоков)
     */
    private ExecutionResult executeMassDestruction(List<BlockPos> targetBlocks, Float power, 
                                                 ActionContext context, Level world) {
        CombatMetaphysics.LOGGER.info("Using WorldEdit-style mass destruction for {} blocks", targetBlocks.size());
        
        // Конфигурируем side effects для максимальной производительности
        CombatSideEffects sideEffects = CombatSideEffects.Presets.VISUAL_ONLY
            .with(CombatSideEffects.SPAWN_DROPS); // Дропы нужны для gameplay
        
        // Создаём WorldEdit-style buffer
        WorldEditStyleBlockBuffer buffer = new WorldEditStyleBlockBuffer(world, sideEffects);
        
        // Предварительная загрузка чанков и проверка защиты
        Map<ChunkPos, List<BlockPos>> chunkGroups = targetBlocks.stream()
            .collect(Collectors.groupingBy(pos -> new ChunkPos(pos)));
        
        Map<ChunkPos, LevelChunk> loadedChunks = chunkGroups.keySet().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                pos -> world.getChunk(pos.x, pos.z)
            ));
        
        List<BlockPos> protectedBlocks = new ArrayList<>();
        int queuedForDestruction = 0;
        
        for (BlockPos pos : targetBlocks) {
            ChunkPos chunkPos = new ChunkPos(pos);
            LevelChunk chunk = loadedChunks.get(chunkPos);
            BlockState currentState = chunk.getBlockState(pos);
            
            if (isBlockProtected(currentState, power, context)) {
                protectedBlocks.add(pos);
                continue;
            }
            
            if (!currentState.isAir()) {
                // WorldEdit-style queueing - НЕ применяем сразу
                buffer.queueBlockDestruction(pos, currentState);
                queuedForDestruction++;
            }
        }
        
        CombatMetaphysics.LOGGER.info("WorldEdit buffer: queued {} blocks, {} protected", 
            queuedForDestruction, protectedBlocks.size());
        
        // FLUSH - WorldEdit-style массовое применение
        WorldEditStyleBlockBuffer.FlushResult flushResult = buffer.flush();
        
        // Сохраняем результаты
        List<BlockPos> destroyedBlocks = targetBlocks.stream()
            .filter(pos -> !protectedBlocks.contains(pos))
            .toList();
            
        context.setPipelineData("destroyedBlocks", destroyedBlocks);
        context.setPipelineData("protectedBlocks", protectedBlocks);
        context.setPipelineData("destructionPower", power);
        
        BlockDestructionResult result = new BlockDestructionResult(
            flushResult.blocksDestroyed(),
            protectedBlocks.size(),
            destroyedBlocks,
            protectedBlocks,
            power
        );
        
        CombatMetaphysics.LOGGER.info("WorldEdit-style destruction complete: {} destroyed, {} protected, {} chunks", 
            flushResult.blocksDestroyed(), protectedBlocks.size(), flushResult.chunksModified());
        
        return ExecutionResult.success(result);
    }
    
    /**
     * FALLBACK: Обычное разрушение для небольшого количества блоков (<10)
     */
    private ExecutionResult executeNormalDestruction(List<BlockPos> targetBlocks, Float power, 
                                                   ActionContext context, Level world) {
        CombatMetaphysics.LOGGER.info("Using normal destruction for {} blocks", targetBlocks.size());
        
        // Для небольшого количества блоков используем старый buffer (он работает нормально)
        BlockChangeBuffer buffer = new BlockChangeBuffer(world);
        
        List<BlockPos> protectedBlocks = new ArrayList<>();
        int queuedForDestruction = 0;
        
        for (BlockPos pos : targetBlocks) {
            BlockState currentState = world.getBlockState(pos);
            
            if (isBlockProtected(currentState, power, context)) {
                protectedBlocks.add(pos);
                continue;
            }
            
            if (!currentState.isAir()) {
                buffer.queueBlockDestruction(pos, currentState);
                queuedForDestruction++;
            }
        }
        
        BlockChangeBuffer.FlushResult flushResult = buffer.flush();
        
        List<BlockPos> destroyedBlocks = targetBlocks.stream()
            .filter(pos -> !protectedBlocks.contains(pos))
            .toList();
            
        context.setPipelineData("destroyedBlocks", destroyedBlocks);
        context.setPipelineData("protectedBlocks", protectedBlocks);
        context.setPipelineData("destructionPower", power);
        
        BlockDestructionResult result = new BlockDestructionResult(
            flushResult.blocksDestroyed(),
            protectedBlocks.size(),
            destroyedBlocks,
            protectedBlocks,
            power
        );
        
        return ExecutionResult.success(result);
    }
    
    /**
     * Проверяет, защищен ли блок от разрушения
     * ОБНОВЛЕНО: Использует новую систему BlockProtectionRegistry
     */
    private boolean isBlockProtected(BlockState blockState, float power, ActionContext context) {
        boolean isProtected = BlockProtectionRegistry.isBlockProtected(blockState, power);
        
        // Логируем защищённые блоки для отладки
        if (isProtected) {
            String reason = BlockProtectionRegistry.getProtectionReason(blockState, power);
            CombatMetaphysics.LOGGER.debug("Block protected: {} - Reason: {}", 
                blockState.getBlock().getDescriptionId(), reason);
        }
        
        return isProtected;
    }
    
    /**
     * Результат разрушения блоков
     */
    public record BlockDestructionResult(int destroyedCount,
                                       int protectedCount, 
                                       List<BlockPos> destroyedBlocks,
                                       List<BlockPos> protectedBlocks,
                                       float usedPower) {}
}