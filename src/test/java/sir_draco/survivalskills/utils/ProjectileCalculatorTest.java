package sir_draco.survivalskills.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectileCalculatorTest {

    private static final double ASSERTION_TOLERANCE = 1.0E-6;

    @Test
    void itemProjectileVectorPreservesNormalGroundTwoBlockLaunch() {
        Location start = new Location(null, 0.0D, 1.0D, 0.0D);
        Location end = new Location(null, -2.0D, 0.0D, 0.0D);

        Vector vector = ProjectileCalculator.getItemProjectileVector(start, end, 0.5D);

        assertVectorEquals(new Vector(-0.5152525046424247D, -0.19783643412290752D, 0.0D), vector);
    }

    @Test
    void itemProjectileVectorPreservesNormalGroundFiveBlockLaunch() {
        Location start = new Location(null, 0.0D, 1.0D, 0.0D);
        Location end = new Location(null, -5.0D, 0.0D, 0.0D);

        Vector vector = ProjectileCalculator.getItemProjectileVector(start, end, 0.5D);

        assertVectorEquals(new Vector(-0.5466655793407694D, 0.07359595514766193D, 0.0D), vector);
    }

    @Test
    void itemProjectileVectorPreservesNormalGroundTenBlockLaunch() {
        Location start = new Location(null, 0.0D, 1.0D, 0.0D);
        Location end = new Location(null, -10.0D, 0.0D, 0.0D);

        Vector vector = ProjectileCalculator.getItemProjectileVector(start, end, 0.5D);

        assertVectorEquals(new Vector(-0.6016991474074721D, 0.3384907430965427D, 0.0D), vector);
    }

    @Test
    void livingEntityProjectileVectorPreservesFlatTenBlockLaunch() {
        Location start = new Location(null, 0.0D, 0.0D, 0.0D);
        Location end = new Location(null, -10.0D, 0.0D, 0.0D);

        Vector vector = ProjectileCalculator.getLivingEntityProjectileVector(start, end, 1.0D, true);

        assertVectorEquals(new Vector(-3.7232550415988364D, 0.5353038231066449D, 0.0D), vector);
    }

    @Test
    void livingEntityProjectileVectorPreservesFlatTwentyBlockLaunch() {
        Location start = new Location(null, 0.0D, 0.0D, 0.0D);
        Location end = new Location(null, -20.0D, 0.0D, 0.0D);

        Vector vector = ProjectileCalculator.getLivingEntityProjectileVector(start, end, 1.0D, true);

        assertVectorEquals(new Vector(-3.302673198070298D, 0.79732131567458D, 0.0D), vector);
    }

    @Test
    void livingEntityProjectileVectorPreservesOneBlockUpwardTenBlockLaunch() {
        Location start = new Location(null, 0.0D, 0.0D, 0.0D);
        Location end = new Location(null, -10.0D, 1.0D, 0.0D);

        Vector vector = ProjectileCalculator.getLivingEntityProjectileVector(start, end, 1.0D, true);

        assertVectorEquals(new Vector(-3.8070687020911786D, 0.5429569777469565D, 0.0D), vector);
    }

    @Test
    void projectileVectorsRejectNonPositiveMagnitude() {
        Location start = new Location(null, 0.0D, 0.0D, 0.0D);
        Location end = new Location(null, 1.0D, 0.0D, 0.0D);

        assertThrows(IllegalArgumentException.class,
                () -> ProjectileCalculator.getItemProjectileVector(start, end, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> ProjectileCalculator.getLivingEntityProjectileVector(start, end, 0.0D, true));
    }

    @Test
    void projectileVectorsLogsYDiffWhenHorizontalIsZero() {
        Location start = new Location(null, 0.0D, 0.0D, 0.0D);
        Location end = new Location(null, 0.0D, 2.0D, 0.0D);

        Vector vector = ProjectileCalculator.getLivingEntityProjectileVector(start, end, 1.0D, true);

        assertVectorEquals(new Vector(0, 0.6931471805599453, 0), vector);
    }

    private static void assertVectorEquals(Vector expected, Vector actual) {
        assertEquals(expected.getX(), actual.getX(), ASSERTION_TOLERANCE);
        assertEquals(expected.getY(), actual.getY(), ASSERTION_TOLERANCE);
        assertEquals(expected.getZ(), actual.getZ(), ASSERTION_TOLERANCE);
    }
}
