package LyricaL.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

//import javafx.scene.paint.Color;

public class ThemePanel extends JPanel {
    public ThemePanel(JFrame frame){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 10, 10)); // Grid layout for buttons

        JLabel label = new JLabel("Select Theme:");
        panel.add(label);

        JButton lightButton = new JButton("Light");
        JButton darkButton = new JButton("Dark");
        JButton blueButton = new JButton("Blue");

        // Load saved theme from registry
        //String savedTheme = prefs.get(PREF_THEME, "Light");
        //applyTheme(frame, savedTheme);

        // Button actions
        lightButton.addActionListener(e -> changeTheme(frame, "Light"));
        darkButton.addActionListener(e -> changeTheme(frame, "Dark"));
        blueButton.addActionListener(e -> changeTheme(frame, "Blue"));

        // Add buttons
        panel.add(lightButton);
        panel.add(darkButton);
        panel.add(blueButton);
        this.add(panel);
    }
    private static void changeTheme(JFrame frame, String theme){
        applyTheme(frame,theme);

    }
    private static void applyTheme(JFrame frame, String theme) {
        Color newColor;
        Color textColor;
        switch (theme) {
            case "Dark":
                newColor = Color.DARK_GRAY;
                textColor = Color.WHITE;
                break;
            case "Blue":
                newColor = new Color(70, 130, 180); // SteelBlue
                textColor = Color.WHITE;
                break;
            default:
                newColor = Color.LIGHT_GRAY;
                textColor = Color.DARK_GRAY;
                break;
        }
        
        frame.getContentPane().setBackground(newColor);

        // Ensure all components update their colors
        for (Component c : frame.getContentPane().getComponents()) {
            c.setBackground(newColor);
            c.setForeground(textColor);
            
            if (c instanceof JPanel) {
                for (Component subC : ((JPanel) c).getComponents()) {
                    subC.setBackground(newColor);
                    subC.setForeground(textColor);
                }
            }
            
            // **Fix: Only cast to JTabbedPane if it actually is one**
            if (c instanceof JTabbedPane) {
                for (Component tab : ((JTabbedPane) c).getComponents()) {
                    tab.setForeground(textColor);
                    tab.setBackground(newColor);
                }
            }
        }
        frame.repaint();
    }
}
