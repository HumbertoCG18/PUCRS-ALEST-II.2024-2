package src;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Debugger {
    private JButton debugButton;
    private JFrame debugFrame;
    private JTextArea debugTextArea;
    private List<String> debugLogs;

    public Debugger() {
        debugLogs = new ArrayList<>();
        debugTextArea = new JTextArea(20, 50);
        debugTextArea.setEditable(false);
        debugButton = new JButton("Debug Log");
        debugButton.addActionListener(e -> showDebugLog());
    }

    public void log(String message) {
        debugLogs.add(message);
        debugTextArea.setText(String.join("\n", debugLogs));
        debugTextArea.revalidate();
        debugTextArea.repaint();
    }

    private void showDebugLog() {
        if (debugFrame == null) {
            debugFrame = new JFrame("Debug Log");
            debugFrame.setSize(600, 400);
            debugFrame.add(new JScrollPane(debugTextArea));
        }
        debugFrame.setVisible(true);
    }

    public JButton getDebugButton() {
        return debugButton;
    }
}
