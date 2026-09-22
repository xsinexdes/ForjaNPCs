package com.kurjr.forjanpcs.npc;

import com.kurjr.forjanpcs.ForjaNPCsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Mueve a los NPCs con IA propia y liviana: sin pathfinding real (no esquiva obstáculos
 * complejos), pero funciona igual en Spigot puro y en Paper porque solo usa la API pública
 * de Bukkit (velocity + teleport de rotación), sin tocar clases internas de NMS por versión.
 *
 * Modo COMBO = patrulla los waypoints, y al llegar a cada uno hace un rato de "wander"
 * (caminata aleatoria corta) antes de seguir al siguiente punto.
 */
public class NpcMovementTask extends BukkitRunnable {

    private final ForjaNPCsPlugin plugin;
    private final Random random = new Random();

    // Estado de "esperando/wandereando en el punto" por NPC (para el modo COMBO)
    private final Map<Integer, Integer> idleCounter = new HashMap<>();
    private final Map<Integer, Location> wanderTarget = new HashMap<>();

    public NpcMovementTask(ForjaNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (ForjaNpc npc : plugin.getNpcManager().all()) {
            if (npc.getMode() == MovementMode.NONE) continue;
            LivingEntity entity = npc.resolveEntity();
            if (entity == null || !entity.isValid()) continue;

            switch (npc.getMode()) {
                case PATROL -> tickPatrol(npc, entity);
                case WANDER -> tickWander(npc, entity, npc.getSpawnLocation());
                case COMBO -> tickCombo(npc, entity);
                default -> {}
            }
        }
    }

    private void tickPatrol(ForjaNpc npc, LivingEntity entity) {
        Location target = npc.getCurrentWaypoint();
        if (target == null) return;
        if (moveToward(entity, target, npc.getMoveSpeed())) {
            npc.advanceWaypoint();
        }
    }

    private void tickCombo(ForjaNpc npc, LivingEntity entity) {
        if (npc.getWaypoints().isEmpty()) {
            // sin waypoints, comportate como wander puro alrededor del spawn
            tickWander(npc, entity, npc.getSpawnLocation());
            return;
        }
        int id = npc.getId();
        Integer counter = idleCounter.get(id);
        if (counter != null) {
            // estamos en fase de "wander corto" en el waypoint actual
            Location wt = wanderTarget.get(id);
            if (wt == null || moveToward(entity, wt, npc.getMoveSpeed())) {
                wanderTarget.remove(id);
            }
            counter--;
            if (counter <= 0) {
                idleCounter.remove(id);
                wanderTarget.remove(id);
                npc.advanceWaypoint();
            } else {
                idleCounter.put(id, counter);
                if (wanderTarget.get(id) == null && random.nextInt(20) == 0) {
                    wanderTarget.put(id, randomPointNear(npc.getCurrentWaypoint() != null
                            ? npc.getCurrentWaypoint() : entity.getLocation(), 2.5));
                }
            }
            return;
        }

        Location target = npc.getCurrentWaypoint();
        if (target != null && moveToward(entity, target, npc.getMoveSpeed())) {
            // llegó al waypoint: entra en fase de idle/wander corto antes de seguir
            idleCounter.put(id, npc.getIdleTicksAtPoint());
        }
    }

    private void tickWander(ForjaNpc npc, LivingEntity entity, Location origin) {
        int id = npc.getId();
        Location target = wanderTarget.get(id);
        if (target == null) {
            target = randomPointNear(origin, npc.getWanderRadius());
            wanderTarget.put(id, target);
        }
        if (moveToward(entity, target, npc.getMoveSpeed())) {
            wanderTarget.remove(id);
            // pequeña pausa antes del próximo punto: lo simulamos no recalculando
            // hasta el próximo tick, se ve natural igual porque el tick corre cada 2 ticks.
        }
    }

    private Location randomPointNear(Location origin, double radius) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = random.nextDouble() * radius;
        double dx = Math.cos(angle) * dist;
        double dz = Math.sin(angle) * dist;
        Location loc = origin.clone().add(dx, 0, dz);
        loc.setY(origin.getWorld().getHighestBlockYAt(loc) + 1);
        return loc;
    }

    /**
     * Mueve la entidad un paso hacia el target. Devuelve true si ya llegó (está a menos
     * de 0.5 bloques en XZ).
     */
    private boolean moveToward(LivingEntity entity, Location target, double speed) {
        Location current = entity.getLocation();
        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double distSq = dx * dx + dz * dz;

        if (distSq < 0.25) {
            entity.setVelocity(new Vector(0, entity.getVelocity().getY(), 0));
            return true;
        }

        double dist = Math.sqrt(distSq);
        double vx = (dx / dist) * speed;
        double vz = (dz / dist) * speed;

        // Mantiene la Y actual de la física (gravedad) y solo empuja en XZ.
        Vector vel = entity.getVelocity();
        entity.setVelocity(new Vector(vx, vel.getY(), vz));

        // Orienta al NPC hacia donde camina.
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        Location faced = current.clone();
        faced.setYaw(yaw);
        entity.setRotation(yaw, current.getPitch());

        return false;
    }
}
