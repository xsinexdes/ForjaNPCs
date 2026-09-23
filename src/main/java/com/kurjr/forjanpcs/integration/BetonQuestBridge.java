package com.kurjr.forjanpcs.integration;

import com.kurjr.forjanpcs.ForjaNPCsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Puente "best effort" con BetonQuest.
 *
 * IMPORTANTE: BetonQuest no tiene una API pública genérica para que plugins de NPC
 * de terceros se "registren" (solo reconoce nativamente a Citizens, FancyNpcs,
 * ZNPCsPlus y MythicMobs). Por eso, en vez de intentar que BetonQuest "vea" a nuestros
 * NPCs como un tipo soportado, disparamos directamente el comando que arranca una
 * conversación para el jugador que interactuó.
 *
 * El comando exacto puede variar según la versión de BetonQuest instalada — revisá
 * con /q help en tu servidor y ajustá {@link #COMMAND_TEMPLATE} si hace falta.
 */
public class BetonQuestBridge {

    // Ajustable: algunas versiones usan "/q conversation <id>", otras "/questconversation <id>".
    private static final String COMMAND_TEMPLATE = "q conversation %s";

    private final ForjaNPCsPlugin plugin;
    private boolean available;

    public BetonQuestBridge(ForjaNPCsPlugin plugin) {
        this.plugin = plugin;
        Plugin bq = Bukkit.getPluginManager().getPlugin("BetonQuest");
        this.available = bq != null && bq.isEnabled();
        if (available) {
            plugin.getLogger().info("BetonQuest detectado — se activó el puente de conversaciones.");
        } else {
            plugin.getLogger().info("BetonQuest no está instalado — los NPCs funcionan igual, solo sin conversaciones.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Arranca (best-effort) la conversación indicada para el jugador.
     */
    public void startConversation(Player player, String conversationId) {
        if (!available || conversationId == null || conversationId.isBlank()) return;
        String cmd = String.format(COMMAND_TEMPLATE, conversationId);
        boolean ok = player.performCommand(cmd);
        if (!ok) {
            plugin.getLogger().warning("No se pudo ejecutar '/" + cmd + "' para " + player.getName()
                    + ". Revisá el comando real de tu versión de BetonQuest en BetonQuestBridge.COMMAND_TEMPLATE.");
        }
    }
}
