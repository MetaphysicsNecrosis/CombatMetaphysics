package com.example.examplemod.core.resources;

import com.example.examplemod.core.spells.SpellDefinition;
import com.example.examplemod.core.spells.instances.SpellInstance;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Валидатор доступности ресурсов для заклинаний
 * Проверяет возможность создания и поддержания заклинаний
 */
public class ResourceValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceValidator.class);

    /**
     * Валидировать возможность начала заклинания
     */
    public static ValidationResult validateCasting(SpellDefinition definition, Player caster, ManaPool manaPool) {
        try {
            ResourceCalculator.ResourceAvailability availability = 
                ResourceCalculator.checkAvailability(definition, caster, manaPool);
            
            if (!availability.canCast()) {
                String reason = buildInsufficientResourcesReason(availability);
                return ValidationResult.failure(reason);
            }
            
            // Дополнительные проверки
            ValidationResult stateCheck = validatePlayerState(caster);
            if (!stateCheck.isValid()) {
                return stateCheck;
            }
            
            ValidationResult spellCheck = validateSpellRequirements(definition);
            if (!spellCheck.isValid()) {
                return spellCheck;
            }
            
            return ValidationResult.success();
            
        } catch (Exception e) {
            LOGGER.error("Error validating spell casting", e);
            return ValidationResult.failure("Validation error: " + e.getMessage());
        }
    }

    /**
     * Валидировать возможность продолжения поддерживаемого заклинания
     */
    public static ValidationResult validateChanneling(SpellInstance spellInstance, ManaPool manaPool) {
        try {
            SpellDefinition definition = spellInstance.getDefinition();
            Player caster = spellInstance.getCaster();
            
            float channelCost = ResourceCalculator.calculateChannelCost(definition, caster);
            
            if (!manaPool.hasAmplification(channelCost)) {
                return ValidationResult.failure("Insufficient amplification mana for channeling");
            }
            
            // Проверить состояние игрока
            ValidationResult stateCheck = validatePlayerState(caster);
            if (!stateCheck.isValid()) {
                return stateCheck;
            }
            
            return ValidationResult.success();
            
        } catch (Exception e) {
            LOGGER.error("Error validating spell channeling", e);
            return ValidationResult.failure("Channeling validation error: " + e.getMessage());
        }
    }

    /**
     * Валидировать состояние игрока
     */
    private static ValidationResult validatePlayerState(Player player) {
        // Проверить что игрок жив
        if (!player.isAlive()) {
            return ValidationResult.failure("Caster is not alive");
        }
        
        // Проверить что игрок не в творческом режиме (опционально)
        // В будущем здесь могут быть проверки на:
        // - Статусные эффекты (silence, stun, etc.)
        // - Особые состояния игрока
        // - Ограничения мира/региона
        
        return ValidationResult.success();
    }

    /**
     * Валидировать требования самого заклинания
     */
    private static ValidationResult validateSpellRequirements(SpellDefinition definition) {
        // Проверить что заклинание включено
        if (!definition.metadata().enabled()) {
            return ValidationResult.failure("Spell is disabled");
        }
        
        // В будущем здесь могут быть проверки на:
        // - Требования к уровню персонажа
        // - Необходимые предметы/реагенты
        // - Условия окружения
        // - Кулдауны
        
        return ValidationResult.success();
    }

    /**
     * Построить сообщение о нехватке ресурсов
     */
    private static String buildInsufficientResourcesReason(ResourceCalculator.ResourceAvailability availability) {
        StringBuilder sb = new StringBuilder("Insufficient resources: ");
        
        if (!availability.hasInitiation()) {
            sb.append("initiation mana (need ")
              .append(availability.requiredInitiation())
              .append(") ");
        }
        
        if (!availability.hasAmplification()) {
            sb.append("amplification mana (need ")
              .append(availability.requiredAmplification())
              .append(") ");
        }
        
        return sb.toString().trim();
    }

    /**
     * Результат валидации
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return valid ? "Valid" : "Invalid: " + reason;
        }
    }
}