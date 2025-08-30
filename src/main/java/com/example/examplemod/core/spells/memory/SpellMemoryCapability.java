package com.example.examplemod.core.spells.memory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Capability для хранения SpellMemory игрока
 * Автоматически синхронизируется и сохраняется
 */
public class SpellMemoryCapability implements INBTSerializable<CompoundTag> {
    
    private SpellMemory spellMemory;
    
    public SpellMemoryCapability() {
        this.spellMemory = null; // Инициализируется при первом доступе
    }
    
    public SpellMemoryCapability(Player player) {
        this.spellMemory = new SpellMemory(player.getUUID());
    }
    
    /**
     * Получить SpellMemory игрока
     */
    public SpellMemory getSpellMemory(Player player) {
        if (spellMemory == null) {
            spellMemory = new SpellMemory(player.getUUID());
        }
        return spellMemory;
    }
    
    /**
     * Установить SpellMemory (для загрузки/синхронизации)
     */
    public void setSpellMemory(SpellMemory spellMemory) {
        this.spellMemory = spellMemory;
    }
    
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        if (spellMemory != null) {
            return spellMemory.saveToNBT();
        }
        return new CompoundTag();
    }
    
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (!nbt.isEmpty()) {
            this.spellMemory = SpellMemory.loadFromNBT(nbt);
        }
    }
    
    /**
     * Создать копию для синхронизации клиента
     */
    public SpellMemoryCapability copy() {
        SpellMemoryCapability copy = new SpellMemoryCapability();
        if (this.spellMemory != null) {
            copy.spellMemory = SpellMemory.loadFromNBT(this.spellMemory.saveToNBT());
        }
        return copy;
    }
}