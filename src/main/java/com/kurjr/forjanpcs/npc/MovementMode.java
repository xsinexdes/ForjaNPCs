package com.kurjr.forjanpcs.npc;

/**
 * Modo de movimiento autónomo de un NPC.
 */
public enum MovementMode {
    /** El NPC no se mueve solo. */
    NONE,
    /** Recorre sus waypoints en orden, en loop. */
    PATROL,
    /** Camina a puntos aleatorios dentro de un radio alrededor de su punto de origen. */
    WANDER,
    /** Combinación: patrulla los waypoints y, al llegar a cada uno, hace un rato de wander
     *  cerca antes de seguir al siguiente punto. */
    COMBO
}
