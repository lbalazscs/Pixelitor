/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.gui.ImageComponent;
import pixelitor.tools.shapes.ShapeType;
import pixelitor.tools.util.PPoint;
import pixelitor.tools.util.PRectangle;

import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Shape;

import static pixelitor.utils.RandomUtils.nextGaussian;

public class SprayBrush extends AbstractBrush {
    private static final int DELAY_MILLIS = 50;
    private final SprayBrushSettings settings;
    private Timer timer;
    private double shapeRadius;
    private ShapeType shapeType;
    private int numSimultaneousPoints;
    private boolean randomOpacity;
    private double mouseX;
    private double mouseY;
    private double maxRadiusSoFar;
    private boolean isEraser;

    public SprayBrush(double radius, SprayBrushSettings settings) {
        super(radius);
        this.settings = settings;
    }

    @Override
    public void setTarget(Composition comp, Graphics2D g) {
        super.setTarget(comp, g);
        AlphaComposite ac = (AlphaComposite) g.getComposite();
        isEraser = ac.getRule() == AlphaComposite.DST_OUT;
    }

    @Override
    public double getActualRadius() {
        // The points have a Gaussian distribution, so the actual
        // radius is theoretically infinite, so we return the maximum observed value
        return shapeRadius + maxRadiusSoFar;
    }

    @Override
    public void startAt(PPoint p) {
        super.startAt(p);

        shapeRadius = settings.getShapeRadius();
        shapeType = settings.getShapeType();
        numSimultaneousPoints = settings.getFlow();
        randomOpacity = settings.randomOpacity();
        maxRadiusSoFar = Double.MIN_VALUE;

        timer = new Timer(DELAY_MILLIS, e -> sprayOnce());
        timer.start();

        mouseX = previous.getImX();
        mouseY = previous.getImY();

        sprayOnce();
    }

    private void sprayOnce() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        for (int i = 0; i < numSimultaneousPoints; i++) {
            double dx = nextGaussian() * radius;
            double x = mouseX + dx;
            double dy = nextGaussian() * radius;
            double y = mouseY + dy;
            updateTheMaxRadius(dx, dy);

            if (randomOpacity) {
                if (isEraser) {
                    targetG.setComposite(AlphaComposite.DstOut.derive((float) Math.random()));
                } else {
                    targetG.setComposite(AlphaComposite.SrcOver.derive((float) Math.random()));
                }
            }

            Shape shape = shapeType.getShape(
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
        ImageComponent ic = comp.getIC();
        PRectangle area = PRectangle.fromIm(
                minX - shapeRadius,
                minY - shapeRadius,
                maxX - minX + 2 * shapeRadius,
                maxY - minY + 2 * shapeRadius, ic);

        comp.updateRegion(area);
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
        rememberPrevious(p);

        mouseX = previous.getImX();
        mouseY = previous.getImY();
        sprayOnce();
    }

    @Override
    public void finish() {
        super.finish();

        timer.stop();
    }

    @Override
    public double getPreferredSpacing() {
        return 0;
    }
}
