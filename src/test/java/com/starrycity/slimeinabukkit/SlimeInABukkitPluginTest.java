package com.starrycity.slimeinabukkit;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlimeInABukkitPluginTest {
    private SlimeInABukkitPlugin plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(SlimeInABukkitPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getCalmSlimeCmd() {
        assertEquals(404, plugin.getCalmSlimeCmd());
    }

    @Test
    void getActiveSlimeCmd() {
        assertEquals(200, plugin.getActiveSlimeCmd());
    }

    @Test
    void getSlimeBucketTitle() {
        assertEquals(SlimeInABukkitPlugin.DEFAULT_BUCKET_TITLE, plugin.getSlimeBucketTitle());
    }

    @Test
    void getSlimeChunkMessage() {
        assertEquals(SlimeInABukkitPlugin.DEFAULT_SLIME_CHUNK_MESSAGE, plugin.getSlimeChunkMessage());
    }

    @Test
    void getChunkStatusTrue() {
        assertEquals(SlimeInABukkitPlugin.DEFAULT_CHUNK_STATUS_TRUE, plugin.getChunkStatusTrue());
    }

    @Test
    void getChunkStatusFalse() {
        assertEquals(SlimeInABukkitPlugin.DEFAULT_CHUNK_STATUS_FALSE, plugin.getChunkStatusFalse());
    }

    @Test
    void isCanPickupSlime() {
        assertEquals(SlimeInABukkitPlugin.DEFAULT_CAN_PICKUP_SLIME, plugin.isCanPickupSlime());
    }
}