/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionSetting;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static com.jhlabs.image.WaveType.wave;
import static com.jhlabs.math.Noise.noise2;
import static com.jhlabs.math.Noise.turbulence2;
import static com.jhlabs.math.Noise.turbulence2B;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.cos;
import static net.jafama.FastMath.pow;
import static net.jafama.FastMath.sin;
import static net.jafama.FastMath.sqrt;

/**
 * Marble filter
 */
public class Marble extends FilterWithParametrizedGUI {
    public static final String NAME = "Marble";

    private final RangeParam zoom = new RangeParam("Zoom", 1, 10, 200);
    private final AngleParam angle = new AngleParam("Angle", 0);
    private final RangeParam distortion = new RangeParam("Distortion", 0, 25, 100);

    private final RangeParam detailsLevel = new RangeParam("Level", 0, 3, 8);
    private final RangeParam detailsStrength = new RangeParam("Strength", 0, 12, 50);
    private final GroupedRangeParam details = new GroupedRangeParam("Details",
            new RangeParam[]{detailsLevel, detailsStrength}, false);

    private final IntChoiceParam type = new IntChoiceParam("Type", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Lines", Impl.TYPE_LINES),
            new IntChoiceParam.Value("Rings", Impl.TYPE_RINGS),
            new IntChoiceParam.Value("Grid", Impl.TYPE_GRID),
            new IntChoiceParam.Value("Star", Impl.TYPE_STAR),
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
        setParamSet(new ParamSet(
                type,
                waveType,
                angle,
                zoom.adjustRangeToImageSize(0.25),
                distortion,
                details.setShowLinkedCB(false),
                smoothDetails,
                gradient
        ).withAction(new ReseedNoiseActionSetting()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new Impl(NAME);
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

        protected Impl(String filterName) {
            super(filterName);
        }

        public void setDetailsStrength(float f) {
            this.detailsStrength = f;
        }

        public void setStrength(float f) {
            this.strength = f;
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
            this.rotAngle = angle;
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

            float c;
            float f = strength * (noise2(nx * 0.1f, ny * 0.1f));
            if (smoothDetails) {
                f += detailsStrength * turbulence2B(nx * 0.2f, ny * 0.2f, octaves);
            } else {
                f += detailsStrength * turbulence2(nx * 0.2f, ny * 0.2f, octaves);
            }

            switch (type) {
                case TYPE_LINES:
                    c = (float) ((1 + wave((nx + f), waveType)) / 2);
                    break;
                case TYPE_GRID:
                    float f2 = strength * (noise2(ny * -0.1f, nx * -0.1f));
                    if (smoothDetails) {
                        f2 += detailsStrength * turbulence2B(ny * -0.2f, nx * -0.2f, octaves);
                    } else {
                        f2 += detailsStrength * turbulence2(ny * -0.2f, nx * -0.2f, octaves);
                    }

                    c = ((float) (2.0f + wave(nx + f, waveType) + wave(ny + f2, waveType))) / 4.0f;
                    break;
                case TYPE_RINGS:
                    double dist = sqrt(dx * dx + dy * dy) / zoom;
                    f += dist;

                    c = (float) ((1 + wave(f, waveType)) / 2);
                    break;
                case TYPE_STAR:
                    double pixelAngle = atan2(dy, dx);
                    f += (pixelAngle - rotAngle) * 10.0f;
                    c = (float) ((1 + wave(f, waveType)) / 2);
                    break;
                default:
                    throw new IllegalStateException();
            }

            return colormap.getColor(c);
        }

        public void setColormap(Colormap colormap) {
            this.colormap = colormap;
        }
    }

}