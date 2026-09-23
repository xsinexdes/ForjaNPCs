package com.kurjr.forjanpcs.command;

import com.kurjr.forjanpcs.ForjaNPCsPlugin;
import com.kurjr.forjanpcs.npc.ForjaNpc;
import com.kurjr.forjanpcs.npc.MovementMode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ForjaNpcCommand implements CommandExecutor {

    private final ForjaNPCsPlugin plugin;
    // selección simple por jugador (para saber a qué NPC le aplico waypoint/mode/etc)
    private final Map<java.util.UUID, Integer> selected = new HashMap<>();

    public ForjaNpcCommand(ForjaNPCsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando es solo para jugadores.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usá /forjanpc create|remove|list|select|waypoint|mode|conversation");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> create(player, args);
            case "remove" -> remove(player, args);
            case "list" -> list(player);
            case "select" -> select(player, args);
            case "waypoint" -> waypoint(player, args);
            case "mode" -> mode(player, args);
            case "conversation" -> conversation(player, args);
            case "look" -> look(player, args);
            default -> sender.sendMessage(ChatColor.RED + "Subcomando desconocido.");
        }
        return true;
    }

    private void create(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Uso: /forjanpc create <tipo_entidad> <nombre...>");
            return;
        }
        EntityType type;
        try {
            type = EntityType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Tipo de entidad inválido: " + args[1]);
            return;
        }
        String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        ForjaNpc npc = plugin.getNpcManager().create(type, ChatColor.translateAlternateColorCodes('&', name), player.getLocation());
        selected.put(player.getUniqueId(), npc.getId());
        player.sendMessage(ChatColor.GREEN + "NPC #" + npc.getId() + " creado y seleccionado.");
    }

    private void remove(Player player, String[] args) {
        Integer id = parseIdOrSelected(player, args, 1);
        if (id == null) return;
        boolean ok = plugin.getNpcManager().remove(id);
        player.sendMessage(ok ? ChatColor.GREEN + "NPC #" + id + " eliminado."
                : ChatColor.RED + "No existe ese NPC.");
    }

    private void list(Player player) {
        if (plugin.getNpcManager().all().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No hay NPCs creados todavía.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "=== NPCs de ForjaNPCs ===");
        for (ForjaNpc npc : plugin.getNpcManager().all()) {
            player.sendMessage(ChatColor.GRAY + "#" + npc.getId() + " " + ChatColor.WHITE + npc.getDisplayName()
                    + ChatColor.GRAY + " (" + npc.getEntityType() + ", modo " + npc.getMode() + ")");
        }
    }

    private void select(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /forjanpc select <id>");
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            if (plugin.getNpcManager().get(id) == null) {
                player.sendMessage(ChatColor.RED + "No existe ese NPC.");
                return;
            }
            selected.put(player.getUniqueId(), id);
            player.sendMessage(ChatColor.GREEN + "NPC #" + id + " seleccionado.");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "ID inválido.");
        }
    }

    private void waypoint(Player player, String[] args) {
        ForjaNpc npc = getSelectedNpc(player);
        if (npc == null) return;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /forjanpc waypoint add|clear");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "add" -> {
                npc.addWaypoint(player.getLocation());
                plugin.getNpcManager().save();
                player.sendMessage(ChatColor.GREEN + "Waypoint agregado a NPC #" + npc.getId()
                        + " (" + npc.getWaypoints().size() + " en total).");
            }
            case "clear" -> {
                npc.clearWaypoints();
                plugin.getNpcManager().save();
                player.sendMessage(ChatColor.GREEN + "Waypoints limpiados.");
            }
            default -> player.sendMessage(ChatColor.RED + "Uso: /forjanpc waypoint add|clear");
        }
    }

    private void mode(Player player, String[] args) {
        ForjaNpc npc = getSelectedNpc(player);
        if (npc == null) return;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /forjanpc mode <none|patrol|wander|combo>");
            return;
        }
        try {
            MovementMode m = MovementMode.valueOf(args[1].toUpperCase());
            npc.setMode(m);
            plugin.getNpcManager().save();
            player.sendMessage(ChatColor.GREEN + "Modo de NPC #" + npc.getId() + " -> " + m);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Modo inválido. Usá: none, patrol, wander o combo.");
        }
    }

    private void conversation(Player player, String[] args) {
        ForjaNpc npc = getSelectedNpc(player);
        if (npc == null) return;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /forjanpc conversation <id_conversacion_betonquest>");
            return;
        }
        npc.setBetonQuestConversation(args[1]);
        plugin.getNpcManager().save();
        player.sendMessage(ChatColor.GREEN + "NPC #" + npc.getId() + " ahora dispara la conversación '" + args[1] + "'.");
    }

    private void look(Player player, String[] args) {
        ForjaNpc npc = getSelectedNpc(player);
        if (npc == null) return;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /forjanpc look <on|off> [radio_en_bloques]");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "on" -> {
                npc.setLookAtPlayers(true);
                if (args.length >= 3) {
                    try {
                        npc.setLookRadius(Double.parseDouble(args[2]));
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Radio inválido, dejé el que tenía.");
                    }
                }
                plugin.getNpcManager().save();
                player.sendMessage(ChatColor.GREEN + "NPC #" + npc.getId() + " ahora mira a los jugadores cercanos (radio "
                        + npc.getLookRadius() + ").");
            }
            case "off" -> {
                npc.setLookAtPlayers(false);
                plugin.getNpcManager().save();
                player.sendMessage(ChatColor.GREEN + "NPC #" + npc.getId() + " ya no sigue con la mirada.");
            }
            default -> player.sendMessage(ChatColor.RED + "Uso: /forjanpc look <on|off> [radio_en_bloques]");
        }
    }

    private Integer parseIdOrSelected(Player player, String[] args, int index) {
        if (args.length > index) {
            try {
                return Integer.parseInt(args[index]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "ID inválido.");
                return null;
            }
        }
        Integer sel = selected.get(player.getUniqueId());
        if (sel == null) {
            player.sendMessage(ChatColor.RED + "No tenés ningún NPC seleccionado. Usá /forjanpc select <id>.");
        }
        return sel;
    }

    private ForjaNpc getSelectedNpc(Player player) {
        Integer id = selected.get(player.getUniqueId());
        if (id == null) {
            player.sendMessage(ChatColor.RED + "No tenés ningún NPC seleccionado. Usá /forjanpc select <id>.");
            return null;
        }
        ForjaNpc npc = plugin.getNpcManager().get(id);
        if (npc == null) {
            player.sendMessage(ChatColor.RED + "El NPC seleccionado ya no existe.");
        }
        return npc;
    }
}
