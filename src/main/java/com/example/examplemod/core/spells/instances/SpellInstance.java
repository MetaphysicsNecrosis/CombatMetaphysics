package com.example.examplemod.core.spells.instances;

import com.example.examplemod.core.spells.SpellDefinition;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Живой экземпляр заклинания в игровом мире
 * Thread-safe implementation для многопоточной обработки
 */
public class SpellInstance {
    private final UUID id;
    private final SpellDefinition definition;
    private final Player caster;
    private final long creationTime;
    
    private final AtomicReference<SpellState> state = new AtomicReference<>(SpellState.CREATED);
    private volatile long lastUpdateTime;
    private volatile float currentManaReserved = 0f;

    public SpellInstance(UUID id, SpellDefinition definition, Player caster, long creationTime) {
        this.id = id;
        this.definition = definition;
        this.caster = caster;
        this.creationTime = creationTime;
        this.lastUpdateTime = creationTime;
    }

    public UUID getId() {
        return id;
    }

    public SpellDefinition getDefinition() {
        return definition;
    }

    public Player getCaster() {
        return caster;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public SpellState getState() {
        return state.get();
    }

    public boolean compareAndSetState(SpellState expected, SpellState update) {
        return state.compareAndSet(expected, update);
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void updateLastUpdateTime() {
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public float getCurrentManaReserved() {
        return currentManaReserved;
    }

    public void setCurrentManaReserved(float mana) {
        this.currentManaReserved = mana;
    }

    public long getAge() {
        return System.currentTimeMillis() - creationTime;
    }

    public boolean isActive() {
        SpellState currentState = state.get();
        return currentState == SpellState.CASTING || 
               currentState == SpellState.ACTIVE || 
               currentState == SpellState.CHANNELING;
    }

    public boolean isFinished() {
        SpellState currentState = state.get();
        return currentState == SpellState.COMPLETED || 
               currentState == SpellState.CANCELLED || 
               currentState == SpellState.FAILED;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SpellInstance that = (SpellInstance) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("SpellInstance{id=%s, type=%s, state=%s, age=%d}", 
                           id, definition.id(), state.get(), getAge());
    }
}