package com.danikvitek.slimeinabukkit.command;

import be.seeseemelk.mockbukkit.ChunkMock;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.danikvitek.slimeinabukkit.SlimeInABukkitPlugin;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SlimeChunkCommandTest {
    private ServerMock server;
    private SlimeInABukkitPlugin plugin;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
        player.setLocation(new Location(world, 0, 60, 0));
        plugin = MockBukkit.load(SlimeInABukkitPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void onCommandInSlimeChunk() {
        ChunkMock chunk = world.getChunkAt(0, 0);
        chunk.load();
//        chunk.setSlimeChunk(true);

        final PluginCommand slimeChunkCommand = plugin.getCommand("slime_chunk");

        assertNotNull(slimeChunkCommand);
        slimeChunkCommand.getExecutor().onCommand(
          player,
          slimeChunkCommand,
          "slime_chunk",
          new String[0]
        );

        assertEquals(
          plugin.getSlimeChunkMessage().replace(
            SlimeInABukkitPlugin.CHUNK_STATUS_PLACEHOLDER,
            plugin.getChunkStatusTrue()
          ),
          player.nextMessage()
        );
    }

    @Test
    void onCommandInNonSlimeChunk() {
        ChunkMock chunk = world.getChunkAt(0, 0);
        chunk.load();
//        chunk.setSlimeChunk(false); // just for clarity

        final PluginCommand slimeChunkCommand = plugin.getCommand("slime_chunk");

        assertNotNull(slimeChunkCommand);
        slimeChunkCommand.getExecutor().onCommand(
          player,
          slimeChunkCommand,
          "slime_chunk",
          new String[0]
        );

        assertEquals(
          plugin.getSlimeChunkMessage().replace(
            SlimeInABukkitPlugin.CHUNK_STATUS_PLACEHOLDER,
            plugin.getChunkStatusFalse()
          ),
          player.nextMessage()
        );
    }
}