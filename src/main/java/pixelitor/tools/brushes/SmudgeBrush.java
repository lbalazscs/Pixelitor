/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static pixelitor.colors.FgBgColors.getFGColor;

/**
 * The brush used by the Smudge Tool
 */
public class SmudgeBrush extends CopyBrush {
    /**
     * The smudge brush samples the source image at
     * the last mouse coordinates and puts the pixels to
     * the current coordinates.
     */
    private PPoint last;

    /**
     * The opacity of the brush (strength in the GUI).
     */
    private float strength;

    private boolean firstUsageInStroke = true;

    /**
     * If true, we start with the foreground color
     */
    private boolean fingerPainting = false;

    public SmudgeBrush(double radius, CopyBrushType type) {
        super(radius, type, new FixedDistanceSpacing(1.0));
    }

    public void setupFirstPoint(BufferedImage sourceImage, PPoint src, float strength) {
        this.sourceImage = sourceImage;
        last = src;
        this.strength = strength;
        firstUsageInStroke = true;
    }

    public boolean firstPointWasInitialized() {
        return last != null;
    }

    @Override
    void setupBrushStamp(PPoint p) {
        Graphics2D g = brushImage.createGraphics();
        type.beforeDrawImage(g);

        if (firstUsageInStroke && fingerPainting) {
            // finger painting starts with the foreground color
            g.setColor(getFGColor());
            int size = (int) diameter;
            g.fillRect(0, 0, size, size);
        } else {
            // samples the source image at the last point into the brush image
            g.drawImage(sourceImage,
                AffineTransform.getTranslateInstance(
                    -last.getImX() + radius,
                    -last.getImY() + radius), null);
        }

        type.afterDrawImage(g);
        g.dispose();

        firstUsageInStroke = false;
        debugImage();
    }

    @Override
    public void putDab(PPoint p, double theta) {
        var transform = AffineTransform.getTranslateInstance(
            p.getImX() - radius,
            p.getImY() - radius
        );

        // SrcOver allows to smudge into transparent areas, but transparency
        // can't be smudged into non-transparent areas.
        // DstOver allows only smudging into transparent.
        targetG.setComposite(AlphaComposite.SrcOver.derive(strength));

        targetG.drawImage(brushImage, transform, null);
        last = p;
        repaintComp(p);
    }

    public void setFingerPainting(boolean fingerPainting) {
        this.fingerPainting = fingerPainting;
    }

    @Override
    public DebugNode getDebugNode() {
        var node = super.getDebugNode();

        if (last != null) {
            node.addDouble("last x", last.getImX());
            node.addDouble("last y", last.getImY());
        } else {
            node.addString("last", "null");
        }
        node.addFloat("strength", strength);
        node.addBoolean("first usage in stroke", firstUsageInStroke);

        return node;
    }
}
