package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable data record representing a Power Ore conversion that was already
 * completed and persisted to disk.  Mutable runtime state (reward flag and
 * visual task) is kept in a private static map that is cleaned up when the
 * challenge is rewarded or the effect is stopped.
 */
public record CompletedPowerOreChallenge(Location oreLocation, UUID uuid)
        implements PowerOreChallengeHandle {

    private static final double BLOCK_CENTER_OFFSET = 0.5;

    private static final Map<CompletedPowerOreChallenge, MutableState> STATES = new HashMap<>();

    private MutableState state() {
        return STATES.computeIfAbsent(this, k -> new MutableState());
    }

    @Override
    public Location getOreLocation() {
        return oreLocation();
    }

    @Override
    public UUID getUniqueId() {
        return uuid();
    }

    @Override
    public PowerOreChallenge.Status getStatus() {
        return PowerOreChallenge.Status.SUCCESS;
    }

    @Override
    public boolean isRewardDropped() {
        return state().rewardDropped;
    }

    @Override
    public void reward(Player player) {
        MutableState s = state();
        if (s.rewardDropped)
            return;
        World world = oreLocation.getWorld();
        if (world == null)
            return;
        world.dropItem(oreLocation.clone().add(BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET),
                ItemStackGenerator.getPowerOre());
        world.playSound(oreLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
        player.sendMessage(ChatColor.GREEN + "You successfully converted the Power Ore!");
        s.rewardDropped = true;
        stopVisuals();
    }

    @Override
    public void startVisuals() {
        stopVisuals();
        PowerOreVisualEffect effect = new PowerOreVisualEffect(oreLocation, this::getStatus);
        effect.start();
        state().visualEffect = effect;
    }

    @Override
    public void stopVisuals() {
        MutableState s = STATES.remove(this);
        if (s != null && s.visualEffect != null) {
            s.visualEffect.stop();
        }
    }

    private static class MutableState {
        boolean rewardDropped;
        PowerOreVisualEffect visualEffect;
    }
}
