package com.example.examplemod.core.pipeline;

import com.example.examplemod.core.state.PlayerStateComposition;
import com.example.examplemod.core.state.StateCapability;

/**
 * Стандартные мутации состояния
 */
public class StateMutations {
    public static StateMutation addCapability(StateCapability capability) {
        return new StateMutation() {
            @Override
            public void apply(PlayerStateComposition state) {
                state.addCapability(capability);
            }
            
            @Override
            public String getDescription() {
                return "Add capability: " + capability;
            }
        };
    }
    
    public static StateMutation removeCapability(StateCapability capability) {
        return new StateMutation() {
            @Override
            public void apply(PlayerStateComposition state) {
                state.removeCapability(capability);
            }
            
            @Override
            public String getDescription() {
                return "Remove capability: " + capability;
            }
        };
    }
    
    public static StateMutation setExclusive(StateCapability capability) {
        return new StateMutation() {
            @Override
            public void apply(PlayerStateComposition state) {
                state.setExclusiveCapability(capability);
            }
            
            @Override
            public String getDescription() {
                return "Set exclusive capability: " + capability;
            }
        };
    }
    
    public static StateMutation interrupt(StateCapability newCapability, String reason) {
        return new StateMutation() {
            @Override
            public void apply(PlayerStateComposition state) {
                state.interrupt(newCapability, reason);
            }
            
            @Override
            public String getDescription() {
                return "Interrupt with: " + newCapability + " (" + reason + ")";
            }
        };
    }
}