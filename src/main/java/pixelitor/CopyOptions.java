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

package pixelitor;

import pixelitor.io.FileUtils;
import pixelitor.utils.Utils;

import java.util.function.UnaryOperator;

/**
 * Describes how a composition or layer should be copied.
 */
public record CopyOptions(
    boolean deepContentCopy, // whether a smart object's content is copied
    boolean copySelection,
    boolean copyGuides,
    boolean copyMask,
    boolean preserveFileState,  // dirty flag, file paths, file timestamps
    boolean skipUIUpdates,
    UnaryOperator<String> compNameGenerator,
    UnaryOperator<String> layerNameGenerator
) {
    private static final UnaryOperator<String> SAME_NAME = UnaryOperator.identity();
    private static final UnaryOperator<String> COPY_NAME = Utils::createCopyName;
    private static final UnaryOperator<String> COPY_NAME_NO_EXT = name -> Utils.createCopyName(FileUtils.stripExtension(name));

    public static CopyOptions duplicateLayer() {
        return duplicateLayer(false);
    }

    public static CopyOptions duplicateLayer(boolean sameName) {
        return new CopyOptions(true, false, false, true, false, false, SAME_NAME, sameName ? SAME_NAME : COPY_NAME);
    }

    public static CopyOptions duplicateComposition() {
        return new CopyOptions(true, true, true, true, false, false, COPY_NAME_NO_EXT, SAME_NAME);
    }

    public static CopyOptions fullStateBackup() {
        return new CopyOptions(false, true, true, true, true, true, SAME_NAME, SAME_NAME);
    }

    public static CopyOptions fullStateBackup(boolean copySelection, boolean copyGuides) {
        return new CopyOptions(false, copySelection, copyGuides, true, true, true, SAME_NAME, SAME_NAME);
    }

    public static CopyOptions smartObjectShallowDuplicate() {
        return new CopyOptions(false, false, false, true, true, false, SAME_NAME, name -> name + " clone");
    }

    public CopyOptions withoutMask() {
        return new CopyOptions(deepContentCopy, copySelection, copyGuides, false,
            preserveFileState, skipUIUpdates, compNameGenerator, layerNameGenerator);
    }

    public String createLayerCopyName(String origName) {
        return layerNameGenerator.apply(origName);
    }

    public String createCompCopyName(String origName) {
        return compNameGenerator.apply(origName);
    }
}
