package sir_draco.survivalskills.rewards;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RewardEffectsTest {

    @Test
    void unknownRewardReturnsNull() {
        assertNull(RewardEffects.getEffect("Mining", "DoesNotExist"));
    }

    @Test
    void unknownTypeReturnsNull() {
        assertNull(RewardEffects.getEffect("Bogus", "FortuneI"));
    }

    // -- Mining effects --

    @Test
    void fortuneEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Mining", "FortuneI"));
        assertNotNull(RewardEffects.getEffect("Mining", "FortuneII"));
        assertNotNull(RewardEffects.getEffect("Mining", "FortuneIII"));
    }

    @Test
    void armorEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Mining", "ArmorI"));
        assertNotNull(RewardEffects.getEffect("Mining", "ArmorII"));
        assertNotNull(RewardEffects.getEffect("Mining", "ArmorIII"));
        assertNotNull(RewardEffects.getEffect("Mining", "ArmorIV"));
    }

    @Test
    void unbreakableToolsEffectExists() {
        assertNotNull(RewardEffects.getEffect("Mining", "UnbreakableTools"));
    }

    // -- Exploring effects --

    @Test
    void swimEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Exploring", "SwimI"));
        assertNotNull(RewardEffects.getEffect("Exploring", "SwimII"));
        assertNotNull(RewardEffects.getEffect("Exploring", "SwimIII"));
        assertNotNull(RewardEffects.getEffect("Exploring", "SwimIV"));
        assertNotNull(RewardEffects.getEffect("Exploring", "SwimV"));
    }

    @Test
    void speedEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Exploring", "SpeedI"));
        assertNotNull(RewardEffects.getEffect("Exploring", "SpeedII"));
        assertNotNull(RewardEffects.getEffect("Exploring", "SpeedIII"));
        assertNotNull(RewardEffects.getEffect("Exploring", "SpeedIV"));
        assertNotNull(RewardEffects.getEffect("Exploring", "SpeedV"));
    }

    // -- Farming effects --

    @Test
    void doubleCropsEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Farming", "DoubleCropsI"));
        assertNotNull(RewardEffects.getEffect("Farming", "DoubleCropsII"));
        assertNotNull(RewardEffects.getEffect("Farming", "DoubleCropsIII"));
        assertNotNull(RewardEffects.getEffect("Farming", "DoubleCropsIV"));
    }

    @Test
    void healthEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Farming", "HealthI"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthII"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthIII"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthIV"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthV"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthVI"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthVII"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthVIII"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthIX"));
        assertNotNull(RewardEffects.getEffect("Farming", "HealthX"));
    }

    // -- Building effects --

    @Test
    void blockReturnEffectsExistForAllLevels() {
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnI"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnII"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnIII"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnIV"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnV"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnVI"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnVII"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnVIII"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnIX"));
        assertNotNull(RewardEffects.getEffect("Building", "BlockReturnX"));
    }

    // -- Fighting effects --

    @Test
    void lifestealEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Fighting", "LifestealI"));
        assertNotNull(RewardEffects.getEffect("Fighting", "LifestealII"));
        assertNotNull(RewardEffects.getEffect("Fighting", "LifestealIII"));
        assertNotNull(RewardEffects.getEffect("Fighting", "LifestealIV"));
        assertNotNull(RewardEffects.getEffect("Fighting", "LifestealV"));
    }

    @Test
    void criticalEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Fighting", "CriticalI"));
        assertNotNull(RewardEffects.getEffect("Fighting", "CriticalII"));
    }

    // -- Fishing effects --

    @Test
    void fishingLootChanceEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Fishing", "CommonLootI"));
        assertNotNull(RewardEffects.getEffect("Fishing", "CommonLootV"));
        assertNotNull(RewardEffects.getEffect("Fishing", "RareLootI"));
        assertNotNull(RewardEffects.getEffect("Fishing", "RareLootV"));
        assertNotNull(RewardEffects.getEffect("Fishing", "EpicLootI"));
        assertNotNull(RewardEffects.getEffect("Fishing", "EpicLootV"));
        assertNotNull(RewardEffects.getEffect("Fishing", "LegendaryLootI"));
        assertNotNull(RewardEffects.getEffect("Fishing", "LegendaryLootIII"));
    }

    @Test
    void experienceEffectsExistForAllLevels() {
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceI"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceII"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceIII"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceIV"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceV"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceVI"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceVII"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceVIII"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceIX"));
        assertNotNull(RewardEffects.getEffect("Fishing", "ExperienceX"));
    }

    @Test
    void fasterFishingEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Fishing", "FasterFishingI"));
        assertNotNull(RewardEffects.getEffect("Fishing", "FasterFishingII"));
        assertNotNull(RewardEffects.getEffect("Fishing", "FasterFishingIII"));
        assertNotNull(RewardEffects.getEffect("Fishing", "FasterFishingIV"));
        assertNotNull(RewardEffects.getEffect("Fishing", "FasterFishingV"));
    }

    // -- Crafting effects --

    @Test
    void extraOutputEffectsExistForAllLevels() {
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputI"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputII"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputIII"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputIV"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputV"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputVI"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputVII"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputVIII"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputIX"));
        assertNotNull(RewardEffects.getEffect("Crafting", "ExtraOutputX"));
    }

    @Test
    void materialsBackEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Crafting", "MaterialsBackI"));
        assertNotNull(RewardEffects.getEffect("Crafting", "MaterialsBackII"));
        assertNotNull(RewardEffects.getEffect("Crafting", "MaterialsBackIII"));
        assertNotNull(RewardEffects.getEffect("Crafting", "MaterialsBackIV"));
        assertNotNull(RewardEffects.getEffect("Crafting", "MaterialsBackV"));
    }

    // -- Main effects --

    @Test
    void setHomeEffectsExist() {
        assertNotNull(RewardEffects.getEffect("Main", "SetHomeI"));
        assertNotNull(RewardEffects.getEffect("Main", "SetHomeII"));
        assertNotNull(RewardEffects.getEffect("Main", "SetHomeIII"));
        assertNotNull(RewardEffects.getEffect("Main", "SetHomeIV"));
    }

    // -- Edge cases and notifications-only rewards (no effect expected) --

    @Test
    void notificationOnlyRewardsReturnNull() {
        assertNull(RewardEffects.getEffect("Mining", "PowerOre"));
        assertNull(RewardEffects.getEffect("Exploring", "CaveFinder"));
        assertNull(RewardEffects.getEffect("Farming", "Harvester"));
        assertNull(RewardEffects.getEffect("Building", "AutoSortWand"));
        assertNull(RewardEffects.getEffect("Fighting", "GiantSummon"));
        assertNull(RewardEffects.getEffect("Fishing", "WaterBreathingI"));
        assertNull(RewardEffects.getEffect("Crafting", "EnchantedGapple"));
        assertNull(RewardEffects.getEffect("Main", "Gravestone"));
        assertNull(RewardEffects.getEffect("Main", "DustTrail"));
    }

    // -- Count of all registered effects to prevent accidental omissions --

    @Test
    void registeredEffectCountIsCorrect() {
        // This should match the total number of state/attribute rewards defined in RewardEffects.buildEffects()
        int mining = 3 + 4 + 1 + 1;         // Fortune(3) + Armor(4) + Unbreakable(1) + VeinMinerII(1)
        int exploring = 5 + 5;               // Swim(5) + Speed(5)
        int farming = 4 + 10 + 1;            // DoubleCrops(4) + Health(10) + Timberman(1)
        int building = 10 + 1;               // BlockReturn(10) + ExtendedReach(1)
        int fighting = 5 + 2 + 1;            // Lifesteal(5) + Critical(2) + BloodyDomain(1)
        int fishing = 5 + 5 + 5 + 3 + 10 + 5 + 1; // Common(5) + Rare(5) + Epic(5) + Legendary(3) + Exp(10) + FF(5) + AutoTrashII(1)
        int crafting = 10 + 5;               // ExtraOutput(10) + MaterialsBack(5)
        int main = 4;                        // SetHome(4)

        int expected = mining + exploring + farming + building + fighting + fishing + crafting + main;
        assertEquals(expected, RewardEffects.getRegisteredEffectKeys().size(),
                "Effect count mismatch — if you added or removed rewards, update this test");
    }
}
