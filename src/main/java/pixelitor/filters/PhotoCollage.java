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

import com.jhlabs.image.BoxBlurFilter;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ReseedSupport;
import pixelitor.utils.StatusBarProgressTracker;
import pixelitor.utils.Utils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.*;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.NO_TRANSPARENCY;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.USER_ONLY_TRANSPARENCY;

/**
 * Photo Collage
 */
public class PhotoCollage extends ParametrizedFilter {
    public static final String NAME = "Photo Collage";

    private final GroupedRangeParam size = new GroupedRangeParam("Photo Size", 40, 200, 999);
    private final RangeParam marginSize = new RangeParam("Margin", 0, 5, 20);
    private final RangeParam numImagesParam = new RangeParam("Number of Images", 1, 10, 101);
    private final RangeParam randomRotation = new RangeParam("Random Rotation Amount (%)", 0, 100, 100);
    private final BooleanParam allowOutside = new BooleanParam("Allow Outside", true);
    private final ColorParam bgColor = new ColorParam("Background Color", BLACK, USER_ONLY_TRANSPARENCY);
    private final ColorParam marginColor = new ColorParam("Margin Color", WHITE, NO_TRANSPARENCY);
    private final RangeParam shadowOpacityParam = new RangeParam("Shadow Opacity (%)", 0, 80, 100);
    private final AngleParam shadowAngleParam = new AngleParam("Shadow Angle", 0.7);
    private final RangeParam shadowDistance = new RangeParam("Shadow Distance", 0, 5, 20);
    private final RangeParam shadowSoftnessParam = new RangeParam("Shadow Softness", 0, 3, 10);

    public PhotoCollage() {
        super(ShowOriginal.YES);

        DialogParam shadowDialog = new DialogParam("Shadow Settings",
                shadowOpacityParam,
                shadowAngleParam,
                shadowDistance.withAdjustedRange(0.02),
                shadowSoftnessParam.withAdjustedRange(0.01));
        
        setParams(
                numImagesParam,
                allowOutside,
                size.withAdjustedRange(1.0),
                randomRotation,
                marginSize.withAdjustedRange(0.02),
                bgColor,
                marginColor,
                shadowDialog
        ).withAction(ReseedSupport.createAction());
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int numImages = numImagesParam.getValue();
        var pt = new StatusBarProgressTracker(NAME, numImages);

        Random rand = ReseedSupport.reInitialize();

        int xSize = size.getValue(0);
        int ySize = size.getValue(1);

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);

        // fill with the background color
        Colors.fillWith(bgColor.getColor(), g, dest.getWidth(), dest.getHeight());

        var photoRect = new Rectangle(0, 0, xSize, ySize);
        int margin = marginSize.getValue();
        var photoWOMarginRect = new Rectangle(photoRect);
        photoWOMarginRect.grow(-margin, -margin);

        // the shadow image must be larger than the image size
        // so that there is room for soft shadows
        int shadowSoftness = shadowSoftnessParam.getValue();
        int softShadowRoom = 1 + (int) (2.3 * shadowSoftness);

        BufferedImage shadowImage = createShadowImage(xSize, ySize, shadowSoftness, softShadowRoom);

        Point2D offset = Utils.offsetFromPolar(
                shadowDistance.getValue(),
                shadowAngleParam.getValueInRadians());
        double shadowOffsetX = offset.getX();
        double shadowOffsetY = offset.getY();

        // multiply makes sense only if the shadow color is not black
        Composite shadowComposite = AlphaComposite.getInstance(SRC_OVER,
                shadowOpacityParam.getPercentageValF());

        Paint imagePaint = new TexturePaint(src, new Rectangle2D.Float(
                0, 0, src.getWidth(), src.getHeight()));

        for (int i = 0; i < numImages; i++) {
            // Calculate the transform of the image
            // step 2: translate
            double tx;
            double ty;
            if (allowOutside.isChecked()) {
                tx = rand.nextInt(dest.getWidth()) - xSize / 2.0;
                ty = rand.nextInt(dest.getHeight()) - ySize / 2.0;
            } else {
                // a small part could be still outside because of the rotation,
                // which is ignored here, but it's not a big deal
                int maxTranslateX = dest.getWidth() - xSize;
                int maxTranslateY = dest.getHeight() - ySize;
                if (maxTranslateX <= 0) {
                    maxTranslateX = 1;
                }
                if (maxTranslateY <= 0) {
                    maxTranslateY = 1;
                }
                tx = maxTranslateX * rand.nextDouble();
                ty = maxTranslateY * rand.nextDouble();
            }
            var imageTransform = AffineTransform.getTranslateInstance(tx, ty);

            // step 1: rotate
            // the rotation amount is a number between -PI and PI if maxRandomRot is 1.0;
            double theta = Math.PI * 2 * rand.nextFloat() - Math.PI;
            theta *= randomRotation.getPercentageValF();
            imageTransform.rotate(theta, xSize / 2.0, ySize / 2.0);

            // Calculate the transform of the shadow
            // step 3: final shadow offset
            var shadowTransform = AffineTransform.getTranslateInstance(shadowOffsetX, shadowOffsetY);
            // step 2: rotate and random translate
            shadowTransform.concatenate(imageTransform);
            // step 1: take shadowBorder into account
            shadowTransform.translate(-softShadowRoom, -softShadowRoom);

            // Draw the shadow
            g.setComposite(shadowComposite);
            g.drawImage(shadowImage, shadowTransform, null);

            // Draw the margin and the image
            g.setComposite(AlphaComposite.getInstance(SRC_OVER));
            Shape transformedRect = imageTransform.createTransformedShape(photoRect);
            Shape transformedImageRect;
            if (margin > 0) {
                transformedImageRect = imageTransform.createTransformedShape(photoWOMarginRect);
                g.setColor(marginColor.getColor());
                g.fill(transformedRect);
            } else {
                transformedImageRect = transformedRect;
            }

            g.setPaint(imagePaint);
            g.fill(transformedImageRect);

            pt.unitDone();
        }
        pt.finished();

        g.dispose();
        return dest;
    }

    private static BufferedImage createShadowImage(int xSize, int ySize, int shadowSoftness, int softShadowRoom) {
        int shadowImgWidth = xSize + 2 * softShadowRoom;
        int shadowImgHeight = ySize + 2 * softShadowRoom;
        BufferedImage shadowImage = ImageUtils.createSysCompatibleImage(
                shadowImgWidth, shadowImgHeight);

        Graphics2D gShadow = shadowImage.createGraphics();
        gShadow.setColor(BLACK);
        gShadow.fillRect(softShadowRoom, softShadowRoom, xSize, ySize);
        gShadow.dispose();
        if (shadowSoftness > 0) {
            shadowImage = new BoxBlurFilter(shadowSoftness, shadowSoftness, 1, NAME)
                    .filter(shadowImage, shadowImage);
        }
        return shadowImage;
    }
}