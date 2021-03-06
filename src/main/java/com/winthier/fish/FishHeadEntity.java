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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
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
                as.setSmall(random.nextInt(3) > 0);
                as.setMarker(true);
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
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event, EntityContext context) {
        event.setCancelled(true);
        ((Watcher)context.getEntityWatcher()).remove();
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event, EntityContext context) {
        ((Watcher)context.getEntityWatcher()).scared = 5;
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
        private int cooldown = 20;
        private int scared = 0;

        void remove() {
            entity.remove();
            CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
        }

        void onTick() {
            ticks += 1;
            cooldown -= 1;
            if (cooldown > 0) return;
            cooldown = 20 + random.nextInt(40);
            double eyeHeight = entity.isSmall() ? 0.987 : 1.975;
            Block block = entity.getLocation().add(0, eyeHeight, 0).getBlock();
            if (!block.isLiquid()) {
                remove();
                return;
            }
            int playersNearby = 0;
            for (Entity nearby: entity.getNearbyEntities(4, 4, 4)) {
                if (nearby instanceof Player) {
                    if (plugin.allowedGameModes.contains(((Player)nearby).getGameMode())) playersNearby += 1;
                }
            }
            if (playersNearby > 0) scared = 5;
            Block newBlock;
            if (scared > 0 && block.getRelative(0, -1, 0).isLiquid() && block.getRelative(0, -2, 0).isLiquid()) {
                scared -= 1;
                newBlock = block.getRelative(0, -1, 0);
            } else if (random.nextBoolean()) {
                newBlock = block;
            } else {
                BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
                final List<Block> blocks = new ArrayList<>();
                for (BlockFace face: faces) {
                    Block rel = block.getRelative(face);
                    if (rel.isLiquid()
                        && rel.getRelative(0, -1, 0).isLiquid()
                        && rel.getRelative(0, -2, 0).isLiquid()) {
                        blocks.add(rel);
                        if (rel.getRelative(0, -3, 0).isLiquid()) {
                            blocks.add(rel);
                        }
                    }
                }
                Block rel = block.getRelative(BlockFace.UP);
                if (rel.isLiquid()) {
                    blocks.add(rel);
                    blocks.add(rel);
                    blocks.add(rel);
                }
                if (blocks.isEmpty()) {
                    newBlock = block;
                } else {
                    newBlock = blocks.get(random.nextInt(blocks.size()));
                }
            }
            Location loc = newBlock.getLocation().add(0.5 + random.nextDouble() * 0.3 - random.nextDouble() * 0.3,
                                                      0.7 + random.nextDouble() * 0.3 - random.nextDouble() * 0.3 - eyeHeight,
                                                      0.5 + random.nextDouble() * 0.3 - random.nextDouble() * 0.3);
            loc.setYaw(random.nextFloat() * 360.0f);
            entity.teleport(loc);
        }

        Location getEyeLocation() {
            double eyeHeight = entity.isSmall() ? 0.987 : 1.975;
            return entity.getLocation().add(0, eyeHeight, 0);
        }
    }
}
