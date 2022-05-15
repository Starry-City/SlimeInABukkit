package com.danikvitek.slimeinabukkit;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.danikvitek.slimeinabukkit.Main.RANDOM;

public class SlimeListener implements Listener {
    private static final Material SLIME_BUCKET_MATERIAL = Material.SLIME_BALL;
    private static final String SLIME_BUCKET_UUID_KEY = "SLIME_UUID";

    private static final Set<Player> interactingPlayers = new LinkedHashSet<>();
    private static final Map<Item, Chunk> lastItemChunks = new ConcurrentHashMap<>();

    private final @NotNull Main main;

    @Contract(pure = true)
    public SlimeListener(final @NotNull Main main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerMove(final @NotNull PlayerMoveEvent event) {
        final var fromChunk = event.getFrom().getChunk();
        final var toChunk = event.getTo() == null ? null : event.getTo().getChunk();
        if (Objects.equals(fromChunk, toChunk)) return;
        main.debugLog("PlayerMoveEvent was caught; Changing chunks");

        updateSlimes(event.getPlayer().getInventory(), toChunk != null && toChunk.isSlimeChunk());
    }

    private void updateSlimes(final @NotNull PlayerInventory inventory, final boolean changeToActive) {
        main.debugLog("updateSlimes: changeToActive = " + changeToActive);
        for (final var itemStack : inventory) updateSlime(itemStack, changeToActive);
    }

    private void updateSlime(final @Nullable ItemStack itemStack,
                             final boolean changeToActive) {
        if (itemStack == null || itemStack.getType() != SLIME_BUCKET_MATERIAL ||
                !itemStack.hasItemMeta() || !itemStack.getItemMeta().hasCustomModelData())
            return;

        final var itemMeta = itemStack.getItemMeta();
        main.debugLog("updateSlimes: CMD = " + itemMeta.getCustomModelData());

        if (changeToActive && itemMeta.getCustomModelData() == main.getCalmSlimeCmd())
            itemMeta.setCustomModelData(main.getActiveSlimeCmd());
        else if (!changeToActive && itemMeta.getCustomModelData() == main.getActiveSlimeCmd())
            itemMeta.setCustomModelData(main.getCalmSlimeCmd());

        main.debugLog("updateSlimes: new CMD = " + itemMeta.getCustomModelData());
        itemStack.setItemMeta(itemMeta);
    }

    @EventHandler
    public void onClickAtSlime(final @NotNull PlayerInteractEntityEvent event) {
        main.debugLog("PlayerInteractEntityEvent was caught");

        if (!(event.getRightClicked() instanceof Slime)) return;
        main.debugLog("PlayerInteractEntityEvent: clicked at slime");

        final var slime = (Slime) event.getRightClicked();
        if (slime.getSize() != 1) return;

        final var player = event.getPlayer();
        final var inventory = player.getInventory();

        if (event.getHand() == EquipmentSlot.OFF_HAND && inventory.getItemInMainHand().getType() != Material.AIR)
            return;
        main.debugLog("PlayerInteractEvent: Hand = " + event.getHand());
        final var itemStack = event.getHand() == EquipmentSlot.HAND
                ? inventory.getItemInMainHand()
                : inventory.getItemInOffHand();

        final var itemMeta = itemStack.hasItemMeta()
                ? itemStack.getItemMeta()
                : new ItemStack(SLIME_BUCKET_MATERIAL).getItemMeta();
        assert itemMeta != null;
        if (itemStack.getType() != Material.BUCKET || itemMeta.hasCustomModelData()) return;

        pickupSlime(event, slime, player, itemStack, itemMeta, event.getHand());
    }

    private void pickupSlime(final @NotNull PlayerInteractEntityEvent event,
                             final @NotNull Slime slime,
                             final @NotNull Player player,
                             final @NotNull ItemStack bucketStack,
                             final @NotNull ItemMeta slimeBucketMeta,
                             final @NotNull EquipmentSlot bucketStackSlot) {
        if (interactingPlayers.contains(player)) return;
        interactingPlayers.add(player);

        slime.remove();
        final var slimeBucketStack = bucketStack.clone();
        slimeBucketStack.setAmount(1);
        slimeBucketMeta.setCustomModelData(player.getLocation().getChunk().isSlimeChunk()
                ? main.getActiveSlimeCmd()
                : main.getCalmSlimeCmd()
        );
        if (slime.getCustomName() != null) slimeBucketMeta.setDisplayName(slime.getCustomName());
        else
            slimeBucketMeta.setDisplayName(slimeBucketMeta.hasDisplayName()
                    ? slimeBucketMeta.getDisplayName()
                    : main.getSlimeBucketTitle()
            );

        slimeBucketStack.setItemMeta(slimeBucketMeta);
        slimeBucketStack.setType(SLIME_BUCKET_MATERIAL);
        assignUUID(slimeBucketStack, slime.getUniqueId());
        if (bucketStack.getAmount() > 1) {
            bucketStack.setAmount(bucketStack.getAmount() - 1);
            final var notFittedItems = player.getInventory().addItem(slimeBucketStack);
            notFittedItems.forEach((index, notFittedSlimeBucket) -> {
                final var droppedItem = player.getWorld().dropItem(player.getEyeLocation(), notFittedSlimeBucket);
                droppedItem.setPickupDelay(40);
                droppedItem.setVelocity(player.getLocation().getDirection().clone().multiply(0.2));
            });
        } else player.getInventory().setItem(bucketStackSlot, slimeBucketStack);


        if (event.getHand() == EquipmentSlot.HAND) player.swingMainHand();
        else player.swingOffHand();

        new BukkitRunnable() {
            @Override
            public void run() {
                interactingPlayers.remove(player);
            }
        }.runTask(main);
    }

    private void assignUUID(final @NotNull ItemStack slimeBucketStack,
                            final @NotNull UUID uuid) {
        final var nbtItem = new NBTItem(slimeBucketStack);
        nbtItem.setUUID(SLIME_BUCKET_UUID_KEY, uuid);
        nbtItem.applyNBT(slimeBucketStack);
    }

    @EventHandler
    public void onClickAtBlock(final @NotNull PlayerInteractEvent event) {
        main.debugLog("PlayerInteractEvent was caught");

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        main.debugLog("PlayerInteractEvent: Action = " + event.getAction());

        final var player = event.getPlayer();
        if (event.getHand() == EquipmentSlot.OFF_HAND &&
                player.getInventory().getItemInMainHand().getType() != Material.AIR) return;
        main.debugLog("PlayerInteractEvent: Hand = " + event.getHand());

        final var itemStack = event.getItem();
        if (itemStack == null || itemStack.getType() != SLIME_BUCKET_MATERIAL || !itemStack.hasItemMeta()) return;

        final var itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        if (!itemMeta.hasCustomModelData() ||
                (itemMeta.getCustomModelData() != main.getCalmSlimeCmd() &&
                        itemMeta.getCustomModelData() != main.getActiveSlimeCmd())) return;

        placeSlime(event, player, itemStack, itemMeta);
    }

    private void placeSlime(final @NotNull PlayerInteractEvent event,
                            final @NotNull Player player,
                            final @NotNull ItemStack itemStack,
                            final @NotNull ItemMeta itemMeta) {
        if (interactingPlayers.contains(player)) return;
        interactingPlayers.add(player);

        event.setUseInteractedBlock(Event.Result.DENY);

        final var block = event.getClickedBlock();
        assert block != null;
        final var blockFace = event.getBlockFace();

        final var slimeReleaseLocation = block.getLocation().clone()
                .add(new Vector(0.5, 0d, 0.5))
                .add(blockFace.getDirection());
        slimeReleaseLocation.setYaw(RANDOM.nextFloat() * 360f);

        player.getWorld().spawn(slimeReleaseLocation, Slime.class, slime -> {
            slime.setSize(1);
            if (itemMeta.hasDisplayName() && !Objects.equals(
                    ChatColor.stripColor(itemMeta.getDisplayName()), ChatColor.stripColor(main.getSlimeBucketTitle())
            )) slime.setCustomName(itemMeta.getDisplayName());
        });

        itemMeta.setCustomModelData(null);
        itemMeta.setDisplayName(null);
        itemStack.setItemMeta(itemMeta);
        itemStack.setType(Material.BUCKET);
        removeUUID(itemStack);

        if (event.getHand() == EquipmentSlot.HAND) player.swingMainHand();
        else player.swingOffHand();

        new BukkitRunnable() {
            @Override
            public void run() {
                interactingPlayers.remove(player);
            }
        }.runTask(main);
    }

    private void removeUUID(final @NotNull ItemStack itemStack) {
        final var nbtItem = new NBTItem(itemStack);
        nbtItem.removeKey(SLIME_BUCKET_UUID_KEY);
        nbtItem.applyNBT(itemStack);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraftWithSlimeBucket(final @NotNull CraftItemEvent e) {
        final var slotsAndStacksToReplaceWithSlimeBucket = new LinkedHashMap<Integer, ItemStack>();
        @NotNull ItemStack[] matrix = e.getInventory().getMatrix();
        for (int i = 0, matrixLength = matrix.length; i < matrixLength; i++) {
            ItemStack itemStack = matrix[i];
            if (itemStack == null || itemStack.getType() != SLIME_BUCKET_MATERIAL || !itemStack.hasItemMeta()) continue;
            final var itemMeta = itemStack.getItemMeta();
            assert itemMeta != null;

            if (!itemMeta.hasCustomModelData()) continue;
            final int cmd = itemMeta.getCustomModelData();

            if (cmd != main.getCalmSlimeCmd() && cmd != main.getActiveSlimeCmd()) continue;

            slotsAndStacksToReplaceWithSlimeBucket.put(i, itemStack.clone());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                final var newMatrix = new ItemStack[matrix.length];
                slotsAndStacksToReplaceWithSlimeBucket.forEach((slot, clonedBucket) -> {
                    clonedBucket.setType(Material.BUCKET);
                    final var clonedBucketMeta = clonedBucket.getItemMeta();
                    assert clonedBucketMeta != null;
                    clonedBucketMeta.setCustomModelData(null);
                    clonedBucketMeta.setDisplayName(null);
                    clonedBucket.setItemMeta(clonedBucketMeta);
                    removeUUID(clonedBucket);
                    newMatrix[slot] = clonedBucket;
                });
                @NotNull ItemStack[] matrix1 = e.getInventory().getMatrix();
                for (int i = 0; i < matrix1.length; i++) {
                    if (newMatrix[i] != null) continue;
                    newMatrix[i] = matrix1[i];
                }
                e.getInventory().setMatrix(newMatrix);
            }
        }.runTaskLater(main, 0L);
    }

    @EventHandler
    public void onSlimeBucketDrop(final @NotNull PlayerDropItemEvent event) {
        final var itemDrop = event.getItemDrop();
        final var itemStack = itemDrop.getItemStack();

        if (itemStack.getType() != SLIME_BUCKET_MATERIAL || !itemStack.hasItemMeta()) return;
        final var itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;

        if (!itemMeta.hasCustomModelData()) return;
        final int cmd = itemMeta.getCustomModelData();

        if (cmd != main.getCalmSlimeCmd() && cmd != main.getActiveSlimeCmd()) return;

        lastItemChunks.put(itemDrop, itemDrop.getLocation().getChunk());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!itemDrop.isValid()) {
                    lastItemChunks.remove(itemDrop);
                    this.cancel();
                }

                final var currentChunk = itemDrop.getLocation().getChunk();
                if (!Objects.equals(currentChunk, lastItemChunks.get(itemDrop))) {
                    lastItemChunks.put(itemDrop, currentChunk);
                    updateSlime(itemStack, currentChunk.isSlimeChunk());
                    itemDrop.setItemStack(itemStack);
                }
            }
        }.runTaskTimerAsynchronously(main, 0L, 1L);
    }
}
