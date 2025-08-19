package com.example.examplemod.core.actions.core;

import com.example.examplemod.core.pipeline.ActionContext;
import com.example.examplemod.core.pipeline.ExecutionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.ArrayList;

/**
 * Core Action: Отталкивание сущностей
 */
public class KnockbackAction extends CoreActionExecutor {
    
    public KnockbackAction() {
        super("knockback");
    }
    
    @Override
    protected ExecutionResult executeCore(ActionContext context) {
        Float strength = context.getEvent().getFloatParameter("strength");
        if (strength == null || strength <= 0) {
            return ExecutionResult.failure("Missing or invalid strength parameter");
        }
        
        // Получаем список сущностей из предыдущих шагов
        @SuppressWarnings("unchecked")
        List<Entity> targetEntities = context.getPipelineData("scannedEntities", List.class);
        
        if (targetEntities == null || targetEntities.isEmpty()) {
            return ExecutionResult.failure("No entities found for knockback");
        }
        
        Vec3 epicenter = getKnockbackEpicenter(context);
        List<Entity> knockedEntities = new ArrayList<>();
        List<Entity> immuneEntities = new ArrayList<>();
        
        for (Entity entity : targetEntities) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue; // Отталкиваем только живых существ
            }
            
            // Проверяем иммунитет к отталкиванию
            if (isImmuneToKnockback(livingEntity)) {
                immuneEntities.add(entity);
                continue;
            }
            
            // Вычисляем направление и силу отталкивания
            Vec3 entityPos = entity.position();
            Vec3 direction = entityPos.subtract(epicenter).normalize();
            
            // Если сущность точно в центре - отталкиваем вверх
            if (direction.lengthSqr() < 0.01) {
                direction = new Vec3(0, 1, 0);
            }
            
            // Применяем отталкивание
            Vec3 knockbackVelocity = direction.scale(strength);
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockbackVelocity));
            entity.hurtMarked = true; // Принудительное обновление движения
            
            knockedEntities.add(entity);
        }
        
        // Сохраняем результаты
        context.setPipelineData("knockedEntities", knockedEntities);
        context.setPipelineData("immuneEntities", immuneEntities);
        context.setPipelineData("knockbackStrength", strength);
        context.setPipelineData("knockbackEpicenter", epicenter);
        
        KnockbackResult result = new KnockbackResult(
                knockedEntities.size(),
                immuneEntities.size(),
                knockedEntities,
                immuneEntities,
                strength,
                epicenter
        );
        
        return ExecutionResult.success(result);
    }
    
    /**
     * Получает эпицентр отталкивания
     */
    private Vec3 getKnockbackEpicenter(ActionContext context) {
        // Проверяем сохраненную позицию сканирования
        Vec3 scanCenter = context.getPipelineData("scanCenter", Vec3.class);
        if (scanCenter != null) {
            return scanCenter;
        }
        
        // Параметры события
        if (context.getEvent().getBlockPosParameter("position") != null) {
            return Vec3.atCenterOf(context.getEvent().getBlockPosParameter("position"));
        }
        
        // По умолчанию - позиция игрока
        return context.getPlayer().position();
    }
    
    /**
     * Проверяет, имеет ли сущность иммунитет к отталкиванию
     */
    private boolean isImmuneToKnockback(LivingEntity entity) {
        // Некоторые сущности могут быть невосприимчивы к отталкиванию
        // TODO: Добавить проверку особых способностей
        return false;
    }
    
    /**
     * Результат отталкивания
     */
    public record KnockbackResult(int knockedCount,
                                int immuneCount,
                                List<Entity> knockedEntities,
                                List<Entity> immuneEntities,
                                float strength,
                                Vec3 epicenter) {}
}