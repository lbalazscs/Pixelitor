/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jhlabs.image;

import java.awt.geom.Point2D;

public class OffsetFilter extends TransformFilter {
    private final int xOffset;
    private final int yOffset;

    public OffsetFilter(String filterName, int edgeAction, Point2D offset) {
        super(filterName, edgeAction, TransformFilter.NEAREST_NEIGHBOR);

        xOffset = (int) (offset.getX());
        yOffset = (int) (offset.getY());
    }

    @Override
    protected void transformInverse(int x, int y, float[] out) {
        out[0] = x - xOffset;
        out[1] = y - yOffset;
    }
}
