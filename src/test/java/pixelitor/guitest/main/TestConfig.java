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

package pixelitor.guitest.main;

import com.bric.util.JVM;
import pixelitor.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Immutable configuration for {@link MainGuiTest}.
 */
public class TestConfig {
    // in quick mode some tests are skipped
    private final boolean quick;

    private final File baseDir;
    private final File cleanerScript;

    private final File inputDir;
    private final File batchResizeOutputDir;
    private final File batchFilterOutputDir;
    private final File svgOutputDir;

    public TestConfig(String[] args) {
        // enable quick mode with -Dquick=true
        quick = "true".equals(System.getProperty("quick"));

        // process command-line arguments
        if (args.length != 1) {
            System.err.println("Required argument: <base testing directory> or \"help\"");
            System.exit(1);
        }
        if (args[0].equals("help")) {
            System.out.println("Test targets: " + Arrays.toString(TestSuite.values()));
            System.out.println("Mask modes: " + Arrays.toString(MaskMode.values()));

            System.exit(0);
        }
        baseDir = new File(args[0]);
        assertThat(baseDir).exists().isDirectory();

        inputDir = requireDir(baseDir, "input");
        batchResizeOutputDir = requireDir(baseDir, "batch_resize_output");
        batchFilterOutputDir = requireDir(baseDir, "batch_filter_output");
        svgOutputDir = requireDir(baseDir, "svg_output");

        String cleanerScriptExt = JVM.isWindows ? ".bat" : ".sh";
        cleanerScript = new File(baseDir, "0000_clean_outputs" + cleanerScriptExt);

        if (!cleanerScript.exists()) {
            throw new IllegalStateException("Cleaner script " + cleanerScript.getName() + " not found.");
        }
    }

    private static File requireDir(File baseDir, String name) {
        File dir = new File(baseDir, name);
        assertThat(dir).exists().isDirectory();
        return dir;
    }

    public void cleanOutputs() {
        try {
            String cleanerScriptPath = cleanerScript.getCanonicalPath();
            Process process = new ProcessBuilder(cleanerScriptPath).start();
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                throw new IllegalStateException("Exit value for " + cleanerScriptPath + " was " + exitValue);
            }

            assertThat(batchResizeOutputDir).isEmptyDirectory();
            assertThat(batchFilterOutputDir).isEmptyDirectory();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to clean output directories", e);
        }
    }

    public void checkBatchResizeOutputWasCreated() {
        checkOutputFilesWereCreated(batchResizeOutputDir);
    }

    public void checkBatchFilterOutputWasCreated() {
        checkOutputFilesWereCreated(batchFilterOutputDir);
    }

    private void checkOutputFilesWereCreated(File outputDir) {
        for (File inputFile : FileUtils.listSupportedInputFiles(inputDir)) {
            String fileName = inputFile.getName();

            File outFile = new File(outputDir, fileName);
            assertThat(outFile).exists().isFile();
        }
    }

    public File getBaseDir() {
        return baseDir;
    }

    public File getInputDir() {
        return inputDir;
    }

    public File getSvgOutputDir() {
        return svgOutputDir;
    }

    public File getBatchResizeOutputDir() {
        return batchResizeOutputDir;
    }

    public File getBatchFilterOutputDir() {
        return batchFilterOutputDir;
    }

    public boolean isQuick() {
        return quick;
    }
}
