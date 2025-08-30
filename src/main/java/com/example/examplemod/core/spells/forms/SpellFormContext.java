package com.example.examplemod.core.spells.forms;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Контекст для создания геометрии заклинания
 * Содержит все необходимые данные для определения формы и поведения
 */
public record SpellFormContext(
    Level level,
    Player caster,
    Vec3 origin,
    Vec3 direction,
    Entity target,
    BlockPos targetBlock,
    Map<String, Float> parameters,
    Map<String, Float> elements
) {
    
    /**
     * Получить параметр или значение по умолчанию
     */
    public float getParameter(String key, float defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }
    
    /**
     * Получить элемент или значение по умолчанию
     */
    public float getElement(String key, float defaultValue) {
        return elements.getOrDefault(key, defaultValue);
    }
    
    /**
     * Проверить наличие параметра
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key) && parameters.get(key) > 0;
    }
    
    /**
     * Проверить наличие элемента
     */
    public boolean hasElement(String key) {
        return elements.containsKey(key) && elements.get(key) > 0;
    }
    
    /**
     * Получить позицию цели (entity или block)
     */
    public Vec3 getTargetPosition() {
        if (target != null) {
            return target.position();
        }
        if (targetBlock != null) {
            return Vec3.atCenterOf(targetBlock);
        }
        return origin.add(direction.scale(getParameter("range", 20.0f)));
    }
    
    /**
     * Вычислить дистанцию до цели
     */
    public double getDistanceToTarget() {
        return origin.distanceTo(getTargetPosition());
    }
    
    /**
     * Установить параметр (создает новую копию контекста)
     */
    public void setParameter(String key, float value) {
        parameters.put(key, value);
    }
    
    /**
     * Установить элемент (создает новую копию контекста)
     */
    public void setElement(String key, float value) {
        elements.put(key, value);
    }
    
    /**
     * Создать Builder для удобного создания контекста
     */
    public static Builder builder(Level level, Player caster) {
        return new Builder(level, caster);
    }
    
    public static class Builder {
        private final Level level;
        private final Player caster;
        private Vec3 origin;
        private Vec3 direction;
        private Entity target;
        private BlockPos targetBlock;
        private Map<String, Float> parameters = new java.util.HashMap<>();
        private Map<String, Float> elements = new java.util.HashMap<>();
        
        public Builder(Level level, Player caster) {
            this.level = level;
            this.caster = caster;
            this.origin = caster.getEyePosition();
            this.direction = caster.getViewVector(1.0f);
        }
        
        public Builder origin(Vec3 origin) {
            this.origin = origin;
            return this;
        }
        
        public Builder direction(Vec3 direction) {
            this.direction = direction;
            return this;
        }
        
        public Builder target(Entity target) {
            this.target = target;
            return this;
        }
        
        public Builder targetBlock(BlockPos targetBlock) {
            this.targetBlock = targetBlock;
            return this;
        }
        
        public Builder parameters(Map<String, Float> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder elements(Map<String, Float> elements) {
            this.elements = elements;
            return this;
        }
        
        public Builder parameter(String key, float value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public Builder element(String key, float value) {
            this.elements.put(key, value);
            return this;
        }
        
        public SpellFormContext build() {
            return new SpellFormContext(level, caster, origin, direction, target, targetBlock, parameters, elements);
        }
    }
}