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

package pixelitor.filters;

import com.jhlabs.image.Colormap;
import com.jhlabs.image.PointFilter;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.gui.GUIText;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static com.jhlabs.image.WaveType.wave;
import static com.jhlabs.image.WaveType.wave01;
import static com.jhlabs.math.Noise.noise2;
import static com.jhlabs.math.Noise.turbulence2;
import static com.jhlabs.math.Noise.turbulence2Smooth;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.cos;
import static net.jafama.FastMath.pow;
import static net.jafama.FastMath.sin;
import static net.jafama.FastMath.sqrt;

/**
 * Marble filter
 */
public class Marble extends ParametrizedFilter {
    public static final String NAME = "Marble";

    @Serial
    private static final long serialVersionUID = -4289737664285529580L;

    private final RangeParam zoom = new RangeParam(GUIText.ZOOM, 1, 10, 200);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final RangeParam distortion = new RangeParam("Distortion", 0, 25, 100);
    private final RangeParam time = new RangeParam("Time (Phase)", 0, 0, 100);

    private final RangeParam detailsLevel = new RangeParam("Level", 0, 3, 8);
    private final RangeParam detailsStrength = new RangeParam("Strength", 0, 12, 48);

    private final IntChoiceParam type = new IntChoiceParam(GUIText.TYPE, new Item[]{
        new Item("Lines", Impl.TYPE_LINES),
        new Item("Rings", Impl.TYPE_RINGS),
        new Item("Spiral", Impl.TYPE_SPIRAL),
        new Item("Grid", Impl.TYPE_GRID),
        new Item("Star", Impl.TYPE_STAR),
    });

    private final IntChoiceParam waveType = IntChoiceParam.forWaveType();
    private final BooleanParam smoothDetails = new BooleanParam("Smoother Details", false);

    private final GradientParam gradient = new GradientParam("Colors",
        new float[]{0.0f, 0.5f, 1.0f},
        new Color[]{
            new Color(1, 14, 5),
            new Color(20, 50, 38),
            new Color(235, 255, 251),
        });

    private Impl filter;

    public Marble() {
        super(false);

        var details = new GroupedRangeParam("Details",
            new RangeParam[]{detailsLevel, detailsStrength}, false);

        type.setPresetKey("Type");
        zoom.setPresetKey("Zoom");
        setParams(
            type,
            waveType,
            time,
            angle,
            zoom.withAdjustedRange(0.25),
            distortion,
            details.notLinkable(),
            smoothDetails,
            gradient
        ).withReseedNoiseAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new Impl();
        }

        filter.setType(type.getValue());
        filter.setWaveType(waveType.getValue());

        double angleShift = Math.PI / 2;
        if (type.getValue() == Impl.TYPE_GRID) {
            angleShift = Math.PI / 4;
        }
        filter.setAngle(angle.getValueInRadians() + angleShift);

        filter.setZoom(zoom.getValueAsDouble());
        filter.setStrength(distortion.getValueAsDouble() / 5.0);
        filter.setDetails(detailsLevel.getValueAsDouble());
        filter.setDetailsStrength(detailsStrength.getValueAsDouble() / 4.0);
        filter.setColormap(gradient.getColorMap());
        filter.setSmoothDetails(smoothDetails.isChecked());
        filter.setTime(time.getValueAsDouble() / 5.0);

        return filter.filter(src, dest);
    }

    private static class Impl extends PointFilter {
        private static final int TYPE_LINES = 1;
        private static final int TYPE_GRID = 2;
        private static final int TYPE_RINGS = 3;
        private static final int TYPE_SPIRAL = 4;
        private static final int TYPE_STAR = 5;

        private double m00, m01, m10, m11;
        private double rotAngle;

        private double zoom = 200;
        private double detailsStrength;
        private double strength;
        private double octaves;
        private int type;
        private Colormap colormap;
        private double cx, cy;
        private int waveType;
        private boolean smoothDetails;
        private double time;

        protected Impl() {
            super(NAME);
        }

        public void setDetailsStrength(double f) {
            detailsStrength = f;
        }

        public void setStrength(double f) {
            strength = f;
        }

        public void setDetails(double f) {
            octaves = pow(2.0, f - 1.0);
        }

        public void setSmoothDetails(boolean smoothDetails) {
            this.smoothDetails = smoothDetails;
        }

        public void setType(int type) {
            this.type = type;
        }

        public void setWaveType(int waveType) {
            this.waveType = waveType;
        }

        public void setAngle(double angle) {
            rotAngle = angle;
            double cos = cos(angle);
            double sin = sin(angle);
            m00 = cos;
            m01 = sin;
            m10 = -sin;
            m11 = cos;
        }

        public void setZoom(double zoom) {
            this.zoom = zoom;
        }

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dst) {
            cx = src.getWidth() / 2.0;
            cy = src.getHeight() / 2.0;
            return super.filter(src, dst);
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            double dy = y - cy;
            double dx = x - cx;
            double nx = m00 * dx + m01 * dy;
            double ny = m10 * dx + m11 * dy;
            nx /= zoom;
            ny /= zoom;

            double f = strength * noise2((float) (nx * 0.1), (float) (ny * 0.1));
            if (smoothDetails) {
                f += detailsStrength * turbulence2Smooth(nx * 0.2, ny * 0.2, octaves);
            } else {
                f += detailsStrength * turbulence2(nx * 0.2, ny * 0.2, octaves);
            }
            f += time;

            float c = switch (type) {
                case TYPE_LINES -> calcLinesColor(nx, f);
                case TYPE_GRID -> calcGridColor(nx, ny, f);
                case TYPE_RINGS -> calcRingsColor(dy, dx, f);
                case TYPE_SPIRAL -> calcSpiralColor(dy, dx, f);
                case TYPE_STAR -> calcStarColor(dy, dx, f);
                default -> throw new IllegalStateException();
            };

            return colormap.getColor(c);
        }

        private float calcLinesColor(double nx, double f) {
            return (float) wave01(nx + f, waveType);
        }

        private float calcGridColor(double nx, double ny, double f) {
            double f2 = strength * noise2((float) (ny * -0.1), (float) (nx * -0.1));
            if (smoothDetails) {
                f2 += detailsStrength * turbulence2Smooth(ny * -0.2, nx * -0.2, octaves);
            } else {
                f2 += detailsStrength * turbulence2(ny * -0.2, nx * -0.2, octaves);
            }

            return (float) (wave01(nx + f, waveType) + wave01(ny + f2, waveType)) / 2.0f;
        }

        private float calcRingsColor(double dy, double dx, double f) {
            double dist = sqrt(dx * dx + dy * dy) / zoom;
            f += dist;

            return (float) wave01(f, waveType);
        }

        private float calcSpiralColor(double dy, double dx, double f) {
            double dist = sqrt(dx * dx + dy * dy) / zoom;
            double pixelAngle = atan2(dy, dx);
            f += (dist + pixelAngle - rotAngle);

            return (float) wave01(f, waveType);
        }

        private float calcStarColor(double dy, double dx, double f) {
            double pixelAngle = atan2(dy, dx);
            f += ((pixelAngle - rotAngle) * 10.0);
            return (float) ((1 + wave(f, waveType)) / 2);
        }

        public void setColormap(Colormap colormap) {
            this.colormap = colormap;
        }

        public void setTime(double time) {
            this.time = time;
        }
    }

}