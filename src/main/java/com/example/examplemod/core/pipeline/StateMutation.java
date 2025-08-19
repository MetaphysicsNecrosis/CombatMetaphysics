package com.example.examplemod.core.pipeline;

import com.example.examplemod.core.state.PlayerStateComposition;

/**
 * Мутация состояния - изменение состояния игрока
 */
public interface StateMutation {
    void apply(PlayerStateComposition state);
    String getDescription();
}