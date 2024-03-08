package pixelitor.filters.truchets.editableToolBar;

import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.gui.utils.Themes;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.gui.ToolButton;
import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridLayout;

import static java.awt.Color.BLACK;

public class EditableToolBar extends JToolBar {

    public Color iconColor;
    public int iconSize;
    private ButtonGroup buttonGroup = new ButtonGroup();
    private JButton showEditorButton;
    private JPopupMenu editorMenu;
    private STool selectedTool;
    private JPanel __editorMenuRow = null;
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

        JToggleButton toolSelectionButton = new JToggleButton(tool.getName(), icon);
        toolSelectionButton.setHorizontalAlignment(SwingConstants.LEFT);
        toolSelectionButton.addActionListener(e -> {
            if (toolSelectionButton.isSelected()) {
                toolActivateButton.setVisible(true);
                activeToolCount++;
                listener.toolAdded(tool);
                listener.selectionChanged();
                revalidate();
            } else if (activeToolCount > 1) {
                toolActivateButton.setVisible(false);
                activeToolCount--;
                listener.toolRemoved(tool);
                listener.selectionChanged();
                revalidate();
            } else {
                toolSelectionButton.setSelected(true);
                // Notify the user that they can't remove the last,
                // add one another and then remove the current?
            }
        });
        editorMenu.add(toolSelectionButton);

        if (getComponentCount() == 2) {
            toolSelectionButton.setSelected(true);
            toolActivateButton.setVisible(true);
            toolActivateButton.setSelected(true);
            add(toolActivateButton);
            selectedTool = tool;
            activeToolCount = 1;
        }
    }

    public void setToolSelectionListener(ToolSelectionListener listener) {
        this.listener = listener;
    }

    public STool getSelectedTool() {
        return selectedTool;
    }

    public void takeAction(int x, int y) {
        selectedTool.takeAction(x, y);
    }

    public interface ToolSelectionListener {
        void selectionChanged();

        void toolAdded(STool tool);

        void toolRemoved(STool tool);
    }
}