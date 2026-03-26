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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.composite.AddComposite;
import com.jhlabs.image.RaysFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.ResizingFilterHelper;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GradientParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.ResizingFilterHelper.ScaleUpQuality.BILINEAR_FAST;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;
import static pixelitor.gui.GUIText.OPACITY;

/**
 * Rays filter based on the JHLabs {@link RaysFilter}.
 */
public class JHRays extends ParametrizedFilter {
    public static final String NAME = "Rays";

    @Serial
    private static final long serialVersionUID = -6212129082965328281L;

    private final ImagePositionParam center = new ImagePositionParam("Light Source");
    private final RangeParam rotation = new RangeParam("Twirl", -90, 0, 90);
    private final RangeParam length = new RangeParam("Length", 0, 20, 200);
    private final RangeParam opacity = new RangeParam(OPACITY, 0, 80, 100);
    private final RangeParam strength = new RangeParam("Strength", 0, 200, 500);

    private final GradientParam colors = GradientParam.createUniformWhite();

    private final RangeParam threshold = new RangeParam("Threshold (%)", 0, 25, 100);
    private final BooleanParam raysOnly = new BooleanParam("Rays Only", false, IGNORE_RANDOMIZE);

    private RaysFilter filter;

    public JHRays() {
        super(true);

        opacity.setPresetKey("Opacity (%)");

        initParams(
            center,
            length,
            threshold,
            strength,
            colors,
            opacity,
            rotation,
            raysOnly
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new RaysFilter(NAME);
        }

        filter.setCenter(center.getRelativePoint());

        filter.setStrength((float) strength.getPercentage());
        filter.setRotation(rotation.getValueInRadians());
        filter.setThreshold((float) threshold.getPercentage());
        filter.setColormap(colors.getColorMap());

        // this value should not be divided by resizeFactor because
        // this is a scale and not really a length
        filter.setZoom((float) length.getPercentage());

        var helper = new ResizingFilterHelper(src);
        boolean shouldResize = helper.shouldResize();

        int filterUnits = 3;
        int workUnits = filterUnits + 3; // +3 for the rays only step at the end
        if (shouldResize) {
            int resizeUnits = helper.getResizeWorkUnits(BILINEAR_FAST);
            workUnits += resizeUnits;
        }
        var pt = new StatusBarProgressTracker(NAME, workUnits);
        filter.setProgressTracker(pt);

        BufferedImage rays;
        if (shouldResize) {
            rays = helper.invoke(BILINEAR_FAST, filter, pt, 0);
        } else {
            // normal case, no resizing
            rays = filter.filter(src, dest);
        }

        // RaysFilter outputs an image with an opaque black background
        // (alpha=255, R=0, G=0, B=0) and the colored rays drawn over it
        if (raysOnly.isChecked()) {
            int[] rayPixels = ImageUtils.getPixels(rays);
            float opacityMult = (float) opacity.getPercentage();
            boolean isPremult = rays.isAlphaPremultiplied();

            for (int i = 0; i < rayPixels.length; i++) {
                int rgb = rayPixels[i];
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                // use the maximum channel value as the alpha to preserve the true color intensity
                int max = Math.max(r, Math.max(g, b));
                int a = (int) (max * opacityMult);

                if (a == 0) {
                    rayPixels[i] = 0; // transparent black
                } else {
                    if (isPremult) {
                        // if the image was converted to premultiplied
                        // we must provide premultiplied RGB values
                        int pr = (int) (r * opacityMult);
                        int pg = (int) (g * opacityMult);
                        int pb = (int) (b * opacityMult);
                        rayPixels[i] = (a << 24) | (pr << 16) | (pg << 8) | pb;
                    } else {
                        // Unpremultiply the RGB channels for standard compositing.
                        // Since `max` is at least 1, we divide safely.
                        int ur = r * 255 / max;
                        int ug = g * 255 / max;
                        int ub = b * 255 / max;
                        rayPixels[i] = (a << 24) | (ur << 16) | (ug << 8) | ub;
                    }
                }
            }

            pt.unitsDone(3);
            pt.finished();

            return rays; // return the modified image directly
        }

        // add the rays on top of the source
        dest = ImageUtils.copyImage(src);
        Graphics2D g = dest.createGraphics();
        g.setComposite(new AddComposite((float) opacity.getPercentage()));
        g.drawRenderedImage(rays, null);
        g.dispose();

        pt.unitsDone(3);
        pt.finished();

        return dest;
    }

    @Override
    public boolean supportsGray() {
        return false;
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}
