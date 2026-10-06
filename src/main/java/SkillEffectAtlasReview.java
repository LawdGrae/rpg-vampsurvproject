import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Headless import review: shows every approved source frame, timing and mirror. */
public final class SkillEffectAtlasReview {
    private SkillEffectAtlasReview() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: SkillEffectAtlasReview <manifest.properties> <review-directory>");
        }
        System.setProperty("java.awt.headless", "true");
        SkillEffectAtlas.Atlas atlas = SkillEffectAtlas.inspectManifest(Path.of(args[0]));
        Path directory = Path.of(args[1]).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        List<String> report = new ArrayList<>();
        report.add("Loaded animations: " + atlas.getAnimationCount());
        report.addAll(atlas.getDiagnostics());
        for (SkillEffectAtlas.SkillMetadata skill : SkillEffectAtlas.getMetadata()) {
            for (String track : skill.tracks()) {
                SkillEffectAtlas.SkillAnimation animation = atlas.getAnimation(skill.id(), track);
                if (animation == null) continue;
                if (track.equals("action")) {
                    AbilityDefinition definition = AbilityDefinition.createAll().stream()
                            .filter(d -> d.getId().equals(skill.id())).findFirst().orElseThrow();
                    String problem = AbilityAnimationTiming.sourceTimingProblem(definition, animation);
                    if (problem != null) report.add("REJECTED COMBAT CLOCK: " + skill.name() + ": " + problem);
                }
                String filename = skill.id() + "-" + track + ".png";
                ImageIO.write(render(animation), "png", directory.resolve(filename).toFile());
                report.add(skill.name() + " / " + track + ": " + filename + ", "
                        + animation.getFrameCount() + " frames, " + animation.getDurationSeconds()
                        + "s, hits=" + java.util.Arrays.toString(animation.getHitTimesSeconds()) + "s, impact="
                        + animation.getImpactTimeSeconds() + "s");
                for (int index = 0; index < animation.getFrames().size(); index++) {
                    report.add("  frame " + index + " " + animation.getFrames().get(index));
                }
            }
        }
        Files.write(directory.resolve("review.txt"), report);
        for (String line : report) System.out.println(line);
        if (atlas.getAnimationCount() == 0 || !atlas.getDiagnostics().isEmpty()
                || report.stream().anyMatch(line -> line.startsWith("REJECTED COMBAT CLOCK"))) System.exit(2);
    }

    private static BufferedImage render(SkillEffectAtlas.SkillAnimation animation) {
        int columns = Math.min(6, animation.getFrameCount());
        int rows = (animation.getFrameCount() + columns - 1) / columns;
        int cellWidth = 196, cellHeight = 220, header = 44;
        BufferedImage image = new BufferedImage(columns * cellWidth, rows * cellHeight * 2 + header,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(22, 25, 33));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.WHITE); graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            graphics.drawString(animation.getSkillName() + " / " + animation.getTrack()
                    + " — upper: right, lower: left", 8, 23);
            double elapsed = animation.getStartDelaySeconds();
            for (int index = 0; index < animation.getFrameCount(); index++) {
                SkillEffectAtlas.FrameMetadata frame = animation.getFrames().get(index);
                double sampleTime = elapsed + frame.durationSeconds() * 0.5;
                for (int facing = 0; facing < 2; facing++) {
                    int x = index % columns * cellWidth;
                    int y = header + (index / columns + facing * rows) * cellHeight;
                    checker(graphics, x + 4, y + 4, cellWidth - 8, cellHeight - 32);
                    double pivotX = x + cellWidth / 2.0, pivotY = y + (cellHeight - 32) * 0.68;
                    graphics.setColor(new Color(255, 160, 90));
                    graphics.drawLine((int) pivotX - 4, (int) pivotY, (int) pivotX + 4, (int) pivotY);
                    graphics.drawLine((int) pivotX, (int) pivotY - 4, (int) pivotX, (int) pivotY + 4);
                    animation.drawAt(graphics, pivotX, pivotY, 140, 0, sampleTime, 1, facing == 1);
                    graphics.setColor(Color.WHITE);
                    graphics.drawString("#" + index + "  " + frame.durationSeconds() + "s  "
                            + (facing == 0 ? "right" : "left"), x + 8, y + cellHeight - 11);
                }
                elapsed += frame.durationSeconds();
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void checker(Graphics2D graphics, int x, int y, int width, int height) {
        for (int row = 0; row < height; row += 12) {
            for (int column = 0; column < width; column += 12) {
                graphics.setColor((row / 12 + column / 12) % 2 == 0
                        ? new Color(44, 47, 56) : new Color(65, 69, 80));
                graphics.fillRect(x + column, y + row, Math.min(12, width - column), Math.min(12, height - row));
            }
        }
    }
}
