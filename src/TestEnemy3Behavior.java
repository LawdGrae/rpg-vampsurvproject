public class TestEnemy3Behavior {
    public static void main(String[] args) {
        TemplateEnemy3 boss = new TemplateEnemy3(100, 100);
        if (boss.getMaxHealth() <= 0) {
            throw new AssertionError("Boss should have health");
        }
        if (boss.getReflectEveryHits() != 5) {
            throw new AssertionError("Boss reflection interval should be five hits");
        }
        System.out.println("Enemy3 behavior checks passed");
    }
}
