package ua.anatoliy.anarhystones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.LocationFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.stream.Collectors;

public class AsCommand implements CommandExecutor {
    private final AnarhyStones plugin;

    public AsCommand(AnarhyStones plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;
        ConfigManager cm = plugin.getConfigManager();

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(p);
            return true;
        }

        String sub = args[0].toLowerCase();
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()));

        switch (sub) {
            case "get":
                if (args.length < 2) {
                    p.sendMessage(cm.getMsg("prefix") + cm.getMsg("get-usage"));
                } else {
                    handleGet(p, args[1]);
                }
                break;

            case "toggle":
                boolean active = plugin.togglePlacement(p);
                if (active) {
                    p.sendMessage(cm.getMsg("prefix") + cm.getMsg("toggle-on"));
                } else {
                    p.sendMessage(cm.getMsg("prefix") + cm.getMsg("toggle-off"));
                }
                break;

            case "info":
                showInfo(p, rm);
                break;

            case "add":
                if (args.length < 2) {
                    p.sendMessage(cm.getMsg("prefix") + "Usage: /as add <name>");
                } else {
                    modifyMember(p, rm, args[1], true, false);
                }
                break;

            case "remove":
                if (args.length < 2) {
                    p.sendMessage(cm.getMsg("prefix") + "Usage: /as remove <name>");
                } else {
                    modifyMember(p, rm, args[1], false, false);
                }
                break;

            case "addowner":
                if (args.length < 2) {
                    p.sendMessage(cm.getMsg("prefix") + "Usage: /as addowner <name>");
                } else {
                    modifyMember(p, rm, args[1], true, true);
                }
                break;

            case "removeowner":
                if (args.length < 2) {
                    p.sendMessage(cm.getMsg("prefix") + "Usage: /as removeowner <name>");
                } else {
                    modifyMember(p, rm, args[1], false, true);
                }
                break;

            case "count":
                showCount(p);
                break;

            case "sethome":
                handleSetHome(p, rm);
                break;

            case "home":
                handleHome(p, rm);
                break;

            case "reload":
                if (p.hasPermission(cm.getMainConfig().permAdmin)) {
                    cm.loadConfigs();
                    p.sendMessage(cm.getMsg("prefix") + cm.getMsg("reload-success"));
                } else {
                    p.sendMessage(cm.getMsg("prefix") + cm.getMsg("no-permission"));
                }
                break;

            case "debug":
                if (p.hasPermission(cm.getMainConfig().permDebug) || p.hasPermission(cm.getMainConfig().permAdmin)) {
                    boolean state = plugin.toggleDebug(p);
                    String stateText = state ? "§aON" : "§cOFF";
                    p.sendMessage(cm.getMsg("prefix") + cm.getMsg("debug-toggle").replace("%state%", stateText));
                } else {
                    p.sendMessage(cm.getMsg("prefix") + cm.getMsg("no-permission"));
                }
                break;

            default:
                p.sendMessage(cm.getMsg("prefix") + cm.getMsg("unknown-command"));
                break;
        }
        return true;
    }

    private void showHelp(Player p) {
        p.sendMessage("");
        p.sendMessage("§b§lAnarhyStones §8| §fHelp Menu");
        p.sendMessage(" §b● §f/as get <alias> §8— §7Get protection block");
        p.sendMessage(" §b● §f/as toggle §8— §7Enable/Disable region creation");
        p.sendMessage(" §b● §f/as info §8— §7Region information");
        p.sendMessage(" §b● §f/as home §8— §7Teleport to region home");
        p.sendMessage(" §b● §f/as add/remove <name> §8— §7Manage members");
        p.sendMessage(" §b● §f/as addowner/removeowner <name> §8— §7Manage owners");
        p.sendMessage(" §b● §f/as sethome §8— §7Set home location");
        p.sendMessage(" §b● §f/as count §8— §7Your regions list");

        if (p.hasPermission(plugin.getConfigManager().getMainConfig().permAdmin)) {
            p.sendMessage(" §c● §f/as reload §8— §7Reload config");
            p.sendMessage(" §c● §f/as debug §8— §7Toggle your personal debug");
        }
        p.sendMessage("");
    }

    private void handleGet(Player p, String alias) {
        ConfigManager cm = plugin.getConfigManager();

        BlockSettings target = null;
        for (BlockSettings s : cm.getAllBlocks().values()) {
            if (s.alias.equalsIgnoreCase(alias)) {
                target = s;
                break;
            }
        }

        if (target == null) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("block-not-found").replace("%alias%", alias));
            return;
        }

        ItemStack item = cm.createProtectionItem(target);
        HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(item);

        if (!leftover.isEmpty() && cm.getMainConfig().dropItemWhenInventoryFull) {
            p.getWorld().dropItemNaturally(p.getLocation(), leftover.get(0));
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("inventory-full"));
        } else {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("get-success"));
        }
    }

    private void showInfo(Player p, RegionManager rm) {
        ConfigManager cm = plugin.getConfigManager();
        ProtectedRegion r = getReg(p, rm);
        if (r == null) return;

        String owners = r.getOwners().getUniqueIds().stream().map(u -> Bukkit.getOfflinePlayer(u).getName()).collect(Collectors.joining(", "));
        String members = r.getMembers().getUniqueIds().stream().map(u -> Bukkit.getOfflinePlayer(u).getName()).collect(Collectors.joining(", "));
        int rad = (r.getMaximumPoint().getBlockX() - r.getMinimumPoint().getBlockX() + 1) / 2;

        p.sendMessage(cm.getMsg("region-info-header"));
        p.sendMessage(cm.getMsg("region-info-id").replace("%id%", r.getId()));

        if (owners.isEmpty()) {
            p.sendMessage(cm.getMsg("region-info-owners").replace("%owners%", "None"));
        } else {
            p.sendMessage(cm.getMsg("region-info-owners").replace("%owners%", owners));
        }

        if (members.isEmpty()) {
            p.sendMessage(cm.getMsg("region-info-members").replace("%members%", "None"));
        } else {
            p.sendMessage(cm.getMsg("region-info-members").replace("%members%", members));
        }

        p.sendMessage(cm.getMsg("region-info-priority").replace("%priority%", String.valueOf(r.getPriority())));
        p.sendMessage(cm.getMsg("region-info-radius").replace("%radius%", rad + "x" + rad));
        p.sendMessage(cm.getMsg("region-info-footer"));
    }

    private void modifyMember(Player p, RegionManager rm, String name, boolean add, boolean isOwner) {
        ConfigManager cm = plugin.getConfigManager();
        ProtectedRegion r = getReg(p, rm);
        if (r == null) return;

        if (!r.getOwners().contains(p.getUniqueId()) && !p.hasPermission(cm.getMainConfig().permAdmin)) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("not-owner"));
            return;
        }

        OfflinePlayer t = Bukkit.getOfflinePlayer(name);
        if (isOwner) {
            if (add) {
                r.getOwners().addPlayer(t.getUniqueId());
            } else {
                r.getOwners().removePlayer(t.getUniqueId());
            }
        } else {
            if (add) {
                r.getMembers().addPlayer(t.getUniqueId());
            } else {
                r.getMembers().removePlayer(t.getUniqueId());
            }
        }

        try {
            rm.saveChanges();
        } catch (Exception ignored) {}

        String msgKey;
        if (isOwner) {
            msgKey = add ? "owner-added" : "owner-removed";
        } else {
            msgKey = add ? "member-added" : "member-removed";
        }

        String actionText = cm.getMsg(msgKey);
        p.sendMessage(cm.getMsg("prefix") + cm.getMsg("member-modified").replace("%player%", name).replace("%action%", actionText));
    }

    private void showCount(Player p) {
        ConfigManager cm = plugin.getConfigManager();
        int total = 0;
        p.sendMessage(cm.getMsg("region-count-header"));

        for (World world : Bukkit.getWorlds()) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            if (rm == null) continue;

            for (ProtectedRegion r : rm.getRegions().values()) {
                if (r.getOwners().contains(p.getUniqueId())) {
                    total++;
                    BlockVector3 c = r.getMaximumPoint().add(r.getMinimumPoint()).divide(2);
                    String msg = cm.getMsg("region-count-entry")
                            .replace("%id%", r.getId())
                            .replace("%x%", String.valueOf(c.getX()))
                            .replace("%y%", String.valueOf(c.getY()))
                            .replace("%z%", String.valueOf(c.getZ()));
                    p.sendMessage(msg);
                }
            }
        }
        p.sendMessage(cm.getMsg("region-count-footer").replace("%total%", String.valueOf(total)));
    }

    private void handleSetHome(Player p, RegionManager rm) {
        ConfigManager cm = plugin.getConfigManager();
        ProtectedRegion r = getReg(p, rm);
        if (r == null) return;

        if (!r.getOwners().contains(p.getUniqueId())) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("not-owner"));
            return;
        }

        Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get("teleport");
        if (flag instanceof LocationFlag) {
            r.setFlag((LocationFlag) flag, BukkitAdapter.adapt(p.getLocation()));
            try {
                rm.saveChanges();
            } catch (Exception ignored) {}
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("sethome-success"));
        }
    }

    private void handleHome(Player p, RegionManager rm) {
        ConfigManager cm = plugin.getConfigManager();
        ProtectedRegion r = getReg(p, rm);
        if (r == null) return;

        if (!r.getOwners().contains(p.getUniqueId()) && !r.getMembers().contains(p.getUniqueId())) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("no-region-access"));
            return;
        }

        if (r.getMembers().contains(p.getUniqueId()) && !cm.getMainConfig().allowHomeTeleportForMembers) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("member-tp-disabled"));
            return;
        }

        // ВИПРАВЛЕННЯ: Тепер змінна s є final, як вимагає Java для лямбда-виразів
        final BlockSettings s = cm.getAllBlocks().isEmpty() ? null : cm.getAllBlocks().values().iterator().next();

        Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get("teleport");
        com.sk89q.worldedit.util.Location weLoc = null;
        if (flag instanceof LocationFlag) {
            weLoc = r.getFlag((LocationFlag) flag);
        }

        // ВИПРАВЛЕННЯ: Тепер Location finalLoc є final
        final Location targetLoc;
        if (weLoc != null) {
            targetLoc = BukkitAdapter.adapt(weLoc);
        } else {
            double offX = s != null ? s.homeXOffset : 0.5;
            double offY = s != null ? s.homeYOffset : 1.0;
            double offZ = s != null ? s.homeZOffset : 0.5;

            BlockVector3 maxP = r.getMaximumPoint();
            int hX = maxP.getBlockX();
            int hZ = maxP.getZ();
            int hY = p.getWorld().getHighestBlockYAt(hX, hZ);

            targetLoc = new Location(p.getWorld(), hX + offX, hY + offY, hZ + offZ);
        }

        if (s != null && s.tpWaitingSeconds > 0) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("tp-waiting").replace("%time%", String.valueOf(s.tpWaitingSeconds)));

            // ВИПРАВЛЕННЯ: Змінна start також final
            final Location start = p.getLocation().clone();

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (s.noMovingWhenTpWaiting && start.distanceSquared(p.getLocation()) > 1.0) {
                    p.sendMessage(cm.getMsg("prefix") + cm.getMsg("tp-cancelled-move"));
                    return;
                }
                p.teleport(targetLoc);
                p.sendMessage(cm.getMsg("prefix") + cm.getMsg("tp-success"));
            }, s.tpWaitingSeconds * 20L);
        } else {
            p.teleport(targetLoc);
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("tp-success"));
        }
    }

    private ProtectedRegion getReg(Player p, RegionManager rm) {
        if (rm == null) return null;

        BlockVector3 pt = BlockVector3.at(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        for (ProtectedRegion r : rm.getApplicableRegions(pt)) {
            if (r.getId().startsWith("as_")) {
                return r;
            }
        }

        p.sendMessage(plugin.getConfigManager().getMsg("prefix") + plugin.getConfigManager().getMsg("not-in-region"));
        return null;
    }
}