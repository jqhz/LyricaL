package LyricaL.gui;
import java.awt.event.*;
import java.net.URL;
import java.awt.*;
import javax.swing.JMenu;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
//import javax.swing.border.Border;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.*;
import java.util.prefs.Preferences;

//import org.w3c.dom.Text;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;

import LyricaL.LyricaL;

public class GUI {
    private static final Preferences prefs = Preferences.userRoot().node("lyrical");
    private JFrame frame;
    private JLabel textArea;
    private JLabel secondText;
    static class KeySelectorEditor extends AbstractCellEditor implements TableCellEditor {
        private JTextField textField = new JTextField();

        public KeySelectorEditor() {
            textField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                    // Set the key pressed as text
                    textField.setText(KeyEvent.getKeyText(e.getKeyCode()));
                    stopCellEditing();
                }
            });
        }
        
        @Override
        public Object getCellEditorValue() {
            return textField.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            textField.setText(value != null ? value.toString() : "");
            return textField;
        }
    }
    

    
    public void frame_gui(){
        FlatDarkLaf.setup();
        frame = new JFrame("LyricaL");
        URL logoURL = LyricaL.class.getResource("/logo3.png");
        ImageIcon logo = new ImageIcon(logoURL);
        Image resizedImage = logo.getImage().getScaledInstance(256, 256, Image.SCALE_SMOOTH);        
        JFrame settingsFrame = new JFrame("Settings");
        //JFrame optionsFrame = new JFrame("Options");
        frame.setUndecorated(true);
        frame.setIconImage(resizedImage);
        //frame.setIconImage(logo.getImage());
        //frame.setBackground(new Color(0, 0, 0, 0));

        frame.setOpacity(0.8f);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setAlwaysOnTop(true);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Options",new OptionsPanel(frame));
        tabbedPane.addTab("Themes",new ThemePanel(frame));
        HotkeysPanel hotkeysPanel = new HotkeysPanel(frame, tabbedPane);
        //tabbedPane.addTab("Keybinds",new HotkeysPanel(frame));
        tabbedPane.addTab("Language",new JPanel());
        JPanel contentPanel = new JPanel();
        //contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
        JPanel titleBar = new JPanel();
        titleBar.setBackground(Color.DARK_GRAY);
        //titleBar.setOpaque(false);
        titleBar.setLayout(new FlowLayout(FlowLayout.RIGHT,5,5));
        URL minimizeIconURL = LyricaL.class.getResource("/minimize_icon.png");
        URL closeIconURL = LyricaL.class.getResource("/exit_icon.png");
        URL settingsIconURL = LyricaL.class.getResource("/settings_icon.png");
        if (minimizeIconURL == null || closeIconURL == null) {
            System.err.println("Resource files not found. Ensure minimize_icon.png and exit_icon.png are in the correct directory.");
            System.exit(1);
        }
        ImageIcon minimizeIcon = new ImageIcon(minimizeIconURL);
        ImageIcon closeIcon = new ImageIcon(closeIconURL);
        ImageIcon settingsIcon = new ImageIcon(settingsIconURL);
        JLabel minimizeLabel = new JLabel(minimizeIcon);
        minimizeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minimizeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                frame.setState(Frame.ICONIFIED); // Minimize the frame
            }
        });

        // Create the close button
        
        JLabel closeLabel = new JLabel(closeIcon);
        closeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.exit(0); // Close the application
            }
        });
        JLabel settingsLabel = new JLabel(settingsIcon);
        settingsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        settingsFrame.setSize(400,300);
        settingsFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        settingsFrame.setLayout(new BorderLayout());
        settingsFrame.setLocationRelativeTo(null);
        settingsFrame.add(tabbedPane);
        settingsFrame.setResizable(false);
        settingsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //frame.setState(Frame.ICONIFIED); // Minimize the frame
                boolean isVisible = settingsFrame.isVisible();
                settingsFrame.setVisible(!isVisible);
            }
        });
        titleBar.add(settingsLabel);
        titleBar.add(minimizeLabel);
        titleBar.add(closeLabel);
        textArea = new JLabel();
        textArea.setFont(new Font("Univers Unicode MS",Font.BOLD,16));
        textArea.setHorizontalAlignment(SwingConstants.CENTER);
        //textArea.setVerticalAlignment(SwingConstants.CENTER);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(textArea);
        contentPanel.add(Box.createVerticalStrut(50));

        secondText = new JLabel();
        secondText.setHorizontalAlignment(SwingConstants.CENTER);
        secondText.setAlignmentX(Component.CENTER_ALIGNMENT);
        secondText.setFont(new Font("Univers Unicode MS",Font.BOLD,16));

        contentPanel.add(secondText);
        contentPanel.add(Box.createVerticalGlue());
        
        Point dragPoint = new Point();
        frame.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragPoint.setLocation(e.getPoint());
                //contentPanel.setOpaque(true);
            }
        });

        frame.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (dragPoint != null) {
                    Point currentScreenLocation = e.getLocationOnScreen();
                    frame.setLocation(currentScreenLocation.x - dragPoint.x,
                            currentScreenLocation.y - dragPoint.y);
                }
                //contentPanel.setOpaque(false);
            }
        });
        frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
            }
        });
        frame.add(titleBar,BorderLayout.NORTH);

        //frame.add(textArea);
        frame.add(contentPanel,BorderLayout.CENTER);
        //frame.getContentPane().add(scrollPane, BorderLayout.CENTER); // Add scrollPane to center
        frame.setVisible(true);
        

    }

    public void Init_GUI() {
        frame_gui();
    }
    public JLabel getTextArea(){
        return textArea;
    }
    public JLabel getSecondText(){
        return secondText;
    }
    public void setTextArea(String s){
        textArea.setText(s);
    }
    public void setSecondText(String s){
        secondText.setText(s);
    }
}
