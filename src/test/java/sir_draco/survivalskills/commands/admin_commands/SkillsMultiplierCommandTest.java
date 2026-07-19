package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AbilityManager;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.skills.SkillManager;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillsMultiplierCommandTest {

    @Test
    void consoleCanApplyExplicitPlayerMultiplier() {
        CommandTestContext context = createContext();

        try (MockedConstruction<AbilityTimer> timers = mockConstruction(AbilityTimer.class)) {
            boolean handled = context.commandExecutor().onCommand(context.sender(), context.command(),
                    "skillsmultiplier", new String[] {"player", "Sir_Draco", "2", "1200"});

            verify(context.skillManager()).setPlayerMultiplier(context.target(), 2.0);
            AbilityTimer timer = timers.constructed().getFirst();
            verify(timer).runTaskTimerAsynchronously(context.plugin(), 0, 20);
            verify(context.abilityManager()).addAbility(context.target(), timer);
            verify(context.sender()).sendMessage(contains("Skills multiplier set to 2.0 for Sir_Draco for 20 minutes"));
            assertTrue(handled);
        }
    }

    @Test
    void consoleReceivesInvalidMultiplierError() {
        CommandTestContext context = createContext();

        boolean handled = context.commandExecutor().onCommand(context.sender(), context.command(),
                "skillsmultiplier", new String[] {"player", "Sir_Draco", "invalid", "1200"});

        verify(context.sender()).sendMessage(contains("Invalid multiplier"));
        verify(context.skillManager(), never()).setPlayerMultiplier(context.target(), 2.0);
        assertFalse(handled);
    }

    private static CommandTestContext createContext() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        PluginCommand registeredCommand = mock(PluginCommand.class);
        Server server = mock(Server.class);
        SkillManager skillManager = mock(SkillManager.class);
        AbilityManager abilityManager = mock(AbilityManager.class);
        Player target = mock(Player.class);
        ConsoleCommandSender sender = mock(ConsoleCommandSender.class);
        Command command = mock(Command.class);

        when(plugin.getCommand("skillsmultiplier")).thenReturn(registeredCommand);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getSkillManager()).thenReturn(skillManager);
        when(plugin.getAbilityManager()).thenReturn(abilityManager);
        doReturn(Set.of(target)).when(server).getOnlinePlayers();
        when(target.getName()).thenReturn("Sir_Draco");

        return new CommandTestContext(plugin, skillManager, abilityManager, target, sender, command,
                new SkillsMultiplierCommand(plugin));
    }

    private record CommandTestContext(SurvivalSkills plugin, SkillManager skillManager, AbilityManager abilityManager,
                                      Player target, ConsoleCommandSender sender, Command command,
                                      SkillsMultiplierCommand commandExecutor) {}
}
