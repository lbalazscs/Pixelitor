package pixelitor.filters.truchets;

import pixelitor.filters.gui.ParamGUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

public class TruchetParamGUI extends JPanel implements ChangeListener, ParamGUI {
    TruchetParam truchetParam;
    public TruchetParamGUI(TruchetParam truchetParam, TruchetSwatch swatch) {
        this.truchetParam = truchetParam;
        add(new JTabbedPane() {
            {
                add("Presets", createPresetsTab(swatch));
                add("Customize", new JPanel() {{
                    setBackground(Color.CYAN);
                }});
                add("Procedural", new JPanel() {{
                    // Random
                    // Gaussian (prefer a specific tile in center and another towards the out)
                    // Wave Func Collapse
                    // Noise
                    setBackground(Color.BLUE);
                }});
            }
        });
    }

    private JPanel createPresetsTab(TruchetSwatch swatch) {
        return new JPanel(new BorderLayout()) {{
            setPreferredSize(new Dimension(400, 400));

            var paletteChoice = new JComboBox<>(TruchetPalette.values());
            var patternChoice = new JComboBox<>(TruchetPattern.values());

            swatch.adapt(
                paletteChoice.getItemAt(Math.max(0, paletteChoice.getSelectedIndex())),
                patternChoice.getItemAt(Math.max(0, patternChoice.getSelectedIndex())));

            var truchetDisplay = new TruchetTileDisplay(swatch);

            ActionListener actionListener = e -> {
                swatch.adapt(
                    paletteChoice.getItemAt(Math.max(0, paletteChoice.getSelectedIndex())),
                    patternChoice.getItemAt(Math.max(0, patternChoice.getSelectedIndex())));
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };
            paletteChoice.addActionListener(actionListener);
            patternChoice.addActionListener(actionListener);

            add(new JPanel(new GridLayout(0, 1)) {{
                add(paletteChoice);
                add(patternChoice);
            }}, BorderLayout.NORTH);
            add(truchetDisplay, BorderLayout.CENTER);
        }};
    }

    @Override
    public void stateChanged(ChangeEvent e) {

    }

    @Override
    public void updateGUI() {

    }

    @Override
    public void setToolTip(String tip) {

    }

    @Override
    public int getNumLayoutColumns() {
        return 1;
    }
}
