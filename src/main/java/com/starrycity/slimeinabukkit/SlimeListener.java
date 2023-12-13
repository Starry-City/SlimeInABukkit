package com.starrycity.slimeinabukkit;

import de.tr7zw.changeme.nbtapi.NBTItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
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

public class SlimeListener implements Listener {
    private static final Set<UUID> interactingPlayers = new LinkedHashSet<>();
    private static final Map<Item, Chunk> lastItemChunks = new ConcurrentHashMap<>();

    private final @NotNull SlimeInABukkitPlugin main;

    @Contract(pure = true)
    public SlimeListener(final @NotNull SlimeInABukkitPlugin main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerMove(final @NotNull PlayerMoveEvent event) {
        final var fromChunk = event.getFrom().getChunk();
        final var toChunk = event.getTo().getChunk();
        if (Objects.equals(fromChunk, toChunk)) return;
        main.debugLog("PlayerMoveEvent was caught; Changing chunks");

        updateSlimes(event.getPlayer().getInventory(), toChunk.isSlimeChunk());
    }

    private void updateSlimes(final @NotNull PlayerInventory inventory, final boolean changeToActive) {
        main.debugLog("updateSlimes: changeToActive = " + changeToActive);
        for (final var itemStack : inventory) updateSlime(itemStack, changeToActive);
    }

    private void updateSlime(final @Nullable ItemStack itemStack, final boolean changeToActive) {
        if (itemStack == null || itemStack.getType() != SlimeInABukkitPlugin.SLIME_BUCKET_MATERIAL || !itemStack.hasItemMeta() || !Objects.requireNonNull(itemStack.getItemMeta()).hasCustomModelData())
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

        if (checkCannotPickupSlime()) return;

        if (!(event.getRightClicked() instanceof Slime) || event.getRightClicked() instanceof MagmaCube) return;
        main.debugLog("PlayerInteractEntityEvent: clicked at slime");

        final var slime = (Slime) event.getRightClicked();
        if (slime.getSize() != 1) return;

        final var player = event.getPlayer();
        final var inventory = player.getInventory();

        if (event.getHand() == EquipmentSlot.OFF_HAND && inventory.getItemInMainHand().getType() != Material.AIR)
            return;
        main.debugLog("PlayerInteractEvent: Hand = " + event.getHand());
        final var itemStack = event.getHand() == EquipmentSlot.HAND ? inventory.getItemInMainHand() : inventory.getItemInOffHand();

        final var itemMeta = itemStack.hasItemMeta() ? itemStack.getItemMeta() : new ItemStack(SlimeInABukkitPlugin.SLIME_BUCKET_MATERIAL).getItemMeta();
        assert itemMeta != null;
        if (itemStack.getType() != Material.BUCKET || itemMeta.hasCustomModelData()) return;

        pickupSlime(event, slime, player, itemStack, itemMeta, event.getHand());
    }

    private void pickupSlime(final @NotNull PlayerInteractEntityEvent event, final @NotNull Slime slime, final @NotNull Player player, final @NotNull ItemStack bucketStack, final @NotNull ItemMeta slimeBucketMeta, final @NotNull EquipmentSlot bucketStackSlot) {
        if (interactingPlayers.contains(player.getUniqueId())) return;
        interactingPlayers.add(player.getUniqueId());

        slime.remove();
        final var slimeBucketStack = bucketStack.clone();
        slimeBucketStack.setAmount(1);
        slimeBucketMeta.setCustomModelData(player.getLocation().getChunk().isSlimeChunk() ? main.getActiveSlimeCmd() : main.getCalmSlimeCmd());
        if (slime.customName() != null)
            slimeBucketMeta.displayName(slime.customName());
        else
//            slimeBucketMeta.displayName(slimeBucketMeta.hasDisplayName() ? slimeBucketMeta.displayName() : main.getSlimeBucketTitle());
            slimeBucketMeta.displayName(slimeBucketMeta.hasDisplayName() ? slimeBucketMeta.displayName() : Component.text(Objects.requireNonNull(main.getSlimeBucketTitle())));

        slimeBucketStack.setItemMeta(slimeBucketMeta);
        slimeBucketStack.setType(SlimeInABukkitPlugin.SLIME_BUCKET_MATERIAL);
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
                interactingPlayers.remove(player.getUniqueId());
            }
        }.runTask(main);
    }

    private void assignUUID(final @NotNull ItemStack slimeBucketStack, final @NotNull UUID uuid) {
        final var nbtItem = new NBTItem(slimeBucketStack);
        nbtItem.setUUID(SlimeInABukkitPlugin.SLIME_BUCKET_UUID_KEY, uuid);
        nbtItem.applyNBT(slimeBucketStack);
    }

    @EventHandler
    public void onClickAtBlock(final @NotNull PlayerInteractEvent event) {
        main.debugLog("PlayerInteractEvent was caught");

        if (checkCannotPickupSlime()) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        main.debugLog("PlayerInteractEvent: Action = " + event.getAction());

        final Player player = event.getPlayer();
        if (event.getHand() == EquipmentSlot.OFF_HAND && player.getInventory().getItemInMainHand().getType() != Material.AIR)
            return;
        main.debugLog("PlayerInteractEvent: Hand = " + event.getHand());

        final ItemStack itemStack = event.getItem();
        if (itemStack == null || itemStack.getType() != SlimeInABukkitPlugin.SLIME_BUCKET_MATERIAL || !itemStack.hasItemMeta())
            return;

        final ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        if (!itemMeta.hasCustomModelData() || (itemMeta.getCustomModelData() != main.getCalmSlimeCmd() && itemMeta.getCustomModelData() != main.getActiveSlimeCmd()))
            return;

        placeSlime(event, player, itemStack, itemMeta);
    }

    private boolean checkCannotPickupSlime() {
        if (!main.isCanPickupSlime()) {
            main.debugLog("can-pickup-slime = false");
            return true;
        }
        main.debugLog("can-pickup-slime = true");
        return false;
    }

    private void placeSlime(final @NotNull PlayerInteractEvent event, final @NotNull Player player, final @NotNull ItemStack itemStack, final @NotNull ItemMeta itemMeta) {
        if (interactingPlayers.contains(player.getUniqueId())) return;
        interactingPlayers.add(player.getUniqueId());

        event.setUseInteractedBlock(Event.Result.DENY);

        final Block block = event.getClickedBlock();
        assert block != null;
        final BlockFace blockFace = event.getBlockFace();

        final Location slimeReleaseLocation = block.getLocation().clone().add(new Vector(0.5, 0d, 0.5)).add(blockFace.getDirection());
        slimeReleaseLocation.setYaw(SlimeInABukkitPlugin.RANDOM.nextFloat() * 360f);

        player.getWorld().spawn(slimeReleaseLocation, Slime.class, slime -> {
            slime.setSize(1);
            if (itemMeta.hasDisplayName())
                if (itemMeta.getDisplayName().equals(main.getSlimeBucketTitle()))
                    slime.customName(null);
                else
                    slime.customName(itemMeta.displayName());
            else
                slime.customName(null);
//            if (itemMeta.hasDisplayName() && !Objects.equals(ChatColor.stripColor(String.valueOf(itemMeta.displayName())), ChatColor.stripColor(main.getSlimeBucketTitle())))
//                slime.customName(itemMeta.displayName());
        });

        itemMeta.setCustomModelData(null);
        itemMeta.displayName(null);
        itemStack.setItemMeta(itemMeta);
        itemStack.setType(Material.BUCKET);
        removeUUID(itemStack);

        if (event.getHand() == EquipmentSlot.HAND) player.swingMainHand();
        else player.swingOffHand();

        new BukkitRunnable() {
            @Override
            public void run() {
                interactingPlayers.remove(player.getUniqueId());
            }
        }.runTask(main);
    }

    private void removeUUID(final @NotNull ItemStack itemStack) {
        final var nbtItem = new NBTItem(itemStack);
        nbtItem.removeKey(SlimeInABukkitPlugin.SLIME_BUCKET_UUID_KEY);
        nbtItem.applyNBT(itemStack);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraftWithSlimeBucket(final @NotNull CraftItemEvent e) {
        final Map<Integer, ItemStack> slotsAndStacksToReplaceWithSlimeBucket = new LinkedHashMap<>();
        @NotNull ItemStack[] matrix = e.getInventory().getMatrix();

        for (int i = 0, matrixLength = matrix.length; i < matrixLength; i++) {
            ItemStack itemStack = matrix[i];
            if (itemStack == null || itemStack.getType() != SlimeInABukkitPlugin.SLIME_BUCKET_MATERIAL || !itemStack.hasItemMeta())
                continue;

            final ItemMeta itemMeta = itemStack.getItemMeta();
            assert itemMeta != null;

            if (!itemMeta.hasCustomModelData()) continue;
            final int cmd = itemMeta.getCustomModelData();

            if (cmd != main.getCalmSlimeCmd() && cmd != main.getActiveSlimeCmd()) continue;

            slotsAndStacksToReplaceWithSlimeBucket.put(i, itemStack.clone());
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                final ItemStack[] newMatrix = new ItemStack[matrix.length];
                slotsAndStacksToReplaceWithSlimeBucket.forEach((slot, clonedBucket) -> {
                    clonedBucket.setType(Material.BUCKET);
                    final ItemMeta clonedBucketMeta = clonedBucket.getItemMeta();
                    assert clonedBucketMeta != null;
                    clonedBucketMeta.setCustomModelData(null);
                    clonedBucketMeta.displayName(null);
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
        final Item itemDrop = event.getItemDrop();
        final ItemStack itemStack = itemDrop.getItemStack();

        if (itemStack.getType() != SlimeInABukkitPlugin.SLIME_BUCKET_MATERIAL || !itemStack.hasItemMeta()) return;
        final ItemMeta itemMeta = itemStack.getItemMeta();
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

                final Chunk currentChunk = itemDrop.getLocation().getChunk();
                if (!Objects.equals(currentChunk, lastItemChunks.get(itemDrop))) {
                    lastItemChunks.put(itemDrop, currentChunk);
                    updateSlime(itemStack, currentChunk.isSlimeChunk());
                    itemDrop.setItemStack(itemStack);
                }
            }
        }.runTaskTimerAsynchronously(main, 0L, 1L);
    }
}
