import javax.swing.JFrame;

public class GameFrame {
    public GameFrame() {
        JFrame frame = new JFrame();
        GamePanel panel = new GamePanel();
        frame.add(panel);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowDeactivated(java.awt.event.WindowEvent event) {
                panel.releaseInputState();
            }
        });
        frame.setTitle("RPG");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
