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
    private static void saveHotkey(String action, String key) {
        prefs.put(action, key);
    }

    private static String loadHotkey(String action) {
        return prefs.get(action, "");
    }

    private static void removeHotkey(String action) {
        prefs.remove(action);
    }
    public static void bindHotkey(JFrame frame, String actionName, Runnable action) {
        String key = loadHotkey(actionName);
        if (key != null && !key.isEmpty()) {
            InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = frame.getRootPane().getActionMap();

            KeyStroke keyStroke = KeyStroke.getKeyStroke(key);
            if (keyStroke != null) {
                inputMap.put(keyStroke, actionName);
                actionMap.put(actionName, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        action.run();
                    }
                });
            }
        }
    }
    public void frame_gui(){
        FlatDarkLaf.setup();
        frame = new JFrame("LyricaL");
        JFrame settingsFrame = new JFrame("Settings");
        JFrame optionsFrame = new JFrame("Options");
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));

        frame.setOpacity(0.8f);
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setAlwaysOnTop(true);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Options",sizePanel(frame));
        tabbedPane.addTab("Themes",createThemePanel(frame));
        tabbedPane.addTab("Keybinds",createHotkeysPanel(settingsFrame));
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
        //frame.add(textArea);
        //textArea.setEditable(false);
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
    public static JPanel sizePanel(JFrame frame) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS)); // Stack buttons vertically

        // Create radio buttons for window sizes
        JRadioButton smallButton = new JRadioButton("Small (400x300)");
        JRadioButton mediumButton = new JRadioButton("Medium (600x400)", true); // Default selected
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

        return panel;
    }



    public void settings_gui(){

    }
    public void Init_GUI() {
        frame_gui();
        /*String[] columnNames = {"Action","Hotkey"};
        Object[][] data = {
            {"Toggle Window UI",""},
            {"Toggle window visibility",""},
            {"Lock",""},
            {"Toggle Transparency",""}
        };
        DefaultTableModel model = new DefaultTableModel(data,columnNames);
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(1).setCellEditor(new KeySelectorEditor());
        JScrollPane scrollPane = new JScrollPane(table);
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel,BoxLayout.Y_AXIS));
        settingsPanel.add(scrollPane);*/
        //bindHotkey(settingsFrame, "Minimize Window");
        bindHotkey(frame, "Toggle window visibility", () -> frame.setState(Frame.ICONIFIED));
        
        //System.out.println(minimizeIconURL);
        //System.out.println(closeIconURL);
        
        
        
        
        
        
        
    }
    private static JPanel createHotkeysPanel(JFrame frame){
        String[] columnNames = {"Action","Hotkey"};
        Object[][] data = {
            {"Toggle Window UI",loadHotkey("Toggle Window UI")},
            {"Toggle window visibility",loadHotkey("Toggle window visibility")},
            {"Lock",loadHotkey("Lock")},
            {"Toggle Transparency",loadHotkey("Toggle Transparency")}
        };
        DefaultTableModel model = new DefaultTableModel(data,columnNames);
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(1).setCellEditor(new KeySelectorEditor());
        table.getModel().addTableModelListener(e-> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if(col ==1){
                String actionName = (String) table.getValueAt(row, 0);
                String key = (String) table.getValueAt(row, 1);
                if(key != null && !key.isEmpty()){
                    saveHotkey(actionName, key);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel,BoxLayout.Y_AXIS));
        settingsPanel.add(scrollPane);
        return settingsPanel;
    }
    public static JPanel createThemePanel(JFrame frame){
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
        return panel;
    }
    private static void changeTheme(JFrame frame, String theme){
        applyTheme(frame,theme);

    }
    private static void applyTheme(JFrame frame, String theme) {
        Color newColor;
        switch (theme) {
            case "Dark":
                newColor =Color.DARK_GRAY;
                break;
            case "Blue":
                newColor = new Color(70, 130, 180); // SteelBlue
                break;
            default:
                newColor=Color.LIGHT_GRAY;
                break;
        }
        frame.getContentPane().setBackground(newColor);
        frame.repaint();
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
