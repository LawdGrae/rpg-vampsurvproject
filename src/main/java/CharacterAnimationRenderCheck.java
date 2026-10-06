import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Generates inspectable temporal evidence from the actual player and skill renderers. */
public class CharacterAnimationRenderCheck {
    private static final String[] NAMES = {"Eumann", "Haze", "Yuexin", "Ziea"};
    private static final String[] CASTS = {"heavy_slash", "twin_fang", "holy_bolt", "ice_shard"};
    private static final String[] COMBOS = {"knights_wrath", "silent_execution", "divine_light", "elemental_storm"};
    private static final Color BACKGROUND = new Color(15, 19, 35);
    private static final Color PANEL = new Color(27, 33, 52);
    private static final Color TEXT = new Color(231, 225, 202);
    private static final int CELL_WIDTH = 240;
    private static final int CELL_HEIGHT = 280;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File directory = new File(args.length > 0 ? args[0] : "out");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create " + directory);
        }
        renderAnimation(new File(directory, "character-animation.gif"),
                new File(directory, "character-animation-frames.png"),
                new File(directory, "character-combo-frames.png"));
        renderSkillContactSheet(new File(directory, "skill-animation-frames.png"));
    }

    private static void renderAnimation(File output, File stripOutput, File comboOutput) throws Exception {
        Player[] players = new Player[4];
        AbilityVisualEffect[] effects = new AbilityVisualEffect[4];
        AbilityDefinition[] casts = new AbilityDefinition[4];
        AbilityDefinition[] combos = new AbilityDefinition[4];
        List<AbilityDefinition> definitions = AbilityDefinition.createAll();
        for (int index = 0; index < players.length; index++) {
            players[index] = player(index);
            players[index].setKeyPressed("down", true);
            players[index].update(0.0);
            players[index].setKeyPressed("down", false);
            String id = CASTS[index];
            casts[index] = definitions.stream().filter(definition -> definition.getId().equals(id))
                    .findFirst().orElseThrow();
            String comboId = COMBOS[index];
            combos[index] = definitions.stream().filter(definition -> definition.getId().equals(comboId))
                    .findFirst().orElseThrow();
        }
        int width = CELL_WIDTH * 4;
        int height = CELL_HEIGHT + 74;
        BufferedImage strip = new BufferedImage(width, height * 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D stripGraphics = strip.createGraphics();
        BufferedImage comboStrip = new BufferedImage(width, height * 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D comboGraphics = comboStrip.createGraphics();
        int stripRow = 0;
        int comboRow = 0;
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (int frame = 0; frame < 132; frame++) {
                String state = frame < 20 ? "Idle breathing" : frame < 39 ? "Walk blend"
                        : frame < 48 ? "Settle" : frame < 70 ? "Weapon and skill cast"
                        : frame < 82 ? "Recovery" : frame < 112 ? "Combo and ultimate cast" : "Recovery";
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(BACKGROUND);
                graphics.fillRect(0, 0, width, height);
                graphics.setColor(TEXT);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
                graphics.drawString(state, 16, 27);
                for (int index = 0; index < players.length; index++) {
                    Player current = players[index];
                    if (frame == 20) current.setKeyPressed("right", true);
                    if (frame == 39) current.setKeyPressed("right", false);
                    if (frame == 48 || frame == 82) {
                        if (frame == 82) casts[index] = combos[index];
                        current.playAttackAnimation(casts[index], AbilityAnimationTiming.duration(casts[index]));
                        double startX = current.getWeaponCastWorldX();
                        double startY = current.getWeaponCastWorldY();
                        effects[index] = new AbilityVisualEffect(casts[index], startX, startY,
                                startX + 24, startY - 12, 56, color(index),
                                AbilityAnimationTiming.duration(casts[index]));
                    }
                    current.update(1.0 / 24.0);
                    if (effects[index] != null) {
                        effects[index].setCasterPosition(current.getWorldX(), current.getWorldY());
                        effects[index].setCastOrigin(current.getWeaponCastWorldX(), current.getWeaponCastWorldY());
                        effects[index].update(1.0 / 24.0);
                    }
                    int left = index * CELL_WIDTH;
                    graphics.setColor(PANEL);
                    graphics.fillRoundRect(left + 5, 40, CELL_WIDTH - 10, CELL_HEIGHT, 18, 18);
                    graphics.setColor(TEXT);
                    graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
                    graphics.drawString(NAMES[index], left + 16, 65);
                    graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                    graphics.drawString(casts[index].getName(), left + 16, 301);
                    Graphics2D scene = (Graphics2D) graphics.create();
                    scene.clipRect(left + 7, 73, CELL_WIDTH - 14, 211);
                    scene.translate(left + CELL_WIDTH / 2.0 - 28, 187);
                    scene.scale(2.0, 2.0);
                    scene.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    if (effects[index] != null && effects[index].getLayer() == AbilityVisualEffect.Layer.GROUND) {
                        effects[index].draw(scene, 0, 0, current.getWorldOffsetX(), current.getWorldOffsetY());
                    }
                    current.draw(scene, 0, 0);
                    if (effects[index] != null && effects[index].getLayer() == AbilityVisualEffect.Layer.FRONT) {
                        effects[index].draw(scene, 0, 0, current.getWorldOffsetX(), current.getWorldOffsetY());
                    }
                    scene.dispose();
                }
                graphics.setColor(TEXT);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                graphics.drawString("Rendered from gameplay poses • 24 frames/sec • idle → walk → skill → recovery",
                        16, height - 14);
                graphics.dispose();
                writer.writeToSequence(new IIOImage(image, null, gifMetadata(writer, image, frame)), null);
                if (frame == 0 || frame == 24 || frame == 42 || frame == 53 || frame == 58 || frame == 77) {
                    stripGraphics.drawImage(image, 0, stripRow++ * height, null);
                }
                if (frame == 83 || frame == 88 || frame == 92 || frame == 98 || frame == 106 || frame == 126) {
                    comboGraphics.drawImage(image, 0, comboRow++ * height, null);
                }
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
            stripGraphics.dispose();
            comboGraphics.dispose();
        }
        ImageIO.write(strip, "png", stripOutput);
        ImageIO.write(comboStrip, "png", comboOutput);
        System.out.println(output.getAbsolutePath());
        System.out.println(stripOutput.getAbsolutePath());
        System.out.println(comboOutput.getAbsolutePath());
    }

    static IIOMetadata gifMetadata(ImageWriter writer, BufferedImage image, int frame)
            throws Exception {
        IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), null);
        String format = metadata.getNativeMetadataFormatName();
        IIOMetadataNode tree = (IIOMetadataNode) metadata.getAsTree(format);
        IIOMetadataNode control = node(tree, "GraphicControlExtension");
        control.setAttribute("disposalMethod", "none");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", "FALSE");
        // GIF uses centiseconds; alternating 4/5 keeps playback at 24 fps on average.
        control.setAttribute("delayTime", frame % 6 == 0 ? "5" : "4");
        control.setAttribute("transparentColorIndex", "0");
        if (frame == 0) {
            IIOMetadataNode extension = new IIOMetadataNode("ApplicationExtension");
            extension.setAttribute("applicationID", "NETSCAPE");
            extension.setAttribute("authenticationCode", "2.0");
            extension.setUserObject(new byte[]{1, 0, 0});
            node(tree, "ApplicationExtensions").appendChild(extension);
        }
        metadata.setFromTree(format, tree);
        return metadata;
    }

    private static IIOMetadataNode node(IIOMetadataNode root, String name) {
        for (int index = 0; index < root.getLength(); index++) {
            if (root.item(index).getNodeName().equalsIgnoreCase(name)) return (IIOMetadataNode) root.item(index);
        }
        IIOMetadataNode result = new IIOMetadataNode(name);
        root.appendChild(result);
        return result;
    }

    private static void renderSkillContactSheet(File output) throws Exception {
        double[] progress = {0.08, 0.24, 0.44, 0.62, 0.82, 0.96};
        int labelWidth = 180;
        int cellWidth = 200;
        int cellHeight = 160;
        int header = 64;
        BufferedImage image = new BufferedImage(labelWidth + cellWidth * progress.length,
                header + cellHeight * 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(TEXT);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.drawString("Equipped skills • continuous anticipation, impact and dissipation", 16, 26);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        for (int frame = 0; frame < progress.length; frame++) {
            graphics.drawString(Math.round(progress[frame] * 100) + "%", labelWidth + frame * cellWidth + 15, 53);
        }
        GameLogic logic = new GameLogic();
        for (int index = 0; index < 4; index++) {
            List<RpgAbility> equipped = logic.getCharacterActiveAbilities(index);
            for (int slot = 0; slot < equipped.size(); slot++) {
                AbilityDefinition definition = equipped.get(slot).getDefinition();
                int rowY = header + (index * 4 + slot) * cellHeight;
                graphics.setColor(TEXT);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                graphics.drawString(NAMES[index], 16, rowY + 52);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                graphics.drawString(definition.getName(), 16, rowY + 74);
                for (int frame = 0; frame < progress.length; frame++) {
                    int cellX = labelWidth + frame * cellWidth;
                    graphics.setColor(PANEL);
                    graphics.fillRect(cellX + 2, rowY + 2, cellWidth - 4, cellHeight - 4);
                    double duration = AbilityAnimationTiming.duration(definition);
                    AbilityVisualEffect effect = new AbilityVisualEffect(definition,
                            -45, 0, 45, -4, 64, color(index), duration);
                    effect.setCasterPosition(-45, 0);
                    effect.update(progress[frame] * duration);
                    Graphics2D cell = (Graphics2D) graphics.create();
                    cell.clipRect(cellX + 2, rowY + 2, cellWidth - 4, cellHeight - 4);
                    effect.draw(cell, cellX + cellWidth / 2, rowY + cellHeight / 2, 0, 0);
                    cell.dispose();
                }
            }
        }
        graphics.dispose();
        ImageIO.write(image, "png", output);
        System.out.println(output.getAbsolutePath());
    }

    private static Color color(int index) {
        return switch (index) {
            case 1 -> new Color(183, 125, 255);
            case 2 -> new Color(255, 224, 143);
            case 3 -> new Color(117, 214, 255);
            default -> new Color(255, 129, 64);
        };
    }

    private static Player player(int index) {
        return switch (index) {
            case 1 -> new Character_Haze();
            case 2 -> new Character_Yuexin();
            case 3 -> new Character_Ziea();
            default -> new Character_Eumann();
        };
    }
}
