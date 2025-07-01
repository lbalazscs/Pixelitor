/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.GUIText;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.utils.Geometry;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Random;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;
import static pixelitor.filters.gui.TransparencyMode.OPAQUE_ONLY;
import static pixelitor.utils.AngleUnit.INTUITIVE_DEGREES;

/**
 * The "Photo Collage" filter.
 */
public class PhotoCollage extends ParametrizedFilter {
    public static final String NAME = "Photo Collage";

    @Serial
    private static final long serialVersionUID = 5651133864767266714L;

    private final GroupedRangeParam photoSize = new GroupedRangeParam("Photo Size", 40, 200, 999, false);
    private final EnumParam<ShapeType> shapeTypeParam = new EnumParam<>("Photo Shape", ShapeType.class);
    private final RangeParam marginSize = new RangeParam("Margin", 0, 5, 20);
    private final RangeParam numPhotosParam = new RangeParam("Number of Photos", 1, 10, 101);
    private final RangeParam randomRotation = new RangeParam("Random Rotation Amount (%)", 0, 100, 100);
    private final BooleanParam allowOutside = new BooleanParam("Allow Outside", true);
    private final ColorParam bgColor = new ColorParam(GUIText.BG_COLOR, BLACK, MANUAL_ALPHA_ONLY);
    private final ColorParam marginColor = new ColorParam("Margin Color", WHITE, OPAQUE_ONLY);
    private final RangeParam shadowOpacityParam = new RangeParam("Shadow Opacity (%)", 0, 80, 100);
    private final AngleParam shadowAngleParam = new AngleParam("Shadow Angle", 315, INTUITIVE_DEGREES);
    private final RangeParam shadowDistance = new RangeParam("Shadow Distance", 0, 5, 20);
    private final RangeParam shadowBlur = new RangeParam("Shadow Softness", 0, 3, 10);

    public PhotoCollage() {
        super(true);

        bgColor.setPresetKey("Background Color");
        numPhotosParam.setPresetKey("Number of Images"); // former name

        CompositeParam shadowSettings = new CompositeParam("Shadow Settings",
            shadowOpacityParam,
            shadowAngleParam,
            shadowDistance.withAdjustedRange(0.02),
            shadowBlur.withAdjustedRange(0.01));

        initParams(
            numPhotosParam,
            allowOutside,
            shapeTypeParam,
            photoSize.withAdjustedRange(1.0),
            randomRotation,
            marginSize.withAdjustedRange(0.02),
            bgColor,
            marginColor,
            shadowSettings
        ).withReseedAction();
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int numPhotos = numPhotosParam.getValue();
        var pt = new StatusBarProgressTracker(NAME, numPhotos);

        Random rand = paramSet.getLastSeedRandom();

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);

        // fill with the background color
        Colors.fillWith(bgColor.getColor(), g, dest.getWidth(), dest.getHeight());

        ShapeType shapeType = shapeTypeParam.getSelected();
        int photoWidth = photoSize.getValue(0);
        int photoHeight = photoSize.getValue(1);
        Shape outerShape = shapeType.createShape(0, 0, photoWidth, photoHeight);
        int margin = marginSize.getValue();

        // adjust the inner photo shape to account for margins
        Shape innerShape = outerShape;
        if (margin > 0) {
            Stroke marginStroke = new BasicStroke(2 * margin, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
            Shape marginShape = marginStroke.createStrokedShape(outerShape);
            Area innerArea = new Area(outerShape);
            innerArea.subtract(new Area(marginShape));
            innerShape = innerArea;
        }

        // prepare a shadow image that must be larger than the
        // photo size so that there is room for soft shadows
        int blurRadius = shadowBlur.getValue();
        int shadowPadding = 1 + (int) (2.3 * blurRadius);
        BufferedImage shadowImage = createShadowImage(photoWidth, photoHeight, blurRadius, shadowPadding, shapeType);

        Point2D shadowOffset = Geometry.polarToCartesian(
            shadowDistance.getValue(),
            shadowAngleParam.getValueInRadians());

        Composite shadowComposite = AlphaComposite.getInstance(SRC_OVER,
            (float) shadowOpacityParam.getPercentage());

        // use the original image as a texture for the photos
        Paint imagePaint = new TexturePaint(src, new Rectangle2D.Float(
            0, 0, src.getWidth(), src.getHeight()));

        for (int i = 0; i < numPhotos; i++) {
            var imageTransform = calcImageTransform(dest, photoWidth, photoHeight, rand);
            var shadowTransform = calcShadowTransform(imageTransform, shadowPadding, shadowOffset);

            // draw the shadow behind the photo
            g.setComposite(shadowComposite);
            g.drawImage(shadowImage, shadowTransform, null);

            // draw the photo and its margin
            g.setComposite(AlphaComposite.getInstance(SRC_OVER));
            Shape transformedShape = imageTransform.createTransformedShape(outerShape);
            Shape transformedImageShape;
            if (margin > 0) {
                transformedImageShape = imageTransform.createTransformedShape(innerShape);
                g.setColor(marginColor.getColor());
                g.fill(transformedShape);
            } else {
                transformedImageShape = transformedShape;
            }

            g.setPaint(imagePaint); // set the original image as a texture
            g.fill(transformedImageShape); // draw the photo

            pt.unitDone();
        }
        pt.finished();
        g.dispose();

        return dest;
    }

    private static BufferedImage createShadowImage(int photoWidth, int photoHeight,
                                                   int blurRadius, int shadowPadding,
                                                   ShapeType shapeType) {
        int shadowImgWidth = photoWidth + 2 * shadowPadding;
        int shadowImgHeight = photoHeight + 2 * shadowPadding;
        BufferedImage shadowImage = ImageUtils.createSysCompatibleImage(
            shadowImgWidth, shadowImgHeight);

        Graphics2D gShadow = shadowImage.createGraphics();
        gShadow.setColor(BLACK);
        Shape shadowShape = shapeType.createShape(shadowPadding, shadowPadding, photoWidth, photoHeight);
        gShadow.fill(shadowShape);
        gShadow.dispose();
        if (blurRadius > 0) {
            shadowImage = new BoxBlurFilter(blurRadius, blurRadius, 1, NAME)
                .filter(shadowImage, shadowImage);
        }
        return shadowImage;
    }

    // calculate the transform of the image
    private AffineTransform calcImageTransform(BufferedImage dest, int photoWidth, int photoHeight, Random rand) {
        // step 2: translate
        double tx, ty;
        if (allowOutside.isChecked()) {
            tx = rand.nextInt(dest.getWidth()) - photoWidth / 2.0;
            ty = rand.nextInt(dest.getHeight()) - photoHeight / 2.0;
        } else {
            // a small part could be still outside because of the
            // rotation, which is ignored here, but it's not a big deal.
            int maxTranslateX = dest.getWidth() - photoWidth;
            int maxTranslateY = dest.getHeight() - photoHeight;
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
        // the rotation amount is a number between -PI and PI if maxRandomRot is 1.0
        double angle = Math.PI * 2 * rand.nextFloat() - Math.PI;
        angle *= randomRotation.getPercentage();
        imageTransform.rotate(angle, photoWidth / 2.0, photoHeight / 2.0);

        return imageTransform;
    }

    // calculate the transform of the shadow
    private static AffineTransform calcShadowTransform(AffineTransform imageTransform, int shadowPadding, Point2D shadowOffset) {
        // step 3: final shadow offset
        var shadowTransform = AffineTransform.getTranslateInstance(
            shadowOffset.getX(), shadowOffset.getY());
        // step 2: rotate and random translate
        shadowTransform.concatenate(imageTransform);
        // step 1: take the shadow padding into account
        shadowTransform.translate(-shadowPadding, -shadowPadding);

        return shadowTransform;
    }
}