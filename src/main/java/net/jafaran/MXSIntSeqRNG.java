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

/**
 * Not thread-safe.
 * 
 * Marsaglia Xor-Shift (32 bits,{13,17,5}) RNG.
 * 
 * Not as good randomness as MTSeqRNG, but faster.
 */
public class MXSIntSeqRNG extends AbstractSeqRNG {
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;
    
    private int state;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor using a random seed.
     */
    public MXSIntSeqRNG() {
    }

    /**
     * Constructor using a specified seed.
     */
    public MXSIntSeqRNG(long seed) {
        super(seed);
    }
    
    @Override
    public int nextInt() {
        int tmp = this.state;
        tmp ^= (tmp << 13);
        tmp ^= (tmp >>> 17);
        tmp ^= (tmp << 5);
        this.state = tmp;
        return tmp;
    }
    
    /*
     * 
     */
    
    @Override
    public byte[] getState() {
        byte[] tab = new byte[4 + RandomUtilz.getEncodingByteSizeForStoredBits()];
        // Big endian.
        ByteBuffer bb = ByteBuffer.wrap(tab);
        bb.putInt(this.state);
        RandomUtilz.encodeNbrOfStoredBits(this.getCurrentNbrOfStoredBits(), bb);
        RandomUtilz.encodeStoredBits(this.getCurrentStoredBits(), bb);
        return tab;
    }

    @Override
    public void setState(byte[] state) {
        ByteBuffer bb = ByteBuffer.wrap(state);
        this.state = bb.getInt();
        this.setCurrentNbrOfStoredBits(RandomUtilz.decodeNbrOfStoredBits(bb));
        this.setCurrentStoredBits(RandomUtilz.decodeStoredBits(bb));
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void setSeedImpl(long seed) {
        super.setSeedImpl(0L);
        this.state = RandomUtilz.intHash(seed);
    }
}
