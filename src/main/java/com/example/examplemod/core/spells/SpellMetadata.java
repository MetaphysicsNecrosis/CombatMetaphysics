package com.example.examplemod.core.spells;

import net.minecraft.network.chat.Component;

/**
 * Метаданные заклинания - описательная информация
 */
public record SpellMetadata(
    Component displayName,
    Component description,
    String author,
    int version,
    boolean enabled
) {
    public SpellMetadata() {
        this(Component.literal("Unknown Spell"), 
             Component.literal("No description"), 
             "Unknown", 
             1, 
             true);
    }

    public SpellMetadata(Component displayName, Component description) {
        this(displayName, description, "Unknown", 1, true);
    }

    public SpellMetadata {
        if (displayName == null) throw new IllegalArgumentException("Display name cannot be null");
        if (description == null) throw new IllegalArgumentException("Description cannot be null");
        if (author == null) throw new IllegalArgumentException("Author cannot be null");
        if (version < 1) throw new IllegalArgumentException("Version must be positive");
    }
}