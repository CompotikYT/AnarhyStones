package ua.anatoliy.anarhystones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlockListener implements Listener {
    private final AnarhyStones plugin;
    private final Map<UUID, Long> placeCooldowns = new HashMap<>();

    public BlockListener(AnarhyStones plugin) {
        this.plugin = plugin;
    }

    private void debugMsg(Player p, String msg) {
        if (plugin.isDebugEnabled(p)) {
            p.sendMessage("§e[AS Дебаг] §f" + msg);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ConfigManager cm = plugin.getConfigManager();
        BlockSettings s = cm.getSettings(event.getBlock().getType());

        if (s == null) {
            return;
        }

        Player p = event.getPlayer();

        if (event.isCancelled()) {
            return;
        }

        // --- ПЕРЕВІРКА ТОҐЛА ---
        if (plugin.isPlacementDisabled(p)) {
            debugMsg(p, "Placement is TOGGLED OFF. Block placed as decoration.");
            return;
        }

        // --- 1. ПЕРЕВІРКА ПРАВА НА СТВОРЕННЯ ---
        if (!p.hasPermission(cm.getMainConfig().permPlace)) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("no-place-permission"));
            event.setCancelled(true);
            return;
        }

        // --- 2. КУЛДАУН ---
        int cd = cm.getMainConfig().placingCooldown;
        if (cd > 0 && !p.hasPermission(cm.getMainConfig().permAdmin)) {
            long last = placeCooldowns.getOrDefault(p.getUniqueId(), 0L);
            long passed = (System.currentTimeMillis() - last) / 1000L;
            if (passed < cd) {
                p.sendMessage(cm.getMsg("prefix") + cm.getMsg("cooldown-msg").replace("%time%", String.valueOf(cd - passed)));
                event.setCancelled(true);
                return;
            }
        }

        // --- 3. ПЕРЕВІРКА ЛІМІТІВ З LUCKPERMS ---
        int maxRegions = cm.getMainConfig().defaultMaxRegions;
        String limitNode = cm.getMainConfig().limitPermissionNode;

        for (PermissionAttachmentInfo pai : p.getEffectivePermissions()) {
            if (pai.getPermission().startsWith(limitNode)) {
                try {
                    int val = Integer.parseInt(pai.getPermission().substring(limitNode.length()));
                    if (val > maxRegions) {
                        maxRegions = val;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (p.hasPermission(cm.getMainConfig().permAdmin)) {
            maxRegions = Integer.MAX_VALUE; // Адміни не мають лімітів
        }

        int currentRegions = 0;
        for (World world : Bukkit.getWorlds()) {
            RegionManager checkRm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            if (checkRm != null) {
                for (ProtectedRegion r : checkRm.getRegions().values()) {
                    if (r.getId().startsWith("as_") && r.getOwners().contains(p.getUniqueId())) {
                        currentRegions++;
                    }
                }
            }
        }

        if (currentRegions >= maxRegions) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("limit-reached").replace("%limit%", String.valueOf(maxRegions)));
            event.setCancelled(true);
            return;
        }

        // --- 4. ЛОГІКА RESTRICT OBTAINING ---
        if (s.restrictObtaining) {
            ItemStack hand = event.getItemInHand();
            if (hand == null || !hand.hasItemMeta() || !hand.getItemMeta().hasDisplayName()) {
                debugMsg(p, "§cБлок не має назви. Ставимо як декор.");
                return;
            }

            String exp = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s.displayName));
            String act = ChatColor.stripColor(hand.getItemMeta().getDisplayName());

            if (!act.equals(exp)) {
                debugMsg(p, "§cНазви не збігаються. Ставимо як декор.");
                return;
            }
        }

        // --- 5. ПЕРЕВІРКА СВІТІВ ---
        String worldName = event.getBlock().getWorld().getName();
        boolean inList = s.worlds.contains(worldName);

        if ((s.worldListType.equalsIgnoreCase("blacklist") && inList) ||
                (s.worldListType.equalsIgnoreCase("whitelist") && !inList)) {
            p.sendMessage(cm.getMsg("prefix") + cm.getMsg("restricted-world"));
            event.setCancelled(true);
            return;
        }

        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(event.getBlock().getWorld()));
        if (rm == null) {
            return;
        }

        Location loc = event.getBlock().getLocation();

        int minY = (s.yRadius == -1) ? loc.getWorld().getMinHeight() : Math.max(loc.getWorld().getMinHeight(), loc.getBlockY() - s.yRadius);
        int maxY = (s.yRadius == -1) ? loc.getWorld().getMaxHeight() : Math.min(loc.getWorld().getMaxHeight(), loc.getBlockY() + s.yRadius);

        BlockVector3 min = BlockVector3.at(loc.getBlockX() - s.xRadius, minY, loc.getBlockZ() - s.zRadius);
        BlockVector3 max = BlockVector3.at(loc.getBlockX() + s.xRadius, maxY, loc.getBlockZ() + s.zRadius);

        String id = "as_" + p.getName().toLowerCase() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(id, min, max);
        region.setPriority(s.priority);
        region.getOwners().addPlayer(p.getUniqueId());

        // --- 6. ПЕРЕВІРКА НА ПЕРЕТИН ---
        for (ProtectedRegion r : rm.getApplicableRegions(region)) {
            if (r.getId().equals("__global__")) continue;

            if (!cm.getMainConfig().allowMergingRegions || !s.allowMerging) {
                p.sendMessage(cm.getMsg("prefix") + cm.getMsg("not-in-region"));
                event.setCancelled(true);
                return;
            }
        }

        // --- 7. СТВОРЕННЯ РЕГІОНУ ---
        applyFlagsToRegion(region, s.flags, p);
        rm.addRegion(region);

        try {
            rm.saveChanges();
            if (cd > 0) {
                placeCooldowns.put(p.getUniqueId(), System.currentTimeMillis());
            }
            debugMsg(p, "Регіон успішно збережено в WorldGuard!");
        } catch (Exception e) {
            plugin.getLogger().warning("Помилка збереження " + id);
        }

        loc.getWorld().strikeLightningEffect(loc);
        p.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        if (s.hologramEnabled) {
            createHolo(loc.clone().add(0.5, 1.2, 0.5), p, s);
        }

        p.sendMessage(cm.getMsg("prefix") + cm.getMsg("region-created"));
    }

    private void applyFlagsToRegion(ProtectedRegion region, List<String> flagsList, Player p) {
        if (flagsList == null || flagsList.isEmpty()) return;

        for (String flagString : flagsList) {
            String[] parts = flagString.split(" ", 2);
            if (parts.length < 2) continue;

            String name = parts[0].toLowerCase();
            String val = parts[1].replace("%player%", p.getName());

            Flag<?> flag = WorldGuard.getInstance().getFlagRegistry().get(name);
            if (flag != null) {
                try {
                    if (flag instanceof StateFlag) {
                        region.setFlag((StateFlag) flag, val.equalsIgnoreCase("allow") ? StateFlag.State.ALLOW : StateFlag.State.DENY);
                    } else if (flag instanceof StringFlag) {
                        region.setFlag((StringFlag) flag, ChatColor.translateAlternateColorCodes('&', val));
                    } else if (flag instanceof BooleanFlag) {
                        region.setFlag((BooleanFlag) flag, Boolean.parseBoolean(val));
                    } else if (flag instanceof IntegerFlag) {
                        region.setFlag((IntegerFlag) flag, Integer.parseInt(val));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void createHolo(Location l, Player p, BlockSettings s) {
        String sz = (s.yRadius == -1) ? s.xRadius + "x" + s.zRadius : s.xRadius + "x" + s.yRadius + "x" + s.zRadius;
        spawnLine(l.clone().add(0, 0.3, 0), "§7Radius: §b" + sz);
        spawnLine(l.clone(), "§b§lOwner: §f" + p.getName());
    }

    private void spawnLine(Location l, String text) {
        ArmorStand as = (ArmorStand) l.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setGravity(false);
        as.setCustomName(text);
        as.setCustomNameVisible(true);
        as.setMarker(true);
        as.addScoreboardTag("as_hologram");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ConfigManager cm = plugin.getConfigManager();
        BlockSettings s = cm.getSettings(event.getBlock().getType());

        if (s == null) return;

        if (removeProtection(event.getBlock().getLocation(), event.getPlayer())) {
            event.setDropItems(false);
            if (!s.noDrop) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), cm.createProtectionItem(s));
            }
        }
    }

    private boolean removeProtection(Location l, Player p) {
        RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(l.getWorld()));
        if (rm == null) return false;

        for (ProtectedRegion r : rm.getApplicableRegions(BlockVector3.at(l.getBlockX(), l.getBlockY(), l.getBlockZ()))) {
            String expectedSuffix = "_" + l.getBlockX() + "_" + l.getBlockY() + "_" + l.getBlockZ();

            if (r.getId().startsWith("as_") && r.getId().endsWith(expectedSuffix)) {
                if (p != null && !r.getOwners().contains(p.getUniqueId()) && !p.hasPermission(plugin.getConfigManager().getMainConfig().permAdmin)) {
                    p.sendMessage(plugin.getConfigManager().getMsg("prefix") + plugin.getConfigManager().getMsg("not-owner"));
                    return false;
                }

                rm.removeRegion(r.getId());

                try {
                    rm.saveChanges();
                } catch (Exception ignored) {}

                l.getWorld().getNearbyEntities(l.add(0.5, 1.5, 0.5), 1, 3, 1).stream()
                        .filter(e -> e.getScoreboardTags().contains("as_hologram"))
                        .forEach(Entity::remove);

                if (p != null) {
                    p.sendMessage(plugin.getConfigManager().getMsg("prefix") + plugin.getConfigManager().getMsg("region-removed"));
                }
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (isProtectionBlock(b)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (isProtectionBlock(b)) {
                e.setCancelled(true);
            }
        }
    }

    private boolean isProtectionBlock(Block b) {
        BlockSettings s = plugin.getConfigManager().getSettings(b.getType());
        if (s != null && s.preventPistonPush) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(b.getWorld()));
            if (rm == null) return false;

            for (ProtectedRegion r : rm.getApplicableRegions(BlockVector3.at(b.getX(), b.getY(), b.getZ()))) {
                if (r.getId().startsWith("as_")) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        handleExplosion(e.blockList());
    }

    @EventHandler
    public void onBlockEx(BlockExplodeEvent e) {
        handleExplosion(e.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        Iterator<Block> it = blocks.iterator();
        while (it.hasNext()) {
            Block b = it.next();
            BlockSettings s = plugin.getConfigManager().getSettings(b.getType());

            if (s != null) {
                RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(b.getWorld()));
                if (rm == null) continue;

                for (ProtectedRegion r : rm.getApplicableRegions(BlockVector3.at(b.getX(), b.getY(), b.getZ()))) {
                    if (r.getId().startsWith("as_")) {
                        if (s.preventExplode && !s.destroyRegionWhenExplode) {
                            it.remove();
                        } else if (s.destroyRegionWhenExplode) {
                            rm.removeRegion(r.getId());
                            try {
                                rm.saveChanges();
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        for (ItemStack item : e.getInventory().getMatrix()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                BlockSettings s = plugin.getConfigManager().getSettings(item.getType());
                if (s != null && !s.allowUseInCrafting) {
                    String clean = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s.displayName));
                    if (ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(clean)) {
                        e.getInventory().setResult(new ItemStack(Material.AIR));
                        return;
                    }
                }
            }
        }
    }
}