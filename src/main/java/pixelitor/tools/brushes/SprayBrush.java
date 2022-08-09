/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.View;
import pixelitor.layers.Drawable;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.CachedFloatRandom;
import pixelitor.utils.Rnd;

import javax.swing.*;
import java.awt.*;

import static pixelitor.utils.Rnd.nextGaussian;

public class SprayBrush extends AbstractBrush {
    private static final int DELAY_MILLIS = 50;

    private final SprayBrushSettings settings;
    private double minShapeRadius;
    private double maxShapeRadius;
    private double mouseX;
    private double mouseY;
    private double maxRadiusSoFar;
    private boolean isEraser;
    private Color baseColor;
    private Timer timer;
    private final CachedFloatRandom rnd = new CachedFloatRandom();

    public SprayBrush(double radius, SprayBrushSettings settings) {
        super(radius);
        this.settings = settings;
    }

    @Override
    public void setTarget(Drawable dr, Graphics2D g) {
        super.setTarget(dr, g);
        AlphaComposite ac = (AlphaComposite) g.getComposite();
        isEraser = ac.getRule() == AlphaComposite.DST_OUT;
        baseColor = g.getColor();
    }

    @Override
    public double getMaxEffectiveRadius() {
        // The points have a Gaussian distribution, the actual radius
        // is theoretically infinite, so return the maximum observed value
        return maxShapeRadius + maxRadiusSoFar;
    }

    @Override
    public void initDrawing(PPoint p) {
        super.initDrawing(p);

        double shapeRadius = settings.getShapeRadius();
        double radiusVariability = settings.getRadiusVariability();
        minShapeRadius = shapeRadius - radiusVariability * shapeRadius;
        maxShapeRadius = shapeRadius + radiusVariability * shapeRadius;

        maxRadiusSoFar = Double.MIN_VALUE;

        timer = new Timer(DELAY_MILLIS, e -> sprayOnce());
        timer.start();

        mouseX = previous.getImX();
        mouseY = previous.getImY();

        sprayOnce();
    }

    private double nextShapeRadius() {
        return minShapeRadius + rnd.nextFloat() * (maxShapeRadius - minShapeRadius);
    }

    private void sprayOnce() {
        View view = dr.getComp().getView();
        if (view == null) {
            // can happen if the composition was reloaded while spraying
            return;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        ShapeType shapeType = settings.getShapeType();
        boolean useRandomOpacity = settings.randomOpacity();
        float colorRandomness = (float) settings.getColorRandomness();
        int numSimultaneousShapes = settings.getFlow();

        for (int i = 0; i < numSimultaneousShapes; i++) {
            double dx = nextGaussian() * radius;
            double x = mouseX + dx;
            double dy = nextGaussian() * radius;
            double y = mouseY + dy;
            updateTheMaxRadius(dx, dy);

            if (useRandomOpacity) {
                setOpacityRandomly();
            }

            if (!isEraser && colorRandomness > 0.0f) {
                Color randomColor = Rnd.createRandomColor();
                Color color = Colors.rgbInterpolate(baseColor, randomColor, colorRandomness);
                targetG.setColor(color);
            }

            double shapeRadius = nextShapeRadius();
            Shape shape = shapeType.createShape(
                x - shapeRadius, y - shapeRadius, 2 * shapeRadius);
            targetG.fill(shape);

            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
        }
        PRectangle area = PRectangle.fromIm(
            minX - maxShapeRadius,
            minY - maxShapeRadius,
            maxX - minX + 2 * maxShapeRadius + 2,
            maxY - minY + 2 * maxShapeRadius + 2, view);

        dr.repaintRegion(area);
    }

    private void setOpacityRandomly() {
        Composite composite;
        float strength = rnd.nextFloat();
        if (isEraser) {
            composite = AlphaComposite.DstOut.derive(strength);
        } else {
            composite = AlphaComposite.SrcOver.derive(strength);
        }
        targetG.setComposite(composite);
    }

    private void updateTheMaxRadius(double dx, double dy) {
        if (dx > 0) {
            if (dx > maxRadiusSoFar) {
                maxRadiusSoFar = dx;
            }
        } else {
            if (-dx > maxRadiusSoFar) {
                maxRadiusSoFar = -dx;
            }
        }
        if (dy > 0) {
            if (dy > maxRadiusSoFar) {
                maxRadiusSoFar = dy;
            }
        } else {
            if (-dy > maxRadiusSoFar) {
                maxRadiusSoFar = -dy;
            }
        }
    }

    @Override
    public void continueTo(PPoint p) {
        // this method does no painting, but the
        // brush outline still has to be repainted
        repaintComp(p);

        rememberPrevious(p);

        mouseX = previous.getImX();
        mouseY = previous.getImY();

        // calling sprayOnce() here would make the flow dependent
        // on the mouse speed and low flow values impossible
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
    public double getPreferredSpacing() {
        return 0;
    }
}
