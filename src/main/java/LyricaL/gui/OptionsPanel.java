package LyricaL.gui;

import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class OptionsPanel extends JPanel{
    public OptionsPanel(JFrame frame) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS)); // Stack buttons vertically

        // Create radio buttons for window sizes
        JRadioButton smallButton = new JRadioButton("Small (400x300)",true);
        JRadioButton mediumButton = new JRadioButton("Medium (600x400)"); // Default selected
        JRadioButton largeButton = new JRadioButton("Large (800x600)");
        ButtonGroup group = new ButtonGroup();
        group.add(smallButton);
        group.add(mediumButton);
        group.add(largeButton);
        ActionListener sizeListener = e -> {
            if (smallButton.isSelected()) {
                frame.setSize(400, 300);
            } else if (mediumButton.isSelected()) {
                frame.setSize(600, 400);
            } else if (largeButton.isSelected()) {
                frame.setSize(800, 600);
            }
        };
        smallButton.addActionListener(sizeListener);
        mediumButton.addActionListener(sizeListener);
        largeButton.addActionListener(sizeListener);

        panel.add(smallButton);
        panel.add(mediumButton);
        panel.add(largeButton);
        this.add(panel);
    }
}
