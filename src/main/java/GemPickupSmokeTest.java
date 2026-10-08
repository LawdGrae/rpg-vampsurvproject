import java.lang.reflect.Field;

public class GemPickupSmokeTest {
    public static void main(String[] args) throws Exception {
        attractionDoesNotDependOnFrameRate();
        speedCapDoesNotDependOnFrameRate();
        fastAttractionCollectsDuringTheFrame();
        coastingPickupUsesTheTravelPath();
        invalidUpdatesDoNotCorruptPickup();
        System.out.println("GemPickupSmokeTest passed");
    }

    private static void attractionDoesNotDependOnFrameRate() throws Exception {
        Gem coarse = new Gem(40.0, 0.0);
        Gem fine = new Gem(40.0, 0.0);
        simulate(coarse, 2, 0.1, 100.0);
        simulate(fine, 24, 1.0 / 120.0, 100.0);
        require(!coarse.isCollected() && !fine.isCollected(), "Gems should still be approaching the player");
        near(read(coarse, "worldX"), read(fine, "worldX"), "Attraction distance differs across frame rates");
        near(read(coarse, "velocityX"), read(fine, "velocityX"), "Attraction speed differs across frame rates");
    }

    private static void speedCapDoesNotDependOnFrameRate() throws Exception {
        Gem coarse = new Gem(10000.0, 0.0);
        Gem fine = new Gem(10000.0, 0.0);
        simulate(coarse, 1, 3.0, 20000.0);
        simulate(fine, 360, 1.0 / 120.0, 20000.0);
        near(read(coarse, "worldX"), read(fine, "worldX"), "Travel differs when a frame crosses the speed cap");
        near(1600.0, Math.abs(read(coarse, "velocityX")), "Attraction should reach its speed limit");
        require(read(coarse, "worldX") > 0.0, "The distant gem should not collect prematurely");
    }

    private static void fastAttractionCollectsDuringTheFrame() throws Exception {
        Gem gem = new Gem(40.0, 0.0);
        write(gem, "velocityX", -1600.0);
        gem.update(0.1, 0.0, 0.0);
        require(gem.isCollected(), "A fast attracted gem crossed the player without collecting");
        near(0.0, read(gem, "worldX"), "Collected gem should stop at the player");
        gem.update(1.0, 100.0, 100.0);
        near(0.0, read(gem, "worldX"), "Collected gems should remain stopped");
    }

    private static void coastingPickupUsesTheTravelPath() throws Exception {
        Gem crossing = new Gem(-100.0, 10.0);
        write(crossing, "velocityX", 2000.0);
        crossing.update(0.1, 0.0, 0.0, 20.0);
        require(crossing.isCollected(), "A coasting gem crossed collection range without collecting");

        Gem nearMiss = new Gem(-100.0, 15.0);
        write(nearMiss, "velocityX", 2000.0);
        nearMiss.update(0.1, 0.0, 0.0, 20.0);
        require(!nearMiss.isCollected(), "A path outside collection range must not award XP");
        near(100.0, read(nearMiss, "worldX"), "Uncollected coasting gem should continue moving");
    }

    private static void invalidUpdatesDoNotCorruptPickup() throws Exception {
        Gem gem = new Gem(40.0, 0.0);
        gem.update(Double.NaN, 0.0, 0.0);
        gem.update(-1.0, 0.0, 0.0);
        gem.update(0.1, Double.POSITIVE_INFINITY, 0.0);
        near(40.0, read(gem, "worldX"), "Invalid updates should preserve a gem's position");
        near(0.0, read(gem, "velocityX"), "Invalid updates should preserve a gem's velocity");
        gem.update(0.3, 0.0, 0.0);
        require(gem.isCollected(), "A gem should still collect after an invalid update is ignored");

        Gem stationary = new Gem(0.0, 0.0);
        stationary.update(0.0, 0.0, 0.0);
        require(stationary.isCollected(), "An overlapping gem should collect without movement");
    }

    private static void simulate(Gem gem, int steps, double deltaTime, double pickupRadius) {
        for (int index = 0; index < steps; index++) {
            gem.update(deltaTime, 0.0, 0.0, pickupRadius);
        }
    }

    private static double read(Gem gem, String name) throws Exception {
        Field field = Gem.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getDouble(gem);
    }

    private static void write(Gem gem, String name, double value) throws Exception {
        Field field = Gem.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setDouble(gem, value);
    }

    private static void near(double expected, double actual, String message) {
        require(Double.isFinite(actual) && Math.abs(expected - actual) <= 1.0e-7,
                message + ": expected " + expected + ", actual " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
