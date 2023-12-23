/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.gmic;

import pixelitor.filters.gui.CommandLineGUI;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.TextParam;
import pixelitor.layers.Filterable;

import java.io.Serial;
import java.util.List;
import java.util.stream.Stream;

public class GMICCommand extends GMICFilter {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String NAME = "G'MIC Command";

    private final TextParam textParam = new TextParam("Command",
        "edges 9%\nnormalize 0,255", true);

    public GMICCommand() {
        textParam.setRandomCommands(List.of(
            "fx_circle_transform 50,50,75,50,-2,-2,0,1,3,1",
            "fx_array_fade 2,2,0,0,80,90,0,0",
            "fx_array_mirror 1,0,0,2,0,0,0",
            "fx_dices 2,24,1",
            "fx_imagegrid_hexagonal 32,0.1,1",
            "fx_imagegrid_triangular 10,18,0,255,255,255,128",
            "fx_puzzle 5,5,0.5,0,0,0.3,100,0.2,255,100,0,0,0,0,0,0",
            "fx_taquin 7,7,0,50,5,0,0,0,0,255,0",
            "fx_normalize_tiles 25,25,0,255,11",
            "cartoon 3,200,20,0.25,1.5,8",
            "fx_ellipsionism 20,10,0.5,0.7,3,0.5",
            "fx_feltpen 300,50,1,0.1,20,5",
            "fx_ghost 200,2,2,1,3,16,0",
            "fx_hardsketchbw 300,50,1,0.1,20,0,4",
            "fx_houghsketchbw 1.25,10,5,80,0.1,4",
            "fx_quadtree 0,1024,0.5,0,3,1.5,1,1",
            "fx_vector_painting 9",
            "fx_draw_whirl 20",
            "fx_engrave 0.5,50,0,8,40,0,0,0,10,1,0,0,0,1",
            "fx_breaks 0,30,30,0.5,3",
            "raindrops 80,0.1,1,0",
            "fx_wind 20,0,0.7,20,1,0,0",
            "fx_lomo 20",
            "fx_scanlines 60,2,0,0,0,0,0",
            "fx_frame_painting 10,0.4,6,225,200,120,2,400,50,10,1,0.5,123456,0",
            "fx_lava 8,5,3,0",
            "fx_maze 24,1,0,1,0",
            "fx_mineral_mosaic 1,2,1,100,0",
            "fx_shapes 1,16,10,2,5,90,0,0,1,1,0",
            "fx_pills 0,4,0,4,0,4,0,4,0",
            "fx_voronoi 160,1,0.5,50,3,1,0,0,0,100,2,255,255,255,40,1"
        ));

        setParams(textParam);
    }

    @Override
    public List<String> getArgs() {
        return Stream.of(textParam.getValue().split(" "))
            .map(String::trim)
            .toList();
    }

    @Override
    public FilterGUI createGUI(Filterable layer, boolean reset) {
        return new CommandLineGUI(this, layer, true, reset);
    }
}
