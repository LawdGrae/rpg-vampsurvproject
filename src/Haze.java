import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Haze extends Player {

    private static final String SPRITE_PATH = "/assets/character/Haze_Final.png";
    private static final double SPEED = 200.0;
    private static final double ANIMATION_SPEED = 5.0;
    private static final int SPRITE_SCALE = 1;
    private static final int SPRITE_WIDTH = 64;
    private static final int SPRITE_HEIGHT = 64;
    private static final double MAX_HEALTH = 100.0;

    // ---- Haze's animation settings. Change these numbers to tune how it feels. ----
    private static final double RUN_MULTIPLIER = 1.6;        // running is 1.6x faster
    private static final double RUN_LEAN_DEGREES = 9.0;      // how far he leans when running sideways
    private static final double JUMP_DURATION = 0.55;        // seconds in the air
    private static final double JUMP_HEIGHT = 40.0;          // pixels up at the peak
    private static final double ATTACK_DURATION = 0.4;       // seconds for one swing
    private static final double SLASH_RADIUS = 30.0;         // size of the slash arc
    private static final double HURT_FLASH_TIME = 0.2;       // seconds the body stays red
    private static final double HURT_SILHOUETTE_TIME = 0.08; // first moment is a solid red shape
    private static final float HURT_TINT_STRENGTH = 0.55f;   // red strength after that
    private static final double DEATH_FALL_TIME = 0.6;       // seconds to fall over
    private static final double DEATH_SETTLE_TIME = 0.25;    // small bounce when he lands
    private static final double DUST_LIFETIME = 0.4;
    private static final double DUST_INTERVAL = 0.07;
    private static final int TINT_PADDING = 20;
    private static final int BAR_START = 3;                  // rows under the feet before the health bar

    private final Set<String> heldDirections = new HashSet<>();
    private final List<Dust> dust = new ArrayList<>();
    private BufferedImage buffer;
    private boolean running;
    private boolean jumping;
    private boolean jumpKeyHeld;
    private boolean attacking;
    private boolean dead;
    private boolean deathDustDone;
    private double health = MAX_HEALTH;
    private double jumpTime;
    private double attackTime;
    private double deathTime;
    private double hurtTimer;
    private double hurtElapsed;
    private double moveTime;
    private double idleTime;
    private double dustTimer;

    // A small puff of dust that stays where it was created in the world.
    private static class Dust {
        double x;
        double y;
        double vx;
        double size;
        double age;

        Dust(double x, double y, double vx, double size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.size = size;
        }
    }

    public Haze() {
        super(SPRITE_PATH, SPEED, ANIMATION_SPEED,
            SPRITE_SCALE, SPRITE_WIDTH, SPRITE_HEIGHT, MAX_HEALTH);
    }

    // Plays one swing. Called by the "attack" key, and can be called by the game later.
    public void startAttack() {
        if (!dead && !attacking) {
            attacking = true;
            attackTime = 0;
        }
    }

    public boolean isDead() {
        return dead;
    }

    @Override
    public void setKeyPressed(String direction, boolean pressed) {
        if (dead) {
            return; // a fallen Haze ignores the keyboard
        }
        // GamePanel sends "run", "jump" and "attack" like it sends "up", "down", etc.
        if (direction.equals("run")) {
            running = pressed;
            return;
        }
        if (direction.equals("jump")) {
            // Only jump on a fresh key press, so holding Space does not bounce forever.
            if (pressed && !jumpKeyHeld && !jumping) {
                jumping = true;
                jumpTime = 0;
            }
            jumpKeyHeld = pressed;
            return;
        }
        if (direction.equals("attack")) {
            if (pressed) {
                startAttack();
            }
            return;
        }
        if (direction.equals("up") || direction.equals("down")
                || direction.equals("left") || direction.equals("right")) {
            if (pressed) {
                heldDirections.add(direction);
            } else {
                heldDirections.remove(direction);
            }
        }
        super.setKeyPressed(direction, pressed);
    }

    @Override
    public void update(double deltaTime) {
        idleTime += deltaTime;
        if (hurtTimer > 0) {
            hurtTimer -= deltaTime;
            hurtElapsed += deltaTime;
        }

        if (dead) {
            // No more movement. Just play the fall and let the dust settle.
            deathTime += deltaTime;
            if (!deathDustDone && deathTime >= DEATH_FALL_TIME) {
                deathDustDone = true;
                spawnLandingDust();
            }
            updateDust(deltaTime);
            return;
        }

        // Running makes time pass faster for movement and for the walk animation.
        double effectiveDelta = running ? deltaTime * RUN_MULTIPLIER : deltaTime;
        super.update(effectiveDelta);

        boolean moving = isMoving();
        if (moving) {
            moveTime += effectiveDelta;
        }

        if (attacking) {
            attackTime += deltaTime;
            if (attackTime >= ATTACK_DURATION) {
                attacking = false;
                attackTime = 0;
            }
        }

        if (jumping) {
            jumpTime += deltaTime;
            if (jumpTime >= JUMP_DURATION) {
                jumping = false;
                jumpTime = 0;
                spawnLandingDust();
            }
        }

        // Puffs of dust behind his feet while running on the ground.
        dustTimer += deltaTime;
        if (running && moving && !jumping && dustTimer >= DUST_INTERVAL) {
            dustTimer = 0;
            addDust((Math.random() - 0.5) * 20.0, 5.0);
        }
        updateDust(deltaTime);
    }

    private void updateDust(double deltaTime) {
        for (Dust puff : dust) {
            puff.age += deltaTime;
            puff.x += puff.vx * deltaTime;
            puff.y -= 12.0 * deltaTime;
        }
        dust.removeIf(puff -> puff.age >= DUST_LIFETIME);
    }

    @Override
    public void takeDamage(double damage) {
        if (dead) {
            return;
        }
        super.takeDamage(damage);
        if (damage > 0) {
            if (hurtTimer <= 0) {
                hurtElapsed = 0; // a new hit starts the flash from the beginning
            }
            hurtTimer = HURT_FLASH_TIME;
        }
        health = Math.max(0, health - damage);
        if (health <= 0) {
            // Out of health: stop everything and start the death animation.
            dead = true;
            deathTime = 0;
            running = false;
            jumping = false;
            attacking = false;
            heldDirections.clear();
        }
    }

    private int directionX() {
        return (heldDirections.contains("right") ? 1 : 0)
                - (heldDirections.contains("left") ? 1 : 0);
    }

    private int directionY() {
        return (heldDirections.contains("down") ? 1 : 0)
                - (heldDirections.contains("up") ? 1 : 0);
    }

    private boolean isMoving() {
        return directionX() != 0 || directionY() != 0;
    }

    // How high above the ground Haze is right now (a smooth up-and-down arc).
    private double jumpLift() {
        if (!jumping) {
            return 0;
        }
        double t = jumpTime / JUMP_DURATION;
        return JUMP_HEIGHT * 4 * t * (1 - t);
    }

    private void addDust(double vx, double size) {
        double feetY = getWorldY() + spriteHeight * spriteScale / 2.0 - 3;
        dust.add(new Dust(getWorldX(), feetY, vx, size));
    }

    private void spawnLandingDust() {
        for (int i = -2; i <= 2; i++) {
            addDust(i * 30.0, 6.0 + Math.abs(i));
        }
    }

    private void drawDust(Graphics2D graphics, int centerX, int centerY) {
        for (Dust puff : dust) {
            double life = puff.age / DUST_LIFETIME;
            int alpha = Math.max(0, Math.min(255, (int) (130 * (1.0 - life))));
            int size = (int) Math.round(puff.size * (1.0 + life * 1.5));
            // Dust is stored in world position, so it stays behind as Haze moves on.
            int screenX = (int) Math.round(centerX + puff.x - getWorldX());
            int screenY = (int) Math.round(centerY + puff.y - getWorldY());
            graphics.setColor(new Color(225, 225, 225, alpha));
            graphics.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        }
    }

    // The glowing slash that sweeps across in front of Haze during an attack.
    private void drawSlash(Graphics2D graphics, int centerX, int feetY) {
        double p = attackTime / ATTACK_DURATION;
        if (!attacking || p < 0.25 || p > 0.85) {
            return;
        }
        double sweep = Math.min(1.0, (p - 0.25) / 0.3);
        double fade = p < 0.6 ? 1.0 : Math.max(0.0, 1.0 - (p - 0.6) / 0.25);
        int facingX = getFacingX();
        int facingY = getFacingY();
        double baseAngle = Math.toDegrees(Math.atan2(-facingY, facingX));
        double arcX = centerX + facingX * 18;
        double arcY = feetY - 28 + facingY * 18;

        Graphics2D slash = (Graphics2D) graphics.create();
        slash.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        double start = baseAngle - 70;
        double extent = 140 * sweep;
        Arc2D outer = new Arc2D.Double(arcX - SLASH_RADIUS, arcY - SLASH_RADIUS,
                SLASH_RADIUS * 2, SLASH_RADIUS * 2, start, extent, Arc2D.OPEN);
        double lineRadius = SLASH_RADIUS + 9;
        Arc2D swoosh = new Arc2D.Double(arcX - lineRadius, arcY - lineRadius,
                lineRadius * 2, lineRadius * 2, start + 10, extent * 0.8, Arc2D.OPEN);

        slash.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        slash.setColor(new Color(150, 200, 255, (int) (70 * fade)));
        slash.draw(outer);
        slash.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        slash.setColor(new Color(210, 235, 255, (int) (150 * fade)));
        slash.draw(outer);
        slash.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        slash.setColor(new Color(255, 255, 255, (int) (240 * fade)));
        slash.draw(outer);
        slash.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        slash.setColor(new Color(255, 255, 255, (int) (160 * fade)));
        slash.draw(swoosh);
        slash.dispose();
    }

    @Override
    public void draw(Graphics2D graphics, int centerX, int centerY) {
        int frameWidth = spriteWidth * spriteScale;
        int frameHeight = spriteHeight * spriteScale;
        int feetY = centerY + frameHeight / 2;
        boolean moving = isMoving();
        boolean hurt = hurtTimer > 0;

        drawDust(graphics, centerX, centerY);

        double lift = jumpLift();
        if (lift > 0) {
            // The shadow stays on the ground while Haze is in the air.
            graphics.setColor(new Color(0, 0, 0, 90));
            graphics.fillOval(centerX - 14, feetY - 8, 28, 8);
        }

        // Work out how the body is squashed, tilted, moved, or bouncing this frame.
        double scaleX = 1.0;
        double scaleY = 1.0;
        double bob = 0.0;
        double lean = 0.0;
        double offsetX = 0.0;
        double offsetY = 0.0;
        if (jumping) {
            double t = jumpTime / JUMP_DURATION;
            if (t < 0.12 || t > 0.88) {          // crouch at take-off and landing
                scaleX = 1.12;
                scaleY = 0.85;
            } else if (t < 0.5) {                // stretch while going up
                scaleX = 0.94;
                scaleY = 1.10;
            } else {                             // slightly stretched while coming down
                scaleX = 0.97;
                scaleY = 1.05;
            }
            lean = directionX() * 8.0;           // tilt a little toward where he is going
        } else if (moving) {
            double amplitude = running ? 3.0 : 1.5;
            bob = Math.abs(Math.sin(moveTime * Math.PI * ANIMATION_SPEED / 2.0)) * amplitude;
            if (running) {
                lean = directionX() * RUN_LEAN_DEGREES
                        + Math.sin(moveTime * Math.PI * ANIMATION_SPEED) * 2.0;
            }
        } else {
            // Idle: gentle breathing.
            scaleY = 1.0 + 0.025 * Math.sin(idleTime * 3.5);
            scaleX = 1.0 - 0.012 * Math.sin(idleTime * 3.5);
        }

        if (attacking) {
            // Wind up (lean back), lunge forward while the slash happens, then recover.
            double p = attackTime / ATTACK_DURATION;
            double lunge;
            double swing;
            if (p < 0.25) {
                double w = p / 0.25;
                swing = -10.0 * w;
                lunge = -3.0 * w;
            } else if (p < 0.6) {
                swing = 14.0;
                lunge = 10.0;
            } else {
                double r = (p - 0.6) / 0.4;
                swing = 14.0 * (1 - r);
                lunge = 10.0 * (1 - r);
            }
            lean += getFacingX() * swing;
            offsetX += getFacingX() * lunge;
            offsetY += getFacingY() * lunge * 0.6;
        }

        double shake = 0.0;
        if (hurt && !dead && hurtElapsed < 0.2) {
            // Knocked back a little, and shaking.
            lean += -getFacingX() * 10.0 * (1 - hurtElapsed / 0.2);
            if (hurtElapsed < 0.15) {
                shake = ((int) (hurtElapsed * 60) % 2 == 0) ? 2.0 : -2.0;
            }
        }

        if (dead) {
            // Death: stagger, fall over sideways, small bounce, then lie still.
            scaleX = 1.0;
            scaleY = 1.0;
            bob = 0.0;
            offsetX = 0.0;
            offsetY = 0.0;
            double fallSide = getFacingX() > 0 ? -1.0 : 1.0;
            double t = Math.min(1.0, deathTime / DEATH_FALL_TIME);
            double angle = 90.0 * t * t;
            if (deathTime > DEATH_FALL_TIME) {
                double s = Math.min(1.0, (deathTime - DEATH_FALL_TIME) / DEATH_SETTLE_TIME);
                angle = 90.0 - 8.0 * Math.sin(Math.PI * s);
                scaleY = 1.0 - 0.06 * Math.sin(Math.PI * s);
            }
            lean = fallSide * angle;
            offsetX = fallSide * 8.0 * t;
            offsetY = 12.0 * t;
        }

        // Draw Haze (and his health bar) on a temporary image first.
        int bufferWidth = frameWidth + TINT_PADDING * 2;
        int bufferHeight = frameHeight + TINT_PADDING * 2 + 20;
        if (buffer == null || buffer.getWidth() != bufferWidth
                || buffer.getHeight() != bufferHeight) {
            buffer = new BufferedImage(bufferWidth, bufferHeight, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D bufferGraphics = buffer.createGraphics();
        bufferGraphics.setComposite(AlphaComposite.Clear);
        bufferGraphics.fillRect(0, 0, bufferWidth, bufferHeight);
        bufferGraphics.setComposite(AlphaComposite.SrcOver);
        super.draw(bufferGraphics, bufferWidth / 2, TINT_PADDING + frameHeight / 2);
        if (hurt) {
            // First a solid red shape, then a softer red tint on his body pixels only.
            float strength = hurtElapsed < HURT_SILHOUETTE_TIME ? 1.0f : HURT_TINT_STRENGTH;
            bufferGraphics.setComposite(AlphaComposite.SrcAtop.derive(strength));
            bufferGraphics.setColor(Color.RED);
            bufferGraphics.fillRect(TINT_PADDING, TINT_PADDING, frameWidth, frameHeight);
        } else if (dead) {
            // A fallen Haze is a little darker.
            bufferGraphics.setComposite(AlphaComposite.SrcAtop.derive(0.3f));
            bufferGraphics.setColor(Color.BLACK);
            bufferGraphics.fillRect(TINT_PADDING, TINT_PADDING, frameWidth, frameHeight);
        }
        bufferGraphics.dispose();

        // Split the temporary image: the body moves and bends, the health bar stays put.
        int bufferX = centerX - bufferWidth / 2;
        int bufferY = centerY - TINT_PADDING - frameHeight / 2;
        int barTop = TINT_PADDING + frameHeight + BAR_START;
        BufferedImage bodyPart = buffer.getSubimage(0, 0, bufferWidth, barTop);
        BufferedImage barPart = buffer.getSubimage(0, barTop, bufferWidth, bufferHeight - barTop);

        Graphics2D body = (Graphics2D) graphics.create();
        // Everything pivots at his feet.
        body.translate(centerX + shake + offsetX, feetY - lift - bob + offsetY);
        body.rotate(Math.toRadians(lean));
        body.scale(scaleX, scaleY);
        body.translate(-centerX, -feetY);
        body.drawImage(bodyPart, bufferX, bufferY, null);
        body.dispose();

        graphics.drawImage(barPart, bufferX, bufferY + barTop, null);
        drawSlash(graphics, centerX, feetY);
    }
}