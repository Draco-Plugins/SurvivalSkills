package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

public final class SilkyShearsListener implements Listener {

    public static final String REWARD_NAME = "SilkyShears";

    private static final int MINIMUM_WOOL_DROP = 1;
    private static final int MAXIMUM_WOOL_DROP = 3;
    private static final int EXTRA_DROP_MULTIPLIER = 2;

    private final SurvivalSkills plugin;
    private final IntSupplier woolDropSupplier;
    private final Predicate<ItemStack> silkyShearsCheck;

    public SilkyShearsListener(SurvivalSkills plugin) {
        this(plugin, () -> ThreadLocalRandom.current().nextInt(
                        MINIMUM_WOOL_DROP, MAXIMUM_WOOL_DROP + 1),
                (ItemStack item) -> ItemStackGeneratorUtils.isCustomItem(
                        item, ItemModelData.SILKY_SHEARS.getId()));
    }

    SilkyShearsListener(SurvivalSkills plugin, IntSupplier woolDropSupplier,
                        Predicate<ItemStack> silkyShearsCheck) {
        this.plugin = Objects.requireNonNull(plugin);
        this.woolDropSupplier = Objects.requireNonNull(woolDropSupplier);
        this.silkyShearsCheck = Objects.requireNonNull(silkyShearsCheck);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShearSheep(PlayerShearEntityEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) return;
        if (!silkyShearsCheck.test(event.getItem())) return;

        Player player = event.getPlayer();
        if (!isUnlocked(player)) return;

        int vanillaDropAmount = woolDropSupplier.getAsInt();
        int extraDropAmount = vanillaDropAmount * EXTRA_DROP_MULTIPLIER;
        Material wool = Material.valueOf(sheep.getColor().name() + "_WOOL");
        World world = sheep.getWorld();
        world.dropItemNaturally(sheep.getLocation(), new ItemStack(wool, extraDropAmount));
    }

    private boolean isUnlocked(Player player) {
        PlayerRewards playerRewards = plugin.getSkillManager().getPlayerRewards(player);
        if (playerRewards == null) return false;
        Reward reward = playerRewards.getReward(SkillCategory.CRAFTING, REWARD_NAME);
        return reward != null && reward.isApplied();
    }
}
