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

import net.jafama.FastMath;
import org.jdesktop.swingx.painter.effects.GlowPathEffect;
import org.jdesktop.swingx.painter.effects.NeonBorderEffect;
import org.jdesktop.swingx.painter.effects.ShadowPathEffect;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.painters.AreaEffects;
import pixelitor.io.IO;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;
import pixelitor.utils.Shapes.NonlinTransform;
import pixelitor.utils.Shapes.PointMapper;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static pixelitor.colors.FgBgColors.getBGColor;
import static pixelitor.colors.FgBgColors.getFGColor;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;
import static pixelitor.utils.Shapes.NonlinTransform.NONE;

/**
 * Abstract superclass for the "Render/Curves" filters.
 * Not to be confused with the "Curves" color adjustment.
 */
public abstract class CurveFilter extends ParametrizedFilter {
    private static final boolean DEBUG_SHAPE_POINTS = false;

    @Serial
    private static final long serialVersionUID = 5917893980467661843L;

    private static final int BG_BLACK = 1;
    private static final int BG_ORIGINAL = 2;
    private static final int BG_TRANSPARENT = 3;
    private static final int BG_TOOL = 4;

    private static final int FG_WHITE = 5;
    private static final int FG_BLACK = 6;
    private static final int FG_TOOL = 7;
    private static final int FG_GRADIENT = 8;
    private static final int FG_TRANSPARENT = 9;

    protected final StrokeParam strokeParam = new StrokeParam("Stroke Settings");
    private final EffectsParam effectsParam = new EffectsParam("Effects");

    private final IntChoiceParam background = new IntChoiceParam("Background", new Item[]{
        new Item("Black", BG_BLACK),
        new Item("Original Image", BG_ORIGINAL),
        new Item("Transparent", BG_TRANSPARENT),
//        new Item(GUIText.BG_COLOR, BG_TOOL),
        new Item("Background Color", BG_TOOL),
    }, IGNORE_RANDOMIZE);

    private final IntChoiceParam foreground = new IntChoiceParam("Foreground", new Item[]{
        new Item("White", FG_WHITE),
        new Item("Black", FG_BLACK),
        new Item("Radial Gradient", FG_GRADIENT),
//        new Item(GUIText.FG_COLOR, FG_TOOL),
        new Item("Foreground Color", FG_TOOL),
        new Item("Transparent", FG_TRANSPARENT),
    }, IGNORE_RANDOMIZE);

    private final BooleanParam waterMark = new BooleanParam("Watermarking", false);
    protected final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam scale = new GroupedRangeParam("Scale (%)", 1, 100, 500);
    private final AngleParam rotate = new AngleParam("Rotate", 0);
    private final EnumParam<NonlinTransform> nonlinType = NonlinTransform.asParam();
    private final RangeParam nonlinTuning = new RangeParam("Nonlinear Tuning", -100, 0, 100);

    private transient Shape exportedShape;

    protected CurveFilter() {
        super(false);

        nonlinType.setupEnableOtherIf(nonlinTuning, NonlinTransform::hasTuning);

        setParams(
            background,
            foreground,
            waterMark,
            new DialogParam("Transform", nonlinType, nonlinTuning, center, rotate, scale),
            strokeParam.withStrokeWidth(2),
            effectsParam
        ).withAction(new FilterButtonModel("Export SVG...", this::exportSVG,
            null, "Export the current shape to an SVG file",
            null, false));

        // disable foreground and background if watermarking is selected
        waterMark.setupDisableOtherIfChecked(foreground);
        waterMark.setupDisableOtherIfChecked(background);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        Graphics2D g2;
        BufferedImage bumpImage = null;

        boolean watermarking = waterMark.isChecked();
        if (watermarking) {
            bumpImage = new BufferedImage(srcWidth, srcHeight, TYPE_INT_RGB);
            g2 = bumpImage.createGraphics();
            Colors.fillWith(BLACK, g2, srcWidth, srcHeight);
            g2.setColor(Color.GRAY);
        } else {
            dest = ImageUtils.createImageWithSameCM(src);
            g2 = dest.createGraphics();
            fillBackground(src, g2, srcWidth, srcHeight);
            setupForeground(srcWidth, srcHeight, g2);
        }

        Stroke stroke = strokeParam.createStroke();
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Shape shape = createCurve(srcWidth, srcHeight);
        if (shape == null) {
            // can happen, for example with Spirograph at time=0
            g2.dispose();
            return dest;
        }

        NonlinTransform transform = nonlinType.getSelected();
        if (transform != NONE) {
            double amount = nonlinTuning.getValueAsDouble();
            Point2D mapCenter = center.getAbsolutePoint(src);
            PointMapper mapper = transform.createMapper(mapCenter, amount, srcWidth, srcHeight);
            shape = Shapes.transformShape(shape, mapper);
        }

        double scaleX = scale.getPercentage(0);
        double scaleY = scale.getPercentage(1);
        boolean hasScaling = scaleX != 1.0 || scaleY != 1.0;

        double relX = center.getRelativeX();
        double relY = center.getRelativeY();
        boolean hasTranslation = relX != 0.5 || relY != 0.5;

        boolean hasRotation = !rotate.hasDefault();

        if (hasTranslation || hasRotation || hasScaling) {
            double cx = srcWidth * relX;
            double cy = srcHeight * relY;

            AffineTransform at;
            if (hasScaling) {
                // scale around the center point
                at = AffineTransform.getTranslateInstance
                    (cx - scaleX * cx, cy - scaleY * cy);
                at.scale(scaleX, scaleY);
            } else {
                at = new AffineTransform();
            }
            if (hasRotation) {
                at.rotate(rotate.getValueInRadians(), cx, cy);
            }
            if (hasTranslation) {
                at.translate(cx - srcWidth / 2.0, cy - srcHeight / 2.0);
            }

            shape = at.createTransformedShape(shape);
        }

        exportedShape = shape;

        AreaEffects effects = effectsParam.getEffects();
        if (effects.getInnerGlow() != null) {
            // work with the outline so that we can have "inner glow"
            shape = stroke.createStrokedShape(shape);
            g2.fill(shape);
        } else {
            g2.setStroke(stroke);
            g2.draw(shape);
        }

        // If there are effects and the foreground is set to transparent,
        // then the effects have to be run on a temporary image, because
        // they also set the composite. Also, inner glow is ignored.
        if (effects.hasEnabledEffects()) {
            if (!watermarking && foreground.getValue() == FG_TRANSPARENT) {
                drawEffectsWithTransparency(effects, g2, shape, srcWidth, srcHeight);
            } else { // the simple case
                effects.apply(g2, shape);
            }
        }

        if (DEBUG_SHAPE_POINTS) {
            List<Point2D> points = Shapes.getAnchorPoints(shape);
            g2.setColor(Color.RED);
            for (Point2D point : points) {
                g2.fill(Shapes.createCircle(point.getX(), point.getY(), 4));
            }
        }

        g2.dispose();

        if (watermarking) {
            dest = ImageUtils.bumpMap(src, bumpImage, getName());
            bumpImage.flush();
        }

        return dest;
    }

    private static void drawEffectsWithTransparency(AreaEffects effects, Graphics2D g2, Shape shape, int srcWidth, int srcHeight) {
        GlowPathEffect glow = effects.getGlow();
        NeonBorderEffect neonBorder = effects.getNeonBorder();
        ShadowPathEffect dropShadow = effects.getDropShadow();
        if (glow != null || neonBorder != null || dropShadow != null) {
            var maskImage = ImageUtils.createSysCompatibleImage(srcWidth, srcHeight);
            Graphics2D tmpG = maskImage.createGraphics();
            // fill with transparent pixels
            tmpG.setComposite(AlphaComposite.Clear);
            tmpG.fillRect(0, 0, srcWidth, srcHeight);

            // reset the composite to normal.
            tmpG.setComposite(AlphaComposite.SrcOver);

            // check the effects individually in order to skip inner glow.
            if (glow != null) {
                glow.apply(tmpG, shape, 0, 0);
            }
            if (neonBorder != null) {
                neonBorder.apply(tmpG, shape, 0, 0);
            }
            if (dropShadow != null) {
                dropShadow.apply(tmpG, shape, 0, 0);
            }
            tmpG.dispose();

            // the effect colors won't matter, only their opacity
            g2.setComposite(AlphaComposite.DstOut);
            g2.drawImage(maskImage, 0, 0, null);
            maskImage.flush();
        }
    }

    private void fillBackground(BufferedImage src, Graphics2D g2, int srcWidth, int srcHeight) {
        int bg = background.getValue();
        switch (bg) {
            case BG_BLACK -> Colors.fillWith(BLACK, g2, srcWidth, srcHeight);
            case BG_TOOL -> Colors.fillWith(getBGColor(), g2, srcWidth, srcHeight);
            case BG_ORIGINAL -> g2.drawImage(src, 0, 0, null);
            case BG_TRANSPARENT -> {
            }
            default -> throw new IllegalStateException("Unexpected value: " + bg);
        }
    }

    private void setupForeground(int srcWidth, int srcHeight, Graphics2D g2) {
        int fg = foreground.getValue();
        switch (fg) {
            case FG_WHITE -> g2.setColor(WHITE);
            case FG_BLACK -> g2.setColor(BLACK);
            case FG_TOOL -> g2.setColor(getFGColor());
            case FG_GRADIENT -> setupGradientForeground(g2, srcWidth, srcHeight);
            case FG_TRANSPARENT -> g2.setComposite(AlphaComposite.Clear);
            default -> throw new IllegalStateException("Unexpected value: " + fg);
        }
    }

    private void setupGradientForeground(Graphics2D g2, int srcWidth, int srcHeight) {
        float cx = srcWidth / 2.0f;
        float cy = srcHeight / 2.0f;
        float radius = getGradientRadius(cx, cy);
        float[] fractions = {0.0f, 1.0f};
        Color[] colors = {getFGColor(), getBGColor()};
        g2.setPaint(new RadialGradientPaint(cx, cy, radius, fractions, colors));
    }

    protected float getGradientRadius(float cx, float cy) {
        return (float) Math.sqrt(cx * cx + cy * cy);
    }

    protected abstract Shape createCurve(int width, int height);

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }

    @Override
    public boolean supportsGray() {
        return !waterMark.isChecked();
    }

    private void exportSVG() {
        IO.saveSVG(exportedShape, strokeParam, getName() + ".svg");
    }

    /**
     * Manages and filters 2D points to create a smooth path.
     */
    protected static class PathConnector {
        private final List<Point2D> points;

        private final double angleThreshold;
        private final double distThreshold;

        double lastX = 0;
        double lastY = 0;
        double lastAngle = 0;

        public PathConnector(int capacity, double distThreshold, double angleThreshold) {
            this.points = new ArrayList<>(capacity);
            this.angleThreshold = angleThreshold;
            this.distThreshold = distThreshold;
        }

        public void add(double x, double y) {
            double angle = FastMath.atan2(y - lastY, x - lastX);
            if (Math.abs(lastAngle - angle) > angleThreshold || Math.abs(x - lastX) > distThreshold || Math.abs(y - lastY) > distThreshold) {
                // this point adds some significant information to the curve
                points.add(new Point2D.Double(x, y));
                lastX = x;
                lastY = y;
                lastAngle = angle;
            }
        }

        public Path2D getPath() {
            if (points.size() < 3) {
                return null;
            }
            return Shapes.smoothConnect(points);
        }
    }

    protected boolean hasNonlinDistort() {
        return nonlinType.getSelected() != NONE;
    }
}