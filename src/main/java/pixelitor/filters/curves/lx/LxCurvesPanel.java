package pixelitor.filters.curves.lx;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.EventListener;

public class LxCurvesPanel extends JPanel implements MouseMotionListener, MouseListener {
    public LxCurve lxCurve;
    public LxCurves lxCurves = new LxCurves();
    private Point.Float mouseStart;
    private int mousePointIndex = -1;
    private BufferedImage img;
    private EventListenerList actionListenerList = new EventListenerList();

    public LxCurvesPanel() {
        super();
        Dimension size = new Dimension(275, 275);
        this.setPreferredSize(size);
//        this.setBorder(new EmptyBorder(10, 10, 10, 10));
        addMouseMotionListener(this);
        addMouseListener(this);

        this.getWidth();
        this.getHeight();
        img = new BufferedImage(275, 275, BufferedImage.TYPE_INT_RGB );

        lxCurves.setG2D(img.createGraphics());
        lxCurves.setSize(275, 275);
        lxCurve = lxCurves.getActiveCurve();
        lxCurve.init();

        lxCurves.draw();
    }

    public void addActionListener(ActionListener actionListener) {
        actionListenerList.add(ActionListener.class, actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionListenerList.remove(ActionListener.class, actionListener);
    }

    protected void fireActionPerformed(ActionEvent actionEvent) {
        EventListener listenerList[] = actionListenerList.getListeners(ActionListener.class);
        for (int i = 0, n = listenerList.length; i < n; i++) {
            ((ActionListener) listenerList[i]).actionPerformed(actionEvent);
        }
    }

    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(img, 0, 0, null);

        fireActionPerformed(
            new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "")
        );
    }

    private void stateChanged() {
        lxCurves.draw();
        repaint();
    }

    public void setActiveCurve(LxCurveType type) {
        lxCurves.setActiveCurve(type.index);
        lxCurve = lxCurves.getActiveCurve();
        stateChanged();
    }

    public void resetActiveCurve() {
        lxCurves.getActiveCurve().reset();
        stateChanged();
    }

    public void reset() {
        lxCurves.reset();
        stateChanged();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mousePointIndex >= 0) {
            Point.Float mousePos = new Point.Float(e.getX(), e.getY());
            lxCurves.normalizePoint(mousePos);
            lxCurve.setPointPosition(mousePointIndex, mousePos);
            stateChanged();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point.Float mousePos =  new Point.Float(e.getX(), e.getY());
        lxCurves.normalizePoint(mousePos);
        boolean isOver = lxCurve.isOverPoint(mousePos);
        if (isOver) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseStart = new Point.Float(e.getX(), e.getY());
        lxCurves.normalizePoint(mouseStart);
        mousePointIndex = lxCurve.getPointIndexAt(mouseStart);

        if (e.getClickCount() == 1 && !e.isConsumed() && mousePointIndex < 0) {
            e.consume();
//            lxCurve = lxCurves.getActiveCurve();
            mousePointIndex = lxCurve.addPoint(mouseStart);
            stateChanged();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (mousePointIndex >= 0) {
            Point.Float mousePos = new Point.Float(e.getX(), e.getY());
            lxCurves.normalizePoint(mousePos);
            if (lxCurve.isOverPoint(mousePointIndex)) {
                lxCurve.deletePoint(mousePointIndex);
                stateChanged();
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // nothing
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() ==2 && !e.isConsumed() && mousePointIndex >= 0) {
            e.consume();
//            lxCurve = lxCurves.getActiveCurve();
            lxCurve.deletePoint(mousePointIndex);
            stateChanged();
        }
    }
}
