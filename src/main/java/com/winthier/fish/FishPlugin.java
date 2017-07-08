package com.winthier.fish;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.event.CustomRegisterEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class FishPlugin extends JavaPlugin implements Listener {
    private Score score;
    private int spawnFish;
    private final Random random = new Random(System.currentTimeMillis());

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player)sender;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            List<String> hi = new ArrayList<>(score.getScores().keySet());
            Collections.sort(hi, (a, b) -> Integer.compare(score.getScores().get(b), score.getScores().get(a)));
            int i = 1;
            sender.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "Fishing Highscore");
            for (String name: hi) {
                sender.sendMessage(ChatColor.GREEN + "" + (i++) + ") " + name + " " + score.getScore(name));
            }
        } else {
            return false;
        }
        return true;
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        event.addEntity(new FishHeadEntity(this));
        spawnFish = getConfig().getInt("SpawnFish");
        score = new Score(this);
        score.load();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (event.getCaught() == null || !(event.getCaught() instanceof Item) || ((Item)event.getCaught()).getItemStack().getType() != Material.RAW_FISH) {
            return;
        }
        int fishNearby = 0;
        FishHeadEntity.Watcher fishHead = null;
        Location caughtLocation = event.getCaught().getLocation();
        if (!caughtLocation.getWorld().getName().equals("Resource")) return;
        for (Entity nearby: event.getCaught().getNearbyEntities(16, 16, 16)) {
            EntityWatcher ew = CustomPlugin.getInstance().getEntityManager().getEntityWatcher(nearby);
            if (ew != null && ew instanceof FishHeadEntity.Watcher) {
                fishNearby += 1;
                if (nearby.getLocation().distance(event.getCaught().getLocation()) <= 2.0) {
                    fishHead = (FishHeadEntity.Watcher)ew;
                }
            }
        }
        if (fishNearby < spawnFish) {
            for (int i = 0; i < spawnFish - fishNearby; i += 1) {
                Block block = event.getCaught().getLocation().getBlock();
                for (int j = 0; j < 20; j += 1) {
                    BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
                    List<Block> blocks = new ArrayList<>();
                    blocks.add(block);
                    for (BlockFace face: faces) {
                        Block rel = block.getRelative(face);
                        if (rel.isLiquid()) {
                            blocks.add(rel);
                        }
                    }
                    block = blocks.get(random.nextInt(blocks.size()));
                }
                CustomPlugin.getInstance().getEntityManager().spawnEntity(block.getLocation(), FishHeadEntity.CUSTOM_ID);
                getLogger().info("Fish spawned at " + block);
            }
        }
        if (fishHead != null) {
            fishHead.getEntity().getWorld().spawnParticle(Particle.CRIT, fishHead.getEntity().getLocation(), 24, 0.5, 0.5, 0.5, 0);
            fishHead.getEntity().getWorld().playSound(fishHead.getEntity().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            score.addScore(event.getPlayer().getName(), 10);
            fishHead.remove();
            for (Entity nearby: event.getPlayer().getNearbyEntities(64, 64, 64)) {
                if (nearby instanceof Player) {
                    ((Player)nearby).sendMessage(ChatColor.GREEN + event.getPlayer().getName() + " caught a big fish and has " + score.getScore(event.getPlayer().getName()) + " points. See /fish");
                }
            }
            event.getPlayer().sendMessage(ChatColor.GREEN + "You caught a big fish and has " + score.getScore(event.getPlayer().getName()) + " points. See /fish");
        } else {
            score.addScore(event.getPlayer().getName(), 1);
        }
        score.save();
    }
}
