package com.danikvitek.slimeinabukkit;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.danikvitek.slimeinabukkit.Main.SLIME_BUCKET_MATERIAL;
import static com.danikvitek.slimeinabukkit.Main.SLIME_BUCKET_UUID_KEY;

public class GetSlimeCommand implements CommandExecutor {
    private final Main main;

    public GetSlimeCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (sender instanceof Player) {
            final var player = (Player) sender;

            final var slimeBucket = new ItemStack(SLIME_BUCKET_MATERIAL);
            final var slimeBucketMeta = slimeBucket.getItemMeta();
            assert slimeBucketMeta != null;
            final var location = player.getLocation();
            slimeBucketMeta.setCustomModelData(
              location.getChunk().isSlimeChunk()
                ? main.getActiveSlimeCmd()
                : main.getCalmSlimeCmd()
            );
            slimeBucketMeta.setDisplayName(main.getSlimeBucketTitle());
            slimeBucket.setItemMeta(slimeBucketMeta);
            final var nbtItem = new NBTItem(slimeBucket);
            nbtItem.setUUID(SLIME_BUCKET_UUID_KEY, UUID.randomUUID()); // for it to be not stackable
            nbtItem.applyNBT(slimeBucket);

            final var world = player.getWorld();
            world.playSound(location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            if (!player.getInventory().addItem(slimeBucket).isEmpty())
                world.dropItem(player.getEyeLocation(), slimeBucket);

        } else sender.sendMessage(ChatColor.RED + "Command can only be used by a player");

        return true;
    }
}
