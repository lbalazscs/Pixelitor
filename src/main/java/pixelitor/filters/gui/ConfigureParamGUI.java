package pixelitor.filters.gui;

import pixelitor.utils.DefaultButton;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.function.Function;

public class ConfigureParamGUI extends JPanel implements ParamGUI {
    private final JButton configureButton;
    private final DefaultButton defaultButton;

    public ConfigureParamGUI(Function<JDialog, JDialog> dialogFactory, DefaultButton defaultButton) {
        super(new BorderLayout());
        this.defaultButton = defaultButton;
        configureButton = new JButton("Configure...");
        configureButton.addActionListener(e -> {
            JDialog owner = (JDialog) SwingUtilities.getWindowAncestor(configureButton);
            JDialog dialog = dialogFactory.apply(owner);
            dialog.setVisible(true);
        });
        add(configureButton, BorderLayout.CENTER);
        add(defaultButton, BorderLayout.EAST);
    }

    @Override
    public void updateGUI() {

    }

    @Override
    public void setToolTip(String tip) {

    }

    @Override
    public void setEnabled(boolean enabled) {
        configureButton.setEnabled(enabled);
        defaultButton.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return configureButton.isEnabled();
    }
}
