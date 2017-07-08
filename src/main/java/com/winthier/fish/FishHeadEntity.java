package com.winthier.fish;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

@Getter
public final class FishHeadEntity implements CustomEntity, TickableEntity {
    private final FishPlugin plugin;
    public static final String CUSTOM_ID = "fish:fish_head";
    private final Random random = new Random(System.currentTimeMillis());
    private List<ItemStack> fishHeads;

    FishHeadEntity(FishPlugin plugin) {
        this.plugin = plugin;
        fishHeads = new ArrayList<>();
        for (Object o: plugin.getConfig().getList("FishHeads")) {
            fishHeads.add((ItemStack)o);
        }
        plugin.getLogger().info("" + fishHeads.size() + " fish heads loaded.");
    }

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setMarker(true);
                as.setSmall(true);
                as.setGravity(false);
                as.getEquipment().setHelmet(fishHeads.get(random.nextInt(fishHeads.size())).clone());
            });
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity e) {
        return new Watcher((ArmorStand)e, this);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @Override
    public void onTick(EntityWatcher watcher) {
        ((Watcher)watcher).onTick();
    }

    @Override
    public void entityWillUnload(EntityWatcher watcher) {
        ((Watcher)watcher).remove();
    }

    @Getter @RequiredArgsConstructor
    final class Watcher implements EntityWatcher {
        private final ArmorStand entity;
        private final FishHeadEntity customEntity;
        private int ticks = 0;

        void remove() {
            entity.remove();
            CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
        }

        void onTick() {
            ticks += 1;
            if (ticks % 20 == 0) {
                Block block = entity.getLocation().getBlock().getRelative(0, 1, 0);
                if (!block.isLiquid()) {
                    remove();
                } else {
                    BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP};
                    List<Block> blocks = new ArrayList<>();
                    blocks.add(block);
                    for (BlockFace face: faces) {
                        Block rel = block.getRelative(face);
                        if (rel.isLiquid()) {
                            blocks.add(rel);
                        }
                    }
                    Block newBlock = blocks.get(random.nextInt(blocks.size())).getRelative(0, -1, 0);
                    Location loc = newBlock.getLocation().add(0.5, 0.9, 0.5);
                    loc.setYaw(random.nextFloat() * 360.0f);
                    entity.teleport(loc);
                }
            }
        }
    }
}
