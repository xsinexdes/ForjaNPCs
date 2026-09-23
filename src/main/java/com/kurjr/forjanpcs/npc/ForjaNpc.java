package com.kurjr.forjanpcs.npc;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa un NPC propio de ForjaNPCs.
 * En esta primera fase, el NPC está respaldado por una entidad viva real de Bukkit
 * (Villager, Zombie, etc.) — no es un jugador falso por paquetes (eso es fase 2).
 */
public class ForjaNpc {

    private final int id;
    private final EntityType entityType;
    private String displayName;
    private Location spawnLocation;
    private UUID entityUuid;

    private MovementMode mode = MovementMode.NONE;
    private final List<Location> waypoints = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private double wanderRadius = 5.0;
    private double moveSpeed = 0.18; // bloques por tick aprox.
    private int idleTicksAtPoint = 60; // cu\u00e1nto se queda quieto/wandereando en cada waypoint (modo COMBO)

    private String betonQuestConversation; // id de conversaci\u00f3n a disparar al interactuar

    private boolean lookAtPlayers = true;
    private double lookRadius = 6.0;

    public ForjaNpc(int id, EntityType entityType, String displayName, Location spawnLocation) {
        this.id = id;
        this.entityType = entityType;
        this.displayName = displayName;
        this.spawnLocation = spawnLocation;
    }

    public int getId() {
        return id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public LivingEntity resolveEntity() {
        if (entityUuid == null) return null;
        org.bukkit.entity.Entity e = org.bukkit.Bukkit.getEntity(entityUuid);
        return (e instanceof LivingEntity) ? (LivingEntity) e : null;
    }

    public MovementMode getMode() {
        return mode;
    }

    public void setMode(MovementMode mode) {
        this.mode = mode;
    }

    public List<Location> getWaypoints() {
        return waypoints;
    }

    public void addWaypoint(Location loc) {
        waypoints.add(loc);
    }

    public void clearWaypoints() {
        waypoints.clear();
        currentWaypointIndex = 0;
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    public void advanceWaypoint() {
        if (waypoints.isEmpty()) return;
        currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size();
    }

    public Location getCurrentWaypoint() {
        if (waypoints.isEmpty()) return null;
        return waypoints.get(currentWaypointIndex);
    }

    public double getWanderRadius() {
        return wanderRadius;
    }

    public void setWanderRadius(double wanderRadius) {
        this.wanderRadius = wanderRadius;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(double moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public int getIdleTicksAtPoint() {
        return idleTicksAtPoint;
    }

    public void setIdleTicksAtPoint(int idleTicksAtPoint) {
        this.idleTicksAtPoint = idleTicksAtPoint;
    }

    public String getBetonQuestConversation() {
        return betonQuestConversation;
    }

    public void setBetonQuestConversation(String betonQuestConversation) {
        this.betonQuestConversation = betonQuestConversation;
    }

    public boolean isLookAtPlayers() {
        return lookAtPlayers;
    }

    public void setLookAtPlayers(boolean lookAtPlayers) {
        this.lookAtPlayers = lookAtPlayers;
    }

    public double getLookRadius() {
        return lookRadius;
    }

    public void setLookRadius(double lookRadius) {
        this.lookRadius = lookRadius;
    }
}
