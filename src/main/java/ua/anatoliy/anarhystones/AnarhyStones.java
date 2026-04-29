package ua.anatoliy.anarhystones;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AnarhyStones extends JavaPlugin {

    private static AnarhyStones instance;
    private ConfigManager configManager;
    private final int CURRENT_CONFIG_VERSION = 16;

    private final Set<UUID> debuggingPlayers = new HashSet<>();
    private final Set<UUID> disabledPlacementPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        checkConfigVersion();

        if (getCommand("as") != null) {
            getCommand("as").setExecutor(new AsCommand(this));
            getCommand("as").setTabCompleter(new AsTabCompleter());
        }

        getServer().getPluginManager().registerEvents(new BlockListener(this), this);

        Bukkit.getConsoleSender().sendMessage("§a[AnarhyStones] Плагін успішно активовано!");
    }

    private void checkConfigVersion() {
        int userVersion = configManager.getMainConfig().configVersion;
        if (userVersion < CURRENT_CONFIG_VERSION) {
            Bukkit.getConsoleSender().sendMessage("§c****************************************************");
            Bukkit.getConsoleSender().sendMessage("§cУВАГА! Ваш config.toml застарілий (Версія: " + userVersion + ")");
            Bukkit.getConsoleSender().sendMessage("§cАктуальна версія плагіна потребує: " + CURRENT_CONFIG_VERSION);
            Bukkit.getConsoleSender().sendMessage("§c****************************************************");
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§c[AnarhyStones] Плагін вимкнено!");
    }

    public static AnarhyStones getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public boolean isDebugEnabled(Player p) {
        return debuggingPlayers.contains(p.getUniqueId());
    }

    public boolean toggleDebug(Player p) {
        if (debuggingPlayers.contains(p.getUniqueId())) {
            debuggingPlayers.remove(p.getUniqueId());
            return false;
        } else {
            debuggingPlayers.add(p.getUniqueId());
            return true;
        }
    }

    public boolean isPlacementDisabled(Player p) {
        return disabledPlacementPlayers.contains(p.getUniqueId());
    }

    public boolean togglePlacement(Player p) {
        if (disabledPlacementPlayers.contains(p.getUniqueId())) {
            disabledPlacementPlayers.remove(p.getUniqueId());
            return true;
        } else {
            disabledPlacementPlayers.add(p.getUniqueId());
            return false;
        }
    }
}