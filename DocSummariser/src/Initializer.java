import javax.swing.*;
public class Initializer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SummarizerGUI().setVisible(true);
            }
        });
    }
}
