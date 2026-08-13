package sir_draco.survivalskills.god_questline.powerore;

/** Tracks the deterministic progress and countdown state for Chicken Herding. */
final class PowerOreChickenHerdingGame {

    static final int TOTAL_CHICKENS = 50;
    static final int DURATION_TICKS = 60 * 20;
    private static final int TICKS_PER_SECOND = 20;

    private int deliveredChickens;
    private int elapsedTicks;

    void deliverChickens(int count) {
        if (count < 0)
            throw new IllegalArgumentException("Delivered chicken count cannot be negative");
        deliveredChickens = Math.min(TOTAL_CHICKENS, deliveredChickens + count);
    }

    void advanceTimer(int ticks) {
        if (ticks < 0)
            throw new IllegalArgumentException("Elapsed ticks cannot be negative");
        elapsedTicks = Math.min(DURATION_TICKS, elapsedTicks + ticks);
    }

    int getChickensRemaining() {
        return TOTAL_CHICKENS - deliveredChickens;
    }

    int getSecondsRemaining() {
        int remainingTicks = DURATION_TICKS - elapsedTicks;
        return (remainingTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
    }

    boolean isComplete() {
        return deliveredChickens == TOTAL_CHICKENS;
    }

    boolean isExpired() {
        return elapsedTicks >= DURATION_TICKS;
    }
}
