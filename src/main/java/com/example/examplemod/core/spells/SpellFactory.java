package com.example.examplemod.core.spells;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.example.examplemod.core.spells.instances.SpellInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Фабрика для создания экземпляров заклинаний
 * Thread-safe создание spell instances из определений
 */
public class SpellFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpellFactory.class);
    private final SpellDefinitionRegistry registry;

    public SpellFactory(SpellDefinitionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Создать экземпляр заклинания по его ID
     */
    public Optional<SpellInstance> createSpell(ResourceLocation spellId, Player caster) {
        Optional<SpellDefinition> definition = registry.getDefinition(spellId);
        
        if (definition.isEmpty()) {
            LOGGER.warn("Failed to create spell {}: definition not found", spellId);
            return Optional.empty();
        }

        try {
            SpellInstance instance = new SpellInstance(
                UUID.randomUUID(),
                definition.get(),
                caster,
                System.currentTimeMillis()
            );
            
            LOGGER.debug("Created spell instance {} of type {} for player {}", 
                        instance.getId(), spellId, caster.getName().getString());
            
            return Optional.of(instance);
        } catch (Exception e) {
            LOGGER.error("Failed to create spell instance for {}", spellId, e);
            return Optional.empty();
        }
    }

    /**
     * Создать модифицированный экземпляр заклинания с кастомными параметрами
     */
    public Optional<SpellInstance> createModifiedSpell(ResourceLocation spellId, Player caster, SpellModifications modifications) {
        Optional<SpellInstance> baseSpell = createSpell(spellId, caster);
        
        if (baseSpell.isEmpty()) {
            return Optional.empty();
        }

        try {
            SpellInstance modified = applyModifications(baseSpell.get(), modifications);
            LOGGER.debug("Created modified spell instance {} with modifications", modified.getId());
            return Optional.of(modified);
        } catch (Exception e) {
            LOGGER.error("Failed to apply modifications to spell {}", spellId, e);
            return baseSpell; // Возвращаем базовую версию
        }
    }

    private SpellInstance applyModifications(SpellInstance baseSpell, SpellModifications modifications) {
        // TODO: Implement modification application
        // Для сейчас просто возвращаем базовое заклинание
        return baseSpell;
    }
}