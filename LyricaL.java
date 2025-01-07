package LyricaL;

import javax.swing.*;
import com.formdev.flatlaf.FlatDarkLaf;

public class LyricaL {
    public static void main(String[] args) {
        FlatDarkLaf.setup();
        JFrame frame = new JFrame("Test");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}