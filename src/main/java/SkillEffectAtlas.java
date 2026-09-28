import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillEffectAtlas {
    private static final String SHEET_PATH = "/main/resources/effects/skill_effect_sheet.png";
    private static final int BASE_WIDTH = 1536;
    private static final int BASE_HEIGHT = 1024;
    private static final Map<String, SkillAnimation> ANIMATIONS = loadAnimations();

    private SkillEffectAtlas() {
    }

    public static SkillAnimation getAnimation(String abilityId) {
        return ANIMATIONS.get(abilityId);
    }

    public static boolean hasAtlas() {
        return !ANIMATIONS.isEmpty();
    }

    private static Map<String, SkillAnimation> loadAnimations() {
        BufferedImage sheet;
        try {
            sheet = ResourceLoader.loadImage(SHEET_PATH);
        } catch (IllegalStateException exception) {
            return Collections.emptyMap();
        }

        Map<String, SkillAnimation> animations = new HashMap<>();

        define(animations, sheet, "heavy_slash", rect(210, 42, 306, 198), 3, 2, 6,
                AnchorMode.BETWEEN, 1.05, true, 0.48, 0.55);
        define(animations, sheet, "shield_bash", rect(522, 42, 270, 198), 3, 2, 6,
                AnchorMode.TARGET, 0.82, true, 0.48, 0.58);
        define(animations, sheet, "earth_shatter", rect(806, 42, 266, 198), 3, 2, 6,
                AnchorMode.TARGET, 1.05, false, 0.5, 0.72);
        define(animations, sheet, "knights_wrath", rect(1080, 42, 240, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.5, false, 0.5, 0.58);
        define(animations, sheet, "iron_guard", rect(1326, 42, 204, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.05, false, 0.5, 0.62);

        define(animations, sheet, "shadow_strike", rect(210, 298, 276, 198), 3, 2, 6,
                AnchorMode.BETWEEN, 1.08, true, 0.46, 0.58);
        define(animations, sheet, "twin_fang", rect(492, 298, 250, 198), 3, 2, 6,
                AnchorMode.TARGET, 0.88, true, 0.5, 0.58);
        define(animations, sheet, "shadow_step", rect(750, 298, 288, 198), 3, 2, 6,
                AnchorMode.BETWEEN, 1.08, true, 0.46, 0.58);
        define(animations, sheet, "silent_execution", rect(1048, 298, 252, 198), 3, 2, 6,
                AnchorMode.TARGET, 1.28, true, 0.5, 0.58);
        define(animations, sheet, "shadow_assassin", rect(1310, 298, 220, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.12, false, 0.5, 0.66);

        define(animations, sheet, "holy_bolt", rect(210, 554, 276, 198), 4, 2, 8,
                AnchorMode.BETWEEN, 0.82, true, 0.5, 0.58);
        define(animations, sheet, "heal", rect(492, 554, 254, 198), 3, 2, 6,
                AnchorMode.CASTER, 1.0, false, 0.5, 0.72);
        define(animations, sheet, "holy_shield", rect(754, 554, 270, 198), 3, 2, 6,
                AnchorMode.CASTER, 1.08, false, 0.5, 0.66);
        define(animations, sheet, "divine_light", rect(1034, 554, 266, 198), 3, 2, 6,
                AnchorMode.CASTER, 1.36, false, 0.5, 0.78);
        define(animations, sheet, "blessing", rect(1310, 554, 220, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.2, false, 0.5, 0.72);

        define(animations, sheet, "flame_burst", rect(210, 810, 270, 198), 3, 2, 6,
                AnchorMode.TARGET, 1.05, false, 0.5, 0.68);
        define(animations, sheet, "ice_shard", rect(486, 810, 260, 198), 3, 2, 6,
                AnchorMode.BETWEEN, 0.92, true, 0.5, 0.58);
        define(animations, sheet, "lightning_strike", rect(752, 810, 280, 198), 3, 2, 6,
                AnchorMode.TARGET, 1.18, false, 0.5, 0.78);
        define(animations, sheet, "elemental_storm", rect(1040, 810, 266, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.55, false, 0.5, 0.62);
        define(animations, sheet, "cataclysm", rect(1310, 810, 220, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.16, false, 0.5, 0.72);

        return animations;
    }

    private static Rectangle rect(int x, int y, int width, int height) {
        return new Rectangle(x, y, width, height);
    }

    private static void define(Map<String, SkillAnimation> animations,
            BufferedImage sheet, String abilityId, Rectangle baseRegion,
            int columns, int rows, int frameCount, AnchorMode anchorMode,
            double worldScale, boolean directional, double pivotX, double pivotY) {
        Rectangle region = scaleRegion(baseRegion, sheet.getWidth(), sheet.getHeight());
        List<BufferedImage> frames = sliceFrames(sheet, region, columns, rows, frameCount);
        if (!frames.isEmpty()) {
            animations.put(abilityId, new SkillAnimation(frames, anchorMode,
                    worldScale, directional, pivotX, pivotY));
        }
    }

    private static Rectangle scaleRegion(Rectangle baseRegion, int sheetWidth, int sheetHeight) {
        double scaleX = sheetWidth / (double) BASE_WIDTH;
        double scaleY = sheetHeight / (double) BASE_HEIGHT;
        int x = clamp((int) Math.round(baseRegion.x * scaleX), 0, sheetWidth - 1);
        int y = clamp((int) Math.round(baseRegion.y * scaleY), 0, sheetHeight - 1);
        int right = clamp((int) Math.round((baseRegion.x + baseRegion.width) * scaleX), x + 1, sheetWidth);
        int bottom = clamp((int) Math.round((baseRegion.y + baseRegion.height) * scaleY), y + 1, sheetHeight);
        return new Rectangle(x, y, right - x, bottom - y);
    }

    private static List<BufferedImage> sliceFrames(BufferedImage sheet, Rectangle region,
            int columns, int rows, int frameCount) {
        List<BufferedImage> frames = new ArrayList<>();
        int cellWidth = Math.max(1, region.width / columns);
        int cellHeight = Math.max(1, region.height / rows);
        int total = Math.min(frameCount, columns * rows);
        for (int index = 0; index < total; index++) {
            int column = index % columns;
            int row = index / columns;
            int x = region.x + column * cellWidth;
            int y = region.y + row * cellHeight;
            int width = column == columns - 1 ? region.x + region.width - x : cellWidth;
            int height = row == rows - 1 ? region.y + region.height - y : cellHeight;
            if (width > 0 && height > 0) {
                frames.add(sheet.getSubimage(x, y, width, height));
            }
        }
        return frames;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum AnchorMode {
        CASTER,
        TARGET,
        BETWEEN
    }

    public static final class SkillAnimation {
        private final List<BufferedImage> frames;
        private final AnchorMode anchorMode;
        private final double worldScale;
        private final boolean directional;
        private final double pivotX;
        private final double pivotY;

        private SkillAnimation(List<BufferedImage> frames, AnchorMode anchorMode,
                double worldScale, boolean directional, double pivotX, double pivotY) {
            this.frames = frames;
            this.anchorMode = anchorMode;
            this.worldScale = worldScale;
            this.directional = directional;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
        }

        public void draw(Graphics2D graphics, int startX, int startY, int targetX,
                int targetY, double radius, double progress, double alpha) {
            if (frames.isEmpty()) {
                return;
            }

            int frameIndex = Math.min(frames.size() - 1,
                    (int) Math.floor(Math.max(0.0, Math.min(0.999, progress)) * frames.size()));
            BufferedImage frame = frames.get(frameIndex);
            double drawX = anchorX(startX, targetX);
            double drawY = anchorY(startY, targetY);
            double targetWidth = Math.max(54.0, radius * worldScale);
            double scale = targetWidth / Math.max(1.0, frame.getWidth());
            double width = frame.getWidth() * scale;
            double height = frame.getHeight() * scale;
            double angle = directional ? Math.atan2(targetY - startY, targetX - startX) : 0.0;
            double fade = Math.max(0.0, Math.min(1.0, alpha * 1.35));

            Object previousInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            Composite previousComposite = graphics.getComposite();
            AffineTransform previousTransform = graphics.getTransform();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setComposite(AlphaComposite.SrcOver.derive((float) fade));
            graphics.translate(drawX, drawY);
            graphics.rotate(angle);
            graphics.drawImage(frame,
                    (int) Math.round(-width * pivotX),
                    (int) Math.round(-height * pivotY),
                    (int) Math.round(width),
                    (int) Math.round(height), null);
            graphics.setTransform(previousTransform);
            graphics.setComposite(previousComposite);
            if (previousInterpolation != null) {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previousInterpolation);
            }
        }

        private double anchorX(int startX, int targetX) {
            return switch (anchorMode) {
                case CASTER -> startX;
                case TARGET -> targetX;
                case BETWEEN -> startX + (targetX - startX) * 0.58;
            };
        }

        private double anchorY(int startY, int targetY) {
            return switch (anchorMode) {
                case CASTER -> startY;
                case TARGET -> targetY;
                case BETWEEN -> startY + (targetY - startY) * 0.58;
            };
        }
    }
}
