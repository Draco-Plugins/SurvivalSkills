package sir_draco.survivalskills.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrialSelectionClassifierTest {

    @Test
    void coopOptionIsNotHandledAsPartyMember() {
        boolean partyMemberSelection = TrialSelectionClassifier.isPartyMemberSelection(
                Material.PLAYER_HEAD, ChatColor.GREEN + "Co-op");

        assertFalse(partyMemberSelection);
    }

    @Test
    void partyLeaderHeadIsHandledAsPartyMember() {
        boolean partyMemberSelection = TrialSelectionClassifier.isPartyMemberSelection(
                Material.PLAYER_HEAD, ChatColor.GREEN + "TrialLeader");

        assertTrue(partyMemberSelection);
    }
}
