package ua.anatoliy.anarhystones;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AsTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList("help", "get", "toggle", "info", "add", "remove", "addowner", "removeowner", "count", "home", "sethome"));
            if (sender.hasPermission("anarhystones.admin")) {
                list.add("reload");
                list.add("debug");
            }
            return StringUtil.copyPartialMatches(args[0], list, new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            List<String> aliases = new ArrayList<>();
            for (BlockSettings s : AnarhyStones.getInstance().getConfigManager().getAllBlocks().values()) {
                aliases.add(s.alias);
            }
            return StringUtil.copyPartialMatches(args[1], aliases, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}