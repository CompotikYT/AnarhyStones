package ua.anatoliy.anarhystones;

import com.moandjiezana.toml.Toml;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigManager {
    private final AnarhyStones plugin;
    private final Map<Material, BlockSettings> blocks = new HashMap<>();
    private FileConfiguration langConfig;
    private MainConfig mainConfig;

    public ConfigManager(AnarhyStones plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        blocks.clear();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        File configFile = new File(plugin.getDataFolder(), "config.toml");
        if (!configFile.exists()) {
            plugin.saveResource("config.toml", false);
        }

        loadMainConfig(configFile);
        loadLanguage();

        File blocksFolder = new File(plugin.getDataFolder(), "blocks");
        if (!blocksFolder.exists()) {
            blocksFolder.mkdir();
            plugin.saveResource("blocks/block1.toml", false);
        }

        File[] files = blocksFolder.listFiles((dir, name) -> name.endsWith(".toml"));
        if (files != null) {
            for (File file : files) {
                try {
                    Toml toml = new Toml().read(file);
                    BlockSettings s = new BlockSettings();

                    s.type = Material.valueOf(toml.getString("type").toUpperCase());
                    s.alias = toml.getString("alias");

                    Toml holo = toml.getTable("hologram");
                    if (holo != null) {
                        s.hologramEnabled = holo.getBoolean("enabled", true);
                    } else {
                        s.hologramEnabled = true;
                    }

                    s.restrictObtaining = getFallbackBoolean(toml, holo, "restrict_obtaining", true);
                    s.worldListType = getFallbackString(toml, holo, "world_list_type", "blacklist");
                    s.worlds = getFallbackList(toml, holo, "worlds");

                    Toml reg = toml.getTable("region");
                    if (reg != null) {
                        s.xRadius = reg.getLong("x_radius", 64L).intValue();
                        s.yRadius = reg.getLong("y_radius", -1L).intValue();
                        s.zRadius = reg.getLong("z_radius", 64L).intValue();
                        s.priority = reg.getLong("priority", 0L).intValue();
                        s.allowMerging = reg.getBoolean("allow_merging", true);
                        s.flags = reg.getList("flags", new ArrayList<>());

                        s.homeXOffset = reg.getDouble("home_x_offset", 0.5);
                        s.homeYOffset = reg.getDouble("home_y_offset", 1.0);
                        s.homeZOffset = reg.getDouble("home_z_offset", 0.5);
                    } else {
                        s.xRadius = 64;
                        s.yRadius = -1;
                        s.zRadius = 64;
                        s.priority = 0;
                        s.allowMerging = true;
                        s.flags = new ArrayList<>();
                        s.homeXOffset = 0.5;
                        s.homeYOffset = 1.0;
                        s.homeZOffset = 0.5;
                    }

                    Toml data = toml.getTable("block_data");
                    if (data != null) {
                        s.displayName = data.getString("display_name", "&bБлок Захисту");
                        s.lore = data.getList("lore", new ArrayList<>());
                    } else {
                        s.displayName = "&bБлок Захисту";
                        s.lore = new ArrayList<>();
                    }

                    Toml behaviour = toml.getTable("behaviour");
                    if (behaviour != null) {
                        s.preventPistonPush = behaviour.getBoolean("prevent_piston_push", true);
                        s.preventExplode = behaviour.getBoolean("prevent_explode", true);
                        s.destroyRegionWhenExplode = behaviour.getBoolean("destroy_region_when_explode", false);
                        s.noDrop = behaviour.getBoolean("no_drop", false);
                        s.allowUseInCrafting = behaviour.getBoolean("allow_use_in_crafting", false);
                    } else {
                        s.preventPistonPush = true;
                        s.preventExplode = true;
                        s.destroyRegionWhenExplode = false;
                        s.noDrop = false;
                        s.allowUseInCrafting = false;
                    }

                    Toml playerOpts = toml.getTable("player");
                    if (playerOpts != null) {
                        s.noMovingWhenTpWaiting = playerOpts.getBoolean("no_moving_when_tp_waiting", true);
                        s.tpWaitingSeconds = playerOpts.getLong("tp_waiting_seconds", 0L).intValue();
                    } else {
                        s.noMovingWhenTpWaiting = true;
                        s.tpWaitingSeconds = 0;
                    }

                    blocks.put(s.type, s);
                } catch (Exception e) {
                    Bukkit.getConsoleSender().sendMessage("§c[AnarhyStones] Помилка парсингу файлу " + file.getName());
                    e.printStackTrace();
                }
            }
        }
        Bukkit.getConsoleSender().sendMessage("§b[AnarhyStones] §fКонфігурацію завантажено.");
    }

    private boolean getFallbackBoolean(Toml root, Toml fallback, String key, boolean def) {
        Boolean val = root.getBoolean(key);
        if (val == null && fallback != null) {
            val = fallback.getBoolean(key);
        }
        return val != null ? val : def;
    }

    private String getFallbackString(Toml root, Toml fallback, String key, String def) {
        String val = root.getString(key);
        if (val == null && fallback != null) {
            val = fallback.getString(key);
        }
        return val != null ? val : def;
    }

    private List<String> getFallbackList(Toml root, Toml fallback, String key) {
        List<String> val = root.getList(key);
        if (val == null && fallback != null) {
            val = fallback.getList(key);
        }
        return val != null ? val : new ArrayList<>();
    }

    private void loadLanguage() {
        String lang = mainConfig.language.toLowerCase();
        File langFolder = new File(plugin.getDataFolder(), "lang");

        if (!langFolder.exists()) {
            langFolder.mkdir();
        }

        String[] langs = {"en", "ua", "ru"};
        for (String l : langs) {
            File f = new File(langFolder, l + ".yml");
            if (!f.exists()) {
                plugin.saveResource("lang/" + l + ".yml", false);
            }
        }

        File langFile = new File(langFolder, lang + ".yml");
        if (!langFile.exists()) {
            langFile = new File(langFolder, "en.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getMsg(String path) {
        String msg = langConfig.getString(path, "&cВідсутній переклад: " + path);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void loadMainConfig(File file) {
        Toml toml = new Toml().read(file);
        mainConfig = new MainConfig();

        mainConfig.configVersion = toml.getLong("config_version", 16L).intValue();
        mainConfig.language = toml.getString("language", "EN");
        mainConfig.baseCommand = toml.getString("base_command", "as");
        mainConfig.placingCooldown = toml.getLong("placing_cooldown", -1L).intValue();
        mainConfig.dropItemWhenInventoryFull = toml.getBoolean("drop_item_when_inventory_full", true);
        mainConfig.allowMergingRegions = toml.getBoolean("allow_merging_regions", true);
        mainConfig.defaultProtectionBlockPlacementOff = toml.getBoolean("default_protection_block_placement_off", false);
        mainConfig.allowHomeTeleportForMembers = toml.getBoolean("allow_home_teleport_for_members", true);

        Toml perms = toml.getTable("permissions");
        if (perms != null) {
            mainConfig.permAdmin = perms.getString("admin", "anarhystones.admin");
            mainConfig.permPlace = perms.getString("place", "anarhystones.place");
            mainConfig.permDebug = perms.getString("debug", "anarhystones.debug");
        } else {
            mainConfig.permAdmin = "anarhystones.admin";
            mainConfig.permPlace = "anarhystones.place";
            mainConfig.permDebug = "anarhystones.debug";
        }

        Toml limits = toml.getTable("limits");
        if (limits != null) {
            mainConfig.defaultMaxRegions = limits.getLong("default_max_regions", 1L).intValue();
            mainConfig.limitPermissionNode = limits.getString("limit_permission_node", "anarhystones.limit.");
        } else {
            mainConfig.defaultMaxRegions = 1;
            mainConfig.limitPermissionNode = "anarhystones.limit.";
        }
    }

    public ItemStack createProtectionItem(BlockSettings s) {
        ItemStack item = new ItemStack(s.type);
        ItemMeta meta = item.getItemMeta();

        if (meta != null && s.displayName != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', s.displayName));
            meta.setLore(s.lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }

        return item;
    }

    public BlockSettings getSettings(Material m) {
        return blocks.get(m);
    }

    public Map<Material, BlockSettings> getAllBlocks() {
        return blocks;
    }

    public MainConfig getMainConfig() {
        return mainConfig;
    }
}