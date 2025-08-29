package com.example.examplemod.sounds;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.example.examplemod.CombatMetaphysics;

/**
 * Звуки для системы боевой магии
 */
public class CombatSounds {
    
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
        DeferredRegister.create(Registries.SOUND_EVENT, CombatMetaphysics.MODID);

    // Звуки применения заклинаний
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_CAST = registerSoundEvent("spell_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_IMPACT = registerSoundEvent("spell_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_CHANNEL = registerSoundEvent("spell_channel");
    
    // Звуки форм заклинаний
    public static final DeferredHolder<SoundEvent, SoundEvent> PROJECTILE_LAUNCH = registerSoundEvent("projectile_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEAM_FIRE = registerSoundEvent("beam_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BARRIER_CREATE = registerSoundEvent("barrier_create");
    public static final DeferredHolder<SoundEvent, SoundEvent> AREA_ACTIVATE = registerSoundEvent("area_activate");
    public static final DeferredHolder<SoundEvent, SoundEvent> WAVE_EXPAND = registerSoundEvent("wave_expand");
    
    // QTE звуки
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_HIT = registerSoundEvent("qte_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_MISS = registerSoundEvent("qte_miss");
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_PERFECT = registerSoundEvent("qte_perfect");
    
    // Системные звуки
    public static final DeferredHolder<SoundEvent, SoundEvent> MANA_EMPTY = registerSoundEvent("mana_empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_INTERRUPTED = registerSoundEvent("spell_interrupted");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_CANCELLED = registerSoundEvent("spell_cancelled");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(CombatMetaphysics.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }

    // Utility методы для проигрывания звуков
    public static SoundEvent getSpellFormSound(String formType) {
        return switch (formType.toLowerCase()) {
            case "projectile" -> PROJECTILE_LAUNCH.get();
            case "beam" -> BEAM_FIRE.get();
            case "barrier" -> BARRIER_CREATE.get();
            case "area" -> AREA_ACTIVATE.get();
            case "wave" -> WAVE_EXPAND.get();
            default -> SPELL_CAST.get();
        };
    }

    public static SoundEvent getQTESound(float accuracy) {
        if (accuracy >= 0.95f) {
            return QTE_PERFECT.get();
        } else if (accuracy >= 0.7f) {
            return QTE_HIT.get();
        } else {
            return QTE_MISS.get();
        }
    }
}