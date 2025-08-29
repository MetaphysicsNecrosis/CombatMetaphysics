package com.example.examplemod.core.spells;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe реестр определений заклинаний
 * Компонент SpellCore Module
 */
public class SpellDefinitionRegistry {
    
    private final Map<String, SpellDefinition> definitions = new ConcurrentHashMap<>();
    
    public void register(SpellDefinition definition) {
        definitions.put(definition.id(), definition);
    }
    
    public void register(net.minecraft.resources.ResourceLocation id, SpellDefinition definition) {
        definitions.put(id.toString(), definition);
    }
    
    public Optional<SpellDefinition> get(String id) {
        return Optional.ofNullable(definitions.get(id));
    }
    
    public Optional<SpellDefinition> getDefinition(net.minecraft.resources.ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id.toString()));
    }
    
    public boolean exists(String id) {
        return definitions.containsKey(id);
    }
    
    public int size() {
        return definitions.size();
    }
}