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

package pixelitor.filters.lookup;

import com.jhlabs.image.Curve;
import com.jhlabs.image.PixelUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pixelitor.TestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChannelLookup tests")
class ChannelLookupTest {
    @BeforeAll
    static void beforeAllTests() {
        TestHelper.setUnitTestingMode();
    }

    @Test
    @DisplayName("identity for default levels")
    void defaultLevelsProduceIdentity() {
        ChannelLookup identity = ChannelLookup.getIdentity();
        ChannelLookup fromFactory = ChannelLookup.ofLevels(0, 255, 0, 255);
        ChannelLookup fromConstructor = new ChannelLookup(0, 255, 0, 255);

        for (int i = 0; i < 256; i++) {
            assertThat(identity.map(i)).as("identity at %d", i).isEqualTo((short) i);
            assertThat(fromFactory.map(i)).as("factory at %d", i).isEqualTo((short) i);
            assertThat(fromConstructor.map(i)).as("constructor at %d", i).isEqualTo((short) i);
        }

        assertThat(fromFactory.getTable()).isEqualTo(identity.getTable());
        assertThat(fromConstructor.getTable()).isEqualTo(identity.getTable());
    }

    @Test
    @DisplayName("ofLevels vs inline reference of the old formula")
    void ofLevelsMatchesReferenceFormula() {
        int[][] testCases = {
            {0, 255, 0, 255},
            {50, 200, 20, 220},
            {100, 150, 0, 255},
            {30, 220, 80, 180},
            {0, 255, 255, 0},
            {200, 50, 0, 255},
            {128, 128, 0, 255},
            {0, 0, 50, 200},
            {255, 255, 50, 200},
            {-20, 280, -10, 300}
        };

        for (int[] tc : testCases) {
            int inD = tc[0];
            int inL = tc[1];
            int outD = tc[2];
            int outL = tc[3];

            short[] expected = referenceLevels(inD, inL, outD, outL);
            ChannelLookup lookup = ChannelLookup.ofLevels(inD, inL, outD, outL);
            ChannelLookup fromCtor = new ChannelLookup(inD, inL, outD, outL);

            assertThat(lookup.getTable())
                .as("factory table for [%d, %d] -> [%d, %d]", inD, inL, outD, outL)
                .isEqualTo(expected);
            assertThat(fromCtor.getTable())
                .as("constructor table for [%d, %d] -> [%d, %d]", inD, inL, outD, outL)
                .isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("both clip settings: non-degenerate linear mapping")
    void bothClipSettingsNonDegenerate() {
        int inD = 50;
        int inL = 200;
        int outD = 50;
        int outL = 200;

        ChannelLookup unclipped = ChannelLookup.ofLevels(inD, inL, outD, outL, false);
        ChannelLookup clipped = ChannelLookup.ofLevels(inD, inL, outD, outL, true);

        // below inD: unclipped extends the line down to 0; clipped clamps to outD (50)
        assertThat(unclipped.map(0)).isEqualTo((short) 0);
        assertThat(unclipped.map(25)).isEqualTo((short) 25);
        assertThat(clipped.map(0)).isEqualTo((short) 50);
        assertThat(clipped.map(25)).isEqualTo((short) 50);

        // inside [inD, inL]: both follow the linear segment
        assertThat(unclipped.map(50)).isEqualTo((short) 50);
        assertThat(clipped.map(50)).isEqualTo((short) 50);
        assertThat(unclipped.map(125)).isEqualTo((short) 125);
        assertThat(clipped.map(125)).isEqualTo((short) 125);
        assertThat(unclipped.map(200)).isEqualTo((short) 200);
        assertThat(clipped.map(200)).isEqualTo((short) 200);

        // above inL: unclipped extends the line up to 255; clipped clamps to outL (200)
        assertThat(unclipped.map(225)).isEqualTo((short) 225);
        assertThat(unclipped.map(255)).isEqualTo((short) 255);
        assertThat(clipped.map(225)).isEqualTo((short) 200);
        assertThat(clipped.map(255)).isEqualTo((short) 200);
    }

    @Test
    @DisplayName("both clip settings: threshold case (input dark == input light)")
    void bothClipSettingsThreshold() {
        int threshold = 120;
        int outD = 30;
        int outL = 220;

        ChannelLookup unclipped = ChannelLookup.ofLevels(threshold, threshold, outD, outL, false);
        ChannelLookup clipped = ChannelLookup.ofLevels(threshold, threshold, outD, outL, true);

        // both settings must behave identically in the threshold branch
        assertThat(clipped.getTable()).isEqualTo(unclipped.getTable());

        for (int i = 0; i < 256; i++) {
            short expected = (short) (i < threshold ? outD : outL);
            assertThat(unclipped.map(i)).as("unclipped at %d", i).isEqualTo(expected);
            assertThat(clipped.map(i)).as("clipped at %d", i).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("ofTable(new Curve().makeTable()) and ofCurve are the identity")
    void curveProducesIdentity() {
        Curve curve = new Curve();
        ChannelLookup fromTable = ChannelLookup.ofTable(curve.makeTable());
        ChannelLookup fromCurve = ChannelLookup.ofCurve(curve);

        short[] identityTable = ChannelLookup.getIdentity().getTable();
        assertThat(fromTable.getTable()).isEqualTo(identityTable);
        assertThat(fromCurve.getTable()).isEqualTo(identityTable);

        for (int i = 0; i < 256; i++) {
            assertThat(fromCurve.map(i)).isEqualTo((short) i);
        }
    }

    @Test
    @DisplayName("ofTable rejects wrong lengths")
    void ofTableRejectsWrongLengths() {
        assertThatThrownBy(() -> ChannelLookup.ofTable(new int[0]))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChannelLookup.ofTable(new int[255]))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChannelLookup.ofTable(new int[257]))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChannelLookup.ofTable(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ofTable clamps out-of-range integer values")
    void ofTableClampsValues() {
        int[] table = new int[256];
        table[0] = -50;
        table[10] = 128;
        table[255] = 300;

        ChannelLookup lookup = ChannelLookup.ofTable(table);
        assertThat(lookup.map(0)).isEqualTo((short) 0);
        assertThat(lookup.map(10)).isEqualTo((short) 128);
        assertThat(lookup.map(255)).isEqualTo((short) 255);
    }

    private static short[] referenceLevels(int inputDark, int inputLight, int outputDark, int outputLight) {
        short[] lut = new short[256];
        int inputRange = inputLight - inputDark;

        if (inputRange == 0) {
            short darkValue = (short) PixelUtils.clamp(outputDark);
            short lightValue = (short) PixelUtils.clamp(outputLight);
            for (int i = 0; i < 256; i++) {
                lut[i] = (i < inputDark) ? darkValue : lightValue;
            }
        } else {
            double multiplier = (double) (outputLight - outputDark) / inputRange;
            double offset = outputDark - multiplier * inputDark;
            for (int i = 0; i < 256; i++) {
                lut[i] = (short) PixelUtils.clamp((int) Math.round(multiplier * i + offset));
            }
        }

        return lut;
    }
}
