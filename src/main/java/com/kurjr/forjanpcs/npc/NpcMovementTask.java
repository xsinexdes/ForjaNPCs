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

    // Detección de "atascado contra un bloque" para saltar, por entidad (UUID)
    private final Map<java.util.UUID, Location> lastPosition = new HashMap<>();
    private final Map<java.util.UUID, Integer> stuckTicks = new HashMap<>();
    private static final double JUMP_VELOCITY = 0.42; // misma fuerza de salto que un jugador vanilla
    private static final int STUCK_TICKS_BEFORE_JUMP = 3;

    public NpcMovementTask(ForjaNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (ForjaNpc npc : plugin.getNpcManager().all()) {
            LivingEntity entity = npc.resolveEntity();
            if (entity == null || !entity.isValid()) continue;

            switch (npc.getMode()) {
                case PATROL -> tickPatrol(npc, entity);
                case WANDER -> tickWander(npc, entity, npc.getSpawnLocation());
                case COMBO -> tickCombo(npc, entity);
                default -> {}
            }

            if (npc.isLookAtPlayers()) {
                tickLookAtPlayer(npc, entity);
            }
        }
    }

    /**
     * Si el NPC no se está moviendo activamente este tick (velocidad horizontal casi nula),
     * gira para mirar al jugador vivo más cercano dentro de lookRadius. No pisa el giro del
     * movimiento porque solo actúa cuando el NPC está quieto.
     */
    private void tickLookAtPlayer(ForjaNpc npc, LivingEntity entity) {
        Vector vel = entity.getVelocity();
        double horizontalSpeedSq = vel.getX() * vel.getX() + vel.getZ() * vel.getZ();
        if (horizontalSpeedSq > 0.003) return; // se está moviendo, no le tocamos la mirada

        org.bukkit.entity.Player nearest = null;
        double nearestDistSq = npc.getLookRadius() * npc.getLookRadius();
        for (org.bukkit.entity.Player p : entity.getWorld().getPlayers()) {
            if (!p.isValid() || p.isDead()) continue;
            double dSq = p.getLocation().distanceSquared(entity.getLocation());
            if (dSq < nearestDistSq) {
                nearestDistSq = dSq;
                nearest = p;
            }
        }
        if (nearest == null) return;

        Location from = entity.getLocation();
        Location to = nearest.getEyeLocation();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dy = to.getY() - (from.getY() + entity.getEyeHeight());
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizDist));
        entity.setRotation(yaw, pitch);
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
        entity.setRotation(yaw, current.getPitch());

        checkStuckAndJump(entity, speed);

        return false;
    }

    /**
     * Si la entidad tiene velocidad hacia adelante pero hace varios ticks que no avanza de
     * verdad (está pegada contra un bloque de 1 de alto), le da un salto vanilla. Sin esto,
     * un NPC caminando por movimiento puro de velocidad se queda trabado contra cualquier
     * escalón sin poder subirlo.
     */
    private void checkStuckAndJump(LivingEntity entity, double speed) {
        java.util.UUID id = entity.getUniqueId();
        Location current = entity.getLocation();
        Location last = lastPosition.get(id);
        lastPosition.put(id, current.clone());

        if (last == null) {
            stuckTicks.put(id, 0);
            return;
        }

        double movedSq = last.distanceSquared(current);
        // si se movió razonablemente en relación a su velocidad esperada, no está atascado
        double expectedMinSq = (speed * 0.3) * (speed * 0.3);

        if (movedSq < expectedMinSq) {
            int ticks = stuckTicks.getOrDefault(id, 0) + 1;
            stuckTicks.put(id, ticks);
            if (ticks >= STUCK_TICKS_BEFORE_JUMP && entity.isOnGround()) {
                Vector vel = entity.getVelocity();
                entity.setVelocity(new Vector(vel.getX(), JUMP_VELOCITY, vel.getZ()));
                stuckTicks.put(id, 0);
            }
        } else {
            stuckTicks.put(id, 0);
        }
    }
}
