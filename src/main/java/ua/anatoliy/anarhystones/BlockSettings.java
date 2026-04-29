package ua.anatoliy.anarhystones;

import org.bukkit.Material;
import java.util.List;

public class BlockSettings {
    public Material type;
    public String alias;

    public int xRadius;
    public int yRadius;
    public int zRadius;

    public double homeXOffset;
    public double homeYOffset;
    public double homeZOffset;

    public List<String> flags;
    public String displayName;
    public List<String> lore;

    public List<String> worlds;
    public String worldListType;
    public int priority;
    public boolean allowMerging;
    public boolean restrictObtaining;

    public boolean hologramEnabled;
    public List<String> hologramLines;

    public boolean preventPistonPush;
    public boolean preventExplode;
    public boolean destroyRegionWhenExplode;
    public boolean noDrop;
    public boolean allowUseInCrafting;

    public boolean noMovingWhenTpWaiting;
    public int tpWaitingSeconds;
}