import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Headless review of procedural contact effects and shared glow compositing. */
public final class ContactVfxRenderCheck {
    private ContactVfxRenderCheck() { }

    public static void main(String[] args) throws Exception {
        checkGlow();
        Path outputDirectory = Path.of(args.length == 0 ? "out/survival-review" : args[0]);
        Files.createDirectories(outputDirectory);
        Path output = outputDirectory.resolve("contact-vfx.png");
        ImageIO.write(renderContacts(), "png", output.toFile());
        System.out.println("Contact VFX checks passed; rendered " + output.toAbsolutePath());
    }

    private static void checkGlow() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(AlphaComposite.SrcOver.derive(0.45f));
            g.setColor(Color.MAGENTA);
            g.setStroke(new BasicStroke(3));
            g.translate(3, 2);
            Composite composite = g.getComposite();
            AffineTransform transform = g.getTransform();
            Color color = g.getColor();
            Object stroke = g.getStroke();
            SkillVfxGlow.draw(g, 28, 29, 22, Color.CYAN, 0.8);
            require(g.getComposite().equals(composite), "Glow changed the caller's composite");
            require(g.getTransform().equals(transform), "Glow changed the caller's transform");
            require(g.getColor().equals(color), "Glow changed the caller's color");
            require(g.getStroke().equals(stroke), "Glow changed the caller's stroke");
        } finally {
            g.dispose();
        }

        BufferedImage invalid = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        g = invalid.createGraphics();
        try {
            SkillVfxGlow.draw(g, Double.NaN, 32, 24, Color.CYAN, 1);
            SkillVfxGlow.draw(g, 32, Double.POSITIVE_INFINITY, 24, Color.CYAN, 1);
            SkillVfxGlow.draw(g, 32, 32, Double.NaN, Color.CYAN, 1);
            SkillVfxGlow.draw(g, 32, 32, Double.POSITIVE_INFINITY, Color.CYAN, 1);
            SkillVfxGlow.draw(g, 32, 32, 24, Color.CYAN, Double.NaN);
            SkillVfxGlow.draw(g, 32, 32, 24, new Color(0, 255, 255, 0), 1);
        } finally {
            g.dispose();
        }
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                require(invalid.getRGB(x, y) == 0, "Invalid or transparent glow painted pixels");
            }
        }

        int full = glowCenterAlpha(new Color(180, 100, 255));
        int half = glowCenterAlpha(new Color(180, 100, 255, 128));
        require(full > 150, "Glow core is unexpectedly dim");
        require(Math.abs(full - half * 2) <= 2, "Glow did not honor the color's alpha");
    }

    private static int glowCenterAlpha(Color color) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            SkillVfxGlow.draw(g, 32, 32, 32, color, 1);
        } finally {
            g.dispose();
        }
        return image.getRGB(32, 32) >>> 24;
    }

    private static BufferedImage renderContacts() {
        double[] ages = {0, 0.04, 0.08, 0.14, 0.23, 0.35};
        DamageElement[] elements = DamageElement.values();
        int cellWidth = 140;
        int cellHeight = 128;
        int header = 44;
        BufferedImage image = new BufferedImage(cellWidth * ages.length,
                header + cellHeight * elements.length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(14, 19, 28));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
            g.setColor(new Color(220, 231, 246));
            g.drawString("Procedural skill contacts / time after impact", 16, 27);
            for (int row = 0; row < elements.length; row++) {
                for (int column = 0; column < ages.length; column++) {
                    int left = column * cellWidth;
                    int top = header + row * cellHeight;
                    Graphics2D cell = (Graphics2D) g.create(left, top, cellWidth, cellHeight);
                    try {
                        cell.setColor(new Color(19 + row % 2 * 3, 25 + row % 2 * 3, 36 + row % 2 * 3));
                        cell.fillRect(0, 0, cellWidth, cellHeight);
                        cell.setColor(new Color(41, 50, 66));
                        cell.drawRect(0, 0, cellWidth - 1, cellHeight - 1);
                        cell.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
                        cell.setColor(new Color(163, 180, 202));
                        cell.drawString(elements[row].name() + " / " + ages[column] + "s", 9, 18);
                        cell.setColor(new Color(48, 58, 72));
                        cell.fillOval(cellWidth / 2 - 11, 72, 22, 10);
                        CombatImpactEffect effect = new CombatImpactEffect("polish_preview", 0, 0, elements[row]);
                        effect.update(ages[column]);
                        effect.draw(cell, cellWidth / 2, 76, 0, 0);
                    } finally {
                        cell.dispose();
                    }
                }
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
