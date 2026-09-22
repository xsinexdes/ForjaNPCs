package com.kurjr.forjanpcs.npc;

import com.kurjr.forjanpcs.ForjaNPCsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NpcManager {

    private final ForjaNPCsPlugin plugin;
    private final Map<Integer, ForjaNpc> npcs = new LinkedHashMap<>();
    private int nextId = 1;
    private File storageFile;

    public NpcManager(ForjaNPCsPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "npcs.yml");
    }

    public ForjaNpc create(EntityType type, String name, Location loc) {
        int id = nextId++;
        ForjaNpc npc = new ForjaNpc(id, type, name, loc.clone());
        npcs.put(id, npc);
        spawn(npc);
        save();
        return npc;
    }

    public boolean remove(int id) {
        ForjaNpc npc = npcs.remove(id);
        if (npc == null) return false;
        LivingEntity entity = npc.resolveEntity();
        if (entity != null) {
            entity.remove();
        }
        save();
        return true;
    }

    public ForjaNpc get(int id) {
        return npcs.get(id);
    }

    public Collection<ForjaNpc> all() {
        return npcs.values();
    }

    public void spawn(ForjaNpc npc) {
        Location loc = npc.getSpawnLocation();
        LivingEntity entity = (LivingEntity) loc.getWorld().spawnEntity(loc, npc.getEntityType());
        entity.setCustomName(npc.getDisplayName());
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        entity.setPersistent(true);
        entity.setAI(false); // el movimiento lo maneja ForjaNPCs, no la IA vanilla
        entity.setInvulnerable(true);
        entity.setSilent(false);
        try {
            entity.setCollidable(false);
        } catch (Throwable ignored) {
            // por si alguna versión no expone el método
        }
        npc.setEntityUuid(entity.getUniqueId());
        plugin.getEntityIndex().put(entity.getUniqueId(), npc.getId());
    }

    public void spawnAll() {
        for (ForjaNpc npc : npcs.values()) {
            if (npc.resolveEntity() == null) {
                spawn(npc);
            }
        }
    }

    public void despawnAll() {
        for (ForjaNpc npc : npcs.values()) {
            LivingEntity e = npc.resolveEntity();
            if (e != null) e.remove();
        }
    }

    // ---------- Persistencia simple en YAML ----------

    public void save() {
        FileConfiguration cfg = new YamlConfiguration();
        for (ForjaNpc npc : npcs.values()) {
            String base = "npcs." + npc.getId() + ".";
            cfg.set(base + "type", npc.getEntityType().name());
            cfg.set(base + "name", npc.getDisplayName());
            cfg.set(base + "mode", npc.getMode().name());
            cfg.set(base + "wanderRadius", npc.getWanderRadius());
            cfg.set(base + "moveSpeed", npc.getMoveSpeed());
            cfg.set(base + "idleTicks", npc.getIdleTicksAtPoint());
            cfg.set(base + "conversation", npc.getBetonQuestConversation());
            cfg.set(base + "spawn", npc.getSpawnLocation());
            List<Location> wps = npc.getWaypoints();
            for (int i = 0; i < wps.size(); i++) {
                cfg.set(base + "waypoints." + i, wps.get(i));
            }
        }
        cfg.set("nextId", nextId);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            cfg.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("No se pudo guardar npcs.yml: " + e.getMessage());
        }
    }

    public void load() {
        if (!storageFile.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(storageFile);
        nextId = cfg.getInt("nextId", 1);
        if (cfg.getConfigurationSection("npcs") == null) return;
        for (String key : cfg.getConfigurationSection("npcs").getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String base = "npcs." + key + ".";
                EntityType type = EntityType.valueOf(cfg.getString(base + "type"));
                String name = cfg.getString(base + "name");
                Location spawn = cfg.getLocation(base + "spawn");
                ForjaNpc npc = new ForjaNpc(id, type, name, spawn);
                npc.setMode(MovementMode.valueOf(cfg.getString(base + "mode", "NONE")));
                npc.setWanderRadius(cfg.getDouble(base + "wanderRadius", 5.0));
                npc.setMoveSpeed(cfg.getDouble(base + "moveSpeed", 0.18));
                npc.setIdleTicksAtPoint(cfg.getInt(base + "idleTicks", 60));
                npc.setBetonQuestConversation(cfg.getString(base + "conversation", null));
                if (cfg.getConfigurationSection(base + "waypoints") != null) {
                    for (String wKey : cfg.getConfigurationSection(base + "waypoints").getKeys(false)) {
                        npc.addWaypoint(cfg.getLocation(base + "waypoints." + wKey));
                    }
                }
                npcs.put(id, npc);
                nextId = Math.max(nextId, id + 1);
            } catch (Exception ex) {
                plugin.getLogger().warning("Error cargando NPC " + key + ": " + ex.getMessage());
            }
        }
    }
}
