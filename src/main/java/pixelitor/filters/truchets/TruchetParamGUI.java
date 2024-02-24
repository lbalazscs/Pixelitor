package pixelitor.filters.truchets;

import pixelitor.filters.gui.ParamGUI;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public class TruchetParamGUI extends JPanel implements ChangeListener, ParamGUI {
    TruchetParam truchetParam;

    public TruchetParamGUI(TruchetParam truchetParam, TruchetSwatch swatch) {
        this.truchetParam = truchetParam;
        add(new JTabbedPane() {
            {
                add("Presets", createPresetsTab(this, swatch));
                add("Customize", createCustomizerTab(this, swatch));
                add("Procedural", createProceduralTab(this, swatch));
            }
        });
    }

    private JPanel createPresetsTab(JTabbedPane tabbedPane, TruchetSwatch swatch) {
        return new JPanel(new BorderLayout()) {{
            setPreferredSize(new Dimension(400, 400));

            var paletteChoice = new JComboBox<>(TruchetPreconfiguredPalette.values());
            var patternChoice = new JComboBox<>(TruchetPreconfiguredPattern.values());

            swatch.adapt(
                paletteChoice.getItemAt(Math.max(0, paletteChoice.getSelectedIndex())),
                patternChoice.getItemAt(Math.max(0, patternChoice.getSelectedIndex())));

            var truchetDisplay = new TruchetTileDisplay(swatch);

            ActionListener updateAction = e -> {
                swatch.adapt(
                    paletteChoice.getItemAt(Math.max(0, paletteChoice.getSelectedIndex())),
                    patternChoice.getItemAt(Math.max(0, patternChoice.getSelectedIndex())));
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };
            paletteChoice.addActionListener(updateAction);
            patternChoice.addActionListener(updateAction);

            tabbedPane.addChangeListener(e -> {
                if (tabbedPane.getSelectedComponent() != this) {
                    return;
                }
                updateAction.actionPerformed(null);
            });

            add(new JPanel(new GridLayout(0, 1)) {{
                add(paletteChoice);
                add(patternChoice);
            }}, BorderLayout.NORTH);
            add(truchetDisplay, BorderLayout.CENTER);
        }};
    }

    private JPanel createCustomizerTab(JTabbedPane tabbedPane, TruchetSwatch swatch) {
        return new JPanel(new BorderLayout()) {{
            setBackground(Color.CYAN);

            var palette = new TruchetConfigurablePalette();
            var pattern = new TruchetConfigurablePattern();
            pattern.updateRows(3);
            pattern.updateColumns(4);

            tabbedPane.addChangeListener(e -> {
                if (tabbedPane.getSelectedComponent() != this) {
                    return;
                }
                swatch.adapt(palette, pattern);
                truchetParam.paramAdjusted();
            });

            var toolBar = createPaletteBar(palette, swatch, pattern);

            var truchetDisplay = new TruchetTileDisplay(swatch);

            Consumer<Runnable> updateAction = action -> {
                action.run();
                swatch.adapt(palette, pattern);
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };

            truchetDisplay.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    updateAction.accept(() ->
                        pattern.setState(e.getY(), e.getX(),
                            (pattern.getState(e.getY(), e.getX()) + 1) % palette.getDegree()));
                }
            });

            JSlider rowSlider = new JSlider(1, 20, pattern.getRows());
            rowSlider.addChangeListener(e -> updateAction.accept(() ->
                pattern.updateRows(rowSlider.getValue())));
            JSlider columnSlider = new JSlider(1, 20, pattern.getColumns());
            columnSlider.addChangeListener(e -> updateAction.accept(() ->
                pattern.updateColumns(columnSlider.getValue())));

            var rotationSet = new JComboBox<>(new String[]{"None", "Quarter", "Vertical", "Horizontal"});
            var horSymSet = new JToggleButton("Horizontal");
            var verSymSet = new JToggleButton("Vertical");
            rotationSet.addActionListener(e -> updateAction.accept(() -> {
                if (Objects.equals(rotationSet.getSelectedItem(), "None")) {
                    horSymSet.setEnabled(true);
                    verSymSet.setEnabled(true);
                } else {
                    horSymSet.setSelected(false);
                    verSymSet.setSelected(false);
                    horSymSet.setEnabled(false);
                    verSymSet.setEnabled(false);
                    rowSlider.setValue(rowSlider.getValue() & 0xfffffffe);
                    columnSlider.setValue(columnSlider.getValue() & 0xfffffffe);
                }
                pattern.setRotation(rotationSet.getSelectedIndex());
            }));
            horSymSet.addActionListener(e -> updateAction.accept(() ->
                pattern.setSymmetricAboutHorizontal(horSymSet.isSelected())));
            verSymSet.addActionListener(e -> updateAction.accept(() ->
                pattern.setSymmetricAboutVertical(verSymSet.isSelected())));

            add(new JPanel(new GridLayout(0, 1)) {{
                add(toolBar);
                add(new JLabel("Number of rows:"));
                add(rowSlider);
                add(new JLabel("Number of columns:"));
                add(columnSlider);
            }}, BorderLayout.NORTH);
            add(truchetDisplay, BorderLayout.CENTER);
            add(new JPanel(new GridLayout(1, 0)) {{
                add(rotationSet);
                add(horSymSet);
                add(verSymSet);
            }}, BorderLayout.SOUTH);
        }};
    }

    private JPanel createProceduralTab(JTabbedPane tabbedPane, TruchetSwatch swatch) {
        return new JPanel(new BorderLayout()) {{
            // Random
            // Gaussian (prefer a specific tile inbr center and another towards the out)
            // Wave Func Collapse
            // Noise

            var palette = new TruchetConfigurablePalette();
            var pattern = new TruchetProceduralPattern();
            pattern.updateRows(3);
            pattern.updateColumns(4);

            tabbedPane.addChangeListener(e -> {
                if (tabbedPane.getSelectedComponent() != this) {
                    return;
                }
                swatch.adapt(palette, pattern);
                truchetParam.paramAdjusted();
            });

            var toolBar = createPaletteBar(palette, swatch, pattern);
            var patternChoice = new JComboBox<>(ProceduralStateSpace.values());
            var truchetDisplay = new TruchetTileDisplay(swatch);

            Consumer<Runnable> updateAction = action -> {
                action.run();
                swatch.adapt(palette, pattern);
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };

            patternChoice.setSelectedIndex(0);
            patternChoice.addActionListener(e -> updateAction.accept(() -> {
                System.out.println("hgcfgbn");
                pattern.setState(((ProceduralStateSpace) patternChoice.getSelectedItem()));
            }));
            pattern.setState(((ProceduralStateSpace) patternChoice.getSelectedItem()));

            JSlider rowSlider = new JSlider(1, 20, pattern.getRows());
            rowSlider.addChangeListener(e -> updateAction.accept(() ->
                pattern.updateRows(rowSlider.getValue())));
            JSlider columnSlider = new JSlider(1, 20, pattern.getColumns());
            columnSlider.addChangeListener(e -> updateAction.accept(() ->
                pattern.updateColumns(columnSlider.getValue())));

            var rotationSet = new JComboBox<>(new String[]{"None", "Quarter", "Vertical", "Horizontal"});
            var horSymSet = new JToggleButton("Horizontal");
            var verSymSet = new JToggleButton("Vertical");
            rotationSet.addActionListener(e -> updateAction.accept(() -> {
                if (Objects.equals(rotationSet.getSelectedItem(), "None")) {
                    horSymSet.setEnabled(true);
                    verSymSet.setEnabled(true);
                } else {
                    horSymSet.setSelected(false);
                    verSymSet.setSelected(false);
                    horSymSet.setEnabled(false);
                    verSymSet.setEnabled(false);
                    rowSlider.setValue(rowSlider.getValue() & 0xfffffffe);
                    columnSlider.setValue(columnSlider.getValue() & 0xfffffffe);
                }
                pattern.setRotation(rotationSet.getSelectedIndex());
            }));
            horSymSet.addActionListener(e -> updateAction.accept(() ->
                pattern.setSymmetricAboutHorizontal(horSymSet.isSelected())));
            verSymSet.addActionListener(e -> updateAction.accept(() ->
                pattern.setSymmetricAboutVertical(verSymSet.isSelected())));

            add(new JPanel(new GridLayout(0, 1)) {{
                add(toolBar);
                add(new JLabel("Number of rows:"));
                add(rowSlider);
                add(new JLabel("Number of columns:"));
                add(columnSlider);
                add(new JLabel("Choose a pattern"));
                add(patternChoice);
            }}, BorderLayout.NORTH);
            add(truchetDisplay, BorderLayout.CENTER);
            add(new JPanel(new GridLayout(1, 0)) {{
                add(rotationSet);
                add(horSymSet);
                add(verSymSet);
            }}, BorderLayout.SOUTH);
        }};
    }

    private JToolBar createPaletteBar(TruchetConfigurablePalette palette, TruchetSwatch swatch, TruchetConfigurablePattern pattern) {
        var toolBar = new JToolBar();
        var addButton = new JButton(Icons.loadThemed("add_layer.gif", ThemedImageIcon.BLACK));
        var popupMenu = new JPopupMenu();

        popupMenu.setLayout(new GridLayout(0, 1));

        JPanel panel = null;
        Set<TileType> sourceTiles = new TreeSet<>();
        sourceTiles.add(TileType.TRIANGE);
        palette.updateStates(sourceTiles);
        for (TileType value : TileType.values()) {
            if (panel == null || panel.getComponentCount() >= 2) {
                popupMenu.add(panel = new JPanel(new GridLayout(1, 0)));
            }
            panel.add(new JToggleButton(value.toString(), value.createIcon()) {{
                setHorizontalAlignment(SwingConstants.LEFT);
                JButton addTileButton = new JButton(value.createIcon());
                if (value == TileType.TRIANGE) {
                    toolBar.add(addTileButton);
                }
                addActionListener(e -> {
                    if (isSelected()) {
                        toolBar.remove(addButton);
                        toolBar.add(addTileButton);
                        toolBar.add(addButton);
                        sourceTiles.add(value);
                    } else {
                        toolBar.remove(addTileButton);
                        toolBar.revalidate();
                        sourceTiles.remove(value);
                    }
                    if (sourceTiles.isEmpty()) {
                        return;
                    }
                    palette.updateStates(sourceTiles);
                    swatch.adapt(palette, pattern);
                    truchetParam.paramAdjusted();
                });
            }});
        }
        popupMenu.pack();
        toolBar.add(addButton);
        addButton.addActionListener(e -> popupMenu.show(toolBar, 0 /*(toolBar.getWidth() - popupMenu.getWidth()) / 2*/, addButton.getHeight()));
        return toolBar;
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
