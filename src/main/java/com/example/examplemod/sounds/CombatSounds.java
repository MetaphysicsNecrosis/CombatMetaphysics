package com.example.examplemod.sounds;

import com.example.examplemod.CombatMetaphysics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Звуковые события для combat системы
 */
public class CombatSounds {
    
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
        DeferredRegister.create(Registries.SOUND_EVENT, CombatMetaphysics.MODID);
    
    // Магические звуки
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGIC_PREPARE = 
        registerSound("magic_prepare");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAGIC_CAST = 
        registerSound("magic_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_SUCCESS = 
        registerSound("spell_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPELL_FAIL = 
        registerSound("spell_fail");
    
    // Звуки ближнего боя
    public static final DeferredHolder<SoundEvent, SoundEvent> MELEE_CHARGE = 
        registerSound("melee_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> MELEE_SWING = 
        registerSound("melee_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> MELEE_HIT = 
        registerSound("melee_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MELEE_CHARGED_HIT = 
        registerSound("melee_charged_hit");
    
    // Звуки защиты
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY_SUCCESS = 
        registerSound("parry_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY_FAIL = 
        registerSound("parry_fail");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_HIT = 
        registerSound("block_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> DODGE_WHOOSH = 
        registerSound("dodge_whoosh");
    
    // Системные звуки
    public static final DeferredHolder<SoundEvent, SoundEvent> STATE_TRANSITION = 
        registerSound("state_transition");
    public static final DeferredHolder<SoundEvent, SoundEvent> INTERRUPT = 
        registerSound("interrupt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESOURCE_LOW = 
        registerSound("resource_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> COOLDOWN_READY = 
        registerSound("cooldown_ready");
    
    // QTE звуки
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_START = 
        registerSound("qte_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_SUCCESS = 
        registerSound("qte_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_FAIL = 
        registerSound("qte_fail");
    public static final DeferredHolder<SoundEvent, SoundEvent> QTE_PERFECT = 
        registerSound("qte_perfect");
    
    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(CombatMetaphysics.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }
}