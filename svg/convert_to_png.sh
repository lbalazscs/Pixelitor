#!/usr/bin/bash

INKSCAPE="/cygdrive/c/Program Files/Inkscape/inkscape.exe"
rm *.png

# tools
for i in move_tool crop_tool selection_tool brush_tool clone_tool eraser_tool smudge_tool gradient_tool paint_bucket_tool color_picker_tool pen_tool shapes_tool hand_tool zoom_tool
do
	echo
	echo "converting $i"
	"$INKSCAPE" --without-gui --export-width=30 -e "${i}.png" "$i.svg"
#	"$INKSCAPE" --without-gui --export-width=60 -e "${i}_2x.png" "$i.svg"
done  

for i in eye_closed eye_open text_layer eye_closed_dark eye_open_dark text_layer_dark
do
	echo
	echo "converting $i"
	"$INKSCAPE" --without-gui --export-width=24 -e "${i}.png" "$i.svg"
#	"$INKSCAPE" --without-gui --export-width=48 -e "${i}_2x.png" "$i.svg"
done
