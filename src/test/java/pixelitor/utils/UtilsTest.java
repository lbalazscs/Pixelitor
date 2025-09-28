/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Utils tests")
class UtilsTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    void angleFunctions() {
        for (double a = -Math.PI; a < Math.PI; a += 0.1) {
            double intuitive = Geometry.atan2ToIntuitive(a);
            double atan = Geometry.intuitiveToAtan2(intuitive);
            assertThat(atan).isCloseTo(a, within(0.01));
        }

        for (double a = 0.0; a < 2 * Math.PI; a += 0.1) {
            double atan = Geometry.intuitiveToAtan2(a);
            double b = Geometry.atan2ToIntuitive(atan);
            assertThat(a).isCloseTo(b, within(0.01));
        }
    }

    @Test
    void newlineEncodingDecoding() {
        // Test strings with different types of newlines
        String[] testInputs = {
            "Text without newlines",
            "Text with\nunix newline",
            "Text with\r\nwindows newline",
            "Text with\rmac newline",
            "Multiple\nlines\nwith\ndifferent\r\nnewlines\rhere",
            "",
            "\n",
            "\r\n",
            "\r",
            "Start\nMiddle\r\nEnd\r"
        };

        for (String input : testInputs) {
            String encoded = Utils.encodeNewlines(input);

            // encoded string should not contain any newline characters
            assertThat(encoded)
                .doesNotContain("\n")
                .doesNotContain("\r");

            // encode then decode should normalize all newlines to \n
            String decoded = Utils.decodeNewlines(encoded);
            String expectedDecoded = input.replaceAll("\\R", "\n");
            assertThat(decoded).isEqualTo(expectedDecoded);
        }
    }
}
