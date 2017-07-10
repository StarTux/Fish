package com.winthier.fish;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.event.CustomRegisterEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class FishPlugin extends JavaPlugin implements Listener {
    private int spawnFish;
    private final Random random = new Random(System.currentTimeMillis());
    final Set<GameMode> allowedGameModes = EnumSet.of(GameMode.SURVIVAL, GameMode.ADVENTURE);

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player)sender;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            sender.sendMessage("fish");
        } else {
            return false;
        }
        return true;
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        reloadConfig();
        saveDefaultConfig();
        event.addEntity(new FishHeadEntity(this));
        spawnFish = getConfig().getInt("SpawnFish");
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getCaught() == null || !(event.getCaught() instanceof Item)) {
            return;
        }
        int fishNearby = 0;
        int playersNearby = 0;
        FishHeadEntity.Watcher fishHead = null;
        Location caughtLocation = event.getCaught().getLocation();
        for (Entity nearby: event.getCaught().getNearbyEntities(16, 16, 16)) {
            if (nearby instanceof Player) {
                if (allowedGameModes.contains(((Player)nearby).getGameMode())) playersNearby += 1;
            } else if (nearby instanceof ArmorStand) {
                EntityWatcher ew = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(nearby);
                if (ew != null && ew instanceof FishHeadEntity.Watcher) {
                    fishNearby += 1;
                    if (nearby.getLocation().distance(event.getCaught().getLocation()) <= 1.0) {
                        fishHead = (FishHeadEntity.Watcher)ew;
                    }
                }
            }
        }
        if (fishHead != null) {
            if (fishHead.getEntity().isSmall()) {
                ((Item)event.getCaught()).setItemStack(new ItemStack(Material.EMERALD));
            } else {
                ((Item)event.getCaught()).setItemStack(new ItemStack(Material.DIAMOND));
            }
            fishHead.getEntity().getWorld().spawnParticle(Particle.CRIT, fishHead.getEntity().getLocation(), 24, 0.5, 0.5, 0.5, 0);
            fishHead.getEntity().getWorld().playSound(fishHead.getEntity().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            fishHead.remove();
        }
        int fishToSpawn = spawnFish * playersNearby - fishNearby;
        for (int i = 0; i < fishToSpawn; i += 1) {
            Set<Block> done = new HashSet<>();
            Block block = event.getCaught().getLocation().getBlock().getRelative(0, 1, 0);
            if (!block.isLiquid()) block = block.getRelative(0, -1, 0);
            if (!block.isLiquid() || !block.getRelative(0, -1, 0).isLiquid()) return;
            for (int j = 0; j < 20; j += 1) {
                BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
                List<Block> blocks = new ArrayList<>();
                for (BlockFace face: faces) {
                    Block rel = block.getRelative(face);
                    if (!done.contains(rel)
                        && rel.isLiquid()
                        && rel.getRelative(0, -1, 0).isLiquid()
                        && rel.getRelative(0, -2, 0).isLiquid()) {
                        blocks.add(rel);
                        done.add(rel);
                    }
                }
                if (blocks.isEmpty()) return;
                block = blocks.get(random.nextInt(blocks.size()));
            }
            CustomPlugin.getInstance().getEntityManager().spawnEntity(block.getLocation().add(0.5, -1.9, 0.5), FishHeadEntity.CUSTOM_ID);
        }
    }
}
