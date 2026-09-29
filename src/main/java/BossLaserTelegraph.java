import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

public class BossLaserTelegraph {
    private static final double WARNING_DURATION = 0.75;

    private final double startX;
    private final double startY;
    private final double targetX;
    private final double targetY;
    private final double speed;
    private final double damage;
    private final double radius;
    private final double animationSpeed;
    private final BufferedImage sprite;
    private final Enemy owner;
    private double remaining = WARNING_DURATION;

    public BossLaserTelegraph(double startX, double startY, double targetX, double targetY,
            double speed, double damage, double radius, double animationSpeed,
            BufferedImage sprite, Enemy owner) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
        this.damage = damage;
        this.radius = radius;
        this.animationSpeed = animationSpeed;
        this.sprite = sprite;
        this.owner = owner;
    }

    public boolean update(double deltaTime) {
        remaining = Math.max(0.0, remaining - deltaTime);
        return remaining <= 0.0;
    }

    public Projectile createProjectile() {
        Projectile projectile = new Projectile(startX, startY, targetX, targetY,
                speed, damage, radius, sprite, animationSpeed);
        projectile.setOwner(owner);
        return projectile;
    }

    public boolean isOwnerDead() {
        return owner == null || owner.isDead();
    }

    public void draw(Graphics2D graphics, int centerX, int centerY,
            double cameraX, double cameraY) {
        double progress = 1.0 - remaining / WARNING_DURATION;
        int alpha = (int) (100 + 130 * Math.abs(Math.sin(progress * Math.PI * 7.0)));
        double screenStartX = centerX + startX + cameraX;
        double screenStartY = centerY + startY + cameraY;
        double screenTargetX = centerX + targetX + cameraX;
        double screenTargetY = centerY + targetY + cameraY;
        Graphics2D warningGraphics = (Graphics2D) graphics.create();
        warningGraphics.setColor(new Color(255, 70, 70, alpha));
        warningGraphics.setStroke(new BasicStroke((float) (5.0 + progress * 3.0),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        warningGraphics.draw(new Line2D.Double(screenStartX, screenStartY,
                screenTargetX, screenTargetY));
        warningGraphics.setColor(new Color(255, 235, 160, Math.min(240, alpha + 20)));
        warningGraphics.setStroke(new BasicStroke(2f));
        warningGraphics.draw(new Line2D.Double(screenStartX, screenStartY,
                screenTargetX, screenTargetY));
        warningGraphics.fillOval((int) screenTargetX - 8, (int) screenTargetY - 8, 16, 16);
        warningGraphics.dispose();
    }
}
