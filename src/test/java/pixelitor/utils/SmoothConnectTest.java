/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import pixelitor.colors.Colors;
import pixelitor.particles.Modifier;
import pixelitor.particles.Particle;
import pixelitor.particles.ParticleSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

public class SmoothConnectTest extends JPanel {
    private final Dimension size = new Dimension(300, 300);

    private final ParticleSystem<IndexedParticle> system;
    private final JCheckBox isClosed = new JCheckBox("Close shape");
    private final JSlider smoothness = new JSlider(-1000, 1000, 100);
    private final List<Point2D> pointList;

    public SmoothConnectTest() {
        int particleCount = 5;
        pointList = new ArrayList<>(particleCount);
        Random random = new Random();
        system = ParticleSystem.<IndexedParticle>createSystem(particleCount)
            .setParticleCreator(() -> {
                IndexedParticle particle = new IndexedParticle();
                pointList.add(particle.pos);
                return particle;
            })
            .addModifier(new Modifier.RandomizePosition<>(size.width, size.height, random))
            .build();

        add(isClosed);
        isClosed.setBackground(Color.WHITE);
        add(smoothness);
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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Colors.fillWith(Color.BLACK, g2, 300, 300);
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        system.step();

        List<Point2D> points = new ArrayList<>(pointList);

        if (isClosed.isSelected()) {
            points.add(points.getFirst());
        }

        Path2D path = Shapes.smoothConnect(points, smoothness.getValue() / 100.0);

        g2.setColor(Color.WHITE);
        g2.draw(path);

        g2.setColor(Color.RED);
        for (Point2D point : points) {
            Shape c = Shapes.createCircle(point, 5);
            g2.fill(c);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SmoothConnectTest::buildGUI);
    }

    private static void buildGUI() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame f = new JFrame("Smooth Connect Test");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        SmoothConnectTest test = new SmoothConnectTest();
        f.add(test);

        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        new Thread(() -> {
            while (true) {
                Utils.sleep(40, TimeUnit.MILLISECONDS);
                test.repaint();
            }
        }).start();
    }

    private class IndexedParticle extends Particle {
        private static int idx = 0;
        public final int index;

        public IndexedParticle() {
            pos = new Point2D.Float();
            vel = new Vector2D();
            this.index = idx++;
            reset();
        }

        @Override
        public void flush() {
        }

        @Override
        public void reset() {
            double fact = (2 * Math.PI * Math.random());
            vel.set((float) Math.cos(fact), (float) Math.sin(fact));
        }

        @Override
        public boolean isDead() {
            return false;
        }

        @Override
        public void update() {
            pos.setLocation(
                pos.getX() + vel.x,
                pos.getY() + vel.y
            );
            if (pos.getX() < 0 || pos.getX() > size.width) {
                vel.set(vel.x * -1, vel.y);
            }
            if (pos.getY() < 0 || pos.getY() > size.height) {
                vel.set(vel.x, vel.y * -1);
            }
        }
    }
}
