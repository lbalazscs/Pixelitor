/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters;

import net.jafama.FastMath;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.AbstractLights.Type.STAR;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;

/**
 * The "Abstract Lights" filter.
 * The algorithm is based on <a href="https://codepen.io/tsuhre/pen/BYbjyg">this codepen by Ben Matthews</a>.
 */
public class AbstractLights extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final float MIN_VISIBLE_ALPHA = 0.002f;

    public static final String NAME = "Abstract Lights";

    private static final int DEFAULT_NUM_ITERATIONS = 1000;

    public enum Type {
        // random particles connected in a closed chain
        CHAOS("Chaos"),

        // all particles are connected to a central particle,
        // which orbits around a center point
        STAR("Star"),

        // particles move along the image border, turning at
        // each corner to form a rectangular path
        FRAME("Frame"),

        // all particles orbit on an ellipse that touches the image edges
        ELLIPTIC("Elliptic");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public boolean canBounce() {
            return this == CHAOS || this == STAR;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final EnumParam<Type> typeParam = new EnumParam<>("Type", Type.class);
    private final GroupedRangeParam starSizeParam = new GroupedRangeParam("Size", 0, 20, 100);
    private final ImagePositionParam starCenterParam = new ImagePositionParam("Center");

    private final RangeParam iterationsParam = new RangeParam("Iterations", 1, DEFAULT_NUM_ITERATIONS, 5000);
    private final RangeParam complexityParam = new RangeParam("Complexity", 1, 10, 20);
    private final RangeParam brightnessParam = new RangeParam("Brightness", 1, 6, 10);
    private final AngleParam hueParam = new AngleParam("Hue", 0);
    private final RangeParam hueRandomnessParam = new RangeParam("Hue Variability", 0, 25, 100);
    private final RangeParam whiteBlendParam = new RangeParam("Mix White", 0, 0, 100);
    private final RangeParam blurParam = new RangeParam("Blur", 0, 0, 7);
    private final RangeParam speedParam = new RangeParam("Particle Speed", 1, 1, 10);
    private final BooleanParam bounceParam = new BooleanParam("Edge Bounce", true);

    public AbstractLights() {
        super(false);

        // disable hue variation when complexity is 1 (two particles)
        complexityParam.setupDisableOtherIf(hueRandomnessParam, value -> value == 1);

        // disable hue controls when fully white blend is selected
        whiteBlendParam.setupDisableOtherIf(hueParam, value -> value == 100);
        whiteBlendParam.setupDisableOtherIf(hueRandomnessParam, value -> value == 100);

        CompositeParam advancedParam = new CompositeParam("Advanced",
            hueRandomnessParam, whiteBlendParam, blurParam, speedParam, bounceParam);
        advancedParam.setRandomizeMode(IGNORE_RANDOMIZE);

        CompositeParam starSettingsParam = new CompositeParam("Star Settings",
            starSizeParam, starCenterParam);

        // show star settings only for the star type
        typeParam.setupEnableOtherIf(starSettingsParam, type -> type == STAR);
        // disable bounce if the type doesn't allow it
        typeParam.setupDisableOtherIf(bounceParam, type -> !type.canBounce());

        initParams(
            typeParam,
            starSettingsParam,
            iterationsParam,
            complexityParam,
            brightnessParam,
            hueParam,
            advancedParam
        ).withReseedAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Random random = paramSet.getLastSeedRandom();
        int iterations = iterationsParam.getValue();
        int width = dest.getWidth();
        int height = dest.getHeight();

        var pt = new StatusBarProgressTracker(NAME, iterations);
        Graphics2D g2 = dest.createGraphics();

        double lineWidth = blurParam.getValueAsDouble() + 1.0;
        g2.setStroke(new BasicStroke((float) lineWidth));

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);

        float darkening = 1.0f;
        // the sqrt is an attempt to use linear light calculations
        float alpha = (float) (brightnessParam.getValueAsDouble() / (200.0 * Math.sqrt(lineWidth)));
        if (alpha < MIN_VISIBLE_ALPHA) {
            // if a smaller alpha is used, nothing is drawn, therefore
            // use this alpha and compensate by darkening the color.
            darkening = MIN_VISIBLE_ALPHA / alpha;
            alpha = MIN_VISIBLE_ALPHA;
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // if we increased alpha, we must compensate by reducing the color's brightness
        float colorBri = 1.0f / darkening;

        List<Particle> particles = createParticles(width, height, random, colorBri);
        connectParticles(particles, width, height);

        Line2D.Double reusableLine = new Line2D.Double();
        for (int i = 0; i < iterations; i++) {
            for (Particle particle : particles) {
                particle.update(width, height);
            }
            for (Particle particle : particles) {
                particle.draw(g2, reusableLine);
            }
            pt.unitDone();
        }

        g2.dispose();
        pt.finished();

        return dest;
    }

    private List<Particle> createParticles(int width, int height, Random random, float bri) {
        int numParticles = complexityParam.getValue() + 1;
        int baseHue = hueParam.getValueInNonIntuitiveDegrees();
        int hueRandomness = (int) (hueRandomnessParam.getValue() * 3.6);

        boolean bounce = bounceParam.isChecked();
        double speed = speedParam.getValueAsDouble();
        Type type = typeParam.getSelected();

        List<Particle> particles = new ArrayList<>();
        for (int i = 0; i < numParticles; i++) {
            // each particle is given a random starting position,
            // a random direction of movement, and a random color
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            Color color = generateParticleColor(random, bri, baseHue, hueRandomness);
            double angle = 2 * random.nextDouble() * Math.PI;

            switch (type) {
                case CHAOS, STAR -> particles.add(new Particle(x, y, speed, angle, color, bounce));
                case FRAME -> particles.add(new EdgeParticle(i % 4, speed, angle, color, bounce, x, y, width, height));
                case ELLIPTIC -> {
                    double cx = width / 2.0;
                    double cy = height / 2.0;
                    particles.add(new OrbitingParticle(cx, cy, cx, cy, speed, color, angle));
                }
            }
        }

        return particles;
    }

    /**
     * Connects particles to each other based on the selected type.
     */
    private void connectParticles(List<Particle> particles, int width, int height) {
        Type connectionType = typeParam.getSelected();

        if (connectionType == STAR) {
            connectToStar(particles, width, height);
        } else {
            connectInChain(particles);
        }
    }

    /**
     * Connects particles in a closed chain, where each particle is linked to the previous one.
     */
    private static void connectInChain(List<Particle> particles) {
        int numParticles = particles.size();
        for (int i = 0; i < numParticles; i++) {
            int prevIndex = (i > 0) ? i - 1 : numParticles - 1;
            particles.get(i).sibling = particles.get(prevIndex);
        }
    }

    /**
     * Connects all particles to a central orbiting "star" particle.
     */
    private void connectToStar(List<Particle> particles, int width, int height) {
        // replace the first particle with an orbiting particle
        Color c = particles.getFirst().color;
        OrbitingParticle centerStar = createCenterStarParticle(width, height, c);
        particles.set(0, centerStar);

        int numParticles = particles.size();
        for (int i = 1; i < numParticles; i++) {
            particles.get(i).sibling = centerStar;
        }
    }

    private OrbitingParticle createCenterStarParticle(int width, int height, Color c) {
        double speed = speedParam.getValueAsDouble();
        double maxRadius = Math.min(width, height) / 2.0;
        double radiusX = maxRadius * starSizeParam.getPercentage(0);
        double radiusY = maxRadius * starSizeParam.getPercentage(1);
        double cx = width * starCenterParam.getRelativeX();
        double cy = height * starCenterParam.getRelativeY();

        return new OrbitingParticle(cx, cy, radiusX, radiusY, speed, c, 0.0);
    }

    /**
     * Generates a random color for a particle.
     */
    private Color generateParticleColor(Random random, float bri, int baseHue, int hueRandomness) {
        int hue;
        if (hueRandomness > 0) {
            hue = (baseHue + random.nextInt(hueRandomness) - hueRandomness / 2) % 360;
        } else {
            hue = baseHue;
            random.nextInt(); // consume a random number to maintain random sequence
        }

        Color color = Color.getHSBColor(hue / 360.0f, 1.0f, bri);
        if (whiteBlendParam.getValue() > 0) {
            color = Colors.interpolateRGB(color, Color.WHITE, whiteBlendParam.getPercentage());
        }
        return color;
    }

    private static class Particle {
        protected double x, y; // position
        protected final double speed;
        protected double vx, vy; // velocity vector
        private final Color color;

        // each particle is connected to a sibling, and lines are drawn between them
        public Particle sibling;

        private final boolean bounce;

        public Particle(int x, int y, double speed, double angle, Color color, boolean bounce) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.vx = speed * FastMath.cos(angle);
            this.vy = speed * FastMath.sin(angle);
            this.color = color;
            this.bounce = bounce;
        }

        /**
         * Updates the particle's position for the next iteration.
         */
        public void update(int width, int height) {
            x += vx;
            y += vy;

            if (bounce) {
                if (x < 0 || x >= width) {
                    vx = -vx;
                }
                if (y < 0 || y >= height) {
                    vy = -vy;
                }
            }
        }

        /**
         * Draws a line from this particle to its "sibling".
         */
        public void draw(Graphics2D g, Line2D.Double line) {
            if (sibling == null) {
                return;
            }
            g.setColor(color);
            line.setLine(x, y, sibling.x, sibling.y);
            g.draw(line);
        }
    }

    /**
     * A type of particle that moves in a central, elliptical orbit.
     */
    private static class OrbitingParticle extends Particle {
        private double angle;
        private final double cx, cy;
        private final double radiusX;
        private final double radiusY;
        private final double angleIncrement;

        public OrbitingParticle(double cx, double cy, double radiusX, double radiusY,
                                double speed, Color color, double initialAngle) {
            // the initial x, y, and angle passed to super() are not used, as this
            // particle's movement is determined by its elliptical path, not a velocity vector
            super(0, 0, speed, 0, color, false);
            this.cx = cx;
            this.cy = cy;
            this.radiusX = radiusX;
            this.radiusY = radiusY;
            this.angle = initialAngle;
            this.angleIncrement = 2 * Math.PI * speed / DEFAULT_NUM_ITERATIONS;

            // set initial position on the ellipse
            updatePosition();
        }

        private void updatePosition() {
            x = cx + radiusX * FastMath.cos(angle);
            y = cy + radiusY * FastMath.sin(angle);
        }

        @Override
        public void update(int width, int height) {
            angle += angleIncrement;

            updatePosition();
        }
    }

    /**
     * A type of particle that is restricted to moving only along the four edges of the image.
     */
    private static class EdgeParticle extends Particle {
        private static final int STATE_TOP = 0;
        private static final int STATE_RIGHT = 1;
        private static final int STATE_BOTTOM = 2;
        private static final int STATE_LEFT = 3;
        private int state;

        public EdgeParticle(int initialState, double speed, double angle, Color color, boolean bounce, int x, int y, int width, int height) {
            super(x, y, speed, angle, color, bounce);
            state = initialState;

            // snaps the particle to its edge and initializes its velocity
            switch (state) {
                case STATE_TOP -> {
                    this.y = 0;
                    vx = speed;
                    vy = 0;
                }
                case STATE_RIGHT -> {
                    this.x = width;
                    vx = 0;
                    vy = speed;
                }
                case STATE_BOTTOM -> {
                    this.y = height;
                    vx = -speed;
                    vy = 0;
                }
                case STATE_LEFT -> {
                    this.x = 0;
                    vx = 0;
                    vy = -speed;
                }
                default -> throw new IllegalStateException("state = " + state);
            }
        }

        @Override
        public void update(int width, int height) {
            x += vx;
            y += vy;
            switch (state) {
                case STATE_TOP -> {
                    if (x >= width) { // right edge reached
                        state = STATE_RIGHT;
                        vx = 0;
                        vy = speed;
                    }
                }
                case STATE_RIGHT -> {
                    if (y >= height) { // bottom edge reached
                        state = STATE_BOTTOM;
                        vx = -speed;
                        vy = 0;
                    }
                }
                case STATE_BOTTOM -> {
                    if (x <= 0) { // left edge reached
                        state = STATE_LEFT;
                        vx = 0;
                        vy = -speed;
                    }
                }
                case STATE_LEFT -> {
                    if (y <= 0) { // top edge reached
                        state = STATE_TOP;
                        vx = speed;
                        vy = 0;
                    }
                }
                default -> throw new IllegalStateException("state = " + state);
            }
        }
    }
}
