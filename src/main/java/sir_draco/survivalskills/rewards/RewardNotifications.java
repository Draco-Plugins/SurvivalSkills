package sir_draco.survivalskills.rewards;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RewardNotifications {

    public static void notifyPlayer(String type, String reward, Player p) {
        List<String> notification = RewardData.getNotification(type, reward);
        if (notification != null) notification.forEach(p::sendRawMessage);
    }

    public static String getRewardDescription(String type, String reward) {
        return RewardData.getDescription(type, reward);
    }

    public static ArrayList<String> getLore(String string) {
        String[] split = string.split("\n");
        return new ArrayList<>(Arrays.asList(split));
    }

    public static String cooldown(int time) {
        int minutes = time / 60;
        int seconds = time % 60;
        return ChatColor.AQUA.toString() + minutes + ChatColor.GREEN + " minutes " + ChatColor.AQUA + seconds
                + ChatColor.GREEN + " seconds!";
    }
}
