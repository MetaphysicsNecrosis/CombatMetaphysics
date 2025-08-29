#!/usr/bin/env python3
import os
import re

param_dir = "C:/GYM/MOD/CombatMetaphysics/src/main/java/com/example/examplemod/core/spells/parameters/impl"

files_to_fix = [
    "DurationParameter.java",
    "GrowthRateParameter.java", 
    "HealingParameter.java",
    "HomingStrengthParameter.java",
    "PenetrationParameter.java",
    "PierceCountParameter.java",
    "RadiusParameter.java",
    "RangeParameter.java", 
    "SpeedParameter.java",
    "TickRateParameter.java"
]

def fix_parameter_file(filepath):
    print(f"Fixing: {filepath}")
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 1. Заменить возвращаемый тип метода compute
    content = re.sub(
        r'public Map<String, Object> compute\(SpellComputationContext context, Object inputValue\)',
        'public com.example.examplemod.core.spells.computation.SpellComputationResult compute(SpellComputationContext context, Object inputValue)',
        content
    )
    
    # 2. Заменить emptyResult() на SpellComputationResult.builder().build()
    content = re.sub(
        r'return emptyResult\(\);',
        'return com.example.examplemod.core.spells.computation.SpellComputationResult.builder().build();',
        content
    )
    
    # 3. Заменить buildResult(...) на SpellComputationResult.builder()
    content = re.sub(
        r'return buildResult\(([^)]+)\)',
        lambda m: f'return com.example.examplemod.core.spells.computation.SpellComputationResult.builder()\n            .putValue("{get_param_name(filepath)}", {m.group(1)})',
        content
    )
    
    # 4. Заменить .put( на .putValue(
    content = re.sub(r'            \.put\(', '            .putValue(', content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

def get_param_name(filepath):
    filename = os.path.basename(filepath)
    if "Duration" in filename:
        return "duration"
    elif "Growth" in filename:
        return "growth_rate"
    elif "Healing" in filename:
        return "healing"
    elif "Homing" in filename:
        return "homing_strength"
    elif "Penetration" in filename:
        return "penetration"
    elif "Pierce" in filename:
        return "pierce_count"
    elif "Radius" in filename:
        return "radius"
    elif "Range" in filename:
        return "range"
    elif "Speed" in filename:
        return "speed"
    elif "TickRate" in filename:
        return "tick_rate"
    else:
        return "value"

for filename in files_to_fix:
    filepath = os.path.join(param_dir, filename)
    if os.path.exists(filepath):
        fix_parameter_file(filepath)
    else:
        print(f"Файл не найден: {filepath}")

print("Готово!")
