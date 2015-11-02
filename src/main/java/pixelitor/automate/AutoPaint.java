package pixelitor.automate;

import net.jafama.FastMath;
import pixelitor.Composition;
import pixelitor.FgBgColors;
import pixelitor.ImageComponents;
import pixelitor.history.History;
import pixelitor.history.ImageEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.selection.IgnoreSelection;
import pixelitor.tools.AbstractBrushTool;
import pixelitor.tools.Tool;
import pixelitor.tools.UserDrag;
import pixelitor.tools.shapestool.ShapesTool;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.IntTextField;
import pixelitor.utils.Messages;
import pixelitor.utils.OKCancelDialog;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.SMUDGE;

public class AutoPaint {
    //    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER, SHAPES};
    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER};

    private AutoPaint() {
    }

    public static void showDialog() {
        ConfigPanel configPanel = new ConfigPanel();
        JDialog d = new OKCancelDialog(configPanel, "Auto Paint") {
            @Override
            protected void dialogAccepted() {
                close();
                Settings settings = configPanel.getSettings();
                paintStrokes(settings);
            }
        };
        d.setVisible(true);
    }

    private static void paintStrokes(Settings settings) {
        // So far we are on the EDT
        assert SwingUtilities.isEventDispatchThread();

        Composition comp = ImageComponents.getActiveComp().orElseThrow(() -> new RuntimeException("no active composition"));
        ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor("Auto Paint", "Stop");

        ImageLayer imageLayer = comp.getActiveMaskOrImageLayer();
        BufferedImage backupImage = imageLayer.getImageOrSubImageIfSelected(true, true);
        History.setSuspended(true);

        Runnable notEDTThreadTask = () -> {
            try {
                runStrokesOutsideEDT(settings, comp, progressMonitor);
            } catch (InterruptedException e) {
                Messages.showException(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                Messages.showException(cause);
            } finally {
                Runnable edtFinish = () -> {
                    History.setSuspended(false);
                    ImageEdit edit = new ImageEdit(comp, "Auto Paint",
                            imageLayer, backupImage, IgnoreSelection.NO, false);
                    History.addEdit(edit);
                };
                try {
                    SwingUtilities.invokeAndWait(edtFinish);
                } catch (InterruptedException | InvocationTargetException e) {
                    Messages.showException(e);
                }
            }
        };

        Thread thread = new Thread(notEDTThreadTask);
        // start the non-EDT thread but do not wait until it finishes
        // because this thread needs to put tasks on the EDT
        thread.start();
    }

    private static void runStrokesOutsideEDT(Settings settings, Composition comp, ProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
        assert !SwingUtilities.isEventDispatchThread();

        Random random = new Random();
        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();

        int numStrokes = settings.getNumStrokes();
        for (int i = 0; i < numStrokes; i++) {
            int progressPercentage = (int) ((float) i * 100 / numStrokes);
            progressMonitor.setProgress(progressPercentage);
            progressMonitor.setNote(progressPercentage + "%");

//                    Utils.sleep(1, TimeUnit.SECONDS);

            Runnable edtRunnable = () -> paintSingleStroke(comp, settings, canvasWidth, canvasHeight, random);

            SwingUtilities.invokeAndWait(edtRunnable);

            comp.repaint();
            if (progressMonitor.isCanceled()) {
                break;
            }
        }
        progressMonitor.close();
    }

    private static void paintSingleStroke(Composition comp, Settings settings, int canvasWidth, int canvasHeight, Random rand) {
        // called on the EDT
        assert SwingUtilities.isEventDispatchThread();

        if (settings.withRandomColors()) {
            FgBgColors.randomizeColors();
        }

        int strokeLength = settings.getStrokeLength();

        Point start = new Point(rand.nextInt(canvasWidth), rand.nextInt(canvasHeight));
        double randomAngle = rand.nextDouble() * 2 * Math.PI;
        int endX = (int) (start.x + strokeLength * FastMath.cos(randomAngle));
        int endY = (int) (start.y + strokeLength * FastMath.sin(randomAngle));

        Point end = new Point(endX, endY);

        try {
            Tool tool = settings.getTool();
            // tool.randomize();
            if (tool instanceof AbstractBrushTool) {
                AbstractBrushTool abt = (AbstractBrushTool) tool;
                abt.drawBrushStrokeProgrammatically(comp, start, end);
            } else if (tool instanceof ShapesTool) {
                ShapesTool st = (ShapesTool) tool;
                st.paintShapeOnIC(comp, new UserDrag(start.x, start.y, end.x, end.y));
            } else {
                throw new IllegalStateException("tool = " + tool.getClass().getName());
            }
        } catch (Exception e) {
            Messages.showException(e);
        }
    }

    private static class ConfigPanel extends JPanel {
        private final JComboBox<Tool> toolSelector;
        private static Tool defaultTool = SMUDGE;

        private final IntTextField numStrokesTF;
        private static int defaultNumStrokes = 100;

        private final IntTextField lengthTF;
        private static int defaultLength = 100;

        private final JCheckBox randomColorsCB;
        private final JLabel randomColorsLabel;
        private static boolean defaultRandomColors = true;

        private ConfigPanel() {
            super(new GridBagLayout());
            GridBagHelper gbh = new GridBagHelper(this);

            toolSelector = new JComboBox<>(ALLOWED_TOOLS);
            toolSelector.setSelectedItem(defaultTool);
            toolSelector.setName("toolSelector");
            gbh.addLabelWithControl("Tool:", toolSelector);

            numStrokesTF = new IntTextField(String.valueOf(defaultNumStrokes));
            numStrokesTF.setName("numStrokesTF");
            gbh.addLabelWithControl("Number of Strokes:", numStrokesTF);

            lengthTF = new IntTextField(String.valueOf(defaultLength));
            gbh.addLabelWithControl("Stroke Length:", lengthTF);

            randomColorsLabel = new JLabel("Random Colors:");
            randomColorsCB = new JCheckBox();
            randomColorsCB.setSelected(defaultRandomColors);
            gbh.addTwoControls(randomColorsLabel, randomColorsCB);

            toolSelector.addActionListener(e -> updateRandomColorsEnabledState());
            updateRandomColorsEnabledState();
        }

        private void updateRandomColorsEnabledState() {
            Tool tool = (Tool) toolSelector.getSelectedItem();
            if (tool == BRUSH) {
                randomColorsLabel.setEnabled(true);
                randomColorsCB.setEnabled(true);
            } else {
                randomColorsLabel.setEnabled(false);
                randomColorsCB.setEnabled(false);
            }
        }

        public Settings getSettings() {
            int numStrokes = numStrokesTF.getIntValue();
            defaultNumStrokes = numStrokes;

            int strokeLength = lengthTF.getIntValue();
            defaultLength = strokeLength;

            Tool tool = (Tool) toolSelector.getSelectedItem();
            defaultTool = tool;

            boolean randomColorsEnabled = randomColorsCB.isEnabled();
            boolean randomColorsSelected = randomColorsCB.isSelected();
            boolean randomColors = randomColorsEnabled && randomColorsSelected;
            defaultRandomColors = randomColorsSelected;

            return new Settings(tool, numStrokes, strokeLength, randomColors);
        }
    }

    private static class Settings {
        private final Tool tool;
        private final int numStrokes;
        private final int strokeLength;
        private final boolean randomColors;

        private Settings(Tool tool, int numStrokes, int strokeLength, boolean randomColors) {
            this.tool = tool;
            this.numStrokes = numStrokes;
            this.strokeLength = strokeLength;
            this.randomColors = randomColors;
        }

        public Tool getTool() {
            return tool;
        }

        public int getNumStrokes() {
            return numStrokes;
        }

        public int getStrokeLength() {
            return strokeLength;
        }

        public boolean withRandomColors() {
            return randomColors;
        }
    }
}
