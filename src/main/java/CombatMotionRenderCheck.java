import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/** Reviewable snapshots of actual combat runtime in both horizontal facings. */
public class CombatMotionRenderCheck {
    private static final String[] HEROES = {"Black Knight", "Assassin", "Priest", "Elementalist", "Guardian"};
    private static final String[] STAGES = {"Prepare", "Windup", "Release", "Travel / contact", "Follow through", "Recovery"};
    private static final int CELL_WIDTH = 330, CELL_HEIGHT = 235;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File directory = new File(args.length > 0 ? args[0] : "out/combat-motion-check");
        Files.createDirectories(directory.toPath());
        for (int hero = 0; hero < HEROES.length; hero++) renderClass(directory, hero);
        System.out.println("Rendered 240 combat snapshots: 20 skills in left/right facings across six stages: "
                + directory.getAbsolutePath());
    }

    private static void renderClass(File directory, int hero) throws Exception {
        BufferedImage sheet = new BufferedImage(CELL_WIDTH * STAGES.length, CELL_HEIGHT * 8 + 44,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(17, 22, 28));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(new Color(239, 223, 186));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        g.drawString(HEROES[hero] + " | weapon grips and contact timing"
                + (SkillEffectAtlas.hasAtlas() ? "" : " (built-in skill VFX)"), 12, 30);
        for (int skill = 0; skill < CombatSkillSmokeTest.SKILLS[hero].length; skill++) {
            String id = CombatSkillSmokeTest.SKILLS[hero][skill];
            for (int facing : new int[] {1, -1}) {
                CombatSkillSmokeTest.Fixture fixture = new CombatSkillSmokeTest.Fixture(hero, facing);
                double targetDistance = hero == 3 || id.equals("holy_bolt") ? 160
                        : id.equals("shield_bash") ? 50 : id.equals("shadow_step") ? 160 : 85;
                CombatSkillSmokeTest.TestEnemy target = fixture.visualEnemy(facing * targetDistance, 0);
                fixture.visualEnemy(facing * (targetDistance + 75), 8);
                fixture.player.takeDamage(28);
                fixture.cast(id);
                AbilityDefinition definition = fixture.skill(id).getDefinition();
                double duration = AbilityAnimationTiming.duration(definition);
                double release = duration * AbilityAnimationTiming.releaseProgress(definition);
                double contact = AbilityAnimationTiming.hitTimes(definition)[0];
                double[] times = {duration * 0.09, Math.max(duration * 0.12, release - 0.025),
                        release + 0.016, Math.max(contact + 0.04, release + 0.17),
                        Math.max(contact + 0.25, duration * 0.98), duration + 0.45};
                double elapsed = 0;
                for (int stage = 0; stage < STAGES.length; stage++) {
                    fixture.advance(Math.max(0, times[stage] - elapsed));
                    elapsed = times[stage];
                    int left = stage * CELL_WIDTH;
                    int top = 44 + (skill * 2 + (facing > 0 ? 0 : 1)) * CELL_HEIGHT;
                    g.setColor(new Color(31, 41, 38));
                    g.fillRect(left + 2, top + 2, CELL_WIDTH - 4, CELL_HEIGHT - 4);
                    Graphics2D scene = (Graphics2D) g.create();
                    scene.setClip(left + 3, top + 42, CELL_WIDTH - 6, CELL_HEIGHT - 70);
                    scene.translate(left + CELL_WIDTH / 2.0, top + 138);
                    scene.scale(1.25, 1.25);
                    scene.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    fixture.logic.drawAbilityGroundEffects(scene, 0, 0);
                    fixture.logic.drawEntities(scene, 0, 0);
                    fixture.logic.drawAbilityBursts(scene, 0, 0);
                    scene.dispose();
                    g.setColor(new Color(239, 223, 186));
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
                    g.drawString(definition.getName() + " / " + (facing > 0 ? "right" : "left"), left + 9, top + 21);
                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    g.drawString(STAGES[stage] + String.format("  %.3fs", elapsed), left + 9, top + 38);
                    g.drawString("target HP " + Math.round(target.getHealth()) + " | contacts " + target.damageCount,
                            left + 9, top + CELL_HEIGHT - 11);
                }
            }
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(directory,
                HEROES[hero].toLowerCase().replace(' ', '-') + "-combat.png"));
    }
}
