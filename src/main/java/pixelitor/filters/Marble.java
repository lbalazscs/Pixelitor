/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import static com.jhlabs.image.WaveType.wave;
import static com.jhlabs.math.Noise.*;
import static net.jafama.FastMath.*;
import static pixelitor.filters.gui.ReseedActions.reseedNoise;
import static pixelitor.gui.GUIText.ZOOM;

/**
 * Marble filter
 */
public class Marble extends ParametrizedFilter {
    public static final String NAME = "Marble";

    private final RangeParam zoom = new RangeParam(ZOOM, 1, 10, 200);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final RangeParam distortion = new RangeParam("Distortion", 0, 25, 100);
    private final RangeParam time = new RangeParam("Time (Phase)", 0, 0, 100);

    private final RangeParam detailsLevel = new RangeParam("Level", 0, 3, 8);
    private final RangeParam detailsStrength = new RangeParam("Strength", 0, 12, 48);

    private final IntChoiceParam type = new IntChoiceParam(GUIText.TYPE, new Item[]{
        new Item("Lines", Impl.TYPE_LINES),
        new Item("Rings", Impl.TYPE_RINGS),
        new Item("Grid", Impl.TYPE_GRID),
        new Item("Star", Impl.TYPE_STAR),
    });

    private final IntChoiceParam waveType = new IntChoiceParam("Wave Type",
        IntChoiceParam.waveTypeChoices);
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
        super(ShowOriginal.NO);

        var details = new GroupedRangeParam("Details",
                new RangeParam[]{detailsLevel, detailsStrength}, false);

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
        ).withAction(reseedNoise());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new Impl();
        }

        filter.setType(type.getValue());
        filter.setWaveType(waveType.getValue());

        double angleShift = Math.PI / 2;
        if (type.getValue() == Impl.TYPE_GRID) {
            angleShift = Math.PI / 4;
        }
        filter.setAngle((float) (angle.getValueInRadians() + angleShift));

        filter.setZoom(zoom.getValueAsFloat());
        filter.setStrength(distortion.getValueAsFloat() / 5.0f);
        filter.setDetails(detailsLevel.getValueAsFloat());
        filter.setDetailsStrength(detailsStrength.getValueAsFloat() / 4.0f);
        filter.setColormap(gradient.getValue());
        filter.setSmoothDetails(smoothDetails.isChecked());
        filter.setTime(time.getValueAsFloat() / 5.0f);
        
        dest = filter.filter(src, dest);
        return dest;
    }

    private static class Impl extends PointFilter {
        private static final int TYPE_LINES = 1;
        private static final int TYPE_GRID = 2;
        private static final int TYPE_RINGS = 3;
        private static final int TYPE_STAR = 4;

        private float m00, m01, m10, m11;
        private float rotAngle;

        private float zoom = 200;
        private float detailsStrength;
        private float strength;
        private float octaves;
        private int type;
        private Colormap colormap;
        private float cx, cy;
        private int waveType;
        private boolean smoothDetails;
        private float time;

        protected Impl() {
            super(NAME);
        }

        public void setDetailsStrength(float f) {
            detailsStrength = f;
        }

        public void setStrength(float f) {
            strength = f;
        }

        public void setDetails(float f) {
            octaves = (float) pow(2.0, f - 1.0);
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

        public void setAngle(float angle) {
            rotAngle = angle;
            float cos = (float) cos(angle);
            float sin = (float) sin(angle);
            m00 = cos;
            m01 = sin;
            m10 = -sin;
            m11 = cos;
        }

        public void setZoom(float zoom) {
            this.zoom = zoom;
        }

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dst) {
            cx = src.getWidth() / 2.0f;
            cy = src.getHeight() / 2.0f;
            return super.filter(src, dst);
        }

        @Override
        public int filterRGB(int x, int y, int rgb) {
            double dy = y - cy;
            double dx = x - cx;
            float nx = (float) (m00 * dx + m01 * dy);
            float ny = (float) (m10 * dx + m11 * dy);
            nx /= zoom;
            ny /= zoom;

            float f = strength * noise2(nx * 0.1f, ny * 0.1f);
            if (smoothDetails) {
                f += detailsStrength * turbulence2B(nx * 0.2f, ny * 0.2f, octaves);
            } else {
                f += detailsStrength * turbulence2(nx * 0.2f, ny * 0.2f, octaves);
            }
            f += time;

            float c = switch (type) {
                case TYPE_LINES -> calcLinesColor(nx, f);
                case TYPE_GRID -> calcGridColor(nx, ny, f);
                case TYPE_RINGS -> calcRingsColor(dy, dx, f);
                case TYPE_STAR -> calcStarColor(dy, dx, f);
                default -> throw new IllegalStateException();
            };

            return colormap.getColor(c);
        }

        private float calcLinesColor(float nx, float f) {
            return (float) ((1 + wave(nx + f, waveType)) / 2);
        }

        private float calcGridColor(float nx, float ny, float f) {
            float f2 = strength * noise2(ny * -0.1f, nx * -0.1f);
            if (smoothDetails) {
                f2 += detailsStrength * turbulence2B(ny * -0.2f, nx * -0.2f, octaves);
            } else {
                f2 += detailsStrength * turbulence2(ny * -0.2f, nx * -0.2f, octaves);
            }

            return (float) (2.0f + wave(nx + f, waveType) + wave(ny + f2, waveType)) / 4.0f;
        }

        private float calcRingsColor(double dy, double dx, float f) {
            float dist = (float) (sqrt(dx * dx + dy * dy) / zoom);
            f += dist;

            return (float) ((1 + wave(f, waveType)) / 2);
        }

        private float calcStarColor(double dy, double dx, float f) {
            float pixelAngle = (float) atan2(dy, dx);
            f += (pixelAngle - rotAngle) * 10.0f;
            return (float) ((1 + wave(f, waveType)) / 2);
        }

        public void setColormap(Colormap colormap) {
            this.colormap = colormap;
        }

        public void setTime(float time) {
            this.time = time;
        }
    }

}