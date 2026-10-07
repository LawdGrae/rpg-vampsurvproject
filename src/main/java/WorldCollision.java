import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.List;

/** Solid ground footprints in the same repeating tile coordinates as the scenery. */
public final class WorldCollision {
    private static final double MAX_STEP = 4.0;
    private static final int MAX_STEPS = 4096;
    private final int tileWidth;
    private final int tileHeight;
    private final double originTileX;
    private final double originTileY;
    private final List<Shape> obstacles;

    public WorldCollision(int tileWidth, int tileHeight, double originTileX,
            double originTileY, List<Shape> obstacles) {
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException("Collision tile dimensions must be positive");
        }
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.originTileX = originTileX;
        this.originTileY = originTileY;
        this.obstacles = List.copyOf(obstacles);
    }

    private static class TerrainHolder {
        private static final WorldCollision INSTANCE = new WorldCollision(
                TerrainLayout.TILE_WIDTH, TerrainLayout.TILE_HEIGHT,
                TerrainLayout.SPAWN_TILE_X, TerrainLayout.SPAWN_TILE_Y, TerrainLayout.obstacles());
    }

    public static WorldCollision terrain() {
        return TerrainHolder.INSTANCE;
    }

    public boolean isBlocked(double worldX, double worldY, double halfWidth, double halfHeight) {
        double tileX = wrap(worldX + originTileX, tileWidth);
        double tileY = wrap(worldY + originTileY, tileHeight);
        // Check neighbors too: the body may straddle a repeating tile seam.
        for (int column = -1; column <= 1; column++) {
            double x = tileX + column * tileWidth - halfWidth;
            if (x >= tileWidth || x + halfWidth * 2 <= 0) continue;
            for (int row = -1; row <= 1; row++) {
                double y = tileY + row * tileHeight - halfHeight;
                if (y >= tileHeight || y + halfHeight * 2 <= 0) continue;
                for (Shape obstacle : obstacles) {
                    if (obstacle.intersects(x, y, halfWidth * 2, halfHeight * 2)) return true;
                }
            }
        }
        return false;
    }

    /** Small swept steps stop fast dashes at thin walls; separate axes allow wall sliding. */
    public Point2D.Double move(double worldX, double worldY, double dx, double dy,
            double halfWidth, double halfHeight) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) return new Point2D.Double(worldX, worldY);
        double distance = Math.max(Math.abs(dx), Math.abs(dy));
        // Bound pathological input without increasing step size and tunneling through walls.
        if (distance > MAX_STEP * MAX_STEPS) {
            double scale = MAX_STEP * MAX_STEPS / distance;
            dx *= scale;
            dy *= scale;
            distance = MAX_STEP * MAX_STEPS;
        }
        int steps = Math.max(1, (int) Math.ceil(distance / MAX_STEP));
        double stepX = dx / steps;
        double stepY = dy / steps;
        double x = worldX;
        double y = worldY;
        for (int step = 0; step < steps; step++) {
            x = moveAxis(x, y, stepX, true, halfWidth, halfHeight);
            y = moveAxis(x, y, stepY, false, halfWidth, halfHeight);
        }
        return new Point2D.Double(x, y);
    }

    private double moveAxis(double x, double y, double delta, boolean horizontal,
            double halfWidth, double halfHeight) {
        double start = horizontal ? x : y;
        if (delta == 0.0) return start;
        if (!isBlocked(horizontal ? x + delta : x, horizontal ? y : y + delta,
                halfWidth, halfHeight)) return start + delta;
        // Approach the contact instead of leaving a frame-sized gap from the wall.
        double low = 0.0;
        double high = 1.0;
        for (int attempt = 0; attempt < 10; attempt++) {
            double fraction = (low + high) * 0.5;
            if (isBlocked(horizontal ? x + delta * fraction : x,
                    horizontal ? y : y + delta * fraction, halfWidth, halfHeight)) high = fraction;
            else low = fraction;
        }
        return start + delta * low;
    }

    private static double wrap(double value, int size) {
        return value - Math.floor(value / size) * size;
    }
}
