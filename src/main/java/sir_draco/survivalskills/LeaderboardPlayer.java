package sir_draco.survivalskills;

public class LeaderboardPlayer {

    private final String name;

    private int score;
    private int buildingScore;
    private int miningScore;
    private int farmingScore;
    private int fightingScore;
    private int fishingScore;
    private int exploringScore;
    private int craftingScore;
    private int mainScore;
    private int deathScore;

    public LeaderboardPlayer(String name, int score, int buildingScore, int miningScore, int farmingScore,
                             int fightingScore, int fishingScore, int exploringScore, int craftingScore, int mainScore, int deathScore) {
        this.name = name;
        this.score = score;
        this.buildingScore = buildingScore;
        this.miningScore = miningScore;
        this.farmingScore = farmingScore;
        this.fightingScore = fightingScore;
        this.fishingScore = fishingScore;
        this.exploringScore = exploringScore;
        this.craftingScore = craftingScore;
        this.mainScore = mainScore;
        this.deathScore = deathScore;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getBuildingScore() {
        return buildingScore;
    }

    public void setBuildingScore(int buildingScore) {
        this.buildingScore = buildingScore;
    }

    public int getMiningScore() {
        return miningScore;
    }

    public void setMiningScore(int miningScore) {
        this.miningScore = miningScore;
    }

    public int getFarmingScore() {
        return farmingScore;
    }

    public void setFarmingScore(int farmingScore) {
        this.farmingScore = farmingScore;
    }

    public int getFightingScore() {
        return fightingScore;
    }

    public void setFightingScore(int fightingScore) {
        this.fightingScore = fightingScore;
    }

    public int getFishingScore() {
        return fishingScore;
    }

    public void setFishingScore(int fishingScore) {
        this.fishingScore = fishingScore;
    }

    public int getExploringScore() {
        return exploringScore;
    }

    public void setExploringScore(int exploringScore) {
        this.exploringScore = exploringScore;
    }

    public int getCraftingScore() {
        return craftingScore;
    }

    public void setCraftingScore(int craftingScore) {
        this.craftingScore = craftingScore;
    }

    public int getMainScore() {
        return mainScore;
    }

    public void setMainScore(int mainScore) {
        this.mainScore = mainScore;
    }

    public int getDeathScore() {
        return deathScore;
    }

    public void setDeathScore(int deathScore) {
        this.deathScore = deathScore;
    }
}
