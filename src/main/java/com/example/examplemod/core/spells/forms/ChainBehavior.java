package com.example.examplemod.core.spells.forms;

import com.example.examplemod.core.spells.geometry.ChainGeometry;
import com.example.examplemod.core.spells.geometry.SpellGeometry;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Поведение цепи (CHAIN) - цепная реакция между целями
 * Прыгает от цели к цели в пределах дальности
 * Урон снижается с каждым прыжком
 */
public class ChainBehavior implements SpellFormBehavior {
    
    private SpellFormContext context;
    private ChainGeometry geometry;
    private int ticksAlive = 0;
    private final List<Entity> affectedTargets = new ArrayList<>();
    private Entity currentTarget = null;
    private boolean shouldDestroy = false;
    private int currentChainIndex = 0;
    private boolean isProcessingChain = false;
    
    @Override
    public void initialize(SpellFormContext context, SpellGeometry geometry) {
        this.context = context;
        this.geometry = (ChainGeometry) geometry;
        this.currentTarget = context.target();
        
        // Добавляем первую цель
        if (currentTarget != null) {
            affectedTargets.add(currentTarget);
            ((ChainGeometry) geometry).addChainPoint(currentTarget.position());
        }
    }
    
    @Override
    public void tick(Level level) {
        if (shouldDestroy) return;
        
        ticksAlive++;
        
        // Обрабатываем цепную реакцию
        if (!isProcessingChain && currentTarget != null) {
            processChainReaction(level);
        }
        
        // Проверяем максимальное количество прыжков
        int maxBounces = (int) context.getParameter(SpellParameters.BOUNCE_COUNT, 5.0f);
        if (affectedTargets.size() >= maxBounces) {
            shouldDestroy = true;
        }
        
        // Проверяем максимальную общую дальность цепи
        float maxTotalRange = context.getParameter("max_chain_distance", 50.0f);
        if (calculateTotalChainDistance() > maxTotalRange) {
            shouldDestroy = true;
        }
        
        // Проверяем время жизни
        if (ticksAlive > 200) { // 10 секунд максимум для цепи
            shouldDestroy = true;
        }
    }
    
    private void processChainReaction(Level level) {
        isProcessingChain = true;
        
        // Применяем эффект к текущей цели
        if (currentTarget != null && currentChainIndex < affectedTargets.size()) {
            applyChainEffect(currentTarget, currentChainIndex);
            
            // Ищем следующую цель
            Entity nextTarget = findNextChainTarget(level, currentTarget.position());
            
            if (nextTarget != null) {
                affectedTargets.add(nextTarget);
                geometry.addChainPoint(nextTarget.position());
                currentTarget = nextTarget;
                currentChainIndex++;
            } else {
                // Нет больше целей - цепь заканчивается
                shouldDestroy = true;
            }
        }
        
        isProcessingChain = false;
    }
    
    private Entity findNextChainTarget(Level level, Vec3 currentPos) {
        float chainRange = context.getParameter(SpellParameters.RANGE, 8.0f);
        Set<Entity> potentialTargets = findEntitiesInRadius(level, currentPos, chainRange);
        
        // Исключаем уже пораженные цели
        potentialTargets.removeIf(affectedTargets::contains);
        
        if (potentialTargets.isEmpty()) {
            return null;
        }
        
        // Выбираем ближайшую цель
        Entity closestTarget = null;
        double closestDistance = chainRange;
        
        for (Entity entity : potentialTargets) {
            double distance = entity.position().distanceTo(currentPos);
            if (distance < closestDistance) {
                closestTarget = entity;
                closestDistance = distance;
            }
        }
        
        return closestTarget;
    }
    
    private Set<Entity> findEntitiesInRadius(Level level, Vec3 center, float radius) {
        Set<Entity> entities = new HashSet<>();
        
        // Поиск сущностей в радиусе (заглушка)
        // entities = level.getEntitiesOfClass(Entity.class, AABB.ofSize(center, radius * 2))
        //     .stream().filter(e -> e.position().distanceTo(center) <= radius)
        //     .collect(Collectors.toSet());
        
        return entities;
    }
    
    private void applyChainEffect(Entity target, int chainIndex) {
        float baseDamage = context.getParameter(SpellParameters.DAMAGE, 20.0f);
        float damageReduction = context.getParameter("chain_damage_reduction", 0.8f);
        
        // Урон снижается с каждым прыжком
        float actualDamage = baseDamage * (float)Math.pow(damageReduction, chainIndex);
        
        // target.hurt(..., actualDamage)
        
        // Специальные эффекты цепи
        applyChainSpecialEffects(target, chainIndex);
        
        // Элементальные эффекты распространяются по цепи
        applyChainElementalEffects(target, chainIndex);
    }
    
    private void applyChainSpecialEffects(Entity target, int chainIndex) {
        boolean hasManaDrain = context.getParameter("mana_drain", 0.0f) > 0;
        boolean hasSlowSpread = context.getParameter("slow_spread", 0.0f) > 0;
        boolean hasMarkTargets = context.getParameter("mark_targets", 0.0f) > 0;
        
        if (hasManaDrain) {
            float drainAmount = context.getParameter("mana_drain_amount", 10.0f);
            // Высасываем ману и передаём заклинателю
        }
        
        if (hasSlowSpread) {
            // Замедление распространяется по цепи
            int slowDuration = (int)context.getParameter("slow_duration", 100.0f);
            // target.addStatusEffect(slowness, slowDuration)
        }
        
        if (hasMarkTargets) {
            // Помечаем цели для других заклинаний
            // target.addTag("spell_chain_marked")
        }
    }
    
    private void applyChainElementalEffects(Entity target, int chainIndex) {
        float efficiency = (float)Math.pow(0.9f, chainIndex); // Эффективность падает по цепи
        
        float fireElement = context.getElement(SpellParameters.FIRE, 0.0f) * efficiency;
        float iceElement = context.getElement(SpellParameters.ICE, 0.0f) * efficiency;
        float lightningElement = context.getElement(SpellParameters.LIGHTNING, 0.0f) * efficiency;
        
        if (fireElement > 0) {
            // target.setSecondsOnFire((int)fireElement)
        }
        if (iceElement > 0) {
            // Замораживание цели
        }
        if (lightningElement > 0) {
            // Электрический разряд - идеально для цепей
        }
    }
    
    private float calculateTotalChainDistance() {
        if (geometry.getCollisionPoints().size() < 2) return 0.0f;
        
        float totalDistance = 0.0f;
        List<Vec3> points = geometry.getCollisionPoints();
        
        for (int i = 1; i < points.size(); i++) {
            totalDistance += (float)points.get(i).distanceTo(points.get(i-1));
        }
        
        return totalDistance;
    }
    
    @Override
    public void onCollideWithEntity(Entity entity) {
        // Цепи автоматически находят цели, прямые коллизии не обрабатываются
    }
    
    @Override
    public void onCollideWithBlock(BlockPos blockPos) {
        // Цепи могут быть заблокированы препятствиями
        float penetration = context.getParameter(SpellParameters.PENETRATION, 0.0f);
        if (penetration <= 0) {
            shouldDestroy = true;
        }
    }
    
    @Override
    public boolean shouldContinue() {
        return !shouldDestroy && 
               currentTarget != null &&
               affectedTargets.size() < context.getParameter(SpellParameters.BOUNCE_COUNT, 5.0f);
    }
    
    @Override
    public void cleanup() {
        // Эффекты при завершении цепи
        affectedTargets.clear();
        
        // Финальный эффект на последней цели
        boolean hasChainFinale = context.getParameter("chain_finale", 0.0f) > 0;
        if (hasChainFinale && currentTarget != null) {
            // Усиленный эффект на последней цели
            float finaleDamage = context.getParameter("finale_damage", 50.0f);
            // currentTarget.hurt(..., finaleDamage)
        }
    }
    
    @Override
    public SpellGeometry getGeometry() {
        return geometry;
    }
    
    /**
     * Получить количество пораженных целей
     */
    public int getAffectedTargetsCount() {
        return affectedTargets.size();
    }
    
    /**
     * Получить общую дистанцию цепи
     */
    public float getTotalChainDistance() {
        return calculateTotalChainDistance();
    }
    
    /**
     * Получить текущую цель
     */
    public Entity getCurrentTarget() {
        return currentTarget;
    }
}