package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;

/**
 * Геометрия барьера (BARRIER) - трёхмерная N-угольная структура
 */
public class BarrierGeometry extends PolygonalGeometry {
    
    public BarrierGeometry(SpellFormContext context) {
        super(context, true); // 3D структура
    }
}