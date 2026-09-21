import java.util.*;
public class TempCheck {
  public static void main(String[] args) {
    TemplateAbility a = new TemplateAbility();
    System.out.println("ready=" + a.isReady() + " cooldown=" + a.getCooldownRemaining());
    a.reset();
    System.out.println("after reset ready=" + a.isReady() + " cooldown=" + a.getCooldownRemaining());
    Enemy e = new TemplateEnemy(0,0);
    List<Enemy> enemies = new ArrayList<>();
    enemies.add(e);
    a.trigger(0,0,enemies);
    System.out.println("after trigger ready=" + a.isReady() + " cooldown=" + a.getCooldownRemaining());
  }
}
