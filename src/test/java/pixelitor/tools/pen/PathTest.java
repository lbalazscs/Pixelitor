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

package pixelitor.tools.pen;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import pixelitor.Build;
import pixelitor.Composition;
import pixelitor.TestHelper;
import pixelitor.gui.ImageComponent;
import pixelitor.history.History;
import pixelitor.utils.Shapes;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import static org.assertj.core.api.Assertions.assertThat;

public class PathTest {
    private ImageComponent ic;

    @BeforeClass
    public static void setupClass() {
        Build.setTestingMode();
    }

    @Before
    public void setUp() {
        Composition comp = TestHelper.createMockComposition();
        ic = comp.getIC();
    }

    @Test
    public void testDeletingSubPathPoints() {
        Rectangle shape = new Rectangle(10, 10, 100, 100);
        Path path = Shapes.shapeToPath(shape, ic);
        SubPath sp = path.getActiveSubpath();
        int numPoints = sp.getNumAnchorPoints();
        assertThat(numPoints == 4).isTrue();
        sp.getPoint(3).delete(); // delete last
        sp.getPoint(2).delete(); // delete last
        sp.getPoint(0).delete(); // delete first
        sp.getPoint(0).delete(); // delete first (and last)
        assertThat(path.getActiveSubpath().getNumAnchorPoints()).isEqualTo(0);

        History.undo();
        History.undo();
        // check the active subpath because the undo replaced
        // the subpath reference in the path
        assertThat(path.getActiveSubpath().getNumAnchorPoints()).isEqualTo(2);

        History.redo();
        History.redo();
        assertThat(path.getActiveSubpath().getNumAnchorPoints()).isEqualTo(0);
    }

    @Test
    public void testConversionsForRectangle() {
        testConversionsFor(
                new Rectangle(20, 20, 40, 10));
    }

    @Test
    public void testConversionsForEllipse() {
        testConversionsFor(
                new Ellipse2D.Double(20, 20, 40, 10));
    }

    private void testConversionsFor(Shape shape) {
        Path path = Shapes.shapeToPath(shape, ic);
        Path copy = path.copyForUndo();
        Shape convertedShape = copy.toImageSpaceShape();
        assertThat(Shapes.pathIteratorIsEqual(shape, convertedShape, 0.01)).isTrue();
    }
}