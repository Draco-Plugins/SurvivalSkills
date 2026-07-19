package sir_draco.survivalskills.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.Objects;

final class TrialSelectionClassifier {

    static final String COOP_OPTION_NAME = ChatColor.GREEN + "Co-op";

    private TrialSelectionClassifier() {
    }

    static boolean isPartyMemberSelection(Material type, String name) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        return type == Material.PLAYER_HEAD && !COOP_OPTION_NAME.equals(name);
    }
}
