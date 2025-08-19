package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core Action: Сканирование области для поиска целей
 * Базовый примитив для всех area-effect действий
 */
public class AreaScanAction extends CoreActionExecutor {
    
    public AreaScanAction() {
        super("area_scan");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        Vec3 center = getCenterPosition(context);
        float range = context.getModifiedRange();
        String scanType = context.getEvent().getStringParameter("scanType");
        
        if (range <= 0) {
            return ExecutionResult.failure("Invalid scan range: " + range);
        }
        
        ScanResult scanResult = switch (scanType != null ? scanType : "entities") {
            case "entities" -> scanEntities(context, center, range);
            case "blocks" -> scanBlocks(context, center, range);
            case "both" -> scanBoth(context, center, range);
            default -> new ScanResult(List.of(), List.of(), "Unknown scan type: " + scanType);
        };
        
        if (scanResult.error() != null) {
            return ExecutionResult.failure(scanResult.error());
        }
        
        // Сохраняем результаты для других действий в pipeline
        context.setPipelineData("scannedEntities", scanResult.entities());
        context.setPipelineData("scannedBlocks", scanResult.blocks());
        context.setPipelineData("scanCenter", center);
        context.setPipelineData("scanRange", range);
        
        return ExecutionResult.success(scanResult);
    }
    
    /**
     * Получение центральной позиции для сканирования
     */
    private Vec3 getCenterPosition(ActionContext context) {
        BlockPos blockPos = context.getEvent().getBlockPosParameter("position");
        if (blockPos != null) {
            return Vec3.atCenterOf(blockPos);
        }
        
        Entity targetEntity = context.getEvent().getEntityParameter("target");
        if (targetEntity != null) {
            return targetEntity.position();
        }
        
        // По умолчанию - позиция игрока
        return context.getPlayer().position();
    }
    
    /**
     * Сканирование сущностей в области
     */
    private ScanResult scanEntities(ActionContext context, Vec3 center, float range) {
        Level world = context.getWorld();
        AABB searchBox = new AABB(center.subtract(range, range, range), 
                                  center.add(range, range, range));
        
        List<Entity> entities = world.getEntities(null, searchBox, entity -> {
            // Фильтры
            if (entity.equals(context.getPlayer())) {
                return false; // Исключаем самого игрока
            }
            
            double distance = entity.position().distanceTo(center);
            return distance <= range;
        });
        
        return new ScanResult(entities, List.of(), null);
    }
    
    /**
     * Сканирование блоков в области
     */
    private ScanResult scanBlocks(ActionContext context, Vec3 center, float range) {
        Level world = context.getWorld();
        List<BlockPos> blocks = BlockPos.betweenClosedStream(
                BlockPos.containing(center.subtract(range, range, range)),
                BlockPos.containing(center.add(range, range, range))
        )
        .filter(pos -> {
            Vec3 blockCenter = Vec3.atCenterOf(pos);
            return blockCenter.distanceTo(center) <= range && 
                   !world.getBlockState(pos).isAir();
        })
        .map(BlockPos::immutable)
        .collect(Collectors.toList());
        
        return new ScanResult(List.of(), blocks, null);
    }
    
    /**
     * Сканирование и сущностей, и блоков
     */
    private ScanResult scanBoth(ActionContext context, Vec3 center, float range) {
        ScanResult entityScan = scanEntities(context, center, range);
        ScanResult blockScan = scanBlocks(context, center, range);
        
        return new ScanResult(entityScan.entities(), blockScan.blocks(), null);
    }
    
    /**
     * Результат сканирования области
     */
    public record ScanResult(List<Entity> entities, List<BlockPos> blocks, String error) {}
}