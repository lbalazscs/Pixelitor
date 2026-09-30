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

import pixelitor.tools.util.PPoint;
import pixelitor.utils.debug.DebugNode;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static pixelitor.colors.FgBgColors.getFgColor;

/**
 * The brush used by the Smudge Tool.
 */
public class SmudgeBrush extends CopyBrush {
    // the location of the previous dab, used to sample pixels
    // from the source image for the current dab
    private PPoint lastDabPoint;

    // the brush's strength, applied as opacity for each dab
    private float strength;

    private boolean firstDabInStroke = true;

    private Composite dabComposite;

    // if true, the brush starts with the foreground color
    // instead of sampling the source image
    private boolean fingerPainting = false;

    public SmudgeBrush(double radius, CopyBrushType type) {
        super(radius, type, new FixedDistanceSpacing(1.0));
    }

    public void setSourceImage(BufferedImage sourceImage) {
        this.sourceImage = sourceImage;
    }

    public void initStroke(PPoint startPoint, float strength) {
        lastDabPoint = startPoint;
        this.strength = strength;

        // SrcOver allows smudging into transparent areas, but transparency
        // can't be smudged into non-transparent areas.
        // DstOver allows only smudging into transparent areas.
        dabComposite = AlphaComposite.SrcOver.derive(strength);

        firstDabInStroke = true;
    }

    public void initStroke(BufferedImage sourceImage, PPoint startPoint, float strength) {
        setSourceImage(sourceImage);
        initStroke(startPoint, strength);
    }

    @Override
    void initBrushStamp(PPoint p) {
        Graphics2D g = brushImage.createGraphics();
        edge.beforeDrawImage(g);

        if (firstDabInStroke && fingerPainting) {
            // finger painting: fill the brush with the foreground color
            g.setColor(getFgColor());
            int size = (int) diameter;
            g.fillRect(0, 0, size, size);
        } else {
            // normal smudging: sample the source image at the last point
            g.drawImage(sourceImage,
                AffineTransform.getTranslateInstance(
                    -lastDabPoint.getImX() + radius,
                    -lastDabPoint.getImY() + radius), null);
        }

        edge.afterDrawImage(g);
        g.dispose();

        firstDabInStroke = false;
        debugImage();
    }

    @Override
    public void putDab(PPoint currentPoint, double angle) {
        var transform = AffineTransform.getTranslateInstance(
            currentPoint.getImX() - radius,
            currentPoint.getImY() - radius
        );

        targetG.setComposite(dabComposite);

        targetG.drawImage(brushImage, transform, null);
        lastDabPoint = currentPoint;
        repaintSegment(currentPoint);
    }

    public void setFingerPainting(boolean fingerPainting) {
        this.fingerPainting = fingerPainting;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addNullableDebuggable("last", lastDabPoint);
        node.addFloat("strength", strength);
        node.addBoolean("first dab in stroke", firstDabInStroke);

        return node;
    }
}
