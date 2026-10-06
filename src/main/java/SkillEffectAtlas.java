import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;

/** Cached, reviewed PNG frames. Contact sheets are never divided into guessed grids. */
public final class SkillEffectAtlas {
    private static final String MANIFEST_PATH = "/main/resources/effects/skill_effects.properties";
    private static final Map<String, SkillMetadata> METADATA = createMetadata();
    private static final AlphaComposite[] OPACITY = opacityCache();
    private static final Atlas DEFAULT_ATLAS = loadDefault();
    private static final Map<String, String> TIMING_PROBLEMS = validateCombatClocks(DEFAULT_ATLAS);

    private SkillEffectAtlas() { }

    public static SkillAnimation getAnimation(String abilityId) {
        SkillAnimation action = getAnimation(abilityId, "action");
        return action == null ? getAnimation(abilityId, "passive") : action;
    }

    public static SkillAnimation getAnimation(String abilityId, String track) {
        if (track.equals("action") && TIMING_PROBLEMS.containsKey(abilityId)) return null;
        return DEFAULT_ATLAS.getAnimation(abilityId, track);
    }

    public static boolean hasAtlas() { return !DEFAULT_ATLAS.animations.isEmpty(); }
    public static SkillMetadata getMetadata(String abilityId) { return METADATA.get(abilityId); }
    public static List<SkillMetadata> getMetadata() { return List.copyOf(METADATA.values()); }
    public static List<String> getDiagnostics() {
        List<String> diagnostics = new ArrayList<>(DEFAULT_ATLAS.getDiagnostics());
        TIMING_PROBLEMS.forEach((id, problem) -> diagnostics.add(id + " / action: " + problem));
        return List.copyOf(diagnostics);
    }

    /** Parser inspection and production admission are separate: combat rejects mismatched hit clocks. */
    private static Map<String, String> validateCombatClocks(Atlas atlas) {
        Map<String, String> problems = new LinkedHashMap<>();
        for (AbilityDefinition definition : AbilityDefinition.createAll()) {
            SkillAnimation animation = atlas.getAnimation(definition.getId(), "action");
            String problem = AbilityAnimationTiming.sourceTimingProblem(definition, animation);
            if (problem != null) problems.put(definition.getId(), problem);
        }
        return Map.copyOf(problems);
    }

    /** Read-only inspection used by import tools and smoke tests; does not replace live assets. */
    public static Atlas inspectManifest(Path manifestPath) throws IOException {
        Path absolute = manifestPath.toAbsolutePath().normalize();
        try (InputStream input = Files.newInputStream(absolute)) {
            return load(input, absolute.getParent());
        }
    }

    private static Atlas loadDefault() {
        String relative = MANIFEST_PATH.substring(1);
        String packaged = relative.substring("main/resources/".length());
        for (String candidate : List.of(relative, packaged)) {
            try (InputStream input = SkillEffectAtlas.class.getClassLoader().getResourceAsStream(candidate)) {
                if (input != null) return load(input, null);
            } catch (IOException | RuntimeException exception) {
                return emptyAtlas("Could not read VFX manifest: " + exception.getMessage());
            }
        }
        for (Path candidate : List.of(Path.of("src", relative), Path.of(relative))) {
            if (Files.isRegularFile(candidate)) {
                try {
                    return inspectManifest(candidate);
                } catch (IOException | RuntimeException exception) {
                    return emptyAtlas("Could not read VFX manifest: " + exception.getMessage());
                }
            }
        }
        return emptyAtlas("VFX manifest is absent: " + MANIFEST_PATH);
    }

    private static Atlas emptyAtlas(String diagnostic) {
        return new Atlas(Map.of(), List.of(diagnostic));
    }

    private static Atlas load(InputStream input, Path directory) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        if (!"1".equals(properties.getProperty("schema.version"))) {
            return emptyAtlas("Unsupported VFX manifest schema; expected schema.version=1");
        }
        Map<String, SkillAnimation> animations = new LinkedHashMap<>();
        Map<String, BufferedImage> sheets = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();
        for (SkillMetadata metadata : METADATA.values()) {
            String skillPrefix = "skill." + metadata.id() + ".";
            String tracks = properties.getProperty(skillPrefix + "tracks", "");
            for (String rawTrack : tracks.split(",")) {
                String track = rawTrack.trim();
                if (track.isEmpty()) continue;
                String prefix = skillPrefix + track + ".";
                String label = metadata.name() + " / " + track;
                if (!metadata.tracks().contains(track)) {
                    diagnostics.add(label + ": unsupported track");
                    continue;
                }
                if (!"true".equals(properties.getProperty(prefix + "reviewed", "false"))) {
                    diagnostics.add(label + ": awaiting reviewed source crops");
                    continue;
                }
                try {
                    String sheetId = required(properties, prefix + "sheet");
                    BufferedImage sheet = sheets.get(sheetId);
                    if (sheet == null) {
                        sheet = loadSheet(properties, sheetId, directory);
                        sheets.put(sheetId, sheet);
                    }
                    SkillAnimation animation = createAnimation(properties, prefix, metadata,
                            track, sheetId, sheet);
                    String key = metadata.id() + "/" + track;
                    if (animations.putIfAbsent(key, animation) != null) {
                        throw new IllegalArgumentException("duplicate track");
                    }
                } catch (RuntimeException exception) {
                    diagnostics.add(label + ": " + exception.getMessage());
                }
            }
        }
        return new Atlas(animations, diagnostics);
    }

    private static BufferedImage loadSheet(Properties properties, String sheetId, Path directory) {
        String prefix = "sheet." + sheetId + ".";
        String sheetPath = required(properties, prefix + "path");
        BufferedImage sheet;
        if (!sheetPath.startsWith("/") && directory != null) {
            sheet = readSheetFile(directory.resolve(sheetPath).normalize());
        } else if (Path.of(sheetPath).isAbsolute() && !sheetPath.startsWith("/")) {
            sheet = readSheetFile(Path.of(sheetPath));
        } else if (!sheetPath.startsWith("/") && !Path.of(sheetPath).isAbsolute()) {
            sheet = ResourceLoader.loadImage("/main/resources/effects/" + sheetPath);
        } else {
            sheet = ResourceLoader.loadImage(sheetPath);
        }
        int expectedWidth = integer(properties, prefix + "width", 0);
        int expectedHeight = integer(properties, prefix + "height", 0);
        if (expectedWidth <= 0 || expectedHeight <= 0
                || sheet.getWidth() != expectedWidth || sheet.getHeight() != expectedHeight) {
            throw new IllegalArgumentException("source dimensions differ from reviewed manifest: " + sheetId);
        }
        if (!sheet.getColorModel().hasAlpha()) {
            throw new IllegalArgumentException("source PNG has no alpha channel: " + sheetId);
        }
        return sheet;
    }

    private static BufferedImage readSheetFile(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) throw new IllegalArgumentException("unsupported source image: " + path);
            return image;
        } catch (IOException exception) {
            throw new IllegalArgumentException("source PNG is unavailable: " + path, exception);
        }
    }

    private static SkillAnimation createAnimation(Properties properties, String prefix,
            SkillMetadata metadata, String track, String sheetId, BufferedImage sheet) {
        String frameText = required(properties, prefix + "frames");
        List<BufferedImage> frames = new ArrayList<>();
        List<FrameMetadata> regions = new ArrayList<>();
        for (String frame : frameText.split(";")) {
            String[] values = frame.trim().split(",");
            if (values.length != 7) {
                throw new IllegalArgumentException("frame requires x,y,width,height,durationSeconds,pivotX,pivotY");
            }
            int x = Integer.parseInt(values[0].trim()), y = Integer.parseInt(values[1].trim());
            int width = Integer.parseInt(values[2].trim()), height = Integer.parseInt(values[3].trim());
            double duration = finite(values[4].trim()), pivotX = finite(values[5].trim());
            double pivotY = finite(values[6].trim());
            if (x < 0 || y < 0 || width <= 0 || height <= 0
                    || (long) x + width > sheet.getWidth() || (long) y + height > sheet.getHeight()) {
                throw new IllegalArgumentException("frame rectangle is outside source PNG");
            }
            if (duration <= 0 || pivotX < 0 || pivotX > 1 || pivotY < 0 || pivotY > 1) {
                throw new IllegalArgumentException("duration must be positive and pivots must be between 0 and 1");
            }
            BufferedImage image = sheet.getSubimage(x, y, width, height);
            if (!hasVisibleAndTransparentPixels(image)) {
                throw new IllegalArgumentException("frame requires visible artwork and transparent background");
            }
            frames.add(image);
            regions.add(new FrameMetadata(x, y, width, height, duration, pivotX, pivotY));
        }
        int canvasWidth = integer(properties, prefix + "canvasWidth", 0);
        double delay = number(properties, prefix + "startDelaySeconds", 0);
        double scale = number(properties, prefix + "scale", 1);
        boolean loop = bool(properties, prefix + "loop", false);
        boolean playOnce = bool(properties, prefix + "playOnce", !loop);
        if (canvasWidth <= 0 || delay < 0 || scale <= 0 || loop == playOnce) {
            throw new IllegalArgumentException("positive canvasWidth/scale, nonnegative delay and exactly one playback mode required");
        }
        int hitFrame = integer(properties, prefix + "hitFrame", -1);
        int impactFrame = integer(properties, prefix + "impactFrame", -1);
        if (hitFrame < -1 || hitFrame >= frames.size() || impactFrame < -1 || impactFrame >= frames.size()) {
            throw new IllegalArgumentException("event frame index outside animation");
        }
        String hitList = properties.getProperty(prefix + "hitFrames", "").trim();
        int[] hitFrames;
        if (hitList.isEmpty()) {
            hitFrames = hitFrame < 0 ? new int[0] : new int[] {hitFrame};
        } else {
            String[] values = hitList.split(",", -1);
            hitFrames = new int[values.length];
            int previous = -1;
            for (int index = 0; index < values.length; index++) {
                int event = Integer.parseInt(values[index].trim());
                if (event <= previous || event >= frames.size()) {
                    throw new IllegalArgumentException("hitFrames indices must be strictly increasing and inside animation");
                }
                hitFrames[index] = event;
                previous = event;
            }
            if (hitFrame >= 0 && hitFrame != hitFrames[0]) {
                throw new IllegalArgumentException("hitFrame must match first hitFrames index when both are provided");
            }
        }
        AnchorMode anchor = AnchorMode.valueOf(properties.getProperty(prefix + "anchor", "CASTER").trim());
        return new SkillAnimation(metadata.id(), metadata.name(), track, sheetId, frames, regions,
                canvasWidth, delay, scale, Math.toRadians(number(properties, prefix + "rotationDegrees", 0)),
                number(properties, prefix + "offsetX", 0), number(properties, prefix + "offsetY", 0),
                loop, bool(properties, prefix + "directional", false), anchor, hitFrames, impactFrame);
    }

    private static boolean hasVisibleAndTransparentPixels(BufferedImage image) {
        boolean visible = false, transparent = false;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                visible |= alpha > 0;
                transparent |= alpha == 0;
                if (visible && transparent) return true;
            }
        }
        return false;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("missing " + key);
        return value;
    }

    private static double finite(String value) {
        double result = Double.parseDouble(value);
        if (!Double.isFinite(result)) throw new IllegalArgumentException("non-finite manifest number");
        return result;
    }

    private static double number(Properties properties, String key, double fallback) {
        return finite(properties.getProperty(key, Double.toString(fallback)));
    }

    private static int integer(Properties properties, String key, int fallback) {
        return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key, Boolean.toString(fallback)).trim();
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalArgumentException(key + " must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static Map<String, SkillMetadata> createMetadata() {
        Map<String, SkillMetadata> result = new LinkedHashMap<>();
        metadata(result, "heavy_slash", "Flame Slash", "BLACK_KNIGHT", false, "buildup,action,impact");
        metadata(result, "shield_bash", "Shield Bash", "BLACK_KNIGHT", false, "buildup,action,impact");
        metadata(result, "earth_shatter", "Rising Strike", "BLACK_KNIGHT", false, "buildup,action,impact");
        metadata(result, "knights_wrath", "Whirlwind", "BLACK_KNIGHT", false, "buildup,action,impact");
        metadata(result, "iron_guard", "Berserker's Will", "BLACK_KNIGHT", true, "passive");
        metadata(result, "shadow_strike", "Shadow Strike", "ASSASSIN", false, "buildup,travel,action,impact");
        metadata(result, "shadow_step", "Blink Step", "ASSASSIN", false, "buildup,travel,action,impact");
        metadata(result, "death_mark", "Death Mark", "ASSASSIN", false, "buildup,action,impact");
        metadata(result, "twin_fang", "Spiral Cut", "ASSASSIN", false, "buildup,action,impact");
        metadata(result, "shadow_assassin", "Assassin's Instinct", "ASSASSIN", true, "passive");
        metadata(result, "heal", "Healing Touch", "PRIEST", false, "buildup,travel,action,impact");
        metadata(result, "holy_bolt", "Holy Light", "PRIEST", false, "buildup,action,impact");
        metadata(result, "holy_shield", "Divine Barrier", "PRIEST", false, "buildup,action,barrier_block");
        metadata(result, "divine_light", "Judgment", "PRIEST", false, "buildup,action,impact");
        metadata(result, "blessing", "Faith", "PRIEST", true, "passive");
        metadata(result, "flame_burst", "Fire Storm", "ELEMENTALIST", false, "buildup,travel,action,impact");
        metadata(result, "ice_shard", "Ice Spear", "ELEMENTALIST", false, "buildup,travel,action,impact");
        metadata(result, "lightning_strike", "Thunder Break", "ELEMENTALIST", false, "buildup,action,impact");
        metadata(result, "elemental_storm", "Elemental Nova", "ELEMENTALIST", false, "buildup,action,impact");
        metadata(result, "cataclysm", "Elemental Mastery", "ELEMENTALIST", true, "passive");
        metadata(result, "shield_fortress", "Shield Fortress", "GUARDIAN", false, "buildup,action,barrier_block");
        metadata(result, "iron_charge", "Iron Charge", "GUARDIAN", false, "buildup,action,impact");
        metadata(result, "earthbreaker", "Earthbreaker", "GUARDIAN", false, "buildup,action,impact");
        metadata(result, "guardians_roar", "Guardian's Roar", "GUARDIAN", false, "buildup,action,impact");
        metadata(result, "unbreakable", "Unbreakable", "GUARDIAN", true, "passive");
        return java.util.Collections.unmodifiableMap(result);
    }

    private static void metadata(Map<String, SkillMetadata> metadata, String id, String name,
            String abilityClass, boolean passive, String tracks) {
        metadata.put(id, new SkillMetadata(id, name, abilityClass, passive, List.of(tracks.split(","))));
    }

    public enum AnchorMode { CASTER, TARGET, WEAPON, SHIELD, PROJECTILE, TARGET_ATTACHED, AREA, BETWEEN, TRAVEL }

    public record SkillMetadata(String id, String name, String abilityClass,
            boolean passive, List<String> tracks) {
        public SkillMetadata { tracks = List.copyOf(tracks); }
    }

    public record FrameMetadata(int x, int y, int width, int height,
            double durationSeconds, double pivotX, double pivotY) { }

    public static final class Atlas {
        private final Map<String, SkillAnimation> animations;
        private final List<String> diagnostics;

        private Atlas(Map<String, SkillAnimation> animations, List<String> diagnostics) {
            this.animations = Map.copyOf(animations);
            this.diagnostics = List.copyOf(diagnostics);
        }

        public SkillAnimation getAnimation(String id, String track) { return animations.get(id + "/" + track); }
        public List<String> getDiagnostics() { return diagnostics; }
        public int getAnimationCount() { return animations.size(); }
    }

    public static final class SkillAnimation {
        private final String skillId, skillName, track, sheetId;
        private final List<BufferedImage> frames;
        private final List<FrameMetadata> frameMetadata;
        private final double[] frameEnds;
        private final int[] hitFrames;
        private final int canvasWidth, hitFrame, impactFrame;
        private final double startDelay, scale, rotation, offsetX, offsetY, duration;
        private final boolean loop, directional;
        private final AnchorMode anchorMode;

        private SkillAnimation(String skillId, String skillName, String track, String sheetId,
                List<BufferedImage> frames, List<FrameMetadata> frameMetadata, int canvasWidth,
                double startDelay, double scale, double rotation, double offsetX, double offsetY,
                boolean loop, boolean directional, AnchorMode anchorMode, int[] hitFrames, int impactFrame) {
            this.skillId = skillId; this.skillName = skillName; this.track = track; this.sheetId = sheetId;
            this.frames = List.copyOf(frames); this.frameMetadata = List.copyOf(frameMetadata);
            this.canvasWidth = canvasWidth; this.startDelay = startDelay; this.scale = scale;
            this.rotation = rotation; this.offsetX = offsetX; this.offsetY = offsetY;
            this.loop = loop; this.directional = directional; this.anchorMode = anchorMode;
            this.hitFrames = hitFrames.clone();
            this.hitFrame = hitFrames.length == 0 ? -1 : hitFrames[0]; this.impactFrame = impactFrame;
            frameEnds = new double[frames.size()];
            double end = 0;
            for (int index = 0; index < frameEnds.length; index++) {
                end += frameMetadata.get(index).durationSeconds();
                frameEnds[index] = end;
            }
            duration = end;
        }

        public String getSkillId() { return skillId; }
        public String getSkillName() { return skillName; }
        public String getTrack() { return track; }
        public String getSheetId() { return sheetId; }
        public List<FrameMetadata> getFrames() { return frameMetadata; }
        public int getFrameCount() { return frames.size(); }
        public double getDurationSeconds() { return duration; }
        public double getStartDelaySeconds() { return startDelay; }
        public boolean isLooping() { return loop; }
        public boolean isDirectional() { return directional; }
        public AnchorMode getAnchorMode() { return anchorMode; }
        public int getHitFrame() { return hitFrame; }
        public int[] getHitFrames() { return hitFrames.clone(); }
        public int getImpactFrame() { return impactFrame; }
        public double getHitTimeSeconds() { return eventTime(hitFrame); }
        public double[] getHitTimesSeconds() {
            double[] times = new double[hitFrames.length];
            for (int index = 0; index < times.length; index++) times[index] = eventTime(hitFrames[index]);
            return times;
        }
        public double getImpactTimeSeconds() { return eventTime(impactFrame); }
        public boolean isFinished(double elapsedSeconds) { return !loop && elapsedSeconds >= startDelay + duration; }

        private double eventTime(int frame) { return frame < 0 ? -1 : startDelay + (frame == 0 ? 0 : frameEnds[frame - 1]); }

        /** -1 means no frame is visible before delay or after a one-shot animation. */
        public int getFrameIndex(double elapsedSeconds) {
            if (!Double.isFinite(elapsedSeconds) || elapsedSeconds < startDelay) return -1;
            double local = elapsedSeconds - startDelay;
            if (loop) local %= duration;
            else if (local >= duration) return -1;
            for (int index = 0; index < frameEnds.length; index++) {
                if (local < frameEnds[index]) return index;
            }
            return frameEnds.length - 1;
        }

        /** Caller supplies the live source/impact anchor and animation clock, in screen pixels. */
        public void drawAt(Graphics2D parent, double x, double y, double width, double angle,
                double elapsedSeconds, double alpha, boolean mirror) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width)
                    || !Double.isFinite(angle) || !Double.isFinite(alpha) || width <= 0 || alpha <= 0) return;
            int index = getFrameIndex(elapsedSeconds);
            if (index < 0) return;
            double parentAlpha = parent.getComposite() instanceof AlphaComposite composite ? composite.getAlpha() : 1;
            int opacity = (int) Math.round(Math.max(0, Math.min(1, alpha * parentAlpha)) * (OPACITY.length - 1));
            if (opacity == 0) return;
            BufferedImage frame = frames.get(index);
            FrameMetadata region = frameMetadata.get(index);
            double uniformScale = width * scale / canvasWidth;
            Graphics2D graphics = (Graphics2D) parent.create();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                graphics.setComposite(OPACITY[opacity]);
                graphics.translate(x, y);
                graphics.rotate(angle + (mirror ? -rotation : rotation));
                graphics.scale(mirror ? -uniformScale : uniformScale, uniformScale);
                graphics.translate(offsetX - region.width() * region.pivotX(),
                        offsetY - region.height() * region.pivotY());
                graphics.drawImage(frame, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        }

        /** Compatibility helper. New effects should call drawAt with their actual live attachment. */
        public void draw(Graphics2D graphics, int startX, int startY, int targetX,
                int targetY, double radius, double progress, double alpha) {
            double p = Math.max(0, Math.min(1, progress));
            double travel = p * p * (3 - 2 * p);
            double mix = switch (anchorMode) {
                case TARGET, TARGET_ATTACHED, AREA -> 1;
                case BETWEEN -> 0.58;
                case TRAVEL, PROJECTILE -> travel;
                default -> 0;
            };
            double angle = directional ? Math.atan2(targetY - startY, targetX - startX) : 0;
            drawAt(graphics, startX + (targetX - startX) * mix, startY + (targetY - startY) * mix,
                    Math.min(220, Math.max(54, radius)), angle, p * (startDelay + duration), alpha, false);
        }
    }

    private static AlphaComposite[] opacityCache() {
        AlphaComposite[] result = new AlphaComposite[257];
        for (int index = 0; index < result.length; index++) result[index] = AlphaComposite.SrcOver.derive(index / 256f);
        return result;
    }
}
