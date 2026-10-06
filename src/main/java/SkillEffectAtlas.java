import java.awt.AlphaComposite;
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

/** Optional genuine frame atlas. Static HUD icons are never synthesized into VFX frames. */
public final class SkillEffectAtlas {
    private static final String SHEET_PATH = "/main/resources/effects/skill_effect_sheet.png";
    private static final int BASE_WIDTH = 1536;
    private static final int BASE_HEIGHT = 1024;
    private static final double MAX_DRAW_WIDTH = 220.0;
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
                AnchorMode.TRAVEL, 0.82, true, 0.48, 0.58);
        define(animations, sheet, "earth_shatter", rect(806, 42, 266, 198), 3, 2, 6,
                AnchorMode.TARGET, 1.05, false, 0.5, 0.72);
        define(animations, sheet, "knights_wrath", rect(1080, 42, 240, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.5, false, 0.5, 0.58);
        define(animations, sheet, "iron_guard", rect(1326, 42, 204, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.05, false, 0.5, 0.62);

        define(animations, sheet, "shadow_strike", rect(210, 298, 276, 198), 3, 2, 6,
                AnchorMode.TRAVEL, 1.08, true, 0.46, 0.58);
        define(animations, sheet, "twin_fang", rect(492, 298, 250, 198), 3, 2, 6,
                AnchorMode.TARGET, 0.88, true, 0.5, 0.58);
        define(animations, sheet, "shadow_step", rect(750, 298, 288, 198), 3, 2, 6,
                AnchorMode.TRAVEL, 1.08, true, 0.46, 0.58);
        define(animations, sheet, "silent_execution", rect(1048, 298, 252, 198), 3, 2, 6,
                AnchorMode.TARGET, 1.28, true, 0.5, 0.58);
        define(animations, sheet, "shadow_assassin", rect(1310, 298, 220, 198), 2, 2, 4,
                AnchorMode.CASTER, 1.12, false, 0.5, 0.66);

        define(animations, sheet, "holy_bolt", rect(210, 554, 276, 198), 4, 2, 8,
                AnchorMode.TRAVEL, 0.82, true, 0.5, 0.58);
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
                AnchorMode.TRAVEL, 0.92, true, 0.5, 0.58);
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
        BETWEEN,
        TRAVEL
    }

    public static final class SkillAnimation {
        private final List<BufferedImage> frames;
        private final AnchorMode anchorMode;
        private final double worldScale;
        private final boolean directional;
        private final double pivotX;
        private final double pivotY;
        private final double[] frameWeights;
        private final double totalFrameWeight;

        private SkillAnimation(List<BufferedImage> frames, AnchorMode anchorMode,
                double worldScale, boolean directional, double pivotX, double pivotY) {
            this.frames = frames;
            this.anchorMode = anchorMode;
            this.worldScale = worldScale;
            this.directional = directional;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.frameWeights = createFrameWeights(frames.size());
            this.totalFrameWeight = sum(frameWeights);
        }

        public void draw(Graphics2D parentGraphics, int startX, int startY, int targetX,
                int targetY, double radius, double progress, double alpha) {
            if (frames.isEmpty()) {
                return;
            }

            Graphics2D graphics = (Graphics2D) parentGraphics.create();
            double parentAlpha = graphics.getComposite() instanceof AlphaComposite composite
                    ? composite.getAlpha() : 1.0;

            double clampedProgress = Math.max(0.0, Math.min(0.999, progress));
            FrameSample frameSample = frameSampleFor(clampedProgress);
            double drawX = anchorX(startX, targetX, clampedProgress);
            double drawY = anchorY(startY, targetY, clampedProgress);
            BufferedImage frame = frames.get(frameSample.index);
            double targetWidth = Math.min(MAX_DRAW_WIDTH, Math.max(54.0, radius * worldScale));
            double scale = targetWidth / Math.max(1.0, frame.getWidth());
            double width = frame.getWidth() * scale;
            double height = frame.getHeight() * scale;
            double angle = directional ? Math.atan2(targetY - startY, targetX - startX) : 0.0;
            double fade = parentAlpha * Math.max(0.0, Math.min(1.0, alpha * 1.35));

            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setComposite(AlphaComposite.SrcOver.derive((float) fade));
            graphics.translate(drawX, drawY);
            graphics.rotate(angle);
            drawFrame(graphics, frame, width, height, fade * (1.0 - frameSample.blend));
            if (frameSample.nextIndex != frameSample.index && frameSample.blend > 0.001) {
                drawFrame(graphics, frames.get(frameSample.nextIndex), width, height,
                        fade * frameSample.blend);
            }
            graphics.dispose();
        }

        private void drawFrame(Graphics2D graphics, BufferedImage frame,
                double width, double height, double alpha) {
            if (alpha <= 0.0) {
                return;
            }
            graphics.setComposite(AlphaComposite.SrcOver.derive(
                    (float) Math.max(0.0, Math.min(1.0, alpha))));
            AffineTransform placement = new AffineTransform();
            placement.translate(-width * pivotX, -height * pivotY);
            placement.scale(width / frame.getWidth(), height / frame.getHeight());
            graphics.drawImage(frame, placement, null);
        }

        private FrameSample frameSampleFor(double progress) {
            double mark = progress * totalFrameWeight;
            double running = 0.0;
            for (int index = 0; index < frameWeights.length; index++) {
                double start = running;
                running += frameWeights[index];
                if (mark <= running) {
                    double local = frameWeights[index] <= 0.0 ? 0.0
                            : (mark - start) / frameWeights[index];
                    double blend = smooth(local);
                    return new FrameSample(index, Math.min(index + 1, frameWeights.length - 1),
                            blend);
                }
            }
            int last = frameWeights.length - 1;
            return new FrameSample(last, last, 0.0);
        }

        private static double[] createFrameWeights(int frameCount) {
            double[] weights = new double[Math.max(1, frameCount)];
            for (int index = 0; index < weights.length; index++) {
                weights[index] = 1.0;
            }
            if (weights.length > 2) {
                weights[0] = 0.75;
                weights[Math.max(1, weights.length - 2)] = 1.55;
                weights[weights.length - 1] = 1.25;
            }
            return weights;
        }

        private static double sum(double[] values) {
            double total = 0.0;
            for (double value : values) {
                total += value;
            }
            return Math.max(1.0, total);
        }

        private double anchorX(int startX, int targetX, double progress) {
            return switch (anchorMode) {
                case CASTER -> startX;
                case TARGET -> targetX;
                case BETWEEN -> startX + (targetX - startX) * 0.58;
                case TRAVEL -> startX + (targetX - startX) * travelProgress(progress);
            };
        }

        private double anchorY(int startY, int targetY, double progress) {
            return switch (anchorMode) {
                case CASTER -> startY;
                case TARGET -> targetY;
                case BETWEEN -> startY + (targetY - startY) * 0.58;
                case TRAVEL -> startY + (targetY - startY) * travelProgress(progress);
            };
        }

        private double travelProgress(double progress) {
            if (progress < 0.18) {
                return 0.0;
            }
            if (progress > 0.78) {
                return 1.0;
            }
            double normalized = (progress - 0.18) / 0.6;
            return normalized * normalized * (3.0 - 2.0 * normalized);
        }

        private static double smooth(double value) {
            double clamped = Math.max(0.0, Math.min(1.0, value));
            return clamped * clamped * (3.0 - 2.0 * clamped);
        }

        private static final class FrameSample {
            private final int index;
            private final int nextIndex;
            private final double blend;

            private FrameSample(int index, int nextIndex, double blend) {
                this.index = index;
                this.nextIndex = nextIndex;
                this.blend = blend;
            }
        }
    }
}
