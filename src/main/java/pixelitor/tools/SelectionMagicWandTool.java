package pixelitor.tools;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.filters.gui.UserPreset;
import pixelitor.gui.utils.VectorIcon;
import pixelitor.selection.ShapeCombinator;
import pixelitor.selection.selectionMagicWand.SelectionMagicWandBuilder;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class SelectionMagicWandTool extends DragTool {
    private static final String HELP_TEXT = "<b>Click</b> select a region of similar color, " +
            "<b>Shift</b> adds to an existing selection, hold Ctrl to subtract from the selection.\n";

    private final EnumComboBoxModel<ShapeCombinator> combinatorModel
            = new EnumComboBoxModel<>(ShapeCombinator.class);

    private SelectionMagicWandBuilder selectionMagicWandBuilder;

    SelectionMagicWandTool() {
        super("Magic Wand", 'J', HELP_TEXT, Cursors.DEFAULT, false);
        spaceDragStartPoint = true;
        pixelSnapping = true;
    }

    @Override
    public void saveStateTo(UserPreset preset) {

    }

    @Override
    public void loadUserPreset(UserPreset preset) {

    }

    @Override
    protected void dragStarted(PMouseEvent e) {
        selectionMagicWandBuilder = new SelectionMagicWandBuilder(
                getCombinator(), e.getComp());
    }

    @Override
    protected void ongoingDrag(PMouseEvent e) {

    }

    @Override
    protected void dragFinished(PMouseEvent e) {

    }

    @Override
    public void initSettingsPanel() {

    }

    public ShapeCombinator getCombinator() {
        return combinatorModel.getSelectedItem();
    }

    @Override
    public VectorIcon createIcon() { return new SelectionMagicWardToolIcon(); }

    private static class SelectionMagicWardToolIcon extends Tool.ToolIcon {

        @Override
        public void paintIcon(Graphics2D g) {

            Path2D path = new Path2D.Float();

            path.moveTo(28, -1);
            path.curveTo(-0.0193, 0.0706, -0.0627, 0.1708, -0.0965, 0.2228);
            path.curveTo(-0.0868, 0.1263, -19.4143, 15.0159, -19.67, 15.157);
            path.curveTo(-0.246, 0.1337, -0.5885, 0.1746, -0.8634, 0.1077);
            path.curveTo(-0.2749, -0.0669, -3.4584, -2.507, -3.5693, -2.7373);
            path.curveTo(-0.1109, -0.2228, -0.0627, -0.4865, 0.1206, -0.6908);
            path.curveTo(0.2074, -0.2303, 19.5349, -15.0865, 19.7182, -15.157);
            path.curveTo(0.1929, -0.0743, 0.6367, -0.078, 0.8489, -0.0074);
            path.curveTo(0.0868, 0.0297, 0.8827, 0.6165, 1.804, 1.3259);
            path.curveTo(1.365, 1.0622, 1.6496, 1.2999, 1.6786, 1.4076);
            path.curveTo(0.0145, 0.0706, 0.0386, 0.156, 0.0434, 0.1857);
            path.curveTo(0.0096, 0.0297, 0, 0.1151, -0.0145, 0.1857);

            Path2D pathBorder = new Path2D.Float();
            pathBorder.moveTo(-3.1159, -0.7799);
            pathBorder.lineTo(-0.7959, -0.6128);
            pathBorder.lineTo(-2.2091, 1.701);
            pathBorder.lineTo(-2.2043, 1.6973);
            pathBorder.lineTo(0.8103, 0.6202);
            pathBorder.lineTo(0.8055, 0.624);
            pathBorder.lineTo(2.1947, -1.6899);
            pathBorder.curveTo(1.2059, -0.9285, 2.1947, -1.6973, 2.1947, -1.7085);
            pathBorder.curveTo(0, -0.0111, -0.3569, -0.2934, -0.7959, -0.6314);

            g.setColor(new Color(0x68_00_00_00, true));
            g.fill(path);

            g.setColor(color);

            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4));
            g.draw(path);
        }
    }
}
