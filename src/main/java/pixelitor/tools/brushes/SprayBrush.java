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

package pixelitor.tools.brushes;

import pixelitor.colors.Colors;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.View;
import pixelitor.layers.Drawable;
import pixelitor.tools.Tools;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.BoundingBox;
import pixelitor.utils.CachedFloatRandom;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;

import static pixelitor.utils.Rnd.nextGaussian;

/**
 * A brush that simulates a spray paint effect.
 */
public class SprayBrush extends AbstractBrush {
    private static final int SPRAY_INTERVAL_MILLIS = 50;

    private final SprayBrushSettings settings;
    private double minShapeRadius;
    private double maxShapeRadius;

    // the center of the spray (usually mouse coordinates in image-space)
    private double sprayX;
    private double sprayY;

    // the maximum observed distance between the mouse position
    // and any shape center, over the lifetime of a brush stroke
    private double maxObservedDist;

    private boolean isErasing;
    private Color baseColor;
    private Timer timer;
    private final CachedFloatRandom rnd = new CachedFloatRandom();

    public SprayBrush(SprayBrushSettings settings, double radius) {
        super(radius);
        this.settings = settings;
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        super.setTarget(dr, g);

        // the Graphics2D is not completely configured yet, so we can't check
        // its composite to see if this is called from the eraser tool
        isErasing = Tools.isActive(Tools.ERASER);
        baseColor = FgBgColors.getFgColor();
    }

    @Override
    public void initDrawing(PPoint p) {
        super.initDrawing(p);

        double shapeRadius = settings.getShapeRadius();
        double radiusVariability = settings.getRadiusVariability();
        minShapeRadius = shapeRadius - radiusVariability * shapeRadius;
        maxShapeRadius = shapeRadius + radiusVariability * shapeRadius;

        maxObservedDist = 0;

        timer = new Timer(SPRAY_INTERVAL_MILLIS, _ -> spray());
        timer.start();

        sprayX = prevPos.getImX();
        sprayY = prevPos.getImY();

        spray(); // initial spray
    }

    /**
     * Generates a random shape radius within the allowed range.
     */
    private double genShapeRadius() {
        return minShapeRadius + rnd.nextFloat() * (maxShapeRadius - minShapeRadius);
    }

    /**
     * Sprays a burst of shapes.
     */
    private void spray() {
        View view = dr.getComp().getView();
        if (view == null) {
            // can happen if the composition was reloaded while spraying
            return;
        }

        BoundingBox boundingBox = new BoundingBox();

        ShapeType shapeType = settings.getShapeType();
        boolean useRandomOpacity = settings.randomOpacity();
        float colorRandomness = (float) settings.getColorRandomness();
        int shapesPerSpray = settings.getFlow();

        for (int i = 0; i < shapesPerSpray; i++) {
            double offsetX = nextGaussian() * radius;
            double offsetY = nextGaussian() * radius;

            // TODO is it a bug to calculate the largest per-axis
            //  offset instead of the actual distance?
            maxObservedDist = Math.max(
                maxObservedDist,
                Math.max(Math.abs(offsetX), Math.abs(offsetY)));

            if (useRandomOpacity) {
                setRandomOpacity();
            }

            if (!isErasing && colorRandomness > 0.0f) {
                Color randomColor = Rnd.createRandomColor();
                Color color = Colors.interpolateRgb(baseColor, randomColor, colorRandomness);
                targetG.setColor(color);
            }

            double x = sprayX + offsetX;
            double y = sprayY + offsetY;
            double shapeRadius = genShapeRadius();
            Shape shape = shapeType.createShape(
                x - shapeRadius, y - shapeRadius, 2 * shapeRadius);
            targetG.fill(shape);

            boundingBox.add(x, y);
        }

        Rectangle2D imArea = boundingBox.toRectangle2D(maxShapeRadius + 1);
        dr.repaintRegion(PRectangle.fromIm(imArea, view));
    }

    private void setRandomOpacity() {
        float opacity = rnd.nextFloat();
        Composite composite = isErasing
            ? AlphaComposite.DstOut.derive(opacity)
            : AlphaComposite.SrcOver.derive(opacity);
        targetG.setComposite(composite);
    }

    @Override
    public void continueTo(PPoint p) {
        // this method does no painting, but the
        // brush outline still has to be repainted
        repaintSegment(p);

        setPrevPos(p);

        sprayX = prevPos.getImX();
        sprayY = prevPos.getImY();

        // calling spray() here would make the flow depend
        // on the mouse speed and make low flow values impossible
    }

    @Override
    public void finishBrushStroke() {
        super.finishBrushStroke();
        assert timer != null;

        stopTimer();
    }

    @Override
    public void dispose() {
        assert timer == null;
        // should be stopped already, but to be sure
        stopTimer();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    @Override
    public double getMaxEffectiveRadius() {
        // the points have a Gaussian distribution, making the actual radius
        // theoretically infinite, so return the maximum observed value
        return maxObservedDist + maxShapeRadius;
    }

    @Override
    public double getPreferredSpacing() {
        return 0;
    }
}
