package pixelitor.automate;

import net.jafama.FastMath;
import pixelitor.Composition;
import pixelitor.FgBgColors;
import pixelitor.ImageComponents;
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
import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import static pixelitor.tools.Tools.BRUSH;
import static pixelitor.tools.Tools.CLONE;
import static pixelitor.tools.Tools.ERASER;
import static pixelitor.tools.Tools.SMUDGE;

public class AutoPaint {
    //    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER, SHAPES};
    public static final Tool[] ALLOWED_TOOLS = {SMUDGE, BRUSH, CLONE, ERASER};

    public static void showDialog() {
        ConfigPanel configPanel = new ConfigPanel();
        JDialog d = new OKCancelDialog(configPanel, "Auto Paint") {
            @Override
            protected void dialogAccepted() {
                close();
                int numStrokes = configPanel.getNumStrokes();
                int length = configPanel.getStrokeLength();
                Tool tool = configPanel.getSelectedTool();
                autoPaint(tool, numStrokes, length);
            }
        };
        d.setVisible(true);
    }

    private static void autoPaint(Tool tool, int numStrokes, int strokeLength) {
        Composition comp = ImageComponents.getActiveComp().get();
        Random random = new Random();

        if (comp != null) {
            int canvasWidth = comp.getCanvasWidth();
            int canvasHeight = comp.getCanvasHeight();

            ProgressMonitor progressMonitor = Utils.createPercentageProgressMonitor("Auto Paint");

            // So far we are on the EDT
            assert SwingUtilities.isEventDispatchThread();

            Runnable notEDTThreadTask = () -> {
                assert !SwingUtilities.isEventDispatchThread();
                for (int i = 0; i < numStrokes; i++) {
                    int progressPercentage = (int) ((float) i * 100 / numStrokes);
                    progressMonitor.setProgress(progressPercentage);
                    progressMonitor.setNote(progressPercentage + "%");

//                    Utils.sleep(1, TimeUnit.SECONDS);

                    Runnable edtRunnable = () -> paintSingleStroke(comp, tool, canvasWidth, canvasHeight, strokeLength, random);

                    try {
                        SwingUtilities.invokeAndWait(edtRunnable);
                    } catch (InterruptedException | InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    comp.repaint();
                    if (progressMonitor.isCanceled()) {
                        break;
                    }
                }
                progressMonitor.close();
            };
            new Thread(notEDTThreadTask).start();
        }
    }

    // called on the EDT
    private static void paintSingleStroke(Composition comp, Tool tool, int canvasWidth, int canvasHeight, int strokeLength, Random rand) {
        assert SwingUtilities.isEventDispatchThread();

        FgBgColors.setRandomColors();

        Point start = new Point(rand.nextInt(canvasWidth), rand.nextInt(canvasHeight));
        double randomAngle = rand.nextDouble() * 2 * Math.PI;
        int endX = (int) (start.x + strokeLength * FastMath.cos(randomAngle));
        int endY = (int) (start.y + strokeLength * FastMath.sin(randomAngle));

        Point end = new Point(endX, endY);

        try {
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

        private IntTextField numStrokesTF;
        private static int defaultNumStrokes = 100;

        private IntTextField lengthTF;
        private static int defaultLength = 100;

        public ConfigPanel() {
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
        }

        public int getNumStrokes() {
            int retVal = numStrokesTF.getIntValue();
            defaultNumStrokes = retVal;
            return retVal;
        }

        public int getStrokeLength() {
            int retVal = lengthTF.getIntValue();
            defaultLength = retVal;
            return retVal;
        }

        public Tool getSelectedTool() {
            Tool retVal = (Tool) toolSelector.getSelectedItem();
            defaultTool = retVal;
            return retVal;
        }
    }
}
