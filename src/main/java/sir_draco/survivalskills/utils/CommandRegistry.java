package sir_draco.survivalskills.utils;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.commands.admin_commands.*;
import sir_draco.survivalskills.commands.default_commands.*;
import sir_draco.survivalskills.commands.skill_commands.*;

/**
 * Registers all plugin commands during startup.
 * Extracted from FileUtils to maintain single responsibility.
 */
public final class CommandRegistry {

    private CommandRegistry() {}

    public static void registerAll(SurvivalSkills plugin) {
        // Default Player Commands
        new FlightCommand(plugin);
        new AutoEatCommand(plugin);
        new AutoTrashCommand(plugin);
        new DeathLocationCommand(plugin);
        new DeathReturnCommand(plugin);
        new EatCommand(plugin);
        new MobScannerCommand(plugin);
        new NightVisionCommand(plugin);
        new PeacefulMinerCommand(plugin);
        new PermaTrashCommand(plugin);
        new SpelunkerCommand(plugin);
        new ToggleMaxSkillMessageCommand(plugin);
        new TogglePhantomsCommand(plugin);
        new ToggleScoreboardCommand(plugin);
        new ToggleSpeedCommand(plugin);
        new ToggleTrailCommand(plugin);
        new ToolBeltCommand(plugin);
        new VeinminerCommand(plugin);
        new WaterBreathingCommand(plugin);
        new ToggleBloodyDomainCommand();
        new ToggleTrashCommand(plugin);
        new GodQuestCommand(plugin);
        new ToggleBossMusic();
        new UpCommand(plugin);
        new GodTrialCommand(plugin);
        new CreativeCommand(plugin);

        // Admin Commands
        new BossCommand(plugin);
        new BossMusicCommand(plugin);
        new CaveFinderCommand(plugin);
        new GetTrophyCommand(plugin);
        new ResetFirstDragonCommand(plugin);
        new SkillsMultiplierCommand(plugin);
        new SurvivalSkillsCommand(plugin);
        new SurvivalSkillsGetCommand(plugin);
        new ToggleOverworldFirstDragonCommand(plugin);
        new DragonStatusCommand();
        new ToggleGodQuestCommand(plugin);
        new ResetAllCommand(plugin);
        new StoreTrialBuildingCommand(plugin);
        new CancelAbilityCooldownsCommand(plugin);
    }
}
