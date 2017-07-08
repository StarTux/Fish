package com.winthier.fish;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

@Getter
public final class Score {
    private JavaPlugin plugin;
    private Objective objective;
    private final Map<String, Integer> scores = new HashMap<>();
    private final Map<String, ChatColor> colors = new HashMap<>();

    public Score(JavaPlugin plugin) {
        this.plugin = plugin;
        this.objective = Bukkit.getServer().getScoreboardManager().getNewScoreboard().registerNewObjective("score", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public Scoreboard getScoreboard() {
        return objective.getScoreboard();
    }

    String getScoreboardName(String name) {
        ChatColor col = colors.get(name);
        String scol = col == null ? "" : col.toString();
        return scol + name;
    }

    void setScore(String name, int score) {
        scores.put(name, score);
        objective.getScore(getScoreboardName(name)).setScore(score);
    }

    int getScore(String name) {
        Integer result = scores.get(name);
        return result == null ? 0 : result;
    }

    void addScore(String name, int score) {
        setScore(name, getScore(name) + score);
    }

    void setColor(String name, ChatColor color) {
        objective.getScoreboard().resetScores(getScoreboardName(name));
        colors.put(name, color);
        objective.getScore(getScoreboardName(name)).setScore(scores.get(name));
    }

    File getSaveFile() {
        return new File(plugin.getDataFolder(), "score.yml");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Integer> entry: scores.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        plugin.getDataFolder().mkdirs();
        try {
            config.save(getSaveFile());
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(getSaveFile());
        for (String key: config.getKeys(false)) {
            setScore(key, config.getInt(key));
        }
    }
}
