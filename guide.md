# CombatMetaphysics Architecture Specification v2.1

## Executive Summary

CombatMetaphysics implements a **deterministic, skill-based combat framework** where every outcome is predictable and controllable through player mastery. The system prioritizes **tactical decision-making**, **rhythm-based gameplay**, and **stamina-centric resource management** over mathematical abstractions.

---

## I. Core Philosophy: Deterministic Combat

### 1.1 Predictability Principle

**Fundamental Rule**: Every combat outcome must be completely predictable given full knowledge of game state.

```
Combat_Outcome := Function(
    attacker_state,
    defender_state, 
    action_properties,
    environmental_factors
)

NO_RANDOM_ELEMENTS allowed in core combat resolution
```

**Player Agency**: Players always know:
- When their action will succeed/fail
- Why an interruption occurred
- How to prevent/cause specific outcomes
- Exact resource costs and recovery times

### 1.2 Rhythm-Based Interaction Model

**Gothic-Style Timing**:
```
Attack_Execution := {
    input_phase: hold_attack_button(),
    direction_selection: movement_key_during_windup(),
    execution_phase: release_attack_button(),
    recovery_phase: mandatory_cooldown_period()
}

Combo_Chaining := {
    timing_window: [end_of_active_frames, start_of_recovery],
    rhythm_requirement: consistent_interval_between_inputs,
    failure_penalty: full_recovery_cycle_required
}
```

**Skill Expression**: Mastery demonstrated through precise timing windows, not reaction speed or button mashing.

---

## II. Stamina-Centric Architecture

### 2.1 Stamina as Primary Defense Mechanism

**Interruption Resistance Model**:
```
interruption_occurs := (interrupt_power * source_priority) > resistance_threshold

resistance_threshold := base_action_resistance + stamina_defense_bonus

stamina_defense_bonus := (current_stamina / max_stamina) * STAMINA_RESISTANCE_MULTIPLIER

where STAMINA_RESISTANCE_MULTIPLIER is substantial (e.g., 50-100% of base resistance)
```

**Critical Thresholds**:
- `stamina > 80%`: Full interruption resistance
- `stamina 50-80%`: Moderate vulnerability
- `stamina 20-50%`: High vulnerability
- `stamina < 20%`: "Exhausted" state, extremely vulnerable

### 2.2 Stamina-Action Integration

**Action Gating**:
```
action_availability := 
    if stamina >= required_cost: FULL_POWER
    elif stamina >= (required_cost * 0.5): REDUCED_EFFECTIVENESS
    else: ACTION_UNAVAILABLE
```

**Recovery Dynamics**:
```
stamina_recovery_rate := 
    if in_combat: BASE_RECOVERY * 0.3
    elif moving: BASE_RECOVERY * 0.7  
    else: BASE_RECOVERY * 1.0
```

---

## III. QTE System: Sequential Input Mastery

### 3.1 Sequence-Based QTE Model

**QTE Definition**:
```
QTE_Challenge := {
    key_sequence: [key₁, key₂, ..., keyₙ],
    time_limit: total_duration,
    per_key_window: individual_timing_tolerance,
    difficulty_scaling: sequence_length + speed_requirement
}
```

**OSU-Style Visual Design**:
- Shrinking circles indicate timing windows
- Key prompts appear in sequence, not simultaneously
- Visual feedback for each keypress (hit/miss/perfect)
- Progressive difficulty through longer sequences + tighter timing

### 3.2 QTE Evaluation Algorithm

**Success Calculation**:
```
qte_evaluation(player_inputs, required_sequence):
    correct_inputs := count_matching_keys(player_inputs, required_sequence)
    timing_precision := average_timing_accuracy(player_inputs)
    
    success_percentage := (correct_inputs / sequence_length) * 100
    
    return match success_percentage:
        >= 95%: PERFECT (1.5x effectiveness)
        >= 85%: GREAT (1.2x effectiveness)  
        >= 70%: GOOD (1.0x effectiveness)
        >= 50%: OKAY (0.8x effectiveness)
        < 50%: FAILED (0.5x effectiveness + penalties)
```

**Difficulty Progression**:
```
qte_difficulty(spell_position, spell_power):
    base_keys := 3
    sequence_length := base_keys + (spell_position - 1) + power_scaling(spell_power)
    time_per_key := BASE_TIME - (difficulty_level * TIME_REDUCTION)
```

---

## IV. Deterministic Interruption System

### 4.1 Threshold-Based Interruption Model

**Core Algorithm**:
```
resolve_interruption(interrupt_event, target_entity):
    action := target_entity.current_action
    
    # Calculate defender's total resistance
    base_resistance := action.interruption_resistance
    stamina_bonus := (target.current_stamina / target.max_stamina) * STAMINA_MULTIPLIER
    state_modifier := compute_state_resistance(target.current_state)
    
    total_resistance := base_resistance + stamina_bonus + state_modifier
    
    # Calculate interruption power
    interrupt_power := event.base_power * event.priority_multiplier
    
    # Deterministic resolution
    if interrupt_power > total_resistance:
        return apply_interruption_type(event.interruption_type, action)
    elif interrupt_power > (total_resistance * 0.7):
        return apply_micro_stun(action, duration=0.2s)
    else:
        return NO_EFFECT
```

**Interruption Types**:
```
Micro_Stun: pause_action(0.1-0.3s) + slight_effectiveness_reduction(10%)
Full_Interruption: cancel_action() + lose_reserved_resources() + recovery_penalty()
Partial_Execution: continue_action() + power_reduction(interrupt_strength / resistance)
```

### 4.2 Absolute Actions with Explicit Exceptions

**Exception System**:
```
Absolute_Action := {
    base_action: any_action,
    exception_conditions: Set[{
        condition_type: DAMAGE_THRESHOLD | SPECIFIC_EFFECT | ENVIRONMENTAL,
        trigger_value: threshold_or_effect_id,
        penalty_multiplier: punishment_for_interruption
    }],
    completion_penalties: mandatory_drawbacks_after_execution
}
```

**Exception Processing**:
```
process_absolute_interruption(event, absolute_action):
    for exception in absolute_action.exceptions:
        if exception.matches(event) AND event.power >= exception.trigger_value:
            return force_interrupt() + apply_penalty(exception.penalty_multiplier)
    
    return continue_absolute_action() # No interruption possible
```

---

## V. Gothic-Style Directional Combat

### 5.1 Directional Attack Specification

**Attack Definition**:
```
Directional_Attack := {
    input_combination: movement_key + attack_button,
    collision_properties: {
        swing_pattern: arc_shape + trajectory,
        active_frames: duration_of_damage_dealing,
        collision_volume: spatial_area_affected
    },
    gameplay_properties: {
        speed: wind_up_duration + recovery_duration,
        stamina_cost: base_cost + direction_modifier,
        block_pierce_chance: probability_to_bypass_defense,
        knockback_vector: force_direction + magnitude,
        combo_compatibility: which_attacks_can_chain
    }
}
```

**Direction Mapping**:
```
Input_Combinations:
    A + Attack: horizontal_left_slash(wide_arc, medium_speed, high_knockback)
    D + Attack: horizontal_right_slash(wide_arc, medium_speed, high_knockback)  
    W + Attack: overhead_strike(narrow_arc, slow_speed, high_pierce)
    S + Attack: thrust_stab(line_collision, fast_speed, low_knockback)
    No_Direction + Attack: default_swing(balanced_properties)
```

### 5.2 Temporal Collision System

**Frame-Based Hit Detection**:
```
collision_detection(weapon_swing, potential_targets):
    for frame in weapon_swing.active_frames:
        collision_volume := transform_to_world_space(
            frame.local_hitbox, 
            player.position, 
            player.facing_direction
        )
        
        affected_entities := spatial_query(collision_volume, potential_targets)
        
        for entity in affected_entities:
            if not already_hit_by_this_swing(entity):
                apply_hit_effects(entity, frame.damage_multiplier, weapon_swing.properties)
                mark_as_hit(entity, weapon_swing.id)
```

**Swept Volume Optimization**:
```
swept_volume_calculation(swing_pattern):
    total_volume := union_of_all_frame_volumes(swing_pattern.frames)
    broad_phase_candidates := spatial_hash_query(total_volume)
    
    for candidate in broad_phase_candidates:
        for frame in swing_pattern.frames:
            if precise_collision_test(frame.volume, candidate.hitbox):
                register_hit(candidate, frame.timestamp)
```

---

## VI. Layered Defense: Deterministic Processing

### 6.1 Defense Layer Pipeline

**Ordered Processing**:
```
process_incoming_damage(damage_source, defending_entity):
    damage := damage_source.base_damage
    
    # Layer 1: Immunity Check
    if defending_entity.has_immunity(damage_source.type):
        return NO_DAMAGE
    
    # Layer 2: Active Defense (Parry/Block)
    active_defense_result := check_active_defense(damage_source, defending_entity)
    if active_defense_result.completely_negated:
        return PERFECT_DEFENSE + apply_counterattack_opportunity()
    else:
        damage := damage * active_defense_result.damage_multiplier
    
    # Layer 3: Passive Resistances
    resistance_multiplier := get_resistance(defending_entity, damage_source.type)
    damage := damage * (1.0 - resistance_multiplier)
    
    # Layer 4: Absorption
    absorbed := min(damage, defending_entity.absorption_amount)
    damage := max(0, damage - absorbed)
    defending_entity.absorption_amount -= absorbed
    
    return apply_final_damage(damage, defending_entity)
```

### 6.2 Perfect Defense Windows

**Parry Timing**:
```
attempt_parry(parry_input_time, incoming_attack_time):
    timing_difference := abs(parry_input_time - incoming_attack_time)
    
    return match timing_difference:
        <= PERFECT_WINDOW (50ms): PERFECT_PARRY(
            damage_negated=100%, 
            attacker_stunned=true,
            counterattack_window=1.5s,
            stamina_cost=5
        )
        <= GOOD_WINDOW (150ms): GOOD_PARRY(
            damage_negated=75%,
            attacker_staggered=true, 
            stamina_cost=10
        )
        <= POOR_WINDOW (300ms): POOR_PARRY(
            damage_negated=25%,
            stamina_cost=20
        )
        > POOR_WINDOW: PARRY_FAILED(
            full_damage_taken=true,
            defender_staggered=true,
            stamina_cost=30
        )
```

---

## VII. Composite Spell System: Skill-Gated Power

### 7.1 Spell Chain Architecture

**Chain Definition**:
```
Composite_Spell := {
    steps: [step₁, step₂, ..., stepₙ],
    resource_reservation: FULL | INCREMENTAL | HYBRID,
    qte_requirements: per_step_qte_configuration,
    failure_policy: what_happens_on_qte_failure | interruption
}

Spell_Step := {
    base_action: primitive_action_reference,
    parameter_substitutions: context_value_mapping,
    execution_conditions: prerequisites_for_step,
    qte_config: {
        sequence_length: number_of_keys,
        time_limit: maximum_duration,
        key_set: available_input_keys
    }
}
```

### 7.2 Chain Execution with Skill Gates

**Progressive Difficulty**:
```
execute_spell_chain(spell, caster):
    reserved_resources := reserve_full_cost(spell.total_cost, caster)
    efficiency_multiplier := 1.0
    
    for step_index, step in enumerate(spell.steps):
        # Validate step conditions
        if not step.conditions.satisfied(current_context):
            return CHAIN_BROKEN(step=step_index, resources_lost=reserved_resources)
        
        # Execute QTE if required
        if step.qte_config exists:
            qte_difficulty := base_difficulty + step_index + power_scaling
            qte_result := execute_qte(step.qte_config, qte_difficulty)
            
            if qte_result.failed():
                return apply_failure_policy(spell.failure_policy, step_index, reserved_resources)
            
            efficiency_multiplier *= qte_result.efficiency_bonus
        
        # Execute the actual action
        step_result := execute_action(step.base_action, step.parameters, current_context)
        current_context := merge_contexts(current_context, step_result.output)
        
        # Check for external interruptions
        if external_interruption_detected():
            return apply_interruption_policy(spell.interrupt_policy, step_index)
    
    # Apply cumulative effectiveness
    final_power := spell.base_power * efficiency_multiplier
    commit_resources(reserved_resources)
    return CHAIN_SUCCESS(power=final_power, context=current_context)
```

**Skill Reward System**:
```
Combo_Effectiveness := base_power × ∏(qte_efficiency_i) × chain_length_bonus

where:
    qte_efficiency_i ∈ [0.5, 1.5] based on QTE performance
    chain_length_bonus := 1.0 + (successful_steps * 0.1)
```

---

## VIII. Critical Feedback Systems

### 8.1 Visual Communication Priority

**Mandatory Visual Indicators** (HIGH priority, not cleanup):
```
Combat_Feedback_Requirements := {
    Action_Telegraphing: {
        windup_animations: clear_visual_buildup,
        collision_preview: show_intended_hit_area,
        timing_indicators: countdown_or_progress_bars
    },
    
    State_Communication: {
        stamina_visualization: always_visible_stamina_bar,
        interruption_feedback: clear_indication_why_action_failed,
        defense_state: visual_indication_of_active_defenses
    },
    
    QTE_Interface: {
        sequence_preview: show_upcoming_keys,
        timing_feedback: real_time_accuracy_indication,
        result_communication: immediate_success_failure_display
    }
}
```

**Audio Feedback Requirements**:
```
Sound_Design_Priorities := {
    combat_rhythm: audio_cues_that_support_timing_gameplay,
    state_transitions: distinct_sounds_for_major_state_changes,
    feedback_confirmation: immediate_audio_response_to_player_input
}
```

### 8.2 Information Transparency

**Player Knowledge Requirements**:
- Current stamina and precise recovery rate
- Exact interruption resistance values
- QTE difficulty and timing windows
- Action costs and cooldown remaining
- Defense layer status and effectiveness

---

## IX. Technical Architecture: NeoForge Integration

### 9.1 Performance-First Implementation

**WorldEdit-Style Batch Processing**:
```
Large_Scale_Operations := {
    spatial_grouping: operations_clustered_by_chunk,
    time_slicing: max_5ms_processing_per_tick,
    deferred_updates: physics_lighting_applied_in_batches,
    memory_pooling: reuse_objects_for_high_frequency_operations
}
```

**Collision Optimization**:
```
Spatial_Indexing := {
    hash_grid: O(1)_entity_lookup_for_collision_candidates,
    swept_volumes: single_calculation_for_entire_swing_arc,
    early_culling: eliminate_impossible_hits_before_precise_testing
}
```

### 9.2 Codec-Based Data Architecture

**Universal Serialization**:
- Every game component has associated Codec
- JSON ↔ Component bidirectional conversion
- Automatic validation during deserialization
- Data generation via RegistrySetBuilder

**Extension Points**:
- Custom actions via ActionType registry
- Custom defense layers via LayerType registry
- Custom QTE patterns via QTEType registry
- Mod integration through event bus priorities

---

## X. Development Roadmap: Gameplay First

### Phase 1: Core Feel (Critical)
1. **Stamina System**: Stamina-based interruption resistance
2. **Basic Combat**: Directional attacks with proper timing
3. **QTE Framework**: Sequence-based mini-games with OSU visualization
4. **Feedback Systems**: Visual/audio indicators for all major events

### Phase 2: Combat Depth (High Priority)
1. **Defense Systems**: Parry/block with precise timing windows
2. **Interruption System**: Deterministic threshold-based resolution
3. **Combo Chains**: Rhythm-based attack combinations
4. **Spell Basics**: Single-step spells with resource management

### Phase 3: Advanced Systems (Medium Priority)
1. **Composite Spells**: Multi-step chains with QTE skill gates
2. **Performance Optimization**: Large-scale operations and collision
3. **Weapon Variety**: Different weapon types with unique properties
4. **AI Integration**: NPCs that use the combat system

### Phase 4: Polish & Extension (Low Priority)
1. **Spell Builder**: Visual creation tools for custom spells
2. **Balance Tools**: Runtime adjustment and testing utilities
3. **Mod APIs**: Extension points for other combat mods
4. **Advanced Features**: Tournaments, skill progression, etc.

---

## XI. Conclusion: Gameplay Over Abstraction

CombatMetaphysics prioritizes **player experience and skill expression** over technical elegance. Every system serves the core goal of creating **predictable, skill-based combat** where:

- Outcomes depend on player skill, not random chance
- Stamina management is central to tactical decision-making
- Timing and rhythm reward mastery over button mashing
- Visual and audio feedback clearly communicate game state
- Interruptions and defenses follow consistent, learnable rules

The architecture supports these goals through **deterministic algorithms**, **transparent mechanics**, and **performance-optimized implementation** that scales from single combat to large battles.