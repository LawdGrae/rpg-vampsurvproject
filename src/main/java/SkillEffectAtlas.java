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
    private static final int MAX_STANDALONE_FRAME_SIZE = 128;
    private static final double MAX_DRAW_WIDTH = 220.0;
    private static final Map<String, SkillAnimation> ANIMATIONS = new HashMap<>();

    private SkillEffectAtlas() {
    }

    public static SkillAnimation getAnimation(String abilityId) {
        synchronized (ANIMATIONS) {
            if (ANIMATIONS.containsKey(abilityId)) {
                return ANIMATIONS.get(abilityId);
            }
            SkillAnimation animation = loadStandaloneAnimation(abilityId);
            ANIMATIONS.put(abilityId, animation);
            return animation;
        }
    }

    public static boolean hasAtlas() {
        return !ANIMATIONS.isEmpty();
    }

    private static Map<String, SkillAnimation> loadAnimations() {
        Map<String, SkillAnimation> standaloneAnimations = loadStandaloneAnimations();
        if (!standaloneAnimations.isEmpty()) {
            return standaloneAnimations;
        }

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

    private static Map<String, SkillAnimation> loadStandaloneAnimations() {
        Map<String, SkillAnimation> animations = new HashMap<>();

        defineStandalone(animations, "heavy_slash", AnchorMode.BETWEEN, 2.0, true, 0.48, 0.55);
        defineStandalone(animations, "shield_bash", AnchorMode.TRAVEL, 1.45, true, 0.48, 0.58);
        defineStandalone(animations, "earth_shatter", AnchorMode.TARGET, 2.0, false, 0.5, 0.72);
        defineStandalone(animations, "knights_wrath", AnchorMode.CASTER, 2.55, false, 0.5, 0.58);
        defineStandalone(animations, "iron_guard", AnchorMode.CASTER, 1.8, false, 0.5, 0.62);

        defineStandalone(animations, "shadow_strike", AnchorMode.TRAVEL, 1.9, true, 0.46, 0.58);
        defineStandalone(animations, "twin_fang", AnchorMode.TARGET, 1.6, true, 0.5, 0.58);
        defineStandalone(animations, "shadow_step", AnchorMode.TRAVEL, 1.9, true, 0.46, 0.58);
        defineStandalone(animations, "silent_execution", AnchorMode.TARGET, 2.15, true, 0.5, 0.58);
        defineStandalone(animations, "shadow_assassin", AnchorMode.CASTER, 1.9, false, 0.5, 0.66);

        defineStandalone(animations, "holy_bolt", AnchorMode.TRAVEL, 1.45, true, 0.5, 0.58);
        defineStandalone(animations, "heal", AnchorMode.CASTER, 1.8, false, 0.5, 0.72);
        defineStandalone(animations, "holy_shield", AnchorMode.CASTER, 1.95, false, 0.5, 0.66);
        defineStandalone(animations, "divine_light", AnchorMode.CASTER, 2.35, false, 0.5, 0.78);
        defineStandalone(animations, "blessing", AnchorMode.CASTER, 2.0, false, 0.5, 0.72);

        defineStandalone(animations, "flame_burst", AnchorMode.TARGET, 1.95, false, 0.5, 0.68);
        defineStandalone(animations, "ice_shard", AnchorMode.TRAVEL, 1.6, true, 0.5, 0.58);
        defineStandalone(animations, "lightning_strike", AnchorMode.TARGET, 2.05, false, 0.5, 0.78);
        defineStandalone(animations, "elemental_storm", AnchorMode.CASTER, 2.65, false, 0.5, 0.62);
        defineStandalone(animations, "cataclysm", AnchorMode.CASTER, 2.0, false, 0.5, 0.72);

        return animations;
    }

    private static void defineStandalone(Map<String, SkillAnimation> animations,
            String abilityId, AnchorMode anchorMode, double worldScale,
            boolean directional, double pivotX, double pivotY) {
        SkillAnimation animation = loadStandaloneAnimation(abilityId, anchorMode,
                worldScale, directional, pivotX, pivotY);
        if (animation != null) {
            animations.put(abilityId, animation);
        }
    }

    private static SkillAnimation loadStandaloneAnimation(String abilityId) {
        return switch (abilityId) {
            case "heavy_slash" -> loadStandaloneAnimation(abilityId, AnchorMode.BETWEEN, 2.0, true, 0.48, 0.55);
            case "shield_bash" -> loadStandaloneAnimation(abilityId, AnchorMode.TRAVEL, 1.45, true, 0.48, 0.58);
            case "earth_shatter" -> loadStandaloneAnimation(abilityId, AnchorMode.TARGET, 2.0, false, 0.5, 0.72);
            case "knights_wrath" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 2.55, false, 0.5, 0.58);
            case "iron_guard" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 1.8, false, 0.5, 0.62);
            case "shadow_strike" -> loadStandaloneAnimation(abilityId, AnchorMode.TRAVEL, 1.9, true, 0.46, 0.58);
            case "twin_fang" -> loadStandaloneAnimation(abilityId, AnchorMode.TARGET, 1.6, true, 0.5, 0.58);
            case "shadow_step" -> loadStandaloneAnimation(abilityId, AnchorMode.TRAVEL, 1.9, true, 0.46, 0.58);
            case "silent_execution" -> loadStandaloneAnimation(abilityId, AnchorMode.TARGET, 2.15, true, 0.5, 0.58);
            case "shadow_assassin" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 1.9, false, 0.5, 0.66);
            case "holy_bolt" -> loadStandaloneAnimation(abilityId, AnchorMode.TRAVEL, 1.45, true, 0.5, 0.58);
            case "heal" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 1.8, false, 0.5, 0.72);
            case "holy_shield" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 1.95, false, 0.5, 0.66);
            case "divine_light" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 2.35, false, 0.5, 0.78);
            case "blessing" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 2.0, false, 0.5, 0.72);
            case "flame_burst" -> loadStandaloneAnimation(abilityId, AnchorMode.TARGET, 1.95, false, 0.5, 0.68);
            case "ice_shard" -> loadStandaloneAnimation(abilityId, AnchorMode.TRAVEL, 1.6, true, 0.5, 0.58);
            case "lightning_strike" -> loadStandaloneAnimation(abilityId, AnchorMode.TARGET, 2.05, false, 0.5, 0.78);
            case "elemental_storm" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 2.65, false, 0.5, 0.62);
            case "cataclysm" -> loadStandaloneAnimation(abilityId, AnchorMode.CASTER, 2.0, false, 0.5, 0.72);
            default -> null;
        };
    }

    private static SkillAnimation loadStandaloneAnimation(String abilityId,
            AnchorMode anchorMode, double worldScale, boolean directional,
            double pivotX, double pivotY) {
        try {
            BufferedImage image = ResourceLoader.loadImage("/main/resources/abilities/" + abilityId + ".png");
            List<BufferedImage> frames = createStandaloneFrames(image, directional);
            if (!frames.isEmpty()) {
                return new SkillAnimation(frames, anchorMode,
                        worldScale, directional, pivotX, pivotY);
            }
        } catch (IllegalStateException exception) {
            // Missing optional art falls back to procedural VFX for that ability.
        }
        return null;
    }

    private static List<BufferedImage> createStandaloneFrames(BufferedImage image,
            boolean directional) {
        List<BufferedImage> frames = new ArrayList<>();
        BufferedImage source = prepareStandaloneSource(image);
        int frameCount = 5;
        for (int index = 0; index < frameCount; index++) {
            double progress = (index + 1) / (double) frameCount;
            BufferedImage frame = new BufferedImage(source.getWidth(), source.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = frame.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            if (directional) {
                int revealedWidth = Math.max(1, (int) Math.round(source.getWidth() * progress));
                graphics.setClip(0, 0, revealedWidth, source.getHeight());
                graphics.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1.0, 0.35 + progress * 0.85)));
                graphics.drawImage(source, 0, 0, null);
            } else {
                double scale = 0.42 + progress * 0.58;
                int width = (int) Math.round(source.getWidth() * scale);
                int height = (int) Math.round(source.getHeight() * scale);
                int x = (source.getWidth() - width) / 2;
                int y = (source.getHeight() - height) / 2;
                graphics.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1.0, 0.3 + progress * 0.9)));
                graphics.drawImage(source, x, y, width, height, null);
            }
            graphics.dispose();
            frames.add(frame);
        }
        return frames;
    }

    private static BufferedImage prepareStandaloneSource(BufferedImage image) {
        Rectangle bounds = contentBounds(image);
        int sourceWidth = bounds.width;
        int sourceHeight = bounds.height;
        double scale = Math.min(1.0, MAX_STANDALONE_FRAME_SIZE
                / (double) Math.max(sourceWidth, sourceHeight));
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        BufferedImage prepared = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = prepared.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.drawImage(image,
                0, 0, width, height,
                bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height,
                null);
        graphics.dispose();
        return prepared;
    }

    private static Rectangle contentBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xff) > 8) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return new Rectangle(0, 0, image.getWidth(), image.getHeight());
        }
        int padding = 3;
        int x = clamp(minX - padding, 0, image.getWidth() - 1);
        int y = clamp(minY - padding, 0, image.getHeight() - 1);
        int right = clamp(maxX + padding + 1, x + 1, image.getWidth());
        int bottom = clamp(maxY + padding + 1, y + 1, image.getHeight());
        return new Rectangle(x, y, right - x, bottom - y);
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

        public void draw(Graphics2D graphics, int startX, int startY, int targetX,
                int targetY, double radius, double progress, double alpha) {
            if (frames.isEmpty()) {
                return;
            }

            double clampedProgress = Math.max(0.0, Math.min(0.999, progress));
            int frameIndex = frameIndexFor(clampedProgress);
            BufferedImage frame = frames.get(frameIndex);
            double drawX = anchorX(startX, targetX, clampedProgress);
            double drawY = anchorY(startY, targetY, clampedProgress);
            double targetWidth = Math.min(MAX_DRAW_WIDTH, Math.max(54.0, radius * worldScale));
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

        private int frameIndexFor(double progress) {
            double mark = progress * totalFrameWeight;
            double running = 0.0;
            for (int index = 0; index < frameWeights.length; index++) {
                running += frameWeights[index];
                if (mark <= running) {
                    return index;
                }
            }
            return frameWeights.length - 1;
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
    }
}
