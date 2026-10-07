import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Contact sheets and looping previews of the real skill actors, poses, contacts and VFX. */
public final class SkillVfxRenderCheck {
    private static final String[] HEROES = {"Black Knight", "Assassin", "Priest", "Elementalist", "Guardian"};
    private static final String[] STAGES = {"Anticipation", "Windup", "Release", "Contact / travel", "Afterglow", "Recovery"};
    private static final int CELL_WIDTH = 380, CELL_HEIGHT = 280;
    private SkillVfxRenderCheck() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path directory = Path.of(args.length > 0 ? args[0] : "out/skill-vfx-preview").toAbsolutePath();
        Files.createDirectories(directory);
        List<String> selected = args.length > 1 ? List.of(args[1].toLowerCase().split(",")) : List.of();
        int rendered = 0;
        for (int hero = 0; hero < HEROES.length; hero++) {
            if (!selected.isEmpty() && !selected.contains(filename(hero))) continue;
            contactSheet(directory, hero);
            animatedPreview(directory, hero);
            rendered++;
            System.out.println("Rendered " + HEROES[hero] + " VFX contact sheet and looping GIF");
        }
        if (rendered == 0) throw new IllegalArgumentException("Unknown hero preview filter: " + args[1]);
        System.out.println("Rendered " + (rendered * 48) + " live runtime snapshots and "
                + rendered + " animated skill previews: " + directory);
    }

    private static void contactSheet(Path directory, int hero) throws Exception {
        BufferedImage sheet = new BufferedImage(CELL_WIDTH * STAGES.length, CELL_HEIGHT * 8 + 44,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        try {
            g.setColor(new Color(12, 16, 25));
            g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            title(g, HEROES[hero] + " | live skill effects, both facings", 12, 30, 23);
            for (int slot = 0; slot < CombatSkillSmokeTest.SKILLS[hero].length; slot++) {
                String id = CombatSkillSmokeTest.SKILLS[hero][slot];
                for (int facing : new int[] {1, -1}) {
                    CombatSkillSmokeTest.Fixture fixture = fixture(hero, id, facing);
                    AbilityDefinition definition = fixture.skill(id).getDefinition();
                    double duration = AbilityAnimationTiming.duration(definition);
                    double release = duration * AbilityAnimationTiming.releaseProgress(definition);
                    double contact = AbilityAnimationTiming.hitTimes(definition)[0];
                    double[] times = {duration * 0.10, Math.max(duration * 0.15, release - 0.025),
                            release + 0.025, Math.max(contact + 0.08, release + 0.22),
                            Math.max(duration * 0.88, contact + 0.30), duration + 0.55};
                    double elapsed = 0;
                    for (int stage = 0; stage < times.length; stage++) {
                        fixture.advance(Math.max(0, times[stage] - elapsed));
                        elapsed = times[stage];
                        int x = stage * CELL_WIDTH;
                        int y = 44 + (slot * 2 + (facing > 0 ? 0 : 1)) * CELL_HEIGHT;
                        drawCell(g, fixture, x, y, definition,
                                STAGES[stage] + String.format(" | %.2fs | %s", elapsed, facing > 0 ? "right" : "left"));
                    }
                }
            }
        } finally { g.dispose(); }
        ImageIO.write(sheet, "png", directory.resolve(filename(hero) + "-skills.png").toFile());
    }

    private static void animatedPreview(Path directory, int hero) throws Exception {
        List<CombatSkillSmokeTest.Fixture> fixtures = new ArrayList<>();
        for (String id : CombatSkillSmokeTest.SKILLS[hero]) fixtures.add(fixture(hero, id, 1));
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(
                directory.resolve(filename(hero) + "-skills.gif").toFile())) {
            writer.setOutput(output);
            writer.prepareWriteSequence(null);
            for (int frame = 0; frame < 64; frame++) {
                if (frame > 0) for (CombatSkillSmokeTest.Fixture fixture : fixtures) fixture.advance(0.05);
                BufferedImage image = new BufferedImage(CELL_WIDTH * 2, CELL_HEIGHT * 2 + 40,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics();
                try {
                    g.setColor(new Color(12, 16, 25));
                    g.fillRect(0, 0, image.getWidth(), image.getHeight());
                    title(g, HEROES[hero] + " | skill effects", 12, 27, 19);
                    for (int slot = 0; slot < fixtures.size(); slot++) {
                        CombatSkillSmokeTest.Fixture fixture = fixtures.get(slot);
                        AbilityDefinition definition = fixture.skill(CombatSkillSmokeTest.SKILLS[hero][slot]).getDefinition();
                        double elapsed = frame * 0.05;
                        drawCell(g, fixture, (slot % 2) * CELL_WIDTH, 40 + (slot / 2) * CELL_HEIGHT,
                                definition, AbilityAnimationTiming.stateAt(definition, elapsed)
                                        + String.format(" | %.2fs", elapsed));
                    }
                } finally { g.dispose(); }
                IIOMetadata metadata = writer.getDefaultImageMetadata(
                        javax.imageio.ImageTypeSpecifier.createFromRenderedImage(image), writer.getDefaultWriteParam());
                configureGif(metadata, frame == 0);
                writer.writeToSequence(new IIOImage(image, null, metadata), writer.getDefaultWriteParam());
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
    }

    private static CombatSkillSmokeTest.Fixture fixture(int hero, String id, int facing) throws Exception {
        CombatSkillSmokeTest.Fixture fixture = new CombatSkillSmokeTest.Fixture(hero, facing);
        double distance = hero == 3 || id.equals("holy_bolt") || id.equals("shadow_step") ? 150
                : id.equals("shield_bash") ? 50 : 80;
        fixture.visualEnemy(facing * distance, 0);
        fixture.visualEnemy(facing * (distance + 60), 20);
        fixture.player.takeDamage(25);
        fixture.cast(id);
        return fixture;
    }

    private static void drawCell(Graphics2D graphics, CombatSkillSmokeTest.Fixture fixture,
            int x, int y, AbilityDefinition definition, String detail) throws Exception {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.setClip(2, 2, CELL_WIDTH - 4, CELL_HEIGHT - 4);
            g.setColor(new Color(20, 29, 32));
            g.fillRect(0, 0, CELL_WIDTH, CELL_HEIGHT);
            g.setColor(new Color(29, 41, 43));
            for (int lineX = 0; lineX < CELL_WIDTH; lineX += 30) g.drawLine(lineX, 46, lineX, CELL_HEIGHT);
            for (int lineY = 60; lineY < CELL_HEIGHT; lineY += 30) g.drawLine(0, lineY, CELL_WIDTH, lineY);
            Graphics2D scene = (Graphics2D) g.create();
            try {
                scene.setClip(2, 44, CELL_WIDTH - 4, CELL_HEIGHT - 58);
                boolean wave = definition.getId().equals("elemental_storm")
                        || definition.getId().equals("earthbreaker") || definition.getId().equals("guardians_roar");
                scene.translate(CELL_WIDTH / 2.0, wave ? 155 : 181);
                double zoom = wave ? Math.min(0.9, 100 / Math.max(100, definition.getRadius())) : 0.9;
                scene.scale(zoom, zoom);
                scene.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                fixture.logic.drawAbilityGroundEffects(scene, 0, 0);
                fixture.logic.drawEntities(scene, 0, 0);
                fixture.logic.drawAbilityBursts(scene, 0, 0);
            } finally { scene.dispose(); }
            g.setColor(new Color(15, 22, 28));
            g.fillRect(0, 0, CELL_WIDTH, 44);
            title(g, definition.getName(), 10, 20, 14);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            g.setColor(new Color(154, 181, 183));
            g.drawString(detail, 10, 36);
        } finally { g.dispose(); }
    }

    private static void configureGif(IIOMetadata metadata, boolean first) throws Exception {
        String format = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
        IIOMetadataNode control = node(root, "GraphicControlExtension");
        control.setAttribute("disposalMethod", "none");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", "FALSE");
        control.setAttribute("delayTime", "5");
        control.setAttribute("transparentColorIndex", "0");
        if (first) {
            IIOMetadataNode extensions = node(root, "ApplicationExtensions");
            IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
            loop.setAttribute("applicationID", "NETSCAPE");
            loop.setAttribute("authenticationCode", "2.0");
            loop.setUserObject(new byte[] {1, 0, 0});
            extensions.appendChild(loop);
        }
        metadata.setFromTree(format, root);
    }

    private static IIOMetadataNode node(IIOMetadataNode root, String name) {
        for (int index = 0; index < root.getLength(); index++) {
            if (root.item(index).getNodeName().equals(name)) return (IIOMetadataNode) root.item(index);
        }
        IIOMetadataNode child = new IIOMetadataNode(name);
        root.appendChild(child);
        return child;
    }

    private static void title(Graphics2D g, String text, int x, int y, int size) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
        g.setColor(new Color(229, 233, 226));
        g.drawString(text, x, y);
    }

    private static String filename(int hero) { return HEROES[hero].toLowerCase().replace(' ', '-'); }
}
