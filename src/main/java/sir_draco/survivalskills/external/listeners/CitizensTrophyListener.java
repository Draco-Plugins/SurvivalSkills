package sir_draco.survivalskills.external.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.citizensnpcs.api.ai.speech.Talkable;
import net.citizensnpcs.api.ai.speech.event.NPCSpeechEvent;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.GodTrophyQuest;
import sir_draco.survivalskills.trophy.TrophyManager;

public class CitizensTrophyListener implements Listener {

    private final SurvivalSkills plugin;

    public CitizensTrophyListener(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void formatGodNPCSpeech(NPCSpeechEvent event) {
        if (!plugin.getTrophyManager().getGodNPCIDs().containsValue(event.getNPC().getId()))
            return;

        event.setCancelled(true);
        String message = TrophyManager.npcName + ChatColor.WHITE + ": " + event.getContext().getMessage();
        for (Talkable recipient : event.getContext()) {
            if (recipient.getEntity() instanceof Player player)
                player.sendRawMessage(message);
        }
    }

    @EventHandler
    public void clickGodNPC(NPCRightClickEvent e) {
        Player p = e.getClicker();
        if (!plugin.getTrophyManager().getGodNPCIDs().containsKey(p.getUniqueId())) {
            p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": Are you expecting something?");
            p.playSound(p, Sound.ENTITY_VILLAGER_YES, 1, 1);
            return;
        }

        if (plugin.getTrophyManager().getGodNPCIDs().get(p.getUniqueId()) != e.getNPC().getId()) {
            p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": You have your own god to talk to!");
            p.playSound(p, Sound.ENTITY_VILLAGER_YES, 1, 1);
            return;
        }

        // Check if the god quest is enabled
        if (!plugin.getTrophyManager().isGodQuestEnabled()) {
            p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": The God Quest is not enabled on this server");
            p.playSound(p, Sound.ENTITY_VILLAGER_YES, 1, 1);
            return;
        }

        GodTrophyQuest quest;
        if (plugin.getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId()))
            quest = plugin.getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
        else {
            quest = new GodTrophyQuest(p.getUniqueId());
            plugin.getTrophyManager().getPlayerGodQuestData().put(p.getUniqueId(), quest);
        }

        quest.handleNPCInteract(p);
    }
    
}
