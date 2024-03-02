package pixelitor.filters.truchets;

import pixelitor.filters.gui.*;
import pixelitor.gui.utils.GridBagHelper;
import pixelitor.gui.utils.ThemedImageIcon;
import pixelitor.utils.Icons;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public class TruchetParamGUI extends JPanel implements ChangeListener, ParamGUI {
    TruchetParam truchetParam;
    TruchetSwatch swatch;


    public TruchetParamGUI(TruchetParam truchetParam, TruchetSwatch swatch) {
        this.truchetParam = truchetParam;
        this.swatch = swatch;

        add(new JTabbedPane() {{
            add("Presets", createPresetsTab(this));
            add("Customize", createCustomizerTab(this));
            add("Procedural", createProceduralTab(this));
        }});
    }

    private JPanel createPresetsTab(JTabbedPane tabbedPane) {
        return new JPanel(new BorderLayout()) {{
            var paletteChoice = new EnumParam<>("Palette", TruchetPreconfiguredPalette.class);
            var patternChoice = new EnumParam<>("Pattern", TruchetPreconfiguredPattern.class);
            var truchetDisplay = new TruchetTileDisplay(swatch);
            swatch.adapt(paletteChoice.getSelected(), patternChoice.getSelected());

            Runnable updateAction = () -> {
                swatch.adapt(paletteChoice.getSelected(), patternChoice.getSelected());
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };
            paletteChoice.setAdjustmentListener(updateAction::run);
            patternChoice.setAdjustmentListener(updateAction::run);
            tabbedPane.addChangeListener(e -> (tabbedPane.getSelectedComponent() == this ? updateAction : (Runnable) () -> {}).run());

            add(new JPanel(new GridBagLayout()) {{
                GridBagHelper helper = new GridBagHelper(this);
                helper.addParam(paletteChoice);
                helper.addParam(patternChoice);
            }}, BorderLayout.NORTH);
            add(truchetDisplay, BorderLayout.CENTER);
        }};
    }

    private JPanel createCustomizerTab(JTabbedPane tabbedPane) {
        return new JPanel(new BorderLayout()) {{
            var palette = new TruchetConfigurablePalette();
            var pattern = new TruchetConfigurablePattern(3, 4);
            var toolBar = createPaletteBar(palette, swatch, pattern);
            var rotationSet = new JComboBox<>(new String[]{"None", "Quarter", "Vertical", "Horizontal"});
            var horSymSet = new JToggleButton("Horizontal");
            var verSymSet = new JToggleButton("Vertical");
            var rangeParam = new GroupedRangeParam("Swatch Shape", new RangeParam[]{
                new RangeParam("Rows", 1, pattern.getRows(), 20),
                new RangeParam("Columns", 1, pattern.getColumns(), 20)
            }, false);
            var truchetDisplay = new TruchetTileDisplay(swatch);

            Consumer<Runnable> updateAction = action -> {
                action.run();
                swatch.adapt(palette, pattern);
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };
            tabbedPane.addChangeListener(e -> (tabbedPane.getSelectedComponent() == this ? updateAction : (Consumer<Runnable>) (x) -> {}).accept(() -> {}));
            truchetDisplay.addMousePressListener(e -> updateAction.accept(() ->
                pattern.setState(e.getY(), e.getX(),
                    (pattern.getState(e.getY(), e.getX()) + 1) % palette.getDegree())));
            rangeParam.setAdjustmentListener(() -> updateAction.accept(() ->
                pattern.update(rangeParam.getValue(0), rangeParam.getValue(1))));

            rotationSet.addActionListener(e -> updateAction.accept(() -> {
                if (Objects.equals(rotationSet.getSelectedItem(), "None")) {
                    horSymSet.setEnabled(true);
                    verSymSet.setEnabled(true);
                } else {
                    horSymSet.setSelected(false);
                    verSymSet.setSelected(false);
                    horSymSet.setEnabled(false);
                    verSymSet.setEnabled(false);
//                    rowSlider.setValue(rowSlider.getValue() & 0xfffffffe);
//                    columnSlider.setValue(columnSlider.getValue() & 0xfffffffe);
                }
                pattern.setRotation(rotationSet.getSelectedIndex());
            }));
            horSymSet.addActionListener(e -> updateAction.accept(() ->
                pattern.setSymmetricAboutHorizontal(horSymSet.isSelected())));
            verSymSet.addActionListener(e -> updateAction.accept(() ->
                pattern.setSymmetricAboutVertical(verSymSet.isSelected())));

            add(new JPanel(new GridBagLayout()) {{
                GridBagConstraints constraints = new GridBagConstraints() {{
                    weighty = (gridx = (anchor = WEST) - WEST) + 1;
                }};
                add(toolBar, constraints);
                add(rangeParam.createGUI(), constraints);
            }}, BorderLayout.NORTH);
            add(truchetDisplay, BorderLayout.CENTER);
            add(new JPanel(new GridLayout(1, 0)) {{
                add(rotationSet);
                add(horSymSet);
                add(verSymSet);
            }}, BorderLayout.SOUTH);
        }};
    }

    private JPanel createProceduralTab(JTabbedPane tabbedPane) {
        return new JPanel(new BorderLayout()) {{
            var palette = new TruchetConfigurablePalette();
            var pattern = new TruchetProceduralPattern(3, 4);
            var toolBar = createPaletteBar(palette, swatch, pattern);
            var rotationSet = new JComboBox<>(new String[]{"None", "Quarter", "Vertical", "Horizontal"});
            var horSymSet = new JToggleButton("Horizontal");
            var verSymSet = new JToggleButton("Vertical");
            var rangeParam = new GroupedRangeParam("Swatch Shape", new RangeParam[]{
                new RangeParam("Rows", 1, pattern.getRows(), 20),
                new RangeParam("Columns", 1, pattern.getColumns(), 20)
            }, false);
            var truchetDisplay = new TruchetTileDisplay(swatch);
            var patternChoice = new EnumParam<>("Pattern", ProceduralStateSpace.class);
            pattern.setState(patternChoice.getSelected());

            Consumer<Runnable> updateAction = action -> {
                action.run();
                swatch.adapt(palette, pattern);
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };
            tabbedPane.addChangeListener(e -> (tabbedPane.getSelectedComponent() == this ? updateAction : (Consumer<Runnable>) (x) -> {}).accept(() -> {}));
            patternChoice.withAction(FilterButtonModel.createNoOpReseed());
            patternChoice.setAdjustmentListener(() -> updateAction.accept(() ->
                pattern.setState(patternChoice.getSelected())));
            rangeParam.setAdjustmentListener(() -> updateAction.accept(() ->
                pattern.update(rangeParam.getValue(0), rangeParam.getValue(1))));
            rotationSet.addActionListener(e -> updateAction.accept(() -> {
                if (Objects.equals(rotationSet.getSelectedItem(), "None")) {
                    horSymSet.setEnabled(true);
                    verSymSet.setEnabled(true);
                } else {
                    horSymSet.setSelected(false);
                    verSymSet.setSelected(false);
                    horSymSet.setEnabled(false);
                    verSymSet.setEnabled(false);
                }
                pattern.setRotation(rotationSet.getSelectedIndex());
            }));
            horSymSet.addActionListener(e -> updateAction.accept(() ->
                pattern.setSymmetricAboutHorizontal(horSymSet.isSelected())));
            verSymSet.addActionListener(e -> updateAction.accept(() ->
                pattern.setSymmetricAboutVertical(verSymSet.isSelected())));

            add(new JPanel(new GridBagLayout()) {{
                GridBagConstraints constraints = new GridBagConstraints() {{
                    weighty = (gridx = (anchor = WEST) - WEST) + 1;
                }};
                add(toolBar, constraints);
                add(rangeParam.createGUI(), constraints);
                add(patternChoice.createGUI(), constraints);
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

    public void setAdjustmentListener(ParamAdjustmentListener listener) {
//        paramList.forEach(fp -> fp.setAdjustmentListener(listener));
    }


}