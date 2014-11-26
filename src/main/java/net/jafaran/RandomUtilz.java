/*
 * Copyright 2014 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jafaran;

import java.nio.ByteBuffer;
import java.util.Random;

class RandomUtilz {
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Possibly faster than java.lang.Math.abs(long).
     * 
     * @return The absolute value, except if value is Long.MIN_VALUE, for which it returns Long.MIN_VALUE.
     */
    static long abs(long a) {
        return (a^(a>>63))-(a>>63);
    }

    /**
     * @return The negative of the absolute value (always exact).
     */
    static long absNeg(long a) {
        return (a>>63)-(a^(a>>63));
    }
    
    /**
     * If the specified value is in int range, the returned value is identical.
     * 
     * @return An int hash of the specified value.
     */
    static int intHash(long a) {
        if (false) {
            // also works
            int hash = ((int)(a>>32)) ^ ((int)a);
            if (a < 0) {
                hash = -hash-1;
            }
            return hash;
        }
        int hash = ((int)(a>>32)) + ((int)a);
        if (a < 0) {
            hash++;
        }
        return hash;
    }

    /*
     * 
     */
    
    /**
     * Not suited for cryptographic usage.
     * 
     * @return A pseudo-entropic uniform long value.
     */
    static long longPseudoEntropy() {
        /*
         * Using pseudo-entropy from same pseudo-entropy source than Random.
         * Using a copy-pasted version of JDK's mechanics might cause both
         * pseudo-entropy sources to be effectively correlated if used in
         * parallel (even though we don't use the actual seed, since it's not
         * available, but nextLong()).
         * NB: Using ThreadLocalRandom (Java7) might be a bad idea here,
         * since the value would be obviously correlated with current TLR's
         * numbers sequence.
         */
        return new Random().nextLong();
    }
    
    static int getEncodingByteSizeForStoredBits() {
        // 1 for the number of them, 4 for room for 32 bits.
        return 1 + 4;
    }
    
    static void encodeStoredBits(
            int currentStoredBits,
            ByteBuffer bb) {
        bb.putInt(currentStoredBits);
    }

    static void encodeNbrOfStoredBits(
            int currentNbrOfStoredBits,
            ByteBuffer bb) {
        bb.put((byte)currentNbrOfStoredBits);
    }

    static int decodeStoredBits(ByteBuffer bb) {
        return bb.getInt();
    }

    static int decodeNbrOfStoredBits(ByteBuffer bb) {
        return (int)bb.get();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private RandomUtilz() {
    }
}
