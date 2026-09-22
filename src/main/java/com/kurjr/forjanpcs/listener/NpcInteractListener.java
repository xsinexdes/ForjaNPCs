package com.kurjr.forjanpcs.listener;

import com.kurjr.forjanpcs.ForjaNPCsPlugin;
import com.kurjr.forjanpcs.npc.ForjaNpc;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NpcInteractListener implements Listener {

    private final ForjaNPCsPlugin plugin;

    public NpcInteractListener(ForjaNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        ForjaNpc npc = resolveNpc(event.getRightClicked());
        if (npc == null) return;
        event.setCancelled(true);
        handleClick(event.getPlayer(), npc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Los NPCs son invulnerable=true, pero por las dudas cancelamos cualquier daño igual
        // y evitamos que un "attack" cuente como click normal (evita duplicar conversación).
        ForjaNpc npc = resolveNpc(event.getEntity());
        if (npc != null) {
            event.setCancelled(true);
        }
    }

    private ForjaNpc resolveNpc(Entity entity) {
        Integer id = plugin.getEntityIndex().get(entity.getUniqueId());
        if (id == null) return null;
        return plugin.getNpcManager().get(id);
    }

    private void handleClick(Player player, ForjaNpc npc) {
        if (npc.getBetonQuestConversation() != null) {
            plugin.getBetonQuestBridge().startConversation(player, npc.getBetonQuestConversation());
        }
        // Punto de extensión: acá podés enganchar cualquier otro plugin de quests
        // (o tu propia lógica) simplemente agregando más ramas como esta.
    }
}
