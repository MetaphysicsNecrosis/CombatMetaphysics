package com.example.examplemod.core.spells;

import com.example.examplemod.core.spells.parameters.SpellParameters;
import java.util.Map;
import java.util.HashMap;

/**
 * Модификации заклинания для создания вариантов
 */
public record SpellModifications(
    Map<String, Float> parameterMultipliers,
    Map<String, Object> parameterOverrides,
    Map<String, String> elementalModifiers
) {
    public SpellModifications() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Float> multipliers = new HashMap<>();
        private final Map<String, Object> overrides = new HashMap<>();
        private final Map<String, String> elementals = new HashMap<>();

        public Builder multiplyParameter(String parameter, float multiplier) {
            multipliers.put(parameter, multiplier);
            return this;
        }

        public Builder overrideParameter(String parameter, Object value) {
            overrides.put(parameter, value);
            return this;
        }

        public Builder addElementalModifier(String element, String effect) {
            elementals.put(element, effect);
            return this;
        }

        public SpellModifications build() {
            return new SpellModifications(
                new HashMap<>(multipliers),
                new HashMap<>(overrides),
                new HashMap<>(elementals)
            );
        }
    }
}