/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.ImageMath;
import com.jhlabs.math.Noise;
import pd.OpenSimplex2F;
import pixelitor.ThreadPool;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.GroupedRangeParam.GroupedRangeParamState;
import pixelitor.filters.gui.RangeParam.RangeParamState;
import pixelitor.tools.shapes.StrokeType;
import pixelitor.utils.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static net.jafama.FastMath.cos;
import static net.jafama.FastMath.exp;
import static net.jafama.FastMath.pow;
import static net.jafama.FastMath.sin;
import static pixelitor.filters.gui.BooleanParam.BooleanParamState.YES;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;
import static pixelitor.filters.gui.TransparencyMode.ALPHA_ENABLED;
import static pixelitor.gui.utils.SliderSpinner.LabelPosition.BORDER;

public class FlowField extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = -2704082273724898173L;

    public static final String NAME = "Flow Field";

    private static final boolean IS_MULTI_THREADED = true;
    private static final int PAD = 100;
    private static final int PARTICLES_PER_GROUP = 100;
    private static final float QUALITY = 0.8f;
    private static final float SMOOTHNESS = 1224.3649f;
    private static final float LIMITING_ITERATIONS = 100;
    private static final float FORCE_MAGNITUDE = (float) pow(10.0f, 330 / 100.0f) / 100.0f;
    private static final int TOLERANCE = 30;
    private static final float ITERATION_TO_FORCE_RATIO = FORCE_MAGNITUDE / LIMITING_ITERATIONS;
    private static final float ITERATION_TO_TOLERANCE_RATIO = TOLERANCE / LIMITING_ITERATIONS;

    private static void createSinkForce(Vector2D position, Vector2D center, float magnitude, Vector2D out) {
        if (magnitude == 0) {
            out.set(0, 0);
            return;
        }

        out.set(center);
        out.subtract(position);
        out.setMagnitude(magnitude);
    }

    private static void createRevolveForce(Vector2D position, Vector2D center, float magnitude, Vector2D out) {
        if (magnitude == 0) {
            out.set(0, 0);
            return;
        }

        out.set(center);
        out.subtract(position);
        out.rotateBy90Degrees();
        out.setMagnitude(magnitude);
    }

    private static void createNoiseForce(float magnitude, float startAngle, float variance,
                                         double sampleX, double sampleY, double sampleZ,
                                         int turbulence, OpenSimplex2F noise, Vector2D out) {
        double angle = startAngle + noise.turbulence3(sampleX, sampleY, sampleZ, turbulence) * variance;
        out.set(cos(angle) * magnitude, sin(angle) * magnitude);
    }

    /**
     * Force modes define how the forces affect a particle's movement.
     */
    private enum ForceMode {
        VELOCITY("No Mass") {
            @Override
            public void modify(Particle particle) {
                particle.delta.translatePoint(particle.pos);
            }
        },
        ACCELERATION("Uniform Mass") {
            @Override
            public void modify(Particle particle) {
                particle.vel.add(particle.delta);
                particle.vel.translatePoint(particle.pos);
            }
        },
        JOLT("Jolt") {
            @Override
            public void modify(Particle particle) {
                particle.acc.add(particle.delta);
                particle.vel.add(particle.acc);
                particle.vel.translatePoint(particle.pos);
            }
        },
        VELOCITY_AND_NOISE_BASED_RANDOMNESS("Thicken") {
            @Override
            public void modify(Particle particle) {
                float x = (float) particle.delta.x;
                float y = (float) particle.delta.y;
                assert Float.isFinite(x) && Float.isFinite(y);

                particle.delta.add(Noise.noise2(x, y) * 10);
                particle.delta.translatePoint(particle.pos);
            }
        };

        final String name;

        ForceMode(String s) {
            name = s;
        }

        public abstract void modify(Particle particle);

        @Override
        public String toString() {
            return name;
        }
    }

    private enum ColorSource {
        DEFAULT("Default", false) {
            @Override
            Color getColor(Particle particle) {
                return particle.startingColor;
            }
        },
        SOURCE_IMAGE("Source Image", true) {
            @Override
            public void initializeColorField(Config config) {
                fill(config.fieldColors(), config.fieldWidth(), config.fieldHeight(), (x, y) -> config.hueWalker()
                    .next(colorFromSourceImage(x, y, config.imgWidth(), config.sourcePixels(), config.fieldDensity())));
            }

            @Override
            Color getColor(Particle particle) {
                return particle.config.fieldColors()[particle.getFieldX()][particle.getFieldY()];
            }
        },
        RGB("RGB", false) {
            @Override
            Color getColor(Particle particle) {
                return rgbColorFromAcceleration(particle.delta, particle.startingColor);
            }
        },
        HSB_CYCLE("HSB Cycle", false) {
            @Override
            Color getColor(Particle particle) {
                return hsbColorFromAcceleration(particle.delta, particle.startingColor, 6);
            }
        },
        WARM("Warm", false) {
            @Override
            Color getColor(Particle particle) {
                return hsbColorFromAcceleration(particle.delta, particle.startingColor, 400);
            }
        };

        private final String name;
        private final boolean requiresColorField;

        ColorSource(String name, boolean requiresColorField) {
            this.name = name;
            this.requiresColorField = requiresColorField;
        }

        public void initializeColorField(Config config) {
        }

        abstract Color getColor(Particle particle);

        @Override
        public String toString() {
            return name;
        }

        public boolean requiresColorField() {
            return requiresColorField;
        }
    }

    private final RangeParam noiseParam = new RangeParam("Noise", 0, 100, 100);
    private final RangeParam sinkParam = new RangeParam("Sink", 0, 0, 100);
    private final RangeParam revolveParam = new RangeParam("Revolve", 0, 0, 100);

    private final RangeParam zoomParam = new RangeParam("Zoom (%)", 100, 4000, 10000);
    private final RangeParam varianceParam = new RangeParam("Variance", 1, 20, 100);
    private final RangeParam turbulenceParam = new RangeParam("Turbulence", 1, 1, 8);
    private final RangeParam windParam = new RangeParam("Wind", 0, 0, 200);

    private final EnumParam<ForceMode> forceModeParam = new EnumParam<>("Force Mode", ForceMode.class);
    private final RangeParam maxVelocityParam = new RangeParam("Maximum Velocity", 1, 4000, 5000);
    private final RangeParam pathLengthParam = new RangeParam("Path Length", 1, 100, 100, true, BORDER, IGNORE_RANDOMIZE);

    private final RangeParam numParticlesParam = new RangeParam("Particle Count", 1, 1000, 20000, true, BORDER, IGNORE_RANDOMIZE);
    private final StrokeParam strokeParam = new StrokeParam("Stroke");
    private final BooleanParam antiAliasParam = new BooleanParam("Use Antialiasing");
    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1.0f), ALPHA_ENABLED);
    private final ColorParam particleColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), ALPHA_ENABLED);
    private final EnumParam<ColorSource> initialColorsParam = new EnumParam<>("Initialize Colors", ColorSource.class);
    private final BooleanParam startFlowFromSourceParam = new BooleanParam("Start Flow from Source Image", false, IGNORE_RANDOMIZE);
    private final RangeParam colorRandomnessParam = new RangeParam("Color Randomness (%)", 0, 0, 100);
    private final RangeParam widthRandomnessParam = new RangeParam("Stroke Width Randomness (%)", 0, 0, 100);

    public FlowField() {
        super(false);

        GroupedRangeParam forceMixerParam = new GroupedRangeParam("Force Mixer", new RangeParam[]{
            noiseParam,
            sinkParam,
            revolveParam
        }, false).autoNormalized();

        CompositeParam noiseAdjustmentParam = new CompositeParam("Noise", zoomParam, varianceParam, turbulenceParam, windParam);
        noiseParam.setupEnableOtherIfNotZero(noiseAdjustmentParam);

        CompositeParam advancedParam = new CompositeParam("Advanced", forceModeParam, maxVelocityParam, pathLengthParam);

        initParams(
            forceMixerParam,
            noiseAdjustmentParam,

            numParticlesParam,
            strokeParam,
            widthRandomnessParam,
            antiAliasParam,
            backgroundColorParam,
            particleColorParam,
            initialColorsParam,
            startFlowFromSourceParam,
            colorRandomnessParam,

            advancedParam

        ).withAction(paramSet.createReseedSimplexAction(true));

        UserPreset confetti = createConfettiPreset();
        UserPreset smoke = createSmokePreset();
        UserPreset vanGogh = createVanGoghPreset();
        FilterState vortex = createVortexPreset(forceMixerParam);
        paramSet.setBuiltinPresets(confetti, smoke, vanGogh, vortex);

        noiseParam.setToolTip("Add smooth randomness to the flow of particles.");
        sinkParam.setToolTip("Make particles flow towards the center point.");
        revolveParam.setToolTip("Make particles flow around the center point.");

        noiseAdjustmentParam.setToolTip("Some additional tweaks to manipulate the evaluation system.");
        zoomParam
            .setToolTip("Change zoom to make fields look bigger or smaller. Particle width will have no effect.");
        varianceParam.setToolTip("Increase variance to add more turns and twists to the flow.");
        turbulenceParam.setToolTip("Increase to make the flow path rougher.");
        windParam.setToolTip("Spreads the particles away from the flow path.");

        advancedParam.setToolTip("Advanced or rarely used settings.");
        forceModeParam.setToolTip("Advanced control over how forces act.");
        maxVelocityParam.setToolTip("Adjust maximum velocity to make particles look more organized.");
        pathLengthParam.setToolTip("Make individual particles cover longer paths.");

        numParticlesParam.setToolTip("Adjust the number of particles flowing in the field.");
        strokeParam.setToolTip("Adjust how particles are drawn - their width, shape, joins...");
        backgroundColorParam
            .setToolTip("Fills the canvas with a color. Decrease transparency to show the previous image.");
        particleColorParam
            .setToolTip("Change the initial color of the particles. Play with transparency to get interesting fills.");
        initialColorsParam
            .setToolTip("Make particles use the same color from their positions on the source image.");
        startFlowFromSourceParam.setToolTip("Prevent particles from spawning at any transparent regions.");
        colorRandomnessParam.setToolTip("Increase to add randomness to the particle color.");
        widthRandomnessParam.setToolTip("Increase to draw particles with a randomized width.");
    }

    private static UserPreset createConfettiPreset() {
        UserPreset confetti = new UserPreset("Confetti");

        confetti.put("Force Mixer", "60.00,20.00,20.00,false");
        confetti.put("Zoom (%)", "2450");
        confetti.put("Variance", "100");
        confetti.put("Turbulence", "5");
        confetti.put("Wind", "0");
        confetti.put("Particle Count", "500");
        confetti.put("Stroke Width", "7");
        confetti.put("Endpoint Cap", "Round");
        confetti.put("Corner Join", "Round");
        confetti.put("Line Type", "Zigzag");
        confetti.put("Shape", "Kiwi");
        confetti.put("Dashed", "no");
        confetti.put("Stroke Width Randomness (%)", "100");
        confetti.put("Use Antialiasing", "yes");
        confetti.put("Background Color", "00000000");
        confetti.put("Particle Color", "FFFFFFC9");
        confetti.put("Initialize Colors", "HSB Cycle");
        confetti.put("Start Flow from Source Image", "yes");
        confetti.put("Color Randomness (%)", "0");
        confetti.put("Force Mode", "Thicken");
        confetti.put("Maximum Velocity", "843");
        confetti.put("Path Length", "13");

        return confetti;
    }

    private static UserPreset createSmokePreset() {
        UserPreset smoke = new UserPreset("Smoke");

        smoke.put("Force Mixer", "70.00,15.00,15.00,false");
        smoke.put("Zoom (%)", "6500");
        smoke.put("Variance", "35");
        smoke.put("Turbulence", "5");
        smoke.put("Wind", "5");
        smoke.put("Particle Count", "3272");
        smoke.put("Stroke Width", "3");
        smoke.put("Endpoint Cap", "Round");
        smoke.put("Corner Join", "Round");
        smoke.put("Line Type", "Reversed Tapering");
        smoke.put("Shape", "Kiwi");
        smoke.put("Dashed", "no");
        smoke.put("Stroke Width Randomness (%)", "0");
        smoke.put("Use Antialiasing", "yes");
        smoke.put("Background Color", "000000FF");
        smoke.put("Particle Color", "FFFFFF08");
        smoke.put("Initialize Colors", "Default");
        smoke.put("Start Flow from Source Image", "no");
        smoke.put("Color Randomness (%)", "0");
        smoke.put("Force Mode", "No Mass");
        smoke.put("Maximum Velocity", "4000");
        smoke.put("Path Length", "100");

        return smoke;
    }

    private static UserPreset createVanGoghPreset() {
        UserPreset vanGogh = new UserPreset("Van Gogh");

        vanGogh.put("Force Mixer", "100.00,0.00,0.00,false");
        vanGogh.put("Zoom (%)", "3000");
        vanGogh.put("Variance", "25");
        vanGogh.put("Turbulence", "5");
        vanGogh.put("Wind", "0");
        vanGogh.put("Particle Count", "10000");
        vanGogh.put("Stroke Width", "5");
        vanGogh.put("Endpoint Cap", "Round");
        vanGogh.put("Corner Join", "Round");
        vanGogh.put("Line Type", "Basic");
        vanGogh.put("Shape", "Kiwi");
        vanGogh.put("Dashed", "no");
        vanGogh.put("Stroke Width Randomness (%)", "0");
        vanGogh.put("Use Antialiasing", "yes");
        vanGogh.put("Background Color", "000000FF");
        vanGogh.put("Particle Color", "FFFFFF1F");
        vanGogh.put("Initialize Colors", "Source Image");
        vanGogh.put("Start Flow from Source Image", "no");
        vanGogh.put("Color Randomness (%)", "0");
        vanGogh.put("Force Mode", "No Mass");
        vanGogh.put("Maximum Velocity", "4000");
        vanGogh.put("Path Length", "15");

        return vanGogh;
    }

    private FilterState createVortexPreset(GroupedRangeParam forceMixerParam) {
        return new FilterState("Vortex")
            .with(forceMixerParam, new GroupedRangeParamState(new double[]{5, 35, 60}, false))
            .with(strokeParam, StrokeSettings.defaultsWith(StrokeType.TAPERING_REV, 4))
            .with(widthRandomnessParam, new RangeParamState(100))
            .with(antiAliasParam, YES)
            .withReset();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        float zoom = zoomParam.getValue() * 0.25f;
        int turbulence = turbulenceParam.getValue();
        float wind = windParam.getValueAsFloat() / 10000;

        ForceMode forceMode = forceModeParam.getSelected();
        float maxVelocitySq = maxVelocityParam.getValue() * maxVelocityParam.getValue() / 10000.0f;
        int iterationCount = pathLengthParam.getValue() + 1;

        int particleCount = numParticlesParam.getValue();
        Stroke stroke = strokeParam.createStroke();
        boolean antialias = antiAliasParam.isChecked();
        Color bgColor = backgroundColorParam.getColor();
        Color particleColor = particleColorParam.getColor();
        ColorSource colorSource = initialColorsParam.getSelected();
        boolean inheritSpawnPoints = startFlowFromSourceParam.isChecked();
        double colorRandomness = colorRandomnessParam.getPercentage();
        float widthRandomness = (float) widthRandomnessParam.getPercentage();

        float quality = Math.min(QUALITY, SMOOTHNESS / zoom);
        int tolerance = Math.min(TOLERANCE, ((int) (iterationCount * ITERATION_TO_TOLERANCE_RATIO)));
        float force = Math.min(FORCE_MAGNITUDE, iterationCount * ITERATION_TO_FORCE_RATIO);

        float noiseMultiplier = (float) (noiseParam.getPercentage() * force);
        float sinkMultiplier = (float) (sinkParam.getPercentage() * force);
        float revolveMultiplier = (float) (revolveParam.getPercentage() * force);

        int imgWidth = dest.getWidth();
        int imgHeight = dest.getHeight();

        int fieldWidth = (int) (imgWidth * quality + 1);
        float fieldDensity = fieldWidth * 1.0f / imgWidth;
        int fieldHeight = (int) (imgHeight * fieldDensity);

        Random r = paramSet.getRandomWithLastSeed();
        OpenSimplex2F noise = paramSet.getSimplexWithLastSeed();

        Vector2D center = new Vector2D(fieldWidth / 2.0f, fieldHeight / 2.0f);
        Rectangle bounds = new Rectangle(-PAD, -PAD,
            fieldWidth + PAD * 2, fieldHeight + PAD * 2);
        float variance = (float) Math.PI * (varianceParam.getValue() / 10.0f);
        float startAngle = (float) (r.nextFloat() * 2 * Math.PI);

        int groupCount = IS_MULTI_THREADED ? (int) Math.ceil(particleCount / (double) PARTICLES_PER_GROUP) : 1;
        var pt = new StatusBarProgressTracker(NAME, groupCount);

        Graphics2D g2 = dest.createGraphics();
        boolean useColorField = colorRandomness != 0 || colorSource.requiresColorField();
        boolean randomizeWidth = widthRandomness != 0;

        g2.setStroke(stroke);
        Colors.fillWith(bgColor, g2, imgWidth, imgHeight);
        g2.setColor(particleColor);
        if (antialias) {
            g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        }
        Graphics2D[] graphicsCopies = new Graphics2D[groupCount];
        for (int i = 0; i < groupCount; i++) {
            graphicsCopies[i] = (Graphics2D) g2.create();
        }

        Color[][] fieldColors = useColorField ? new Color[fieldWidth][fieldHeight] : null;
        Stroke[] strokes = randomizeWidth ? new Stroke[100] : null;
        Vector2D[][] fieldAccelerations = new Vector2D[fieldWidth][fieldHeight];
        int[] sourcePixels = useColorField || inheritSpawnPoints ? ImageUtils.getPixels(src) : null;

        if (randomizeWidth) {
            for (int i = 0; i < strokes.length; i++) {
                strokes[i] = strokeParam.createStrokeWithRandomWidth(r, widthRandomness);
            }
        }

        initializeAcceleration(noiseMultiplier, sinkMultiplier, revolveMultiplier,
            zoom, turbulence, fieldWidth, fieldHeight, noise, center, variance, startAngle, fieldAccelerations);

        List<Point2D> spawns = inheritSpawnPoints
            ? initializeSpawnPoints(imgWidth, fieldDensity, sourcePixels)
            : null;

        Supplier<Point2D> spawnSupplier;
        if (spawns == null || spawns.isEmpty()) {
            spawnSupplier = () -> Rnd.point2DInRect(bounds, r);
        } else {
            spawnSupplier = () -> Rnd.chooseFrom(spawns, r);
        }

        HueWalker hueWalker = new HueWalker(r, particleColor, colorRandomness);
        Config config = new Config(fieldWidth, fieldHeight,
            fieldDensity, bounds, tolerance, maxVelocitySq, wind, zoom,
            turbulence, noise, noiseMultiplier, startAngle, variance, forceMode, hueWalker, fieldColors, imgWidth, sourcePixels,
            fieldAccelerations, colorSource, particleColor, useColorField, spawnSupplier);

        if (useColorField) {
            if (colorSource.requiresColorField()) {
                colorSource.initializeColorField(config);
            } else if (colorRandomness != 0) {
                fill(fieldColors, fieldWidth, fieldHeight, hueWalker::next);
            }
        }

        ParticleSystem particleSystem = new ParticleSystem(particleCount, () -> new Particle(
            randomizeWidth ? strokes[r.nextInt(strokes.length)] : stroke, config));

        Future<?>[] futures = particleSystem.iterate(iterationCount, groupCount, graphicsCopies);
        ThreadPool.waitFor(futures, pt);
        particleSystem.flush(g2);
        pt.finished();
        for (Graphics2D copy : graphicsCopies) {
            copy.dispose();
        }
        g2.dispose();

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    private static void initializeAcceleration(float noiseMultiplier, float sinkMultiplier, float revolveMultiplier,
                                               float zoom, int turbulence, int fieldWidth, int fieldHeight, OpenSimplex2F noise, Vector2D center, float variance, float startAngle, Vector2D[][] fieldAccelerations) {
        Vector2D position = new Vector2D();
        Vector2D noiseForce = new Vector2D();
        Vector2D sinkForce = new Vector2D();
        Vector2D revolutionForce = new Vector2D();

        for (int i = 0; i < fieldWidth; i++) {
            for (int j = 0; j < fieldHeight; j++) {
                position.set(i, j);

                createSinkForce(position, center, sinkMultiplier, sinkForce);
                createRevolveForce(position, center, revolveMultiplier, revolutionForce);
                createNoiseForce(noiseMultiplier, startAngle, variance, position.x / zoom,
                    position.y / zoom, 0, turbulence, noise, noiseForce);

                fieldAccelerations[i][j] = Vector2D.add(revolutionForce, sinkForce, noiseForce);
            }
        }
    }

    private static List<Point2D> initializeSpawnPoints(int imgWidth, float fieldDensity, int[] sourcePixels) {
        List<Point2D> spawns = new ArrayList<>();
        for (int i = 0; i < sourcePixels.length; i++) {
            if ((sourcePixels[i] & 0xFF_00_00_00) != 0) {
                int x = i % imgWidth;
                int y = i / imgWidth;
                spawns.add(new Point2D.Double(x * fieldDensity, y * fieldDensity));
            }
        }
        return spawns;
    }

    public static <T> void fill(T[][] array, int w, int h, Supplier<T> value) {
        fill(array, w, h, (x, y) -> value.get());
    }

    public static <T> void fill(T[][] array, int w, int h, Coord2DFunction<T> value) {
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                array[i][j] = value.get(i, j);
            }
        }
    }

    private static Color colorFromSourceImage(int x, int y, int imgWidth, int[] sourcePixels, float fieldDensity) {
        x = (int) (x / fieldDensity);
        y = (int) (y / fieldDensity);
        int i = ImageMath.clamp(x + y * imgWidth, 0, sourcePixels.length - 1);
        return new Color(sourcePixels[i], true);
    }

    private static Color rgbColorFromAcceleration(Vector2D acc, Color particleColor) {
        float normAccX = (float) sigmoidFit(acc.x) / 255.0f;
        float normAccY = (float) sigmoidFit(acc.y) / 255.0f;
        return new Color(
            normAccX * particleColor.getRed(),
            normAccY * particleColor.getGreen(),
            normAccX * particleColor.getBlue(),
            particleColor.getAlpha() / 255.0f);
    }

    private static Color hsbColorFromAcceleration(Vector2D acc, Color particleColor, float divisor) {
        int hsbColor = Color.HSBtoRGB((float) (acc.x + acc.y) / divisor, 0.8f, 1.0f);
        int r = (hsbColor >> 16) & 0xFF;
        int g = (hsbColor >> 8) & 0xFF;
        int b = hsbColor & 0xFF;
        int a = particleColor.getAlpha();
        return new Color(
            r * particleColor.getRed() / 65025.0f,
            g * particleColor.getGreen() / 65025.0f,
            b * particleColor.getBlue() / 65025.0f,
            a / 255.0f);
    }

    private static double sigmoidFit(double v) {
        return (1 + sigmoid(v)) / 2;
    }

    private static double sigmoid(double v) {
        return 1 / (1 + exp(-v));
    }

    public interface Coord2DFunction<T> {
        T get(int x, int y);
    }

    /**
     * A read-only configuration object holding the pre-calculated
     * field, bounds, user parameters, and random number generators.
     */
    private record Config(int fieldWidth, int fieldHeight, float fieldDensity, Rectangle bounds,
                          double tolerance, float maxVelocitySq, double wind, double zoom, int turbulence,
                          OpenSimplex2F noise, float noiseMultiplier, float startAngle, float variance,
                          ForceMode forceMode, HueWalker hueWalker, Color[][] fieldColors, int imgWidth,
                          int[] sourcePixels, Vector2D[][] fieldAccelerations, ColorSource colorSource,
                          Color particleColor, boolean randomizeColor, Supplier<Point2D> spawnSupplier) {
    }

    /**
     * A single point moving through the flow field.
     */
    private static class Particle {
        Point2D pos = new Point2D.Double();
        Point2D lastPos = new Point2D.Double();
        final Vector2D delta = new Vector2D();

        Vector2D vel = new Vector2D();
        final Vector2D acc = new Vector2D();

        Color startingColor;
        Color color;

        int index; // the particle's index in the list of all particles

        private final List<Point2D> pathPoints = new ArrayList<>();

        private final Stroke stroke;
        final Config config;

        public Particle(Stroke stroke, Config config) {
            this.stroke = stroke;
            this.config = config;
        }

        /**
         * Called when the particle is initialized or respawned.
         */
        public void init() {
            // clears its path history, and places itself at a starting point
            // (either random or based on the source image)
            pathPoints.clear();
            this.pos.setLocation(config.spawnSupplier().get());
            addPoint(this.pos);

            // picks a starting color
            int fieldX = getFieldX();
            int fieldY = getFieldY();
            this.startingColor = this.color = config.randomizeColor()
                ? config.fieldColors()[fieldX][fieldY]
                : config.particleColor();

            reset();
        }

        /**
         * Called at every simulation iteration.
         */
        private void step(int index, Graphics2D g) {
            if (isDead()) {
                flush(g);
                init();
            }
            this.index = index;
            update();
        }

        public void addPoint(Point2D point) {
            lastPos.setLocation(point);
            pathPoints.add(new Point2D.Double(
                point.getX() / config.fieldDensity(),
                point.getY() / config.fieldDensity()
            ));
        }

        /**
         * Draws the current path onto the image when a particle dies, or when the simulation ends.
         */
        public void flush(Graphics2D g) {
            if (isPathReady()) {
                g.setStroke(stroke);
                g.setColor(color);
                g.draw(getPath());
            }
            pathPoints.clear();
        }

        private boolean isPathReady() {
            return pathPoints.size() >= 3;
        }

        private Shape getPath() {
            return Shapes.smoothConnect(pathPoints, 0.5);
        }

        public void reset() {
            lastPos.setLocation(pos);
        }

        /**
         * Whether the particle has drifted outside the image boundaries.
         */
        public boolean isDead() {
            return !config.bounds().contains(pos);
        }

        /**
         * The physics engine for the particle.
         */
        public void update() {
            // retrieves the pre-calculated acceleration vector
            this.delta.set(config.fieldAccelerations()[getFieldX()][getFieldY()]);

            this.color = config.colorSource().getColor(this);
            Vector2D oldVel = new Vector2D(vel.x, vel.y);

            // if wind is enabled, calculate an additional real-time noise
            // force to push the particle slightly off its predetermined path
            if (config.wind() != 0) {
                double sampleX = pos.getX() / config.zoom();
                double sampleY = pos.getY() / config.zoom();
                double sampleZ = config.wind() * index;
                Vector2D noiseDelta = new Vector2D();
                createNoiseForce(config.noiseMultiplier(), config.startAngle(), config.variance(),
                    sampleX, sampleY, sampleZ, config.turbulence(), config.noise(), noiseDelta);
                delta.add(noiseDelta);
            }

            // apply the force to the particle's movement
            config.forceMode().modify(this);

            // caps the maximum velocity
            if (vel.lengthSq() > config.maxVelocitySq()) {
                vel.set(oldVel);
            }

            if (positionChangedEnough()) {
                addPoint(pos);
            }
        }

        private boolean positionChangedEnough() {
            double xChange = Math.abs(lastPos.getX() - pos.getX());
            double yChange = Math.abs(lastPos.getY() - pos.getY());
            return xChange > config.tolerance() || yChange > config.tolerance();
        }

        public int getFieldX() {
            return ImageMath.clamp((int) pos.getX(), 0, config.fieldWidth() - 1);
        }

        public int getFieldY() {
            return ImageMath.clamp((int) pos.getY(), 0, config.fieldHeight() - 1);
        }
    }

    /**
     * Manages a collection of particles, handling their lifecycle and updates.
     */
    private static class ParticleSystem {
        private final List<Particle> particles;

        ParticleSystem(int particleCount, Supplier<Particle> creator) {
            this.particles = new ArrayList<>(particleCount);
            for (int i = 0; i < particleCount; i++) {
                Particle particle = creator.get();
                particle.init();
                particles.add(particle);
            }
        }

        /**
         * Advances the simulation by a single step for the given range of particles.
         */
        private void step(int start, int end, Graphics2D g) {
            for (int i = start, s = Math.min(particles.size(), end); i < s; i++) {
                particles.get(i).step(i, g);
            }
        }

        /**
         * Runs the simulation for a given number of iterations.
         */
        private void iterate(int iterations, int start, int end, Graphics2D g) {
            for (int i = 0; i < iterations; i++) {
                step(start, end, g);
            }
        }

        /**
         * Runs the simulation for the given number of iterations using multiple threads.
         */
        public Future<?>[] iterate(int iterations, int groupCount, Graphics2D[] graphicsCopies) {
            Future<?>[] futures = new Future[groupCount];
            int size = particles.size();
            int groupSize = (int) Math.ceil(size / (double) groupCount);

            // divides the particles into equal-sized chunks (groups),
            // then submits a task to the thread pool for each chunk
            for (int i = 0, k = 0; i < size; i += groupSize, k++) {
                int finalI = i;
                Graphics2D g = graphicsCopies[k];
                futures[k] = ThreadPool.submit(() -> iterate(iterations, finalI, finalI + groupSize, g));
            }

            return futures;
        }

        /**
         * Flushes all particles, forcing them to draw their final state.
         */
        public void flush(Graphics2D g) {
            for (Particle particle : particles) {
                particle.flush(g);
            }
        }
    }
}
