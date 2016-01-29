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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.EdgeFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.Invert;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * ConvolutionEdge based on the JHLabs EdgeFilter
 */
public class JHConvolutionEdge extends FilterWithParametrizedGUI {
    public static final String NAME = "Convolution Edge Detection";

    private final IntChoiceParam horizontalMethod = new IntChoiceParam("Horizontal Edges", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Sobel", METHOD_SOBEL),
            new IntChoiceParam.Value("Prewitt", METHOD_PREWITT),
            new IntChoiceParam.Value("Roberts", METHOD_ROBERTS),
            new IntChoiceParam.Value("Frei-Chen", METHOD_FREI_CHEN),
            new IntChoiceParam.Value("None", METHOD_NONE),
    });

    private final IntChoiceParam verticalMethod = new IntChoiceParam("Vertical Edges", new IntChoiceParam.Value[]{
            new IntChoiceParam.Value("Sobel", METHOD_SOBEL),
            new IntChoiceParam.Value("Prewitt", METHOD_PREWITT),
            new IntChoiceParam.Value("Roberts", METHOD_ROBERTS),
            new IntChoiceParam.Value("Frei-Chen", METHOD_FREI_CHEN),
            new IntChoiceParam.Value("None", METHOD_NONE),
    });

    private final BooleanParam invertImage = new BooleanParam("Invert", false, IGNORE_RANDOMIZE);

    private EdgeFilter filter;
    private static final int METHOD_SOBEL = 1;
    private static final int METHOD_PREWITT = 2;
    private static final int METHOD_ROBERTS = 3;
    private static final int METHOD_FREI_CHEN = 4;
    private static final int METHOD_NONE = 5;

    private static final float[] NONE_MATRIX = {
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
    };

    public JHConvolutionEdge() {
        super(ShowOriginal.YES);
        setParamSet(new ParamSet(
                horizontalMethod,
                verticalMethod,
                invertImage
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new EdgeFilter(NAME);
        }

        int horizontal = horizontalMethod.getValue();
        switch (horizontal) {
            case METHOD_SOBEL:
                filter.setHEdgeMatrix(EdgeFilter.SOBEL_H);
                break;
            case METHOD_PREWITT:
                filter.setHEdgeMatrix(EdgeFilter.PREWITT_H);
                break;
            case METHOD_ROBERTS:
                filter.setHEdgeMatrix(EdgeFilter.ROBERTS_H);
                break;
            case METHOD_FREI_CHEN:
                filter.setHEdgeMatrix(EdgeFilter.FREI_CHEN_H);
                break;
            case METHOD_NONE:
                filter.setHEdgeMatrix(NONE_MATRIX);
                break;
            default:
                throw new IllegalStateException("horizontal = " + horizontal);
        }

        int vertical = verticalMethod.getValue();
        switch (vertical) {
            case METHOD_SOBEL:
                filter.setVEdgeMatrix(EdgeFilter.SOBEL_V);
                break;
            case METHOD_PREWITT:
                filter.setVEdgeMatrix(EdgeFilter.PREWITT_V);
                break;
            case METHOD_ROBERTS:
                filter.setVEdgeMatrix(EdgeFilter.ROBERTS_V);
                break;
            case METHOD_FREI_CHEN:
                filter.setVEdgeMatrix(EdgeFilter.FREI_CHEN_V);
                break;
            case METHOD_NONE:
                filter.setVEdgeMatrix(NONE_MATRIX);
                break;
            default:
                throw new IllegalStateException("vertical = " + vertical);
        }

        dest = filter.filter(src, dest);

        if (invertImage.isChecked()) {
            Invert.invertImage(dest, dest);
        }

        return dest;
    }
}