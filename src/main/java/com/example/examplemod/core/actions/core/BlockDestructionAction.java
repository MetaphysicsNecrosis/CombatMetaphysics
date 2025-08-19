package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.actions.CoreActionExecutor;
import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import com.example.examplemod.CombatMetaphysics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.ArrayList;

/**
 * Core Action: Разрушение блоков
 */
public class BlockDestructionAction extends CoreActionExecutor {
    
    public BlockDestructionAction() {
        super("block_destruction");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        CombatMetaphysics.LOGGER.info("BlockDestructionAction: Starting execution");
        
        Float power = context.getEvent().getFloatParameter("power");
        if (power == null || power <= 0) {
            CombatMetaphysics.LOGGER.error("BlockDestructionAction: Missing or invalid power parameter: {}", power);
            return ExecutionResult.failure("Missing or invalid power parameter");
        }
        
        CombatMetaphysics.LOGGER.info("BlockDestructionAction: Using power {}", power);
        
        // Получаем список блоков для разрушения из предыдущих шагов
        @SuppressWarnings("unchecked")
        List<BlockPos> targetBlocks = context.getPipelineData("scannedBlocks", List.class);
        
        CombatMetaphysics.LOGGER.info("BlockDestructionAction: Found {} target blocks", 
            targetBlocks != null ? targetBlocks.size() : 0);
        
        if (targetBlocks == null || targetBlocks.isEmpty()) {
            CombatMetaphysics.LOGGER.error("BlockDestructionAction: No blocks found for destruction");
            return ExecutionResult.failure("No blocks found for destruction");
        }
        
        Level world = context.getWorld();
        List<BlockPos> destroyedBlocks = new ArrayList<>();
        List<BlockPos> protectedBlocks = new ArrayList<>();
        
        for (BlockPos pos : targetBlocks) {
            BlockState blockState = world.getBlockState(pos);
            CombatMetaphysics.LOGGER.info("BlockDestructionAction: Processing block at {} - {}", 
                pos, blockState.getBlock().getName().getString());
            
            // Проверяем защищенность блока
            if (isBlockProtected(blockState, power, context)) {
                CombatMetaphysics.LOGGER.info("BlockDestructionAction: Block at {} is protected", pos);
                protectedBlocks.add(pos);
                continue;
            }
            
            // Разрушаем блок
            CombatMetaphysics.LOGGER.info("BlockDestructionAction: Attempting to destroy block at {}", pos);
            if (world.destroyBlock(pos, true)) {
                CombatMetaphysics.LOGGER.info("BlockDestructionAction: Successfully destroyed block at {}", pos);
                destroyedBlocks.add(pos);
            } else {
                CombatMetaphysics.LOGGER.warn("BlockDestructionAction: Failed to destroy block at {}", pos);
            }
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