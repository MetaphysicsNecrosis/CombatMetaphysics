package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Core Action: Телепортация сущности
 */
public class TeleportAction extends CoreActionExecutor {
    
    public TeleportAction() {
        super("teleport");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        Entity target = context.getEvent().getEntityParameter("target");
        if (target == null) {
            target = context.getPlayer(); // По умолчанию телепортируем игрока
        }
        
        Vec3 destination = getDestination(context);
        if (destination == null) {
            return ExecutionResult.failure("No valid destination found");
        }
        
        Vec3 oldPosition = target.position();
        target.teleportTo(destination.x, destination.y, destination.z);
        
        context.setPipelineData("teleportFrom", oldPosition);
        context.setPipelineData("teleportTo", destination);
        context.setPipelineData("teleportDistance", oldPosition.distanceTo(destination));
        
        return ExecutionResult.success(new TeleportResult(oldPosition, destination, target));
    }
    
    private Vec3 getDestination(ActionContext context) {
        // Проверяем прямо указанную позицию
        BlockPos blockPos = context.getEvent().getBlockPosParameter("destination");
        if (blockPos != null) {
            return Vec3.atCenterOf(blockPos);
        }
        
        // Проверяем относительное смещение
        Vec3 offset = context.getEvent().getParameter("offset", Vec3.class);
        if (offset != null) {
            return context.getPlayer().position().add(offset);
        }
        
        return null;
    }
    
    public record TeleportResult(Vec3 from, Vec3 to, Entity teleportedEntity) {}
}