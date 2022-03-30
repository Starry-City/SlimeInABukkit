package com.danikvitek.slimeinabukkit;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Random;

public final class Main extends JavaPlugin {
    public static final Random RANDOM = new Random();
    private static final int PLUGIN_ID = 14716;
    private int calmSlimeCmd;
    private int activeSlimeCmd;
    private @NotNull String slimeBucketTitle = "";
    private boolean debug;

    @Override
    public void onEnable() {
        this.getConfig().options().configuration();
        this.saveDefaultConfig();

        this.calmSlimeCmd = this.getConfig().getInt("custom-model-data.calm-slime", 404);
        this.activeSlimeCmd = this.getConfig().getInt("custom-model-data.active-slime", 200);
        this.slimeBucketTitle = ChatColor.translateAlternateColorCodes(
                '&',
                Objects.requireNonNull(this.getConfig().getString("bucket-title", "&rSlime in a bucket"))
        );
        this.debug = this.getConfig().getBoolean("debug", false);

        new Metrics(this, PLUGIN_ID);

        Bukkit.getPluginManager().registerEvents(new SlimeListener(this), this);
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

    public boolean isDebug() {
        return debug;
    }

    public void debugLog(final @NotNull String message) {
        if (this.isDebug()) this.getLogger().info(message);
    }
}
