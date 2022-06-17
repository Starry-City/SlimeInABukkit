package com.danikvitek.slimeinabukkit;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;

public final class Main extends JavaPlugin {
    private static final int PLUGIN_ID = 14716;
    public static final Random RANDOM = new Random();
    public static final Material SLIME_BUCKET_MATERIAL = Material.SLIME_BALL;
    public static final String SLIME_BUCKET_UUID_KEY = "SLIME_UUID";
    private int calmSlimeCmd;
    private int activeSlimeCmd;
    private @NotNull String slimeBucketTitle = "";
    private boolean canPickupSlime;
    private boolean debug;

    @Override
    public void onEnable() {
        this.getConfig().options().configuration();
        this.saveDefaultConfig();

        getConfigValues();
        updateConfigValues();

        Objects.requireNonNull(getCommand("getslime")).setExecutor(new GetSlimeCommand(this));

        new Metrics(this, PLUGIN_ID);

        Bukkit.getPluginManager().registerEvents(new SlimeListener(this), this);
    }

    private void getConfigValues() {
        this.calmSlimeCmd = this.getConfig().getInt("custom-model-data.calm-slime", 404);
        this.activeSlimeCmd = this.getConfig().getInt("custom-model-data.active-slime", 200);
        this.slimeBucketTitle = ChatColor.translateAlternateColorCodes(
          '&',
          Objects.requireNonNull(this.getConfig().getString("bucket-title", "&rSlime in a bucket"))
        );
        this.canPickupSlime = this.getConfig().getBoolean("can-pickup-slime", true);
        this.debug = this.getConfig().getBoolean("debug", false);
    }

    private void updateConfigValues() {
        this.getConfig().set("custom-model-data.calm-slime", this.calmSlimeCmd);
        this.getConfig().set("custom-model-data.active-slime", this.activeSlimeCmd);
        this.getConfig().set("bucket-title", this.slimeBucketTitle);
        this.getConfig().set("can-pickup-slime", this.canPickupSlime);
        this.getConfig().set("debug", this.debug);
        this.saveConfig();
    }

    public int getCalmSlimeCmd() {
        return this.calmSlimeCmd;
    }

    public int getActiveSlimeCmd() {
        return this.activeSlimeCmd;
    }

    public @NotNull String getSlimeBucketTitle() {
        return slimeBucketTitle;
    }

    public boolean canPickupSlime() {
        return canPickupSlime;
    }

    public boolean isDebug() {
        return debug;
    }

    public void debugLog(final @NotNull String message) {
        if (this.isDebug()) this.getLogger().info(message);
    }
}
