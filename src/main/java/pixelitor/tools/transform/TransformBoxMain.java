/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.tools.transform;

import pixelitor.gui.View;
import pixelitor.tools.PRectangle;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class TransformBoxMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(TransformBoxMain::buildGUI);
    }

    private static void buildGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JFrame f = new JFrame("Test");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        TestView testView = new TestView();
        f.add(testView);

        JMenuBar menuBar = new JMenuBar();
        f.setJMenuBar(menuBar);
        setupMenus(menuBar, testView);

        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private static void setupMenus(JMenuBar menuBar, TestView testView) {
        JMenu zoomMenu = new JMenu("Zoom");
        menuBar.add(zoomMenu);

        zoomMenu.add(new AbstractAction("80%") {
            @Override
            public void actionPerformed(ActionEvent e) {
                testView.setViewScale(0.8);
            }
        });
        zoomMenu.add(new AbstractAction("100%") {
            @Override
            public void actionPerformed(ActionEvent e) {
                testView.setViewScale(1.0);
            }
        });
        zoomMenu.add(new AbstractAction("120%") {
            @Override
            public void actionPerformed(ActionEvent e) {
                testView.setViewScale(1.2);
            }
        });

        JMenu actionsMenu = new JMenu("Actions");
        menuBar.add(actionsMenu);
        actionsMenu.add(new AbstractAction("Reset All") {
            @Override
            public void actionPerformed(ActionEvent e) {
                testView.resetAll();
            }
        });
    }

    static class TestView extends JComponent implements View {
        private double viewScale = 1.0f;
        private double drawStartX;
        private double drawStartY;
        private final int canvasWidth = 300;
        private final int canvasHeight = 300;
        private final Dimension size = new Dimension(600, 400);

        PRectangle prect;
        TransformBox transformBox;
        Shape transformedShape;

        public TestView() {
            init();
            addListeners();
        }

        private void init() {
            setSize(size);
            calcDrawStart();

            prect = PRectangle.fromIm(50, 50, 200, 100, this);
            transformedShape = prect.getIm();
            Rectangle compSpaceRect = prect.getCo();

            transformBox = new TransformBox(compSpaceRect, this,
                    currCo -> {
                        AffineTransform imToCo = getImageToComponentTransform();
                        AffineTransform coToIm = getComponentToImageTransform();
//                    currCo.concatenate(coToIm);

                        AffineTransform at = new AffineTransform();
                        at.concatenate(coToIm); // then go back to image space
                        at.concatenate(currCo); // then apply the component-space transform
                        at.concatenate(imToCo); // first go to component space

                        transformedShape =
                                at.createTransformedShape(prect.getIm());
                    });
        }

        private void addListeners() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    transformBox.mousePressed(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    transformBox.mouseReleased(e);
                }
            });
            addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    transformBox.mouseDragged(e);
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    transformBox.mouseMoved(e);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            calcDrawStart();

            // set up image space
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

//        AffineTransform origTransform = g2.getTransform();
//        g2.translate(drawStartX, drawStartY);
//        g2.scale(viewScale, viewScale);
//
//        // fill background with white
//        g2.setColor(Color.WHITE);
//        g2.fillRect(0, 0, canvasWidth, canvasHeight);
//
//
//        // fill the shape with pink
//        g2.setColor(new Color(255, 191, 141));
//        g2.fill(transformedShape);

//        g2.setTransform(origTransform);

//        g2.transform(transformBox.getTransform());
            transformBox.paint(g2);

        }

        private void calcDrawStart() {
            drawStartX = (getWidth() - canvasWidth) / 2.0;
            drawStartY = (getHeight() - canvasHeight) / 2.0;
        }

        @Override
        public Dimension getPreferredSize() {
            return size;
        }

        @Override
        public Dimension getMinimumSize() {
            return size;
        }

        @Override
        public double componentXToImageSpace(double mouseX) {
            return ((mouseX - drawStartX) / viewScale);
        }

        @Override
        public double componentYToImageSpace(double mouseY) {
            return ((mouseY - drawStartY) / viewScale);
        }

        @Override
        public double imageXToComponentSpace(double x) {
            return drawStartX + x * viewScale;
        }

        @Override
        public double imageYToComponentSpace(double y) {
            return drawStartY + y * viewScale;
        }

        @Override
        public Rectangle2D fromComponentToImageSpace(Rectangle input) {
            return new Rectangle.Double(
                    componentXToImageSpace(input.x),
                    componentYToImageSpace(input.y),
                    (input.getWidth() / viewScale),
                    (input.getHeight() / viewScale)
            );
        }

        @Override
        public Rectangle fromImageToComponentSpace(Rectangle2D input) {
            return new Rectangle(
                    (int) imageXToComponentSpace(input.getX()),
                    (int) imageYToComponentSpace(input.getY()),
                    (int) (input.getWidth() * viewScale),
                    (int) (input.getHeight() * viewScale)
            );
        }

        // TODO untested
        @Override
        public AffineTransform getImageToComponentTransform() {
            AffineTransform t = new AffineTransform();
            t.translate(drawStartX, drawStartY);
            t.scale(viewScale, viewScale);
            return t;
        }

        // TODO untested
        @Override
        public AffineTransform getComponentToImageTransform() {
            AffineTransform t = new AffineTransform();
            double s = 1.0 / viewScale;
            t.scale(s, s);
            t.translate(-drawStartX, -drawStartY);
            return t;
        }

        public void setViewScale(double viewScale) {
            this.viewScale = viewScale;
            transformBox.viewSizeChanged(this);
            repaint();
        }

        public double getViewScale() {
            return viewScale;
        }

        public void resetAll() {
            init();
            repaint();
        }
    }
}
