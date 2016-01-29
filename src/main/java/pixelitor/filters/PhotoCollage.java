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

import com.jhlabs.image.BoxBlurFilter;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.BasicProgressTracker;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.Utils;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.USER_ONLY_OPACITY;

/**
 * Photo Collage
 */
public class PhotoCollage extends FilterWithParametrizedGUI {
    public static final String NAME = "Photo Collage";

    private final GroupedRangeParam sizeParam = new GroupedRangeParam("Photo Size", 40, 200, 999);

    private final RangeParam marginSizeParam = new RangeParam("Margin", 0, 5, 20);
    private final RangeParam imageNumberParam = new RangeParam("Number of Images", 1, 10, 100);
    private final RangeParam randomRotationParam = new RangeParam("Random Rotation Amount (%)", 0, 100, 100);
    private final BooleanParam allowOutsideParam = new BooleanParam("Allow Outside", true);
    private final ColorParam bgColorParam = new ColorParam("Background Color", BLACK, USER_ONLY_OPACITY);

    private final RangeParam shadowOpacityParam = new RangeParam("Shadow Opacity (%)", 0, 80, 100);
    private final AngleParam shadowAngleParam = new AngleParam("Shadow Angle", 0.7);
    private final RangeParam shadowDistanceParam = new RangeParam("Shadow Distance", 0, 5, 20);
    private final RangeParam shadowSoftnessParam = new RangeParam("Shadow Softness", 0, 3, 10);

    public PhotoCollage() {
        super(ShowOriginal.YES);
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
                shadowSoftnessParam.adjustRangeToImageSize(0.01)
        ).withAction(ReseedSupport.createAction()));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int numImages = imageNumberParam.getValue();
        ProgressTracker pt = new BasicProgressTracker(NAME, numImages);

        ReseedSupport.reInitialize();
        Random rand = ReseedSupport.getRand();

        int xSize = sizeParam.getValue(0);
        int ySize = sizeParam.getValue(1);

        // fill with the background color
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
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

        int shadowImgWidth = xSize + 2 * softShadowRoom;
        int shadowImgHeight = ySize + 2 * softShadowRoom;
        BufferedImage shadowImage = ImageUtils.createSysCompatibleImage(shadowImgWidth, shadowImgHeight);

        Graphics2D gShadow = shadowImage.createGraphics();
        gShadow.setColor(BLACK);
        gShadow.fillRect(softShadowRoom, softShadowRoom, xSize, ySize);
        gShadow.dispose();
        if (shadowSoftness > 0) {
            shadowImage = new BoxBlurFilter(shadowSoftness, shadowSoftness, 1, NAME)
                    .filter(shadowImage, shadowImage);
        }

        Point2D offset = Utils.calculateOffset(shadowDistanceParam.getValue(), shadowAngleParam.getValueInRadians());
        int shadowOffsetX = (int) offset.getX();
        int shadowOffsetY = (int) offset.getY();

        // multiply makes sense only if the shadow color is not black
        Composite shadowComposite = AlphaComposite.getInstance(SRC_OVER, shadowOpacityParam.getValueAsPercentage());

        for (int i = 0; i < numImages; i++) {
            // Calculate the transform of the image
            // step 2: translate
            int tx;
            int ty;
            if (allowOutsideParam.isChecked()) {
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
            // the rotation amount is a number between -PI and PI if maxRandomRot is 1.0;
            double theta = Math.PI * 2 * rand.nextFloat() - Math.PI;
            theta *= maxRandomRot;
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
            g.setComposite(AlphaComposite.getInstance(SRC_OVER));
            Shape transformedRect = randomTransform.createTransformedShape(fullImageRect);
            Shape transformedImageRect;
            if (margin > 0) {
                transformedImageRect = randomTransform.createTransformedShape(imageRect);
                g.setColor(WHITE);
                g.fill(transformedRect);
            } else {
                transformedImageRect = transformedRect;
            }

            g.setPaint(imagePaint);
            g.fill(transformedImageRect);

            pt.unitDone();
        }
        pt.finish();

        g.dispose();
        return dest;
    }
}