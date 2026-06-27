package sir_draco.survivalskills.bosses;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

public class BossBarManager {

    private final LivingEntity boss;
    private final String name;
    private final double maxHealth;
    private BossBar bossBar;

    public BossBarManager(LivingEntity boss, String name, double maxHealth) {
        this.boss = boss;
        this.name = name;
        this.maxHealth = maxHealth;
    }

    public void create() {
        Bukkit.getServer().getBossBars().forEachRemaining(bar -> {
            for (Player p : Bukkit.getOnlinePlayers())
                bar.removePlayer(p);
        });
        NamespacedKey key = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                "boss" + boss.getUniqueId());
        bossBar = Bukkit.createBossBar(key, name, BarColor.WHITE, BarStyle.SOLID);
        addNearbyPlayers();
    }

    public void update() {
        if (bossBar == null) return;
        double healthPercentage = boss.getHealth() / maxHealth;
        bossBar.setProgress(healthPercentage);
        if (healthPercentage < 0.33) bossBar.setColor(BarColor.RED);
        else if (healthPercentage < 0.66) bossBar.setColor(BarColor.YELLOW);
        else bossBar.setColor(BarColor.WHITE);
    }

    public void updatePlayers() {
        addNearbyPlayers();
        removeFarPlayers();
    }

    public void removeAll() {
        if (bossBar != null) bossBar.removeAll();
        NamespacedKey key = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                "boss" + boss.getUniqueId());
        BossBar bar = Bukkit.getBossBar(key);
        if (bar != null) Bukkit.removeBossBar(key);
    }

    private void addNearbyPlayers() {
        if (bossBar == null) return;
        for (Entity e : boss.getNearbyEntities(50, 50, 50)) {
            if (!(e instanceof Player p)) continue;
            if (p.getLocation().distance(boss.getLocation()) > 50) continue;
            if (bossBar.getPlayers().contains(p)) continue;
            bossBar.addPlayer(p);
        }
    }

    private void removeFarPlayers() {
        if (bossBar == null) return;
        ArrayList<Player> removePlayers = new ArrayList<>();
        for (Player p : bossBar.getPlayers()) {
            if (p.isOnline()) continue;
            if (p.getLocation().distance(boss.getLocation()) <= 50) continue;
            removePlayers.add(p);
        }
        if (removePlayers.isEmpty()) return;
        for (Player p : removePlayers)
            bossBar.removePlayer(p);
    }
}
