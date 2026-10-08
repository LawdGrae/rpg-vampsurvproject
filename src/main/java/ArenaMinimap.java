import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** A fixed HUD view of nearby threats, loot, and the player's facing direction. */
public final class ArenaMinimap {
    private static final double VIEW_SPAN = 1_800.0;
    private static final int PANEL_WIDTH = 172;
    private static final int PANEL_HEIGHT = 205;
    private static final Color ENEMY_COLOR = new Color(242, 105, 106);
    private static final Color BOSS_COLOR = new Color(245, 198, 91);
    private static final Color GEM_COLOR = new Color(117, 231, 167);
    private static final Color HERO_COLOR = new Color(219, 255, 255);

    private final BufferedImage terrainTexture;
    private final int terrainWorldWidth;
    private final int terrainWorldHeight;

    public ArenaMinimap(BufferedImage terrain) {
        terrainWorldWidth = terrain == null ? 1_536 : terrain.getWidth();
        terrainWorldHeight = terrain == null ? 1_024 : terrain.getHeight();
        if (terrain == null) {
            terrainTexture = null;
            return;
        }
        // Keep enough detail for the small HUD without rescaling the full arena every frame.
        int textureWidth = Math.min(256, terrain.getWidth());
        int textureHeight = Math.max(1,
                (int) Math.round(textureWidth * terrain.getHeight() / (double) terrain.getWidth()));
        terrainTexture = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D textureGraphics = terrainTexture.createGraphics();
        try {
            textureGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            textureGraphics.drawImage(terrain, 0, 0, textureWidth, textureHeight, null);
            textureGraphics.setColor(new Color(7, 19, 28, 85));
            textureGraphics.fillRect(0, 0, textureWidth, textureHeight);
        } finally {
            textureGraphics.dispose();
        }
    }

    public static Rectangle bounds(int width, int height) {
        return new Rectangle(Math.max(12, width - 192),
                Math.min(226, Math.max(12, height - PANEL_HEIGHT - 20)),
                PANEL_WIDTH, PANEL_HEIGHT);
    }

    /** Project world coordinates into a centered, north-up map spanning 1,800 world units. */
    public static Point2D.Double project(double worldX, double worldY,
            double playerX, double playerY, Rectangle mapBounds) {
        return new Point2D.Double(
                mapBounds.getCenterX() + (worldX - playerX) * mapBounds.width / VIEW_SPAN,
                mapBounds.getCenterY() + (worldY - playerY) * mapBounds.height / VIEW_SPAN);
    }

    public void draw(Graphics2D graphics, GameLogic logic, int viewportWidth, int viewportHeight) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            Rectangle panel = bounds(viewportWidth, viewportHeight);
            Rectangle map = new Rectangle(panel.x + 10, panel.y + 32, 152, 152);
            drawPanel(g, panel);

            Graphics2D mapGraphics = (Graphics2D) g.create();
            try {
                mapGraphics.clip(new RoundRectangle2D.Double(map.x, map.y,
                        map.width, map.height, 9, 9));
                double playerX = logic.getPlayerWorldX();
                double playerY = logic.getPlayerWorldY();
                drawTerrain(mapGraphics, map, playerX, playerY);
                drawHome(mapGraphics, map, playerX, playerY);

                List<GameLogic.MapMarker> markers = logic.getMapMarkers();
                // Draw collectible markers first so nearby threats remain easy to read.
                drawMarkers(mapGraphics, markers, GameLogic.MapMarkerKind.GEM, map, playerX, playerY);
                drawMarkers(mapGraphics, markers, GameLogic.MapMarkerKind.ENEMY, map, playerX, playerY);
                drawMarkers(mapGraphics, markers, GameLogic.MapMarkerKind.BOSS, map, playerX, playerY);
                drawPlayer(mapGraphics, map, logic.getPlayerFacingX(), logic.getPlayerFacingY());
            } finally {
                mapGraphics.dispose();
            }

            g.setStroke(new BasicStroke(1.0f));
            g.setColor(new Color(126, 192, 190, 91));
            g.draw(new RoundRectangle2D.Double(map.x + 0.5, map.y + 0.5,
                    map.width - 1, map.height - 1, 9, 9));
            drawLegend(g, panel);
        } finally {
            g.dispose();
        }
    }

    private static void drawPanel(Graphics2D g, Rectangle panel) {
        g.setColor(new Color(0, 0, 0, 75));
        g.fillRoundRect(panel.x - 2, panel.y + 4, panel.width + 4, panel.height + 3, 18, 18);
        g.setPaint(new GradientPaint(panel.x, panel.y, new Color(23, 39, 49, 242),
                panel.x, panel.y + panel.height, new Color(8, 17, 25, 240)));
        g.fillRoundRect(panel.x, panel.y, panel.width, panel.height, 14, 14);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(191, 161, 95, 145));
        g.draw(new RoundRectangle2D.Double(panel.x + 0.5, panel.y + 0.5,
                panel.width - 1, panel.height - 1, 14, 14));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g.setColor(new Color(221, 226, 218));
        g.drawString("AREA MAP", panel.x + 12, panel.y + 20);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.setColor(new Color(127, 203, 205));
        g.drawString("N", panel.x + 142, panel.y + 20);
        Path2D northArrow = new Path2D.Double();
        northArrow.moveTo(panel.x + 156, panel.y + 12);
        northArrow.lineTo(panel.x + 159, panel.y + 19);
        northArrow.lineTo(panel.x + 156, panel.y + 17);
        northArrow.lineTo(panel.x + 153, panel.y + 19);
        northArrow.closePath();
        g.fill(northArrow);
    }

    private void drawTerrain(Graphics2D g, Rectangle map, double playerX, double playerY) {
        g.setColor(new Color(17, 33, 39));
        g.fillRect(map.x, map.y, map.width, map.height);
        if (terrainTexture != null) {
            double scaleX = map.width / VIEW_SPAN;
            double scaleY = map.height / VIEW_SPAN;
            // TexturePaint repeats in both directions; spawn is pixel (640, 360) in tile zero.
            double anchorX = map.getCenterX() - (playerX + TerrainLayout.SPAWN_TILE_X) * scaleX;
            double anchorY = map.getCenterY() - (playerY + TerrainLayout.SPAWN_TILE_Y) * scaleY;
            g.setPaint(new TexturePaint(terrainTexture, new Rectangle2D.Double(anchorX, anchorY,
                    terrainWorldWidth * scaleX, terrainWorldHeight * scaleY)));
            g.fillRect(map.x, map.y, map.width, map.height);
        }

        g.setPaint(new GradientPaint(map.x, map.y, new Color(39, 97, 110, 24),
                map.x, map.y + map.height, new Color(1, 9, 20, 68)));
        g.fillRect(map.x, map.y, map.width, map.height);
        g.setColor(new Color(132, 211, 213, 30));
        g.setStroke(new BasicStroke(0.75f));
        g.drawLine(map.x, (int) map.getCenterY(), map.x + map.width, (int) map.getCenterY());
        g.drawLine((int) map.getCenterX(), map.y, (int) map.getCenterX(), map.y + map.height);
        g.draw(new Ellipse2D.Double(map.getCenterX() - 50, map.getCenterY() - 50, 100, 100));
    }

    private static void drawHome(Graphics2D g, Rectangle map, double playerX, double playerY) {
        Point2D.Double home = project(0.0, 0.0, playerX, playerY, map);
        if (!map.contains(home)) {
            return;
        }
        Path2D house = new Path2D.Double();
        house.moveTo(home.x - 4, home.y);
        house.lineTo(home.x, home.y - 4);
        house.lineTo(home.x + 4, home.y);
        house.moveTo(home.x - 3, home.y - 1);
        house.lineTo(home.x - 3, home.y + 4);
        house.lineTo(home.x + 3, home.y + 4);
        house.lineTo(home.x + 3, home.y - 1);
        g.setColor(new Color(160, 212, 221, 160));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(house);
    }

    private static void drawMarkers(Graphics2D g, List<GameLogic.MapMarker> markers,
            GameLogic.MapMarkerKind kind, Rectangle map, double playerX, double playerY) {
        for (GameLogic.MapMarker marker : markers) {
            if (marker.kind() != kind) {
                continue;
            }
            Point2D.Double point = project(marker.worldX(), marker.worldY(), playerX, playerY, map);
            if (!map.contains(point)) {
                continue;
            }
            if (kind == GameLogic.MapMarkerKind.GEM) {
                g.setColor(GEM_COLOR);
                g.fill(new Ellipse2D.Double(point.x - 1.5, point.y - 1.5, 3, 3));
            } else if (kind == GameLogic.MapMarkerKind.BOSS) {
                g.setColor(new Color(245, 198, 91, 45));
                g.fill(new Ellipse2D.Double(point.x - 7, point.y - 7, 14, 14));
                Path2D diamond = diamond(point.x, point.y, 4.2);
                g.setColor(BOSS_COLOR);
                g.fill(diamond);
                g.setColor(new Color(255, 240, 197, 205));
                g.setStroke(new BasicStroke(0.7f));
                g.draw(diamond);
            } else {
                g.setColor(new Color(242, 105, 106, 48));
                g.fill(new Ellipse2D.Double(point.x - 4.5, point.y - 4.5, 9, 9));
                g.setColor(new Color(15, 20, 27, 205));
                g.fill(new Ellipse2D.Double(point.x - 3.0, point.y - 3.0, 6, 6));
                g.setColor(ENEMY_COLOR);
                g.fill(new Ellipse2D.Double(point.x - 2.2, point.y - 2.2, 4.4, 4.4));
            }
        }
    }

    private static void drawPlayer(Graphics2D g, Rectangle map, double facingX, double facingY) {
        double centerX = map.getCenterX();
        double centerY = map.getCenterY();
        double length = Math.hypot(facingX, facingY);
        if (!Double.isFinite(length) || length < 0.0001) {
            facingX = 0.0;
            facingY = -1.0;
            length = 1.0;
        }
        double directionX = facingX / length;
        double directionY = facingY / length;
        double sideX = -directionY;
        double sideY = directionX;
        g.setColor(new Color(83, 225, 239, 30));
        g.fill(new Ellipse2D.Double(centerX - 11, centerY - 11, 22, 22));
        g.setColor(new Color(83, 225, 239, 72));
        g.fill(new Ellipse2D.Double(centerX - 7.5, centerY - 7.5, 15, 15));
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(centerX + directionX * 7, centerY + directionY * 7);
        arrow.lineTo(centerX - directionX * 4.5 + sideX * 4.3,
                centerY - directionY * 4.5 + sideY * 4.3);
        arrow.lineTo(centerX - directionX * 2, centerY - directionY * 2);
        arrow.lineTo(centerX - directionX * 4.5 - sideX * 4.3,
                centerY - directionY * 4.5 - sideY * 4.3);
        arrow.closePath();
        g.setColor(new Color(5, 27, 36));
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(arrow);
        g.setColor(HERO_COLOR);
        g.fill(arrow);
    }

    private static Path2D diamond(double x, double y, double radius) {
        Path2D shape = new Path2D.Double();
        shape.moveTo(x, y - radius);
        shape.lineTo(x + radius, y);
        shape.lineTo(x, y + radius);
        shape.lineTo(x - radius, y);
        shape.closePath();
        return shape;
    }

    private static void drawLegend(Graphics2D g, Rectangle panel) {
        int baseline = panel.y + 197;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
        g.setColor(ENEMY_COLOR);
        g.fillOval(panel.x + 12, baseline - 5, 4, 4);
        g.setColor(new Color(164, 185, 192));
        g.drawString("ENEMY", panel.x + 20, baseline);
        g.setColor(BOSS_COLOR);
        g.fill(diamond(panel.x + 72, baseline - 3, 2.5));
        g.setColor(new Color(164, 185, 192));
        g.drawString("BOSS", panel.x + 79, baseline);
        g.setColor(GEM_COLOR);
        g.fillOval(panel.x + 123, baseline - 5, 4, 4);
        g.setColor(new Color(164, 185, 192));
        g.drawString("LOOT", panel.x + 131, baseline);
    }
}
