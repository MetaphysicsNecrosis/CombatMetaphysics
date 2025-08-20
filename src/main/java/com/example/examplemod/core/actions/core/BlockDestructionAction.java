package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.actions.CoreActionExecutor;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import com.example.examplemod.CombatMetaphysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
        CombatMetaphysics.LOGGER.info("BlockDestructionAction: Starting optimized execution");
        
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
        
        CombatMetaphysics.LOGGER.info("BlockDestructionAction: Processing {} blocks with FAWE-style optimization", targetBlocks.size());
        
        Level world = context.getWorld();
        
        // 1. Группировка по чанкам (FAWE pattern)
        Map<ChunkPos, List<BlockPos>> chunkGroups = targetBlocks.stream()
            .collect(Collectors.groupingBy(pos -> new ChunkPos(pos)));
        
        // 2. Предварительная загрузка всех чанков
        Map<ChunkPos, LevelChunk> loadedChunks = chunkGroups.keySet().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                pos -> world.getChunk(pos.x, pos.z)
            ));
        
        // 3. Batch проверка защиты (один проход)
        BitSet protectedMask = new BitSet(targetBlocks.size());
        BlockState[] states = new BlockState[targetBlocks.size()];
        
        for (int i = 0; i < targetBlocks.size(); i++) {
            BlockPos pos = targetBlocks.get(i);
            ChunkPos chunkPos = new ChunkPos(pos);
            LevelChunk chunk = loadedChunks.get(chunkPos);
            
            states[i] = chunk.getBlockState(pos);
            if (isBlockProtected(states[i], power, context)) {
                protectedMask.set(i);
            }
        }
        
        // 4. Массовое удаление через direct chunk access
        List<ItemEntity> allDrops = new ArrayList<>();
        List<BlockPos> destroyedBlocks = new ArrayList<>();
        List<BlockPos> protectedBlocks = new ArrayList<>();
        
        chunkGroups.forEach((chunkPos, positions) -> {
            LevelChunk chunk = loadedChunks.get(chunkPos);
            
            for (BlockPos pos : positions) {
                int idx = targetBlocks.indexOf(pos);
                if (protectedMask.get(idx)) {
                    protectedBlocks.add(pos);
                    continue;
                }
                
                BlockState state = states[idx];
                
                // Собираем дропы но НЕ спавним еще
                if (!state.isAir()) {
                    List<ItemStack> drops = Block.getDrops(state, world, pos, null);
                    drops.forEach(stack -> {
                        ItemEntity entity = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                        allDrops.add(entity);
                    });
                }
                
                // Прямая установка AIR через destroyBlock (сохраняет корректность)
                if (world.destroyBlock(pos, false)) { // false = не дропать, мы сами соберем
                    destroyedBlocks.add(pos);
                }
            }
            
            // Помечаем чанк как измененный
            chunk.setUnsaved(true);
        });
        
        // 5. Batch spawn дропов
        if (!allDrops.isEmpty()) {
            allDrops.forEach(world::addFreshEntity);
        }
        
        // Сохраняем результаты
        context.setPipelineData("destroyedBlocks", destroyedBlocks);
        context.setPipelineData("protectedBlocks", protectedBlocks);
        context.setPipelineData("destructionPower", power);
        
        BlockDestructionResult result = new BlockDestructionResult(
                destroyedBlocks.size(),
                protectedBlocks.size(),
                destroyedBlocks,
                protectedBlocks,
                power
        );
        
        CombatMetaphysics.LOGGER.info("BlockDestructionAction: Optimized execution complete - {} destroyed, {} protected", 
            destroyedBlocks.size(), protectedBlocks.size());
        
        return ExecutionResult.success(result);
    }
    
    /**
     * Проверяет, защищен ли блок от разрушения
     */
    private boolean isBlockProtected(BlockState blockState, float power, ActionContext context) {
        // Неразрушимые блоки
        if (blockState.is(Blocks.BEDROCK) || blockState.is(Blocks.BARRIER)) {
            return true;
        }
        
        // Проверяем прочность блока
        float hardness = blockState.getDestroySpeed(context.getWorld(), BlockPos.ZERO);
        
        // Нерушимые блоки (hardness < 0)
        if (hardness < 0) {
            return true;
        }
        
        // Проверяем, хватает ли силы для разрушения
        return power < hardness;
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