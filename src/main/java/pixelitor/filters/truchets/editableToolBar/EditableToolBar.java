package pixelitor.filters.truchets.editableToolBar;

import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.gui.ToolButton;
import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.HashMap;

import static java.awt.Color.BLACK;

public class EditableToolBar extends JToolBar {

    public Color iconColor;
    public int iconSize;
    private ButtonGroup buttonGroup = new ButtonGroup();
    private JButton showEditorButton;
    private JPopupMenu editorMenu;
    private STool selectedTool;
    private JPanel __editorMenuRow = null;
    private final HashMap<STool, JToggleButton> activationButtons = new HashMap<>();
    private final HashMap<STool, JToggleButton> selectionButtons = new HashMap<>();
    private ToolSelectionListener listener;
    private int activeToolCount;

    public EditableToolBar(int orientation) {
        super(orientation);
        iconColor = Themes.getCurrent().isDark() ? Themes.LIGHT_ICON_COLOR : BLACK;
        iconSize = ToolButton.TOOL_ICON_SIZE;

        showEditorButton = new JButton(Icons.loadThemed("add_layer.gif", ThemedImageIcon.BLACK));
        editorMenu = new JPopupMenu();
        editorMenu.setLayout(new GridLayout(0, 1));
        showEditorButton.addActionListener(e -> editorMenu.show(this, 0 /*(toolBar.getWidth() - toolBarEditor.getWidth()) / 2*/, showEditorButton.getHeight()));

        add(showEditorButton);
    }

    public void addTool(STool tool) {
        if (__editorMenuRow == null || __editorMenuRow.getComponentCount() >= 2) {
            editorMenu.add(__editorMenuRow = new JPanel(new GridLayout(1, 0)));
        }

        VectorIcon icon = tool.getIcon(iconColor, iconSize);

        JToggleButton toolActivateButton = new JToggleButton(icon);
        buttonGroup.add(toolActivateButton);
        toolActivateButton.addActionListener(e -> selectedTool = tool);
        toolActivateButton.setVisible(false);
        add(toolActivateButton, getComponentCount() - 1);
        activationButtons.put(tool, toolActivateButton);

        JToggleButton toolSelectionButton = new JToggleButton(tool.getName(), icon);
        toolSelectionButton.setHorizontalAlignment(SwingConstants.LEFT);
        toolSelectionButton.addActionListener(e -> {
            if (toolSelectionButton.isSelected()) {
                selectTool(tool);
                listener.selectionChanged();
                revalidate();
            } else if (activeToolCount > 1) {
                deselectTool(tool);
                listener.toolRemoved(tool);
                listener.selectionChanged();
                revalidate();
            } else {
                toolSelectionButton.setSelected(true);
                // Notify the user that they can't remove the last,
                // add one another and then remove the current?
            }
        });
        __editorMenuRow.add(toolSelectionButton);
        selectionButtons.put(tool, toolSelectionButton);

        if (getComponentCount() == 2) {
            toolSelectionButton.setSelected(true);
            toolActivateButton.setVisible(true);
            toolActivateButton.setSelected(true);
            selectedTool = tool;
            activeToolCount = 1;
        }
    }

    public void deselectTool(STool tool) {
        JToggleButton toolActivateButton = activationButtons.get(tool);
        toolActivateButton.setVisible(false);
        activeToolCount--;
    }

    public void selectTool(STool tool) {
        JToggleButton toolActivateButton = activationButtons.get(tool);
        toolActivateButton.setVisible(true);
        activeToolCount++;
        listener.toolAdded(tool);
    }

    public void setToolSelectionListener(ToolSelectionListener listener) {
        this.listener = listener;
    }

    public ToolSelectionListener getToolSelectionListener() {
        return listener;
    }

    public STool getSelectedTool() {
        return selectedTool;
    }

    public void takeAction(int x, int y) {
        if (selectedTool != null) {
            selectedTool.takeAction(x, y);
        }
    }

    public void deselectCurrentTool() {
        selectedTool = null;
    }

    public interface ToolSelectionListener {
        void selectionChanged();

        void toolAdded(STool tool);

        void toolRemoved(STool tool);
    }

    public HashMap<STool, JToggleButton> getSelectionButtons() {
        return selectionButtons;
    }
}