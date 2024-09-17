package Components;

import src.MonkeyTree;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GUI {

    private JCheckBox followPointerCheckBox;
    private JLabel statusLabel;
    private JButton debugButton;
    private JFrame debugFrame;
    private JTextArea debugTextArea;
    private List<String> debugLogs;
    private MonkeyTree monkeyTree;

    public GUI(MonkeyTree monkeyTree) {
        this.monkeyTree = monkeyTree;

        // Inicializa os componentes da GUI
        followPointerCheckBox = new JCheckBox("Seguir Ponteiro");
        statusLabel = new JLabel("Soma atual: 0 | Soma maxima atual: 0 | Status da arvore: Em Analise");

        debugLogs = new ArrayList<>();
        debugTextArea = new JTextArea(20, 50);
        debugTextArea.setEditable(false);
        debugButton = new JButton("Debug Log");
        debugButton.addActionListener(e -> showDebugLog());
    }

    public JCheckBox getFollowPointerCheckBox() {
        return followPointerCheckBox;
    }

    public void setFollowPointerCheckBox(JCheckBox followPointerCheckBox) {
        this.followPointerCheckBox = followPointerCheckBox;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
    }

    public JButton getDebugButton() {
        return debugButton;
    }

    public void setDebugButton(JButton debugButton) {
        this.debugButton = debugButton;
    }

    public JFrame getDebugFrame() {
        return debugFrame;
    }

    public void setDebugFrame(JFrame debugFrame) {
        this.debugFrame = debugFrame;
    }

    public JTextArea getDebugTextArea() {
        return debugTextArea;
    }

    public void setDebugTextArea(JTextArea debugTextArea) {
        this.debugTextArea = debugTextArea;
    }

    public List<String> getDebugLogs() {
        return debugLogs;
    }

    public void setDebugLogs(List<String> debugLogs) {
        this.debugLogs = debugLogs;
    }

    public MonkeyTree getMonkeyTree() {
        return monkeyTree;
    }

    public void setMonkeyTree(MonkeyTree monkeyTree) {
        this.monkeyTree = monkeyTree;
    }

    public void resetVisualization(String filePath) {
        monkeyTree.resetState();
        monkeyTree.readTreeFromFile(filePath);
        monkeyTree.startVisualization();
    }

    public void togglePauseResume(JButton button) {
        if (monkeyTree.isPaused()) {
            monkeyTree.getStepTimer().start();
            button.setText("Pausar");
        } else {
            monkeyTree.getStepTimer().stop();
            button.setText("Retomar");
        }
        monkeyTree.setPaused(!monkeyTree.isPaused());
    }

    public void zoomIn() {
        monkeyTree.setCellSize(monkeyTree.getCellSize() + 5);
        monkeyTree.setPreferredSize(new Dimension(monkeyTree.getWidth() * monkeyTree.getCellSize(), monkeyTree.getHeight() * monkeyTree.getCellSize()));
    }

    public void zoomOut() {
        if (monkeyTree.getCellSize() > 5) {
            monkeyTree.setCellSize(monkeyTree.getCellSize() - 5);
            monkeyTree.setPreferredSize(new Dimension(monkeyTree.getWidth() * monkeyTree.getCellSize(), monkeyTree.getHeight() * monkeyTree.getCellSize()));
        }
    }

    public void showDebugLog() {
        if (debugFrame == null) {
            debugFrame = new JFrame("Debug Log");
            debugFrame.setSize(600, 400);
            debugFrame.add(new JScrollPane(debugTextArea));
        }
        debugTextArea.setText(String.join("\n", debugLogs));
        debugFrame.setVisible(true);
    }

    public void logDebug(String message) {
        debugLogs.add(message);
        debugTextArea.setText(String.join("\n", debugLogs));
        debugTextArea.revalidate();
        debugTextArea.repaint();
    }

}
