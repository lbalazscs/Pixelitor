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

import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.gui.ImageComponents;
import pixelitor.layers.ImageLayer;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static pixelitor.colors.ColorUtils.TRANSPARENT_COLOR;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.USER_ONLY_OPACITY;

/**
 * Arbitrary Rotate
 */
public class TransformLayer extends FilterWithParametrizedGUI {
    private final ImagePositionParam centerParam = new ImagePositionParam("Pivot Point");
    private final AngleParam angleParam = new AngleParam("Rotate Angle", 0);
    private final ColorParam bgColorParam = new ColorParam("Background Color:", TRANSPARENT_COLOR, USER_ONLY_OPACITY);
    private final GroupedRangeParam scaleParam = new GroupedRangeParam("Scale (%)", 1, 100, 500);
    private final GroupedRangeParam shearParam = new GroupedRangeParam("Shear", -500, 0, 500, false);

    public TransformLayer() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                centerParam,
                angleParam,
                scaleParam,
                shearParam,
                bgColorParam
        ));
        shearParam.setLinked(false);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        // fill with the background color
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        g.setColor(bgColorParam.getColor());
        g.fillRect(0, 0, dest.getWidth(), dest.getHeight());

        double theta = angleParam.getValueInRadians();

        ImageLayer layer = ImageComponents.getActiveImageLayerOrMaskOrNull();

        float relativeX = centerParam.getRelativeX();
        float relativeY = centerParam.getRelativeY();

        double centerShiftX = (-layer.getTX() + src.getWidth()) * relativeX;
        double centerShiftY = (-layer.getTY() + src.getHeight()) * relativeY;

        AffineTransform transform = AffineTransform.getRotateInstance(theta, centerShiftX, centerShiftY);

        int scaleX = scaleParam.getValue(0);
        int scaleY = scaleParam.getValue(1);
        if ((scaleX != 100) || (scaleY != 100)) {
            transform.translate(centerShiftX, centerShiftY);
            transform.scale(scaleX / 100.0, scaleY / 100.0);
            transform.translate(-centerShiftX, -centerShiftY);
        }

        int shearX = shearParam.getValue(0);
        int shearY = shearParam.getValue(1);
        if ((shearX != 0) || (shearY != 0)) {
            transform.translate(centerShiftX, centerShiftY);
            transform.shear(shearX / 100.0, shearY / 100.0);
            transform.translate(-centerShiftX, -centerShiftY);
        }

        g.drawImage(src, transform, null);

        g.dispose();

        return dest;
    }
}