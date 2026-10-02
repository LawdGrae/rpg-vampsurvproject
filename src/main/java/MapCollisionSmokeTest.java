import java.awt.geom.Point2D;

public final class MapCollisionSmokeTest {
    private MapCollisionSmokeTest() {
    }

    public static void main(String[] args) {
        GameMap[] maps = {
            new CityMap(), new BeachRuinsMap(), new ArenaMap(), new DessertMap(),
            new SnowMap(), new UndergroundCaveMap(), new HappyMap()
        };
        for (GameMap map : maps) {
            if (map.getWidth() <= 0 || map.getHeight() <= 0) {
                throw new AssertionError(map.getName() + " image did not load");
            }
            if (!map.isBlocked(map.getWidth(), 0.0, 16.0)) {
                throw new AssertionError(map.getName() + " does not block its map edge");
            }
            double maximumCameraOffset = (map.getWidth() - 1280.0) / 2.0;
            if (map.getWidth() > 1280
                    && Math.abs(map.cameraX(-10000.0, 1280) - maximumCameraOffset) > 0.01) {
                throw new AssertionError(map.getName() + " camera did not clamp at its edge");
            }
                Point2D.Double start = map.findWalkablePointNear(0.0, 0.0, 16.0);
                if (map.isBlocked(start.x, start.y, 16.0)) {
                    throw new AssertionError(map.getName() + " has no walkable start position");
                }
        }

        GameMap city = MapCatalog.forRegion(EnemyRegion.RIVENDALE_TOWN);
        if (!(city instanceof CityMap) || city.isBlocked(0.0, 0.0, 16.0)) {
            throw new AssertionError("City starting area should be walkable");
        }
        if (!MapCatalog.forRegion(EnemyRegion.FROSTPEAK_MOUNTAINS).getName().equals("Snow")
                || !MapCatalog.forRegion(EnemyRegion.SHADOWGRAVE_RUINS)
                        .getName().equals("Underground Cave")) {
            throw new AssertionError("Regional map selection is incorrect");
        }

        DessertMap desert = new DessertMap();
        Point2D.Double destination = desert.activatePortal(
                176 - desert.getWidth() / 2.0, 122 - desert.getHeight() / 2.0);
        if (destination == null
                || Math.abs(destination.x - (1138 - desert.getWidth() / 2.0)) > 0.01) {
            throw new AssertionError("Desert portal should teleport the player horizontally");
        }

            for (int index = 0; index < MapCatalog.selectableMapCount(); index++) {
                GameLogic game = new GameLogic();
                game.selectMap(index);
                game.startGame();
                if (game.getActiveMap().isBlocked(game.getPlayerWorldX(),
                        game.getPlayerWorldY(), game.getPlayerCollisionRadius())) {
                    throw new AssertionError(game.getActiveMap().getName()
                            + " starts the player inside a collision at "
                            + game.getPlayerWorldX() + "," + game.getPlayerWorldY());
                }
            }

            ArenaMap arena = new ArenaMap();
            Point2D.Double arenaStart = arena.getStartingPoint(34.0);
            if (arena.isBlocked(arenaStart.x, arenaStart.y, 34.0)) {
                throw new AssertionError("Arena spawn should be outside the logo collision");
            }
            if (arenaStart.y <= 0.0) {
                throw new AssertionError("Arena spawn should be below the central logo");
            }
    }
}
