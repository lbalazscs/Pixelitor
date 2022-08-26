/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.math.Noise;
import net.jafama.FastMath;
import pd.OpenSimplex2F;
import pixelitor.ThreadPool;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.GroupedRangeParam.GroupedRangeParamState;
import pixelitor.filters.gui.RangeParam.RangeParamState;
import pixelitor.particles.Modifier;
import pixelitor.particles.ParticleSystem;
import pixelitor.particles.SmoothPathParticle;
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

import static net.jafama.FastMath.*;
import static pixelitor.filters.gui.BooleanParam.BooleanParamState.YES;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.FREE_TRANSPARENCY;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

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

    private static Vector2D createSinkForce(Vector2D position, Vector2D center, float magnitude, Vector2D out) {
        if (magnitude == 0) {
            return out;
        }

        out.set(center);
        out.subtract(position);
        out.setMagnitude(magnitude);

        return out;
    }

    private static Vector2D createRevolveForce(Vector2D position, Vector2D center, float magnitude, Vector2D out) {
        if (magnitude == 0) {
            return out;
        }

        out.set(center);
        out.subtract(position);
        out.perpendicular();
        out.setMagnitude(magnitude);
        out.normalizeIfNonzero();
        out.multiply(magnitude);

        return out;
    }

    private static Vector2D createNoiseForce(float magnitude, float initTheta, float variantPI, double sampleX, double sampleY, double sampleZ, int turbulence, OpenSimplex2F noise, Vector2D out) {
        double value = initTheta + noise.turbulence3(sampleX, sampleY, sampleZ, turbulence) * variantPI;
        out.set((float) cos(value), (float) sin(value));
        out.setMagnitude(magnitude);
        return out;
    }

    private enum ForceMode implements Modifier<FlowFieldParticle> {
        FORCE_MODE_VELOCITY("No Mass") {
            @Override
            public void modify(FlowFieldParticle particle) {
                particle.pos.setLocation(
                    particle.pos.getX() + particle.delta.x,
                    particle.pos.getY() + particle.delta.y
                );
            }
        },
        FORCE_MODE_ACCELERATION("Uniform Mass") {
            @Override
            public void modify(FlowFieldParticle particle) {
                particle.vel.add(particle.delta);
                particle.pos.setLocation(
                    particle.pos.getX() + particle.vel.x,
                    particle.pos.getY() + particle.vel.y
                );
            }
        },
        FORCE_MODE_JOLT("Jolt") {
            @Override
            public void modify(FlowFieldParticle particle) {
                particle.acc.add(particle.delta);
                particle.vel.add(particle.acc);
                particle.pos.setLocation(
                    particle.pos.getX() + particle.vel.x,
                    particle.pos.getY() + particle.vel.y
                );
            }
        },
        FORCE_MODE_VELOCITY_AND_NOISE_BASED_RANDOMNESS("Thicken") {
            @Override
            public void modify(FlowFieldParticle particle) {
                particle.delta.add(Noise.noise2((float) particle.delta.x, (float) particle.delta.y) * 10);
                particle.pos.setLocation(
                    particle.pos.getX() + particle.delta.x,
                    particle.pos.getY() + particle.delta.y
                );
            }
        };

        final String name;

        ForceMode(String s) {
            name = s;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum ColorSource implements Modifier<FlowFieldParticle> {
        DEFAULT("Default", false) {
            @Override
            Color getColor(FlowFieldParticle particle) {
                return particle.startingColor;
            }
        },
        SOURCE_IMAGE("Source Image", true) {
            @Override
            public void initializeColorField(FlowFieldMeta meta) {
                fill(meta.fieldColors, meta.fieldWidth, meta.fieldHeight, (x, y) -> meta.goldenRatio
                    .next(colorFromSourceImage(x, y, meta.imgWidth, meta.sourcePixels, meta.fieldDensity)));
            }

            @Override
            Color getColor(FlowFieldParticle particle) {
                return particle.meta.fieldColors[particle.getFieldX()][particle.getFieldY()];
            }
        },
        RGB("RGB", false) {
            @Override
            Color getColor(FlowFieldParticle particle) {
                return rgbColorFromAcceleration(particle.delta, particle.startingColor);
            }
        },
        HSB_Cycle("HSB Cycle", false) {
            @Override
            Color getColor(FlowFieldParticle particle) {
                return hsbColorFromAcceleration(particle.delta, particle.startingColor, 6);
            }
        },
        Warm("Warm", false) {
            @Override
            Color getColor(FlowFieldParticle particle) {
                return hsbColorFromAcceleration(particle.delta, particle.startingColor, 400);
            }
        };

        private final String name;
        private final boolean requiresColorField;

        ColorSource(String name, boolean requiresColorField) {
            this.name = name;
            this.requiresColorField = requiresColorField;
        }

        public void initializeColorField(FlowFieldMeta meta) {
        }

        @Override
        public void modify(FlowFieldParticle particle) {
            particle.color = getColor(particle);
        }

        abstract Color getColor(FlowFieldParticle particle);

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
    private final BooleanParam antiAliasParam = new BooleanParam("Use Antialiasing", false);
    private final ColorParam backgroundColorParam = new ColorParam("Background Color", new Color(0, 0, 0, 1.0f), FREE_TRANSPARENCY);
    private final ColorParam particleColorParam = new ColorParam("Particle Color", new Color(1, 1, 1, 0.12f), FREE_TRANSPARENCY);
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

        DialogParam noiseAdjustmentParam = new DialogParam("Noise", zoomParam, varianceParam, turbulenceParam, windParam);
        noiseParam.setupEnableOtherIfNotZero(noiseAdjustmentParam);

        DialogParam advancedParam = new DialogParam("Advanced", forceModeParam, maxVelocityParam, pathLengthParam);

        setParams(
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

        ).withAction(ReseedSupport.createSimplexAction());

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

        FilterState vortex = new FilterState("Vortex")
            .with(forceMixerParam, new GroupedRangeParamState(new double[]{5, 35, 60}, false))
            .with(strokeParam, StrokeSettings.defaultsWith(StrokeType.TAPERING_REV, 4))
            .with(widthRandomnessParam, new RangeParamState(100))
            .with(antiAliasParam, YES)
            .withReset();

        paramSet.setBuiltinPresets(confetti, smoke, vanGogh, vortex);

        noiseParam.setToolTip("Add smooth randomness to the flow of particles.");
        sinkParam.setToolTip("Make particles flow towards the center point.");
        revolveParam.setToolTip("Make particles flow around the center point.");

        noiseAdjustmentParam.setToolTip("Some additional tweaks to manipulate the evaluation system.");
        zoomParam
            .setToolTip("Change zoom to make fields look bigger or smaller. Please note, that particle width will not take any effect.");
        varianceParam.setToolTip("Increase variance to add more turns and twists to the flow.");
        turbulenceParam.setToolTip("Increase to make the flow path rougher.");
        windParam.setToolTip("Spreads away the particles against the flow path.");

        advancedParam.setToolTip("Advanced or rarely used settings.");
        forceModeParam.setToolTip("Advanced control over how forces act.");
        maxVelocityParam.setToolTip("Adjust maximum velocity to make particles look more organised.");
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
        colorRandomnessParam.setToolTip("Increase to impart the particle color with some randomness.");
        widthRandomnessParam.setToolTip("Increase to draw particles with a randomised width.");
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        float zoom = zoomParam.getValue() * 0.25f;
        float variance = varianceParam.getValue() / 10.0f;
        int turbulence = turbulenceParam.getValue();
        float zFactor = windParam.getValueAsFloat() / 10000;

        ForceMode forceMode = forceModeParam.getSelected();
        float maximumVelocitySq = maxVelocityParam.getValue() * maxVelocityParam.getValue() / 10000.0f;
        int iterationCount = pathLengthParam.getValue() + 1;

        int particleCount = numParticlesParam.getValue();
        Stroke stroke = strokeParam.createStroke();
        boolean antialias = antiAliasParam.isChecked();
        Color bgColor = backgroundColorParam.getColor();
        Color particleColor = particleColorParam.getColor();
        ColorSource colorSource = initialColorsParam.getSelected();
        boolean inheritSpawnPoints = startFlowFromSourceParam.isChecked();
        float colorRandomness = (float) colorRandomnessParam.getPercentage();
        float widthRandomness = (float) widthRandomnessParam.getPercentage();

        float quality = min(QUALITY, SMOOTHNESS / zoom);
        int tolerance = min(TOLERANCE, ((int) (iterationCount * ITERATION_TO_TOLERANCE_RATIO)));
        float force = min(FORCE_MAGNITUDE, iterationCount * ITERATION_TO_FORCE_RATIO);

        float multiplierNoise = (float) (noiseParam.getPercentage() * force);
        float multiplierSink = (float) (sinkParam.getPercentage() * force);
        float multiplierRevolve = (float) (revolveParam.getPercentage() * force);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        int imgWidth = dest.getWidth();
        int imgHeight = dest.getHeight();

        int fieldWidth = (int) (imgWidth * quality + 1);
        float fieldDensity = fieldWidth * 1.0f / imgWidth;
        int fieldHeight = (int) (imgHeight * fieldDensity);

        Random r = ReseedSupport.getLastSeedRandom();
        OpenSimplex2F noise = ReseedSupport.getLastSeedSimplex();

        Vector2D center = new Vector2D(fieldWidth / 2.0f, fieldHeight / 2.0f);
        Rectangle bounds = new Rectangle(-PAD, -PAD,
            fieldWidth + PAD * 2, fieldHeight + PAD * 2);
        float variantPI = (float) FastMath.PI * variance;
        float initTheta = (float) (r.nextFloat() * 2 * FastMath.PI);

        int groupCount = IS_MULTI_THREADED ? ceilToInt(particleCount / (double) PARTICLES_PER_GROUP) : 1;
        var pt = new StatusBarProgressTracker(NAME, groupCount);

        Graphics2D g2 = dest.createGraphics();
        boolean useColorField = colorRandomness != 0 || colorSource.requiresColorField();
        boolean randomizeWidth = widthRandomness != 0;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        g2.setStroke(stroke);
        Colors.fillWith(bgColor, g2, imgWidth, imgHeight);
        g2.setColor(particleColor);
        if (antialias) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        Graphics2D[] graphicsCopies = new Graphics2D[groupCount];
        for (int i = 0; i < groupCount; i++) {
            graphicsCopies[i] = (Graphics2D) g2.create();
        }

        Color[][] fieldColors = getIf(useColorField, () -> new Color[fieldWidth][fieldHeight]);
        Stroke[] strokes = getIf(randomizeWidth, () -> new Stroke[100]);
        Vector2D[][] fieldAccelerations = new Vector2D[fieldWidth][fieldHeight];
        int[] sourcePixels = getIf(useColorField || inheritSpawnPoints, () -> ImageUtils.getPixelsAsArray(src));

        if (randomizeWidth) {
            fill(strokes, strokes.length, () -> strokeParam.createStrokeWithRandomWidth(r, widthRandomness));
        }

        initializeAcceleration(multiplierNoise, multiplierSink, multiplierRevolve, zoom, turbulence, fieldWidth, fieldHeight, noise, center, variantPI, initTheta, fieldAccelerations);

        List<Point2D> spawns = null;
        if (inheritSpawnPoints) {
            spawns = initializeSpawnPoints(imgWidth, fieldDensity, sourcePixels);
        }

        GoldenRatio goldenRatio = new GoldenRatio(r, particleColor, colorRandomness);
        FlowFieldMeta meta = new FlowFieldMeta(fieldWidth - 1, fieldHeight - 1, fieldDensity, bounds, tolerance, maximumVelocitySq, zFactor, zoom, turbulence, noise, multiplierNoise, initTheta, variantPI, forceMode, goldenRatio, fieldColors, imgWidth, sourcePixels);

        if (useColorField) {
            if (colorRandomness != 0) {
                fill(fieldColors, fieldWidth, fieldHeight, goldenRatio::next);
            }

            if (colorSource.requiresColorField) {
                colorSource.initializeColorField(meta);
            }
        }

        PositionRandomizer positionRandomizer = new PositionRandomizer(bounds.x, bounds.y, bounds.width, bounds.height, spawns, r);
        ParticleInitializer particleInitializer = new ParticleInitializer(particleColor, useColorField, fieldColors);
        ForceModeUpdater forceModeUpdater = new ForceModeUpdater(forceMode, fieldAccelerations);

        ParticleSystem<FlowFieldParticle> particleSystem = ParticleSystem.<FlowFieldParticle>createSystem(particleCount)
            .setParticleCreator(() -> new FlowFieldParticle(graphicsCopies, randomizeWidth ? strokes[
                r.nextInt(strokes.length)] : stroke, meta))
            .addModifier(positionRandomizer)
            .addModifier(particleInitializer)
            .addUpdater(forceModeUpdater)
            .addUpdater(colorSource)
            .build();

        Future<?>[] futures = particleSystem.iterate(iterationCount, groupCount);
        ThreadPool.waitFor(futures, pt);
        particleSystem.flush();
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

    private static void initializeAcceleration(float multiplierNoise, float multiplierSink, float multiplierRevolve, float zoom, int turbulence, int fieldWidth, int fieldHeight, OpenSimplex2F noise, Vector2D center, float variantPI, float initTheta, Vector2D[][] fieldAccelerations) {
        Vector2D position = new Vector2D();
        Vector2D forceDueToNoise = new Vector2D();
        Vector2D forceDueToSink = new Vector2D();
        Vector2D forceDueToRevolution = new Vector2D();

        for (int i = 0; i < fieldWidth; i++) {
            for (int j = 0; j < fieldHeight; j++) {

                position.set(i, j);

                createSinkForce(position, center, multiplierSink, forceDueToSink);

                createRevolveForce(position, center, multiplierRevolve, forceDueToRevolution);

                createNoiseForce(multiplierNoise, initTheta, variantPI, position.x / zoom,
                    position.y / zoom, 0, turbulence, noise, forceDueToNoise);

                fieldAccelerations[i][j] = Vector2D.add(forceDueToRevolution, forceDueToSink, forceDueToNoise);
            }
        }
    }

    private static List<Point2D> initializeSpawnPoints(int imgWidth, float fieldDensity, int[] sourcePixels) {
        List<Point2D> spawns = new ArrayList<>();
        for (int i = 0; i < sourcePixels.length; i++) {
            if ((sourcePixels[i] & 0xFF_00_00_00) != 0) {
                int y = i / imgWidth;
                int x = i - y * imgWidth;
                spawns.add(new Point(x *= fieldDensity, y *= fieldDensity));
            }
        }
        return spawns;
    }

    private static <T> T getIf(boolean value, Supplier<T> supplier) {
        if (value) {
            return supplier.get();
        }
        return null;
    }

    public static <T> void fill(T[] array, int w, Supplier<T> value) {
        for (int i = 0; i < w; i++) {
            array[i] = value.get();
        }
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
        x /= fieldDensity;
        y /= fieldDensity;
        int i = toRange(0, sourcePixels.length, x + y * imgWidth);
        return new Color(sourcePixels[i], true);
    }

    private static Color rgbColorFromAcceleration(Vector2D acc, Color particleColor) {
        double ra = sigmoidFit(acc.x) / 255, rb = sigmoidFit(acc.y) / 255;
        return new Color(
            (float) ra * particleColor.getRed(),
            (float) rb * particleColor.getGreen(),
            (float) ra * particleColor.getBlue(),
            (float) particleColor.getAlpha() / 255.0f);
    }

    private static Color hsbColorFromAcceleration(Vector2D acc, Color particleColor, float dividend) {
        int hsbColor = Color.HSBtoRGB((float) (acc.x + acc.y) / dividend, 0.8f, 1.0f);
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
        return 1 / (1 + FastMath.exp(-v));
    }

    public interface Coord2DFunction<T> {
        T get(int x, int y);
    }

    private static class PositionRandomizer implements Modifier<FlowFieldParticle> {
        final Modifier<FlowFieldParticle> modifier;

        public PositionRandomizer(int x, int y, int width, int height, List<Point2D> spawnPoints, Random random) {
            if (spawnPoints == null || spawnPoints.isEmpty()) {
                modifier = new RandomizePosition<>(x, y, width, height, random);
            } else {
                int size = spawnPoints.size();
                modifier = particle -> particle.pos.setLocation(spawnPoints.get(random.nextInt(size)));
            }
        }

        @Override
        public void modify(FlowFieldParticle particle) {
            modifier.modify(particle);
        }
    }

    private record ParticleInitializer(Color particleColor,
                                       boolean randomizeColor,
                                       Color[][] fieldColors) implements Modifier<FlowFieldParticle> {

        @Override
        public void modify(FlowFieldParticle particle) {
            particle.addPoint(particle.pos);
            int fieldX = particle.getFieldX();
            int fieldY = particle.getFieldY();
            particle.startingColor = particle.color = randomizeColor ? fieldColors[fieldX][fieldY] : particleColor;
        }
    }

    private record ForceModeUpdater(ForceMode forceMode,
                                    Vector2D[][] fieldAccelerations) implements Modifier<FlowFieldParticle> {
        @Override
        public void modify(FlowFieldParticle particle) {
            particle.delta.set(fieldAccelerations[particle.getFieldX()][particle.getFieldY()]);
        }
    }

    public static class FlowFieldParticle extends SmoothPathParticle {
        public final Vector2D delta = new Vector2D();
        public final Vector2D acc = new Vector2D();
        public Color startingColor;

        private final Stroke stroke;
        private final FlowFieldMeta meta;

        public FlowFieldParticle(Graphics2D[] gc, Stroke stroke, FlowFieldMeta meta) {
            super(gc);
            this.vel = new Vector2D();
            this.pos = new Point2D.Float();
            this.lastPos = new Point2D.Float();

            this.stroke = stroke;
            this.meta = meta;
        }

        @Override
        public void addPoint(Point2D point) {
            lastPos.setLocation(point);
            super.addPoint(Geometry.deScale(Geometry.newFrom(point), meta.fieldDensity));
        }

        @Override
        public void flush() {
            getGraphics().setStroke(stroke);
            super.flush();
        }

        @Override
        public void reset() {
            lastPos.setLocation(pos);
        }

        @Override
        public boolean isDead() {
            return !meta.bounds.contains(pos);
        }

        @Override
        public void update() {
            Vector2D oldVel = new Vector2D(vel.x, vel.y);

            if (meta.zFactor != 0) {
                double sampleX = pos.getX() / meta.zoom;
                double sampleY = pos.getY() / meta.zoom;
                double sampleZ = meta.zFactor * iterationIndex;
                Vector2D noiseDelta = new Vector2D();
                createNoiseForce(meta.multiplierNoise, meta.initTheta, meta.variantPI, sampleX, sampleY, sampleZ, meta.turbulence, meta.noise, noiseDelta);
                delta.add(noiseDelta);
            }

            meta.forceMode.modify(this);

            if (vel.lengthSq() > meta.maximumVelocitySq) {
                vel.set(oldVel);
            }

            if (positionChangeIsBigEnough()) {
                addPoint(Geometry.newFrom(pos));
            }
        }

        private boolean positionChangeIsBigEnough() {
            double xChange = abs(lastPos.getX() - pos.getX());
            double yChange = abs(lastPos.getY() - pos.getY());
            return xChange > meta.tolerance || yChange > meta.tolerance;
        }

        public int getFieldX() {
            return FastMath.toRange(0, meta.fieldWidth, (int) pos.getX());
        }

        public int getFieldY() {
            return FastMath.toRange(0, meta.fieldHeight, (int) pos.getY());
        }
    }

    private record FlowFieldMeta(int fieldWidth, int fieldHeight, float fieldDensity, Rectangle bounds,
                                 double tolerance,
                                 float maximumVelocitySq, double zFactor, double zoom, int turbulence,
                                 OpenSimplex2F noise, float multiplierNoise, float initTheta, float variantPI,
                                 ForceMode forceMode, GoldenRatio goldenRatio,
                                 Color[][] fieldColors, int imgWidth, int[] sourcePixels) {
    }
}
