import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** A finite, image-backed map with map-local collision geometry. */
public abstract class GameMap {
    private static final int COLLISION_SAMPLES = 12;
    private static final double DESIGN_WIDTH = 1536.0;
    private static final double DESIGN_HEIGHT = 1024.0;
    private static final double COLLISION_FOOTPRINT_SCALE = 0.62;

    private final String name;
    private final BufferedImage image;
    private final List<Rectangle2D.Double> blockers = new ArrayList<>();
    private final List<Rectangle2D.Double> terrainPassages = new ArrayList<>();
    private final List<Portal> portals = new ArrayList<>();
    private Point2D.Double startingPoint;
    private BufferedImage monochromeImage;
    private boolean waterIsBlocked;
    private boolean purpleGapsAreBlocked;
    private boolean darkGapsAreBlocked;
    private boolean grayscale;
    private long portalCooldownUntil;

    protected GameMap(String name, String assetPath) {
        this.name = name;
        this.image = ResourceLoader.loadImage(assetPath);
    }

    protected final void block(int x, int y, int width, int height) {
        double scaleX = getWidth() / DESIGN_WIDTH;
        double scaleY = getHeight() / DESIGN_HEIGHT;
        double scaledWidth = width * scaleX * COLLISION_FOOTPRINT_SCALE;
        double scaledHeight = height * scaleY * COLLISION_FOOTPRINT_SCALE;
        blockers.add(new Rectangle2D.Double(
            x * scaleX + (width * scaleX - scaledWidth) / 2.0,
            y * scaleY + (height * scaleY - scaledHeight) / 2.0,
            scaledWidth, scaledHeight));
    }

    protected final void blockWater() {
        waterIsBlocked = true;
    }

    protected final void blockGlowingPurpleGaps() {
        purpleGapsAreBlocked = true;
    }

    protected final void blockDarkGaps() {
        darkGapsAreBlocked = true;
    }

    protected final void keepTerrainPassage(int x, int y, int width, int height) {
        double scaleX = getWidth() / DESIGN_WIDTH;
        double scaleY = getHeight() / DESIGN_HEIGHT;
        terrainPassages.add(new Rectangle2D.Double(x * scaleX, y * scaleY,
            width * scaleX, height * scaleY));
    }

    protected final void setStartingPoint(int x, int y) {
        startingPoint = new Point2D.Double(
                x * getWidth() / DESIGN_WIDTH - getWidth() / 2.0,
                y * getHeight() / DESIGN_HEIGHT - getHeight() / 2.0);
    }

    protected final void useGrayscale() {
        grayscale = true;
    }

    protected final void addHorizontalPortal(int x1, int y1, int x2, int y2, int radius) {
        portals.add(new Portal((int) Math.round(x1 * getWidth() / DESIGN_WIDTH),
            (int) Math.round(y1 * getHeight() / DESIGN_HEIGHT),
            (int) Math.round(x2 * getWidth() / DESIGN_WIDTH),
            (int) Math.round(y2 * getHeight() / DESIGN_HEIGHT),
            (int) Math.round(radius * Math.min(getWidth() / DESIGN_WIDTH,
                getHeight() / DESIGN_HEIGHT))));
    }

    public final String getName() {
        return name;
    }

    public final BufferedImage getImage() {
        return image;
    }

    public final int getWidth() {
        return image.getWidth();
    }

    public final int getHeight() {
        return image.getHeight();
    }

    public final boolean isGrayscale() {
        return grayscale;
    }

    public final Point2D.Double getStartingPoint(double radius) {
        Point2D.Double requested = startingPoint == null
                ? new Point2D.Double(0.0, 0.0) : startingPoint;
        return findWalkablePointNear(requested.x, requested.y, radius);
    }

    public final double cameraX(double playerWorldX, int viewportWidth) {
        return cameraOffset(playerWorldX, getWidth(), viewportWidth);
    }

    public final double cameraY(double playerWorldY, int viewportHeight) {
        return cameraOffset(playerWorldY, getHeight(), viewportHeight);
    }

    private double cameraOffset(double playerPosition, int mapSize, int viewportSize) {
        if (mapSize <= viewportSize) {
            return 0.0;
        }
        double halfDifference = (mapSize - viewportSize) / 2.0;
        return Math.max(-halfDifference, Math.min(halfDifference, -playerPosition));
    }

    public final void draw(Graphics2D graphics, int viewportWidth, int viewportHeight,
            double cameraX, double cameraY) {
        int left = (int) Math.round((viewportWidth - getWidth()) / 2.0 + cameraX);
        int top = (int) Math.round((viewportHeight - getHeight()) / 2.0 + cameraY);
        if (!grayscale) {
            graphics.drawImage(image, left, top, null);
            return;
        }
        if (monochromeImage == null) {
            monochromeImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < getHeight(); y++) {
                for (int x = 0; x < getWidth(); x++) {
                    Color source = new Color(image.getRGB(x, y), true);
                    int gray = (int) Math.round(source.getRed() * 0.299
                            + source.getGreen() * 0.587 + source.getBlue() * 0.114);
                    monochromeImage.setRGB(x, y, new Color(gray, gray, gray).getRGB());
                }
            }
        }
        graphics.drawImage(monochromeImage, left, top, null);
    }

    public final boolean isBlocked(double worldX, double worldY, double radius) {
        double collisionRadius = radius * COLLISION_FOOTPRINT_SCALE;
        double mapX = worldX + getWidth() / 2.0;
        double mapY = worldY + getHeight() / 2.0;
        if (mapX < collisionRadius || mapY < collisionRadius
            || mapX > getWidth() - collisionRadius
            || mapY > getHeight() - collisionRadius) {
            return true;
        }

        for (Rectangle2D.Double blocker : blockers) {
            Rectangle2D expanded = new Rectangle2D.Double(
                    blocker.x - collisionRadius, blocker.y - collisionRadius,
                    blocker.width + collisionRadius * 2.0,
                    blocker.height + collisionRadius * 2.0);
            if (expanded.contains(mapX, mapY)) {
                return true;
            }
        }

        if (waterIsBlocked || purpleGapsAreBlocked || darkGapsAreBlocked) {
            for (int sample = 0; sample < COLLISION_SAMPLES; sample++) {
                double angle = Math.PI * 2.0 * sample / COLLISION_SAMPLES;
                if (isTerrainBlocked(mapX + Math.cos(angle) * collisionRadius,
                    mapY + Math.sin(angle) * collisionRadius)) {
                    return true;
                }
            }
            return isTerrainBlocked(mapX, mapY);
        }
        return false;
    }

    private boolean isTerrainBlocked(double x, double y) {
        int pixelX = Math.max(0, Math.min(getWidth() - 1, (int) x));
        int pixelY = Math.max(0, Math.min(getHeight() - 1, (int) y));
        for (Rectangle2D.Double passage : terrainPassages) {
            if (passage.contains(pixelX, pixelY)) {
                return false;
            }
        }
        Color pixel = new Color(image.getRGB(pixelX, pixelY), true);
        boolean deepBlueWater = pixel.getBlue() > 85
                && pixel.getBlue() > pixel.getRed() * 1.18
                && pixel.getBlue() > pixel.getGreen() * 1.04;
        boolean glowingPurpleGap = pixel.getRed() > 65 && pixel.getBlue() > 100
                && pixel.getBlue() > pixel.getGreen() * 1.45
                && pixel.getRed() > pixel.getGreen() * 1.2;
        boolean darkGap = (pixel.getRed() + pixel.getGreen() + pixel.getBlue()) / 3 < 24;
        return (waterIsBlocked && deepBlueWater)
            || (purpleGapsAreBlocked && glowingPurpleGap)
            || (darkGapsAreBlocked && darkGap);
    }

    public final void resolveMovement(double oldX, double oldY,
            double newX, double newY, double radius, Player player) {
        if (isBlocked(oldX, oldY, radius)) {
            Point2D.Double safeStart = findWalkablePointNear(oldX, oldY, radius);
            newX += safeStart.x - oldX;
            newY += safeStart.y - oldY;
            oldX = safeStart.x;
            oldY = safeStart.y;
        }
        Point2D.Double resolved = resolveMovement(oldX, oldY, newX, newY, radius);
        newX = resolved.x;
        newY = resolved.y;
        if (newX != player.getWorldX() || newY != player.getWorldY()) {
            player.moveWorld(newX - player.getWorldX(), newY - player.getWorldY());
        }
    }

    public final Point2D.Double resolveMovement(double oldX, double oldY,
            double newX, double newY, double radius) {
        if (isBlocked(oldX, oldY, radius)) {
            Point2D.Double safeStart = findWalkablePointNear(oldX, oldY, radius);
            newX += safeStart.x - oldX;
            newY += safeStart.y - oldY;
            oldX = safeStart.x;
            oldY = safeStart.y;
        }
        if (isBlocked(newX, oldY, radius)) {
            newX = oldX;
        }
        if (isBlocked(newX, newY, radius)) {
            newY = oldY;
        }
        return new Point2D.Double(newX, newY);
    }

    public final Point2D.Double findSpawnPoint(double playerX, double playerY,
            double minimumDistance, double radius, Random random) {
        Point2D.Double bestCandidate = null;
        double bestDistance = -1.0;
        for (int attempt = 0; attempt < 80; attempt++) {
            double x = (random.nextDouble() - 0.5) * (getWidth() - 2.0 * radius);
            double y = (random.nextDouble() - 0.5) * (getHeight() - 2.0 * radius);
            double distance = Point2D.distanceSq(x, y, playerX, playerY);
            if (!isBlocked(x, y, radius) && distance > bestDistance) {
                bestDistance = distance;
                bestCandidate = new Point2D.Double(x, y);
            }
            if (distance >= minimumDistance * minimumDistance && !isBlocked(x, y, radius)) {
                return new Point2D.Double(x, y);
            }
        }
        if (bestCandidate != null) {
            return bestCandidate;
        }
        double direction = random.nextDouble() * Math.PI * 2.0;
        double x = clamp(playerX + Math.cos(direction) * minimumDistance,
                -getWidth() / 2.0 + radius, getWidth() / 2.0 - radius);
        double y = clamp(playerY + Math.sin(direction) * minimumDistance,
                -getHeight() / 2.0 + radius, getHeight() / 2.0 - radius);
        return isBlocked(x, y, radius) ? new Point2D.Double(playerX, playerY)
                : new Point2D.Double(x, y);
    }

    public final Point2D.Double findWalkablePointNear(double worldX, double worldY,
            double radius) {
        if (!isBlocked(worldX, worldY, radius)) {
            return new Point2D.Double(worldX, worldY);
        }
        double maximumRadius = Math.max(getWidth(), getHeight()) / 2.0;
        for (double searchRadius = 16.0; searchRadius < maximumRadius; searchRadius += 16.0) {
            int samples = Math.max(8, (int) Math.ceil(Math.PI * 2.0 * searchRadius / 16.0));
            for (int sample = 0; sample < samples; sample++) {
                double angle = Math.PI * 2.0 * sample / samples;
                double x = worldX + Math.cos(angle) * searchRadius;
                double y = worldY + Math.sin(angle) * searchRadius;
                if (!isBlocked(x, y, radius)) {
                    return new Point2D.Double(x, y);
                }
            }
        }
        return findSpawnPoint(worldX, worldY, 0.0, radius, new Random());
    }

    public final Point2D.Double activatePortal(double worldX, double worldY) {
        long now = System.nanoTime();
        if (now < portalCooldownUntil) {
            return null;
        }
        double mapX = worldX + getWidth() / 2.0;
        double mapY = worldY + getHeight() / 2.0;
        for (Portal portal : portals) {
            Point2D.Double destination = portal.destination(mapX, mapY);
            if (destination != null) {
                portalCooldownUntil = now + 900_000_000L;
                return new Point2D.Double(destination.x - getWidth() / 2.0,
                        destination.y - getHeight() / 2.0);
            }
        }
        return null;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Portal(int x1, int y1, int x2, int y2, int radius) {
        private Point2D.Double destination(double x, double y) {
            if (Point2D.distanceSq(x, y, x1, y1) <= radius * radius) {
                return new Point2D.Double(x2, y2);
            }
            if (Point2D.distanceSq(x, y, x2, y2) <= radius * radius) {
                return new Point2D.Double(x1, y1);
            }
            return null;
        }
    }
}