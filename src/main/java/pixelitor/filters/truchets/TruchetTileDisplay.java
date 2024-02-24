package pixelitor.filters.truchets;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public class TruchetTileDisplay extends JPanel {
    private TruchetSwatch swatch;

    private int mouseX;
    private int mouseY;

    private int __xOffset;
    private int __yOffset;
    private int __tileSize;

    public TruchetTileDisplay(TruchetSwatch swatch) {
        this.swatch = swatch;
        addMouseMotionListener(new MouseAdapter() {
            long then = System.currentTimeMillis();

            @Override
            public void mouseMoved(MouseEvent e) {
                long now = System.currentTimeMillis();
                if (now - then < 20) {
                    return;
                }
                preprocess(e, me -> {
                    mouseX = me.getX();
                    mouseY = me.getY();
                });
                then = now;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int W = getWidth(), H = getHeight();
        if (W * H == 0) {
            return;
        }
        int columns = swatch.getWidth(1), rows = swatch.getHeight(1);
        Graphics2D g2 = (Graphics2D) g;
        boolean widthFirst = W * 1d / H > columns * 1d / rows;
        __tileSize = widthFirst ? H / rows : W / columns;
        g2.translate(__xOffset = (widthFirst ? (W - columns * __tileSize) / 2 : 0),
            __yOffset = (widthFirst ? 0 : (H - rows * __tileSize) / 2));
        g2.setColor(Color.GRAY);
        g2.fillRect(mouseX * __tileSize, mouseY * __tileSize, __tileSize, __tileSize);
        g2.setColor(Color.BLACK);
        swatch.draw(g2, __tileSize, 5);
        g2.translate(widthFirst ? -(W - columns * __tileSize) / 2 : 0, widthFirst ? 0 : -(H - rows * __tileSize) / 2);
    }

    private void preprocess(MouseEvent e, Consumer<MouseEvent> l) {
        int x = e.getX(), y = e.getY();
        e.translatePoint(-x + (x - __xOffset) / __tileSize, -y + (y - __yOffset) / __tileSize);
        x = e.getX();
        y = e.getY();
        if (x < 0 || x >= swatch.columns || y < 0 || y >= swatch.rows) {
            return;
        }
        l.accept(e);
    }

    @Override
    public synchronized void addMouseListener(MouseListener l) {
        super.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                preprocess(e, l::mouseClicked);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                preprocess(e, l::mousePressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                preprocess(e, l::mouseReleased);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                preprocess(e, l::mouseEntered);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                preprocess(e, l::mouseExited);
            }
        });
    }
}