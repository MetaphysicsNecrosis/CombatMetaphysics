package com.example.examplemod.core.spells.geometry;

import com.example.examplemod.core.spells.forms.SpellFormContext;

/**
 * Геометрия зоны (AREA) - двумерная N-угольная зона
 */
public class AreaGeometry extends PolygonalGeometry {
    
    public AreaGeometry(SpellFormContext context) {
        super(context, false); // 2D структура
    }
}