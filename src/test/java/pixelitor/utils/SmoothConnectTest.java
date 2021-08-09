/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
        //<editor-fold defaultstate="collapsed" desc="Initializing Particle System">

        int particleCount = 5;

        pointList = new ArrayList<>(particleCount);

        system = ParticleSystem.<IndexedParticle>createSystem(particleCount)
                .setParticleCreator(() -> {
                    IndexedParticle particle = new IndexedParticle();
                    pointList.add(particle.pos);
                    return particle;
                })
                .addModifier(new Modifier.RandomizePosition<>(size.width, size.height, ReseedSupport.getLastSeedRandom()))
                .build();

        //</editor-fold>

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
            points.add(points.get(0));
        }

//        Path2D path = Shapes.smoothConnect(points);
        Path2D path = Shapes.smoothConnect(points, smoothness.getValue() / 100f);

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
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
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
        public int index;

        public IndexedParticle() {
            pos = new Point2D.Float();
            vel = new Point2D.Float();
            this.index = idx++;
            reset();
        }

        @Override
        public void flush() {

        }

        @Override
        public void reset() {
            double fact = (2 * Math.PI * Math.random());
            vel.setLocation(Math.cos(fact), Math.sin(fact));
        }

        @Override
        public boolean isDead() {
            return false;
        }

        @Override
        public void update() {
            Geometry.add(pos, vel, pos);
            if (pos.getX() < 0 || pos.getX() > size.width) vel.setLocation(vel.getX() * -1, vel.getY());
            if (pos.getY() < 0 || pos.getY() > size.height) vel.setLocation(vel.getX(), vel.getY() * -1);
        }

    }

}
