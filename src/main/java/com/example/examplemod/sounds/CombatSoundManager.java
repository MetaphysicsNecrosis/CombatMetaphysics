package com.example.examplemod.sounds;

import com.example.examplemod.core.PlayerState;
import com.example.examplemod.core.DirectionalAttackSystem;
import com.example.examplemod.core.DefensiveActionsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Менеджер звуков для combat системы
 * Обеспечивает подходящие звуковые эффекты для всех действий
 */
public class CombatSoundManager {
    
    /**
     * Воспроизводит звук перехода состояния
     */
    public static void playStateTransition(Player player, PlayerState fromState, PlayerState toState) {
        if (player == null) return;
        
        // Специфичные звуки для переходов
        switch (toState) {
            case MAGIC_PREPARING -> playSound(player, CombatSounds.MAGIC_PREPARE.get(), 0.8f, 1.0f);
            case MAGIC_CASTING -> playSound(player, CombatSounds.MAGIC_CAST.get(), 1.0f, 1.0f);
            case MELEE_PREPARING -> playSound(player, CombatSounds.MELEE_CHARGE.get(), 0.6f, 0.8f);
            case MELEE_CHARGING -> playSound(player, CombatSounds.MELEE_CHARGE.get(), 0.8f, 1.2f);
            case INTERRUPTED -> playSound(player, CombatSounds.INTERRUPT.get(), 1.0f, 1.0f);
            case IDLE -> {
                if (fromState.isActive()) {
                    playSound(player, CombatSounds.COOLDOWN_READY.get(), 0.5f, 1.0f);
                }
            }
            default -> {
                if (fromState != PlayerState.IDLE && toState != PlayerState.IDLE) {
                    playSound(player, CombatSounds.STATE_TRANSITION.get(), 0.3f, 1.0f);
                }
            }
        }
    }
    
    /**
     * Воспроизводит звук магического действия
     */
    public static void playMagicAction(Player player, String action, boolean success) {
        if (player == null) return;
        
        switch (action.toLowerCase()) {
            case "prepare", "preparation" -> playSound(player, CombatSounds.MAGIC_PREPARE.get(), 0.8f, 1.0f);
            case "cast", "casting" -> playSound(player, CombatSounds.MAGIC_CAST.get(), 1.0f, 1.0f);
            case "complete", "success" -> {
                if (success) {
                    playSound(player, CombatSounds.SPELL_SUCCESS.get(), 1.0f, 1.0f);
                } else {
                    playSound(player, CombatSounds.SPELL_FAIL.get(), 0.8f, 0.8f);
                }
            }
        }
    }
    
    /**
     * Воспроизводит звук атаки ближнего боя
     */
    public static void playMeleeAction(Player player, DirectionalAttackSystem.AttackDirection direction, 
                                     String action, boolean isCharged, boolean success) {
        if (player == null) return;
        
        switch (action.toLowerCase()) {
            case "start", "charge" -> {
                float pitch = switch (direction) {
                    case LEFT_ATTACK -> 1.2f;  // Быстрая атака - высокий тон
                    case RIGHT_ATTACK -> 1.0f; // Средняя атака - нормальный тон
                    case TOP_ATTACK -> 0.8f;   // Медленная атака - низкий тон
                    case THRUST_ATTACK -> 1.4f; // Колющая атака - очень высокий тон
                };
                playSound(player, CombatSounds.MELEE_CHARGE.get(), 0.6f, pitch);
            }
            case "swing", "execute" -> {
                float volume = isCharged ? 1.2f : 0.8f;
                float pitch = isCharged ? 0.8f : 1.0f;
                playSound(player, CombatSounds.MELEE_SWING.get(), volume, pitch);
            }
            case "hit", "impact" -> {
                if (isCharged) {
                    playSound(player, CombatSounds.MELEE_CHARGED_HIT.get(), 1.2f, 0.9f);
                } else {
                    playSound(player, CombatSounds.MELEE_HIT.get(), 1.0f, 1.0f);
                }
            }
        }
    }
    
    /**
     * Воспроизводит звук защитного действия
     */
    public static void playDefenseAction(Player player, DefensiveActionsManager.DefensiveType type, 
                                       String action, boolean success) {
        if (player == null) return;
        
        switch (type) {
            case PARRY -> {
                if (success) {
                    playSound(player, CombatSounds.PARRY_SUCCESS.get(), 1.0f, 1.0f);
                } else {
                    playSound(player, CombatSounds.PARRY_FAIL.get(), 0.6f, 0.8f);
                }
            }
            case BLOCK -> {
                if (action.equals("hit")) {
                    playSound(player, CombatSounds.BLOCK_HIT.get(), 0.8f, 1.0f);
                }
            }
            case DODGE -> {
                if (action.equals("activate")) {
                    playSound(player, CombatSounds.DODGE_WHOOSH.get(), 0.7f, 1.2f);
                }
            }
        }
    }
    
    /**
     * Воспроизводит звук QTE события
     */
    public static void playQTESound(Player player, String event, float accuracy) {
        if (player == null) return;
        
        switch (event.toLowerCase()) {
            case "start" -> playSound(player, CombatSounds.QTE_START.get(), 0.8f, 1.0f);
            case "success" -> {
                if (accuracy >= 0.9f) {
                    playSound(player, CombatSounds.QTE_PERFECT.get(), 1.0f, 1.0f);
                } else {
                    playSound(player, CombatSounds.QTE_SUCCESS.get(), 0.8f, 1.0f);
                }
            }
            case "fail" -> playSound(player, CombatSounds.QTE_FAIL.get(), 0.6f, 0.8f);
        }
    }
    
    /**
     * Воспроизводит звук прерывания
     */
    public static void playInterruptSound(Player player, String interruptType) {
        if (player == null) return;
        
        float pitch = switch (interruptType.toLowerCase()) {
            case "physical_hit" -> 0.8f;  // Низкий тон для физического удара
            case "magical_hit" -> 1.2f;   // Высокий тон для магического удара
            case "mass_aoe_hit" -> 1.0f;  // Средний тон для массового эффекта
            default -> 1.0f;
        };
        
        playSound(player, CombatSounds.INTERRUPT.get(), 1.0f, pitch);
    }
    
    /**
     * Воспроизводит звук предупреждения о низких ресурсах
     */
    public static void playResourceWarning(Player player, String resourceType) {
        if (player == null) return;
        
        float pitch = resourceType.equals("mana") ? 1.2f : 0.8f; // Высокий для маны, низкий для стамины
        playSound(player, CombatSounds.RESOURCE_LOW.get(), 0.5f, pitch);
    }
    
    /**
     * Воспроизводит звук заряжания атаки (непрерывный)
     */
    public static void playChargeLoop(Player player, float chargeProgress) {
        if (player == null) return;
        
        // Изменяем высоту тона в зависимости от прогресса заряжания
        float pitch = 0.8f + (chargeProgress * 0.8f); // От 0.8 до 1.6
        float volume = 0.3f + (chargeProgress * 0.4f); // От 0.3 до 0.7
        
        playSound(player, CombatSounds.MELEE_CHARGE.get(), volume, pitch);
    }
    
    /**
     * Базовый метод для воспроизведения звука
     */
    private static void playSound(Player player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (player.level().isClientSide) {
            // На клиенте воспроизводим звук локально
            Minecraft.getInstance().level.playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS,
                volume, pitch, false
            );
        } else {
            // На сервере отправляем звук всем игрокам поблизости
            player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
        }
    }
    
    /**
     * Останавливает все combat звуки (для чрезвычайных ситуаций)
     */
    public static void stopAllCombatSounds() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            // Можно добавить логику остановки звуков если потребуется
        }
    }
    
    /**
     * Воспроизводит случайный звук из массива
     */
    public static void playRandomSound(Player player, net.minecraft.sounds.SoundEvent[] sounds, 
                                     float volume, float pitchVariation) {
        if (player == null || sounds.length == 0) return;
        
        net.minecraft.sounds.SoundEvent sound = sounds[player.getRandom().nextInt(sounds.length)];
        float pitch = 1.0f + (player.getRandom().nextFloat() - 0.5f) * pitchVariation;
        
        playSound(player, sound, volume, pitch);
    }
}