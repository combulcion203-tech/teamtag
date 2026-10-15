
package com.tuservidor.teamtags;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Locale;
import java.util.UUID;

public final class TeamTagsPlugin extends JavaPlugin implements Listener {

    private Scoreboard scoreboard;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("teams") != null) {
            getCommand("teams").setExecutor(this);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayer(player);
        }

        getLogger().info("TeamTags activado.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyPlayer(event.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String path = "players." + player.getUniqueId();

        String role = getConfig().getString(path + ".role", "");
        int number = getConfig().getInt(path + ".team", 0);

        Tag tag = getTag(role, number);

        event.renderer((source, displayName, message, viewer) ->
            Component.text(tag.label + " ", tag.color)
                .append(Component.text(source.getName(), tag.color))
                .append(Component.text(" » ", NamedTextColor.DARK_GRAY))
                .append(message.colorIfAbsent(NamedTextColor.WHITE))
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission("teamtags.admin")) {
            sender.sendMessage(Component.text(
                "No tienes permiso.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text(
                "/teams lider <jugador> <1-5>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text(
                "/teams miembro <jugador> <1-5>", NamedTextColor.GREEN));
            sender.sendMessage(Component.text(
                "/teams recompensa <jugador>", NamedTextColor.RED));
            sender.sendMessage(Component.text(
                "/teams quitarrecompensa <jugador>", NamedTextColor.RED));
            sender.sendMessage(Component.text(
                "/teams quitar <jugador>", NamedTextColor.GRAY));
            sender.sendMessage(Component.text(
                "/teams info <jugador>", NamedTextColor.AQUA));
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        if (action.equals("lider") || action.equals("miembro")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text(
                    "Uso: /teams " + action + " <jugador> <1-5>",
                    NamedTextColor.RED));
                return true;
            }

            int number;
            try {
                number = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                number = 0;
            }

            if (number < 1 || number > 5) {
                sender.sendMessage(Component.text(
                    "El equipo debe ser del 1 al 5.",
                    NamedTextColor.RED));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            UUID uuid = target.getUniqueId();
            String path = "players." + uuid;

            getConfig().set(path + ".name", args[1]);
            getConfig().set(path + ".role", action);
            getConfig().set(path + ".team", number);
            saveConfig();

            if (target.isOnline()) {
                applyPlayer(target.getPlayer());
            }

            sender.sendMessage(Component.text(
                args[1] + " asignado a " + action
                    + " del equipo " + number + ".",
                NamedTextColor.GREEN));
            return true;
        }

        if (action.equals("recompensa")
                || action.equals("quitarrecompensa")
                || action.equals("quitar")) {

            if (args.length < 2) {
                sender.sendMessage(Component.text(
                    "Indica el nombre del jugador.",
                    NamedTextColor.RED));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            String path = "players." + target.getUniqueId();

            if (action.equals("recompensa")) {
                getConfig().set(path + ".name", args[1]);
                getConfig().set(path + ".role", "recompensa");
                saveConfig();

                sender.sendMessage(Component.text(
                    "Rol RECOMPENSA asignado a " + args[1] + ".",
                    NamedTextColor.RED));

            } else if (action.equals("quitarrecompensa")) {
                if (!getConfig().getString(path + ".role", "")
                        .equals("recompensa")) {
                    sender.sendMessage(Component.text(
                        "Ese jugador no tiene el rol RECOMPENSA.",
                        NamedTextColor.YELLOW));
                    return true;
                }

                int team = getConfig().getInt(path + ".team", 0);

                if (team >= 1 && team <= 5) {
                    getConfig().set(path + ".role", "miembro");
                } else {
                    getConfig().set(path, null);
                }

                saveConfig();
                sender.sendMessage(Component.text(
                    "Recompensa retirada a " + args[1] + ".",
                    NamedTextColor.GREEN));

            } else {
                getConfig().set(path, null);
                saveConfig();
                sender.sendMessage(Component.text(
                    "Etiqueta retirada a " + args[1] + ".",
                    NamedTextColor.GREEN));
            }

            if (target.isOnline()) {
                applyPlayer(target.getPlayer());
            }
            return true;
        }

        if (action.equals("info") && args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            String path = "players." + target.getUniqueId();

            String role = getConfig().getString(
                path + ".role", "sin asignar");
            int team = getConfig().getInt(path + ".team", 0);

            sender.sendMessage(Component.text(
                args[1] + " | Rol: " + role + " | Equipo: " + team,
                NamedTextColor.AQUA));
            return true;
        }

        sender.sendMessage(Component.text(
            "Comando desconocido. Usa /teams",
            NamedTextColor.RED));
        return true;
    }

    private void applyPlayer(Player player) {
        if (player == null) return;

        String path = "players." + player.getUniqueId();
        String role = getConfig().getString(path + ".role", "");
        int number = getConfig().getInt(path + ".team", 0);

        Tag tag = getTag(role, number);
        String boardName = getBoardName(role, number);

        Team team = scoreboard.getTeam(boardName);

        if (team == null) {
            team = scoreboard.registerNewTeam(boardName);
        }

        // Retira al jugador de los grupos de orden anteriores
        // que pertenecen a este plugin.
        for (Team existing : scoreboard.getTeams()) {
            if (existing.getName().startsWith("ttg")
                    && !existing.getName().equals(boardName)) {
                existing.removeEntry(player.getName());
            }
        }

        team.addEntry(player.getName());
        team.prefix(Component.text(tag.label + " ", tag.color));
        team.color(tag.color);
    }

    private String getBoardName(String role, int number) {
        return switch (role) {
            case "lider" -> "ttg0L" + number;
            case "miembro" -> "ttg1T" + number;
            case "recompensa" -> "ttg2R";
            default -> "ttg9Z";
        };
    }

    private Tag getTag(String role, int number) {
        return switch (role) {
            case "lider" -> new Tag(
                "ldteam" + number, NamedTextColor.YELLOW);
            case "miembro" -> new Tag(
                "team" + number, NamedTextColor.GREEN);
            case "recompensa" -> new Tag(
                "RECOMPENSA", NamedTextColor.RED);
            default -> new Tag(
                "jugador", NamedTextColor.WHITE);
        };
    }

    private record Tag(String label, NamedTextColor color) {}
}
