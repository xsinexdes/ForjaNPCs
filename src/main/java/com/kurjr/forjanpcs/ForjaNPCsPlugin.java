package com.kurjr.forjanpcs;

import com.kurjr.forjanpcs.command.ForjaNpcCommand;
import com.kurjr.forjanpcs.integration.BetonQuestBridge;
import com.kurjr.forjanpcs.listener.NpcInteractListener;
import com.kurjr.forjanpcs.npc.NpcManager;
import com.kurjr.forjanpcs.npc.NpcMovementTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ForjaNPCsPlugin extends JavaPlugin {

    private NpcManager npcManager;
    private BetonQuestBridge betonQuestBridge;
    private NpcMovementTask movementTask;

    // Índice rápido: UUID de la entidad de Bukkit -> id del ForjaNpc
    private final Map<UUID, Integer> entityIndex = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.npcManager = new NpcManager(this);
        this.npcManager.load();

        // reconstruye el índice entidad->npc para los NPCs que ya existían antes del reload/restart
        // (se llena de nuevo al hacer spawnAll, porque ahí se crean entidades nuevas)
        this.npcManager.spawnAll();

        this.betonQuestBridge = new BetonQuestBridge(this);

        getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);
        getCommand("forjanpc").setExecutor(new ForjaNpcCommand(this));

        this.movementTask = new NpcMovementTask(this);
        this.movementTask.runTaskTimer(this, 20L, 2L); // arranca después de 1s, corre cada 2 ticks

        getLogger().info("ForjaNPCs habilitado con " + npcManager.all().size() + " NPC(s).");
    }

    @Override
    public void onDisable() {
        if (movementTask != null) movementTask.cancel();
        if (npcManager != null) {
            npcManager.save();
            npcManager.despawnAll();
        }
    }

    public NpcManager getNpcManager() {
        return npcManager;
    }

    public BetonQuestBridge getBetonQuestBridge() {
        return betonQuestBridge;
    }

    public Map<UUID, Integer> getEntityIndex() {
        return entityIndex;
    }
}
