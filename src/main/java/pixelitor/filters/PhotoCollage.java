/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import org.jdesktop.swingx.image.StackBlurFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.Utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Photo Collage
 */
public class PhotoCollage extends FilterWithParametrizedGUI {
    //    private RangeParam sizeParam = new RangeParam("Image Size", 40, 999, 200);
    private final GroupedRangeParam sizeParam = new GroupedRangeParam("Photo Size", 40, 999, 200);

    private final RangeParam marginSizeParam = new RangeParam("Margin", 0, 20, 5);
    private final RangeParam imageNumberParam = new RangeParam("Number of Images", 1, 100, 10);
    private final RangeParam randomRotationParam = new RangeParam("Random Rotation Amount (%)", 0, 100, 100);
    private final BooleanParam allowOutsideParam = new BooleanParam("Allow Outside", true);
    private final ColorParam bgColorParam = new ColorParam("Background Color", Color.BLACK, true, false);

    private final RangeParam shadowOpacityParam = new RangeParam("Shadow Opacity (%)", 0, 100, 80);
    private final AngleParam shadowAngleParam = new AngleParam("Shadow Angle", 0.7);
    private final RangeParam shadowDistanceParam = new RangeParam("Shadow Distance", 0, 20, 5);
    private final RangeParam shadowSoftnessParam = new RangeParam("Shadow Softness", 0, 10, 3);

    public PhotoCollage() {
        super("Photo Collage", true, false);
        setParamSet(new ParamSet(
                imageNumberParam,
                sizeParam.adjustRangeToImageSize(1.0),
                randomRotationParam,
                allowOutsideParam,
                marginSizeParam.adjustRangeToImageSize(0.02),
                bgColorParam,
                shadowOpacityParam,
                shadowAngleParam,
                shadowDistanceParam.adjustRangeToImageSize(0.02),
                shadowSoftnessParam.adjustRangeToImageSize(0.01),
                ReseedSupport.createParam()
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        int xSize = sizeParam.getValue(0);
        int ySize = sizeParam.getValue(1);

        // fill with the background color
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(bgColorParam.getColor());
        g.fillRect(0, 0, dest.getWidth(), dest.getHeight());


        Rectangle fullImageRect = new Rectangle(0, 0, xSize, ySize);
        int margin = marginSizeParam.getValue();
        Rectangle imageRect = new Rectangle(fullImageRect);
        imageRect.grow(-margin, -margin);

        Paint imagePaint = new TexturePaint(src, new Rectangle2D.Float(0, 0, src.getWidth(), src.getHeight()));

        // the shadow image must be larger than the image size so that there is room for soft shadows
        int shadowSoftness = shadowSoftnessParam.getValue();
        int softShadowRoom = 1 + (int) (2.3 * shadowSoftness);
        BufferedImage shadowImage = new BufferedImage(xSize + 2 * softShadowRoom, ySize + 2 * softShadowRoom, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gShadow = shadowImage.createGraphics();
        gShadow.setColor(Color.BLACK);
        gShadow.fillRect(softShadowRoom, softShadowRoom, xSize, ySize);
        gShadow.dispose();
        if (shadowSoftness > 0) {
            shadowImage = new StackBlurFilter(shadowSoftness).filter(shadowImage, shadowImage);
        }

//        Rectangle debugShadowRect = new Rectangle(0, 0, shadowImage.getWidth(), shadowImage.getHeight());

        Point2D offset = Utils.calculateOffset(shadowDistanceParam.getValue(), shadowAngleParam.getValueInRadians());
        int shadowOffsetX = (int) offset.getX();
        int shadowOffsetY = (int) offset.getY();

//        Composite shadowComposite = new MultiplyComposite(shadowOpacityParam.getValueAsPercentage());
// TODO multiply makes sense only if the shadow color is not black
        Composite shadowComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shadowOpacityParam.getValueAsPercentage());

        for (int i = 0; i < imageNumberParam.getValue(); i++) {
            // Calculate the transform of the image
            // step 2: translate
            int tx;
            int ty;
            if (allowOutsideParam.getValue()) {
                tx = rand.nextInt(dest.getWidth() + xSize) - xSize;
                ty = rand.nextInt(dest.getHeight() + ySize) - ySize;
            } else {
                int maxTranslateX = dest.getWidth() - xSize;
                int maxTranslateY = dest.getHeight() - ySize;
                if (maxTranslateX <= 0) {
                    maxTranslateX = 1;
                }
                if (maxTranslateY <= 0) {
                    maxTranslateY = 1;
                }
                tx = rand.nextInt(maxTranslateX);
                ty = rand.nextInt(maxTranslateY);
            }
            AffineTransform randomTransform = AffineTransform.getTranslateInstance(tx, ty);
            // step 1: rotate

            float maxRandomRot = randomRotationParam.getValueAsPercentage();
            double theta;
            if (maxRandomRot == 0.0f) {
                // no not rotate
                theta = 0;
            } else {
                // the rotation amount is a number between -PI and PI if maxRandomRot is 1.0;
                theta = Math.PI * 2 * rand.nextFloat() - Math.PI;
                // normalize
                theta *= maxRandomRot;
            }
            randomTransform.rotate(theta, xSize / 2.0, ySize / 2.0);

            // Calculate the transform of the shadow
            // step 3: final shadow offset
            AffineTransform shadowTransform = AffineTransform.getTranslateInstance(shadowOffsetX, shadowOffsetY);
            // step 2: rotate and random translate
            shadowTransform.concatenate(randomTransform);
            // step 1: take shadowBorder into account
            shadowTransform.translate(-softShadowRoom, -softShadowRoom);

            // Draw the shadow
            g.setComposite(shadowComposite);
            g.drawImage(shadowImage, shadowTransform, null);

            // Draw the margin and the image
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            Shape transformedRect = randomTransform.createTransformedShape(fullImageRect);
            Shape transformedImageRect;
            if (margin > 0) {
                transformedImageRect = randomTransform.createTransformedShape(imageRect);
                g.setColor(Color.WHITE);
                g.fill(transformedRect);
            } else {
                transformedImageRect = transformedRect;
            }

            g.setPaint(imagePaint);
            g.fill(transformedImageRect);
        }

        g.dispose();
        return dest;
    }
}