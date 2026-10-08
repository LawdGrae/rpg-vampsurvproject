public class App {
    public static void main(String[] args) throws Exception {
        AbilitySoundPlayer.preload();
        javax.swing.SwingUtilities.invokeLater(GameFrame::new);
    }
}
