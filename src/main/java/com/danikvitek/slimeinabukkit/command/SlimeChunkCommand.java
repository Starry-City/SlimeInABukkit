package com.danikvitek.slimeinabukkit.command;

import com.danikvitek.slimeinabukkit.SlimeInABukkitPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SlimeChunkCommand implements CommandExecutor {
    private final @NotNull SlimeInABukkitPlugin main;

    public SlimeChunkCommand(@NotNull SlimeInABukkitPlugin main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        slimeChunkImpl(commandSender);
        return true;
    }

    private void slimeChunkImpl(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Command can only be used by a player");
            return;
        }

        final Player player = (Player) sender;

        final String chunkStatus = player.getLocation().getChunk().isSlimeChunk()
                                   ? this.main.getChunkStatusTrue()
                                   : this.main.getChunkStatusFalse();
        player.sendMessage(this.main.getSlimeChunkMessage().replace(SlimeInABukkitPlugin.CHUNK_STATUS_PLACEHOLDER, chunkStatus));
    }
}
