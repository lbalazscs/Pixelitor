package pixelitor.filters.truchets;

import pixelitor.filters.gui.*;
import pixelitor.filters.truchets.editableToolBar.EditableToolBar;
import pixelitor.filters.truchets.editableToolBar.STool;
import pixelitor.gui.utils.GridBagHelper;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TruchetParamGUI extends JPanel implements ChangeListener, ParamGUI {
    public static final String PRESETS = "Presets";
    public static final String DESIGN = "Design";
    public static final String GENERATE = "Generate";

    TruchetParam truchetParam;
    TruchetSwatch swatch;

    // Inter-tab communication states
    JPanel currentTab = null;
    TruchetPalette currentPalette = null;
    TruchetPattern currentPattern = null;


    public TruchetParamGUI(TruchetParam truchetParam, TruchetSwatch swatch) {
        this.truchetParam = truchetParam;
        this.swatch = swatch;

        add(new JTabbedPane() {{
            add(PRESETS, createPresetsTab(this));
            add(DESIGN, createCustomizerTab(this));
            add(GENERATE, createProceduralTab(this));
        }});
    }

    private JPanel createPresetsTab(JTabbedPane tabbedPane) {
        return new JPanel(new BorderLayout()) {{
            setName(PRESETS);

            var paletteChoice = new EnumParam<>("Palette", TruchetPreconfiguredPalette.class);
            var patternChoice = new EnumParam<>("Pattern", TruchetPreconfiguredPattern.class);
            var truchetDisplay = new TruchetTileDisplay(swatch, null);

            Updater updater = () -> {
                swatch.adapt(currentPalette = paletteChoice.getSelected(), currentPattern = patternChoice.getSelected());
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };
            paletteChoice.setAdjustmentListener(updater::update);
            patternChoice.setAdjustmentListener(updater::update);
            tabbedPane.addChangeListener(e -> (tabbedPane.getSelectedComponent() == this ? updater : (Updater) () -> {}).accept(() -> currentTab = this));
            updater.update();

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
            setName(DESIGN);

            var palette = new TruchetConfigurablePalette();
            var pattern = new TruchetConfigurablePattern(3, 4);
            var toolBar = createPaletteBar(palette, swatch, pattern);
            var rotationSet = new JComboBox<>(new String[]{"No Rotation", "Quarter", "Half"});
            var horSymSet = new JToggleButton("Horizontal");
            var verSymSet = new JToggleButton("Vertical");
            var rangeParam = new GroupedRangeParam("Swatch Shape", new RangeParam[]{
                new RangeParam("Rows", 1, pattern.getRows(), 20),
                new RangeParam("Columns", 1, pattern.getColumns(), 20)
            }, false);
            var truchetDisplay = new TruchetTileDisplay(swatch, pattern);

            Consumer<Runnable> updateAction = action -> {
                action.run();
//                swatch.adapt(currentPalette = palette, currentPattern = pattern);
                swatch.adapt(palette, pattern);
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };
            tabbedPane.addChangeListener(e -> (tabbedPane.getSelectedComponent() == this ? updateAction : (Consumer<Runnable>) (x) -> {}).accept(() -> {
                if (PRESETS.equals(currentTab.getName()) ||
                    GENERATE.equals(currentTab.getName())) {
                    rotationSet.setSelectedIndex(0);
                    horSymSet.setSelected(false);
                    verSymSet.setSelected(false);
                    rangeParam.setValue(0, currentPattern.getRows());
                    rangeParam.setValue(1, currentPattern.getColumns());
                    pattern.updateFrom(currentPattern);
                    palette.updateFrom(currentPalette);
                    toolBar.deselectCurrentTool();

                    HashMap<STool, JToggleButton> toolMap = toolBar.getSelectionButtons();
                    Map<STool, TileType> map = toolMap.keySet().stream().collect(Collectors.toMap(tool -> tool, tool -> ((TileTypeTool) tool).tileType));

                    ArrayList<TileType> tileTypes = new ArrayList<>(palette.tileTypes);
                    toolMap.forEach((tool, button) -> {
                        toolBar.getToolSelectionListener().toolRemoved(tool);
                        toolBar.deselectTool(tool);
                    });
                    toolMap.forEach((tool, button) -> {
                        toolBar.getToolSelectionListener().toolAdded(tool);
                        if (tileTypes.contains(map.get(tool))) {
                            toolBar.selectTool(tool);
                        }
                    });
                    toolBar.repaint();

                    updateAction.accept(() -> {});
                }
                currentTab = this;
            }));
            truchetDisplay.addMousePressListener(e -> updateAction.accept(() ->
                toolBar.takeAction(e.getX(), e.getY())));
            rangeParam.setAdjustmentListener(() -> updateAction.accept(() ->
                pattern.update(rangeParam.getValue(0), rangeParam.getValue(1))));

            rotationSet.addActionListener(e -> updateAction.accept(() -> {
                if (Objects.equals(rotationSet.getSelectedItem(), "No Rotation")) {
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
            setName(GENERATE);

            var palette = new TruchetConfigurablePalette();
            var pattern = new TruchetProceduralPattern(3, 4);
            var toolBar = createPaletteBar(palette, swatch, pattern);
            var rotationSet = new JComboBox<>(new String[]{"No Rotation", "Quarter", "Half"});
            var horSymSet = new JToggleButton("Horizontal");
            var verSymSet = new JToggleButton("Vertical");
            var rangeParam = new GroupedRangeParam("Swatch Shape", new RangeParam[]{
                new RangeParam("Rows", 1, pattern.getRows(), 20),
                new RangeParam("Columns", 1, pattern.getColumns(), 20)
            }, false);
            var truchetDisplay = new TruchetTileDisplay(swatch, pattern);
            var patternChoice = new EnumParam<>("Pattern", ProceduralStateSpace.class);
            pattern.setState(patternChoice.getSelected(), palette.getDegree() + 1);

            Consumer<Runnable> updateAction = action -> {
                action.run();
                swatch.adapt(currentPalette = palette, currentPattern = pattern);
                truchetDisplay.repaint();
                truchetParam.paramAdjusted();
            };
            tabbedPane.addChangeListener(e -> (tabbedPane.getSelectedComponent() == this ? updateAction : (Consumer<Runnable>) (x) -> {}).accept(() -> {
                currentTab = this;
            }));
            truchetDisplay.addMousePressListener(e -> updateAction.accept(() ->
                toolBar.takeAction(e.getX(), e.getY())));
            patternChoice.withAction(FilterButtonModel.createNoOpReseed());
            patternChoice.setAdjustmentListener(() -> updateAction.accept(() ->
                pattern.setState(patternChoice.getSelected(), palette.getDegree() + 1)));
            rangeParam.setAdjustmentListener(() -> updateAction.accept(() ->
                pattern.update(rangeParam.getValue(0), rangeParam.getValue(1))));
            rotationSet.addActionListener(e -> updateAction.accept(() -> {
                if (Objects.equals(rotationSet.getSelectedItem(), "No Rotation")) {
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

    private EditableToolBar createPaletteBar(TruchetConfigurablePalette palette, TruchetSwatch swatch, TruchetConfigurablePattern pattern) {
        var toolBar = new EditableToolBar(JToolBar.HORIZONTAL);

        ArrayList<TileType> selectedTileTypes = new ArrayList<>();
        HashMap<STool, TileType> availableTools = new HashMap<>();
        for (TileType tileType : TileType.values()) {
            TileTypeTool tool = new TileTypeTool(tileType, palette, pattern);
            toolBar.addTool(tool);
            availableTools.put(tool, tileType);
        }
        toolBar.setToolSelectionListener(new EditableToolBar.ToolSelectionListener() {
            {
                toolAdded(toolBar.getSelectedTool());
                palette.updateStates(selectedTileTypes);
//                swatch.adapt(palette, pattern);
            }

            @Override
            public void toolAdded(STool tool) {
                TileType tileType = availableTools.get(tool);
                if (!selectedTileTypes.contains(tileType)) {
                    selectedTileTypes.add(tileType);
                }
            }

            @Override
            public void toolRemoved(STool tool) {
                TileType tileType = availableTools.get(tool);
                if (tileType != null) {
                    selectedTileTypes.remove(tileType);
                }
            }

            @Override
            public void selectionChanged() {
                if (selectedTileTypes.isEmpty()) {
                    return;
                }
                palette.updateStates(selectedTileTypes);
                swatch.adapt(palette, pattern);
                truchetParam.paramAdjusted();
            }
        });

        palette.updateStates(selectedTileTypes);
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

    public interface Updater extends Consumer<Runnable> {
        @Override
        default void accept(Runnable runnable) {
            if (runnable != null) {
                runnable.run();
            }
            update();
        }

        void update();

        default void update(Runnable runnable) {
            accept(runnable);
        }
    }
}