package com.starrycity.slimeinabukkit;

import com.starrycity.slimeinabukkit.command.GetSlimeCommand;
import com.starrycity.slimeinabukkit.command.SlimeChunkCommand;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.Random;

public final class SlimeInABukkitPlugin extends JavaPlugin {

    public static final String CHUNK_STATUS_PLACEHOLDER = "%chunk-status%";
    static final String DEFAULT_BUCKET_TITLE = "§rSlime in a bucket";
    static final String DEFAULT_SLIME_CHUNK_MESSAGE = "§7This chunk %chunk-status%§7 a Slime chunk";
    static final String DEFAULT_CHUNK_STATUS_TRUE = "§ais";
    static final String DEFAULT_CHUNK_STATUS_FALSE = "§cis not";
    static final boolean DEFAULT_CAN_PICKUP_SLIME = true;
    private final boolean isTest;
    private @NotNull String slimeChunkMessage = "";

    private static final int PLUGIN_ID = 14716;
    public static final Random RANDOM = new Random();
    public static final Material SLIME_BUCKET_MATERIAL = Material.SLIME_BALL;
    public static final String SLIME_BUCKET_UUID_KEY = "SLIME_UUID";
    private @NotNull String chunkStatusTrue = "";
    private int calmSlimeCmd;
    private int activeSlimeCmd;
    private @NotNull String slimeBucketTitle = "";
    private @NotNull String chunkStatusFalse = "";

    public SlimeInABukkitPlugin() {
        super();
        this.isTest = false;
    }

    private SlimeInABukkitPlugin(JavaPluginLoader loader,
                                 PluginDescriptionFile descriptionFile,
                                 File dataFolder,
                                 File file) {
        super(loader, descriptionFile, dataFolder, file);
        this.isTest = true;
    }

    private boolean canPickupSlime;
    private boolean debug;

    @Override
    public void onEnable() {
        this.getConfig().options().configuration();
        this.saveDefaultConfig();

        getConfigValues();
        updateConfigValues();

        Objects.requireNonNull(getCommand("get_slime")).setExecutor(new GetSlimeCommand(this));
        Objects.requireNonNull(getCommand("slime_chunk")).setExecutor(new SlimeChunkCommand(this));

        if (!this.isTest) new Metrics(this, PLUGIN_ID);

        Bukkit.getPluginManager().registerEvents(new SlimeListener(this), this);
    }

    private void getConfigValues() {
        this.calmSlimeCmd = this.getConfig().getInt("custom-model-data.calm-slime", 404);
        this.activeSlimeCmd = this.getConfig().getInt("custom-model-data.active-slime", 200);
        this.slimeBucketTitle = ChatColor.translateAlternateColorCodes(
                '&',
                Objects.requireNonNull(this.getConfig().getString("bucket-title", DEFAULT_BUCKET_TITLE))
        );
        this.slimeChunkMessage = ChatColor.translateAlternateColorCodes(
                '&',
                Objects.requireNonNull(this.getConfig().getString(
                        "slime-chunk-message", DEFAULT_SLIME_CHUNK_MESSAGE
                ))
        );
        this.chunkStatusTrue = ChatColor.translateAlternateColorCodes(
                '&',
                Objects.requireNonNull(this.getConfig().getString(
                        "chunk-status.true", DEFAULT_CHUNK_STATUS_TRUE
                ))
        );
        this.chunkStatusFalse = ChatColor.translateAlternateColorCodes(
                '&',
                Objects.requireNonNull(this.getConfig().getString(
                        "chunk-status.false", DEFAULT_CHUNK_STATUS_FALSE
                ))
        );
        this.canPickupSlime = this.getConfig().getBoolean("can-pickup-slime", DEFAULT_CAN_PICKUP_SLIME);
        this.debug = this.getConfig().getBoolean("debug", false);
    }

    private void updateConfigValues() {
        this.getConfig().set("custom-model-data.calm-slime", this.calmSlimeCmd);
        this.getConfig().set("custom-model-data.active-slime", this.activeSlimeCmd);
        this.getConfig().set("bucket-title", this.slimeBucketTitle);
        this.getConfig().set("slime-chunk-message", this.slimeChunkMessage);
        this.getConfig().set("chunk-status.true", this.chunkStatusTrue);
        this.getConfig().set("chunk-status.false", this.chunkStatusFalse);
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

    public @NotNull String getSlimeChunkMessage() {
        return slimeChunkMessage;
    }

    public @NotNull String getChunkStatusTrue() {
        return chunkStatusTrue;
    }

    public @NotNull String getChunkStatusFalse() {
        return chunkStatusFalse;
    }

    public boolean isCanPickupSlime() {
        return canPickupSlime;
    }

    public boolean isDebug() {
        return debug;
    }

    public void debugLog(final @NotNull String message) {
        if (this.isDebug()) this.getLogger().info(message);
    }
}
