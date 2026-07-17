package sir_draco.survivalskills.rewards;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RewardDataTest {

    @Test
    void allSkillTypesAreRegistered() {
        var rewards = RewardData.getRegisteredRewards();
        assertTrue(rewards.containsKey("Mining"));
        assertTrue(rewards.containsKey("Exploring"));
        assertTrue(rewards.containsKey("Farming"));
        assertTrue(rewards.containsKey("Building"));
        assertTrue(rewards.containsKey("Fighting"));
        assertTrue(rewards.containsKey("Fishing"));
        assertTrue(rewards.containsKey("Crafting"));
        assertTrue(rewards.containsKey("Main"));
    }

    @Test
    void everySkillHasRewards() {
        var rewards = RewardData.getRegisteredRewards();
        for (var entry : rewards.entrySet())
            assertFalse(entry.getValue().isEmpty(), entry.getKey() + " has no rewards");
    }

    @Test
    void everyRewardHasNotification() {
        var rewards = RewardData.getRegisteredRewards();
        for (var typeEntry : rewards.entrySet()) {
            String type = typeEntry.getKey();
            for (String reward : typeEntry.getValue()) {
                var notif = RewardData.getNotification(type, reward);
                assertNotNull(notif, type + ":" + reward + " notification is null");
            }
        }
    }

    @Test
    void everyRewardHasAtLeastOneNotificationLine() {
        var rewards = RewardData.getRegisteredRewards();
        for (var typeEntry : rewards.entrySet()) {
            String type = typeEntry.getKey();
            for (String reward : typeEntry.getValue()) {
                var notif = RewardData.getNotification(type, reward);
                assertFalse(notif.isEmpty(), type + ":" + reward + " notification is empty");
            }
        }
    }

    @Test
    void everyRewardHasDescription() {
        var rewards = RewardData.getRegisteredRewards();
        for (var typeEntry : rewards.entrySet()) {
            String type = typeEntry.getKey();
            for (String reward : typeEntry.getValue()) {
                String desc = RewardData.getDescription(type, reward);
                assertNotNull(desc, type + ":" + reward + " description is null");
                assertFalse(desc.isEmpty(), type + ":" + reward + " description is empty");
            }
        }
    }

    @Test
    void noUnsubstitutedPlaceholdersInNotifications() {
        var rewards = RewardData.getRegisteredRewards();
        for (var typeEntry : rewards.entrySet()) {
            String type = typeEntry.getKey();
            for (String reward : typeEntry.getValue()) {
                var notif = RewardData.getNotification(type, reward);
                for (String line : notif)
                    assertFalse(line.contains("{value}"),
                            type + ":" + reward + " notification contains {value}");
            }
        }
    }

    @Test
    void noUnsubstitutedPlaceholdersInDescriptions() {
        var rewards = RewardData.getRegisteredRewards();
        for (var typeEntry : rewards.entrySet()) {
            String type = typeEntry.getKey();
            for (String reward : typeEntry.getValue()) {
                String desc = RewardData.getDescription(type, reward);
                assertFalse(desc.contains("{value}"),
                        type + ":" + reward + " description contains {value}");
            }
        }
    }

    @Test
    void unknownRewardReturnsNullNotification() {
        assertNull(RewardData.getNotification("Mining", "DoesNotExist"));
    }

    @Test
    void unknownRewardReturnsEmptyDescription() {
        assertEquals("", RewardData.getDescription("Mining", "DoesNotExist"));
    }

    @Test
    void unknownTypeReturnsNullNotification() {
        assertNull(RewardData.getNotification("Bogus", "FortuneI"));
    }

    @Test
    void unknownTypeReturnsEmptyDescription() {
        assertEquals("", RewardData.getDescription("Bogus", "FortuneI"));
    }

    // -- Spot checks: Mining --

    @Test
    void fortuneINotificationContainsPercent() {
        var notif = RewardData.getNotification("Mining", "FortuneI");
        assertNotNull(notif);
        assertFalse(notif.isEmpty());
        assertTrue(notif.get(0).contains("20%"),
                "FortuneI should mention 20%, got: " + notif.get(0));
    }

    @Test
    void fortuneIINotificationContainsPercent() {
        var notif = RewardData.getNotification("Mining", "FortuneII");
        assertNotNull(notif);
        assertTrue(notif.get(0).contains("40%"),
                "FortuneII should mention 40%, got: " + notif.get(0));
    }

    @Test
    void fortuneIIINotificationContainsPercent() {
        var notif = RewardData.getNotification("Mining", "FortuneIII");
        assertNotNull(notif);
        assertTrue(notif.get(0).contains("50%"),
                "FortuneIII should mention 50%, got: " + notif.get(0));
    }

    @Test
    void powerOreNotificationContainsPowerOre() {
        var notif = RewardData.getNotification("Mining", "PowerOre");
        assertNotNull(notif);
        assertFalse(notif.isEmpty());
        assertTrue(notif.get(0).contains("Power Ore"),
                "PowerOre should mention 'Power Ore', got: " + notif.get(0));
    }

    @Test
    void powerOreNotificationUsesLightPurple() {
        var notif = RewardData.getNotification("Mining", "PowerOre");
        assertNotNull(notif);
        assertTrue(notif.get(0).contains(ChatColor.LIGHT_PURPLE.toString()),
                "PowerOre should use LIGHT_PURPLE, got: " + notif.get(0));
    }

    @Test
    void unlimitedTorchHasTwoNotificationLines() {
        var notif = RewardData.getNotification("Mining", "UnlimitedTorch");
        assertNotNull(notif);
        assertEquals(2, notif.size(), "UnlimitedTorch should have 2 notification lines");
    }

    @Test
    void unlimitedTorchDescriptionIsCraftableItem() {
        String desc = RewardData.getDescription("Mining", "UnlimitedTorch");
        assertTrue(desc.contains("Craftable Item"), "UnlimitedTorch description should mention Craftable Item");
    }

    @Test
    void unbreakableToolsNotification() {
        var notif = RewardData.getNotification("Mining", "UnbreakableTools");
        assertNotNull(notif);
        assertTrue(notif.get(0).contains("never break"),
                "UnbreakableTools notification should mention 'never break'");
    }

    // -- Spot checks: Farming --

    @Test
    void healthHeartsIncreaseByLevel() {
        var h1 = RewardData.getNotification("Farming", "HealthI");
        var h10 = RewardData.getNotification("Farming", "HealthX");
        assertNotNull(h1);
        assertNotNull(h10);
        assertTrue(h1.get(0).contains("11"), "HealthI should show 11 hearts, got: " + h1.get(0));
        assertTrue(h10.get(0).contains("20"), "HealthX should show 20 hearts, got: " + h10.get(0));
    }

    @Test
    void doubleCropsPercentages() {
        var dc1 = RewardData.getNotification("Farming", "DoubleCropsI");
        var dc4 = RewardData.getNotification("Farming", "DoubleCropsIV");
        assertNotNull(dc1);
        assertNotNull(dc4);
        assertTrue(dc1.get(0).contains("25%"), "DoubleCropsI should show 25%");
        assertTrue(dc4.get(0).contains("100%"), "DoubleCropsIV should show 100%");
    }

    @Test
    void timbermanIsNotRegisteredWhenPluginIsUnavailable() {
        var notif = RewardData.getNotification("Farming", "Timberman");
        assertNull(notif);
        assertFalse(RewardData.getRegisteredRewards().get("Farming").contains("Timberman"));
    }

    // -- Spot checks: Building --

    @Test
    void blockReturnIsLinear() {
        var br1 = RewardData.getNotification("Building", "BlockReturnI");
        var br10 = RewardData.getNotification("Building", "BlockReturnX");
        assertNotNull(br1);
        assertNotNull(br10);
        assertTrue(br1.get(0).contains("5%"), "BlockReturnI should have 5%");
        assertTrue(br10.get(0).contains("50%"), "BlockReturnX should have 50%");
    }

    @Test
    void extendedReachNotification() {
        var notif = RewardData.getNotification("Building", "ExtendedReach");
        assertNotNull(notif);
        assertTrue(notif.get(0).contains("reach"),
                "ExtendedReach should mention 'reach'");
    }

    // -- Spot checks: Fighting --

    @Test
    void lifestealIsTiered() {
        var l1 = RewardData.getNotification("Fighting", "LifestealI");
        var l5 = RewardData.getNotification("Fighting", "LifestealV");
        assertNotNull(l1);
        assertNotNull(l5);
        assertTrue(l1.get(0).contains("5%"), "LifestealI should have 5%");
        assertTrue(l5.get(0).contains("25%"), "LifestealV should have 25%");
    }

    // -- Spot checks: Fishing --

    @Test
    void epicLootNonLinearPercentages() {
        var e1 = RewardData.getNotification("Fishing", "EpicLootI");
        var e4 = RewardData.getNotification("Fishing", "EpicLootIV");
        assertNotNull(e1);
        assertNotNull(e4);
        assertTrue(e1.get(0).contains("0.5%"), "EpicLootI should have 0.5%");
        assertTrue(e4.get(0).contains("2%"), "EpicLootIV should have 2%");
    }

    @Test
    void experienceIsLinear() {
        var e1 = RewardData.getNotification("Fishing", "ExperienceI");
        var e10 = RewardData.getNotification("Fishing", "ExperienceX");
        assertNotNull(e1);
        assertNotNull(e10);
        assertTrue(e1.get(0).contains("10%"), "ExperienceI should have 10%");
        assertTrue(e10.get(0).contains("100%"), "ExperienceX should have 100%");
    }

    @Test
    void fasterFishingSharesMessages() {
        var n2 = RewardData.getNotification("Fishing", "FasterFishingII");
        var n3 = RewardData.getNotification("Fishing", "FasterFishingIII");
        var n4 = RewardData.getNotification("Fishing", "FasterFishingIV");
        assertNotNull(n2);
        assertNotNull(n3);
        assertNotNull(n4);
        assertEquals(n2.get(0), n3.get(0));
        assertEquals(n3.get(0), n4.get(0));
    }

    // -- Spot checks: Crafting --

    @Test
    void extraOutputIsLinear() {
        var e1 = RewardData.getNotification("Crafting", "ExtraOutputI");
        var e10 = RewardData.getNotification("Crafting", "ExtraOutputX");
        assertNotNull(e1);
        assertNotNull(e10);
        assertTrue(e1.get(0).contains("5%"), "ExtraOutputI should have 5%");
        assertTrue(e10.get(0).contains("50%"), "ExtraOutputX should have 50%");
    }

    @Test
    void materialsBackIsLinear() {
        var m1 = RewardData.getNotification("Crafting", "MaterialsBackI");
        var m5 = RewardData.getNotification("Crafting", "MaterialsBackV");
        assertNotNull(m1);
        assertNotNull(m5);
        assertTrue(m1.get(0).contains("10%"), "MaterialsBackI should have 10%");
        assertTrue(m5.get(0).contains("50%"), "MaterialsBackV should have 50%");
    }

    @Test
    void itemTransferPipesDescribeCraftingAndUse() {
        List<String> notification = RewardData.getNotification("Crafting", "ItemTransferPipes");
        assertNotNull(notification);
        assertTrue(notification.get(0).contains("craft and use Item Transfer Pipes"));
        assertTrue(RewardData.getDescription("Crafting", "ItemTransferPipes").contains("transfer items"));
    }

    // -- Spot checks: Main --

    @Test
    void setHomeIncrements() {
        var h1 = RewardData.getNotification("Main", "SetHomeI");
        var h4 = RewardData.getNotification("Main", "SetHomeIV");
        assertNotNull(h1);
        assertNotNull(h4);
        String s1 = ChatColor.stripColor(h1.get(0));
        String s4 = ChatColor.stripColor(h4.get(0));
        assertTrue(s1.contains("2 homes"), "SetHomeI should say '2 homes', got: " + s1);
        assertTrue(s4.contains("5 homes"), "SetHomeIV should say '5 homes', got: " + s4);
    }

    @Test
    void gravestoneNotification() {
        var notif = RewardData.getNotification("Main", "Gravestone");
        assertNotNull(notif);
        assertTrue(notif.get(0).contains("chest"), "Gravestone should mention 'chest'");
    }

    @Test
    void keepInventoryNotification() {
        var notif = RewardData.getNotification("Main", "KeepInventory");
        assertNotNull(notif);
        assertTrue(notif.get(0).contains("keep"), "KeepInventory should mention 'keep'");
    }

    // -- Trail tests --

    @Test
    void trailNotificationsAlwaysHaveTwoLines() {
        var rewards = RewardData.getRegisteredRewards();
        for (String reward : rewards.get("Main")) {
            if (reward.endsWith("Trail") && !reward.equals("SetHomeTrail")) {
                var notif = RewardData.getNotification("Main", reward);
                assertNotNull(notif);
                assertEquals(2, notif.size(), reward + " should have 2 notification lines");
                assertTrue(notif.get(0).contains("unlocked"), reward + " first line should mention 'unlocked'");
                assertTrue(notif.get(1).contains("/toggletrail"), reward + " second line should mention /toggletrail");
                assertTrue(notif.get(0).contains(ChatColor.DARK_BLUE.toString()), reward + " should use DARK_BLUE");
            }
        }
    }

    @Test
    void trailDescriptionsContainParticles() {
        String desc = RewardData.getDescription("Main", "DustTrail");
        assertNotNull(desc);
        assertFalse(desc.isEmpty());
        assertTrue(desc.contains("particles"), "DustTrail description should mention 'particles'");
    }

    // -- Color consistency --

    @Test
    void notificationUsesCorrectColorBody() {
        var rewards = RewardData.getRegisteredRewards();
        for (var typeEntry : rewards.entrySet()) {
            String type = typeEntry.getKey();
            for (String reward : typeEntry.getValue()) {
                if (reward.contains("Trail")) continue;
                var notif = RewardData.getNotification(type, reward);
                if (notif == null || notif.isEmpty()) continue;
                String firstLine = notif.get(0);
                // All notifications should start with Light Purple or Yellow (this is the convention)
                boolean startsValid = firstLine.startsWith(ChatColor.LIGHT_PURPLE.toString())
                        || firstLine.startsWith(ChatColor.YELLOW.toString());
                assertTrue(startsValid,
                        type + ":" + reward + " should start with LIGHT_PURPLE or YELLOW, got: " + firstLine);
            }
        }
    }

    @Test
    void descriptionUsesCorrectColorBody() {
        var rewards = RewardData.getRegisteredRewards();
        for (var typeEntry : rewards.entrySet()) {
            String type = typeEntry.getKey();
            for (String reward : typeEntry.getValue()) {
                String desc = RewardData.getDescription(type, reward);
                if (desc == null || desc.isEmpty()) continue;
                boolean startsValid = desc.startsWith(ChatColor.GRAY.toString());
                assertTrue(startsValid,
                        type + ":" + reward + " description should start with GRAY, got: " + desc);
            }
        }
    }
}
