import javax.swing.JFrame;

public class GameFrame {
    public GameFrame() {
        JFrame frame = new JFrame();
        frame.add(new GamePanel());
        frame.setTitle("RPG");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
