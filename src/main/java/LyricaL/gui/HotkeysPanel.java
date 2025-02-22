package LyricaL.gui;

import java.util.prefs.Preferences;

import javax.swing.AbstractAction.*;
//import javax.swing.AbstractAction.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.Frame;
//import org.apache.hc.core5.http2.frame.Frame;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import LyricaL.gui.GUI.KeySelectorEditor;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyEvent;

public class HotkeysPanel{
    private static final Preferences prefs = Preferences.userRoot().node("lyrical");
    public HotkeysPanel(JFrame frame, JTabbedPane tabbedPane){
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
        registerGlobalListener(frame); 
        tabbedPane.addTab("Keybinds", settingsPanel);

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

    private void registerGlobalListener(JFrame frame){
        try{
            GlobalScreen.registerNativeHook();

        }catch(NativeHookException e){
            e.printStackTrace();
        }
        GlobalScreen.addNativeKeyListener(new NativeKeyListener(){
            @Override
            public void nativeKeyPressed(NativeKeyEvent e){
                String keyText = NativeKeyEvent.getKeyText(e.getKeyCode());
                String minimizeKey = loadHotkey("Toggle window visibility");
                if(keyText.equalsIgnoreCase(minimizeKey)){
                    toggleMinimize(frame);
                }
            }
            @Override
            public void nativeKeyReleased(NativeKeyEvent e){}

            @Override
            public void nativeKeyTyped(NativeKeyEvent e){}
        });
    }
    private void toggleMinimize(JFrame frame) {
        if (frame.getState() == Frame.ICONIFIED) {
            frame.setState(Frame.NORMAL);
        } else {
            frame.setState(Frame.ICONIFIED);
        }
    }
}
