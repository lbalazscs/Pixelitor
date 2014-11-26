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

/**
 * Abstract class for sequential (non-concurrent) RNGs,
 * which implements bits storing in next(int).
 */
public abstract class AbstractSeqRNG extends AbstractRNG {

    /*
     * Not doing such a class for concurrent RNGs, since storing benefit might
     * be neglectable compared to synchronization costs, and storing mechanics
     * could have to be hacked into each RNG's concurrency logic.
     */
    
    //--------------------------------------------------------------------------
    // MEMBERS
    //--------------------------------------------------------------------------
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Bits stored as LSBits.
     */
    private int currentStoredBits;
    
    /**
     * In [0,32].
     */
    private int currentNbrOfStoredBits;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Constructor using a random seed.
     */
    public AbstractSeqRNG() {
    }

    /**
     * Constructor using a specified seed.
     */
    public AbstractSeqRNG(long seed) {
        super(seed);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    protected AbstractSeqRNG(Void dummy) {
        super(dummy);
    }

    /**
     * Must be called by overriding implementation, as well as any setSeed
     * method, to clear stored bits.
     * Not doing a specific method for stored bits clearing, else extending
     * class creator would have greater chances of forgetting to call it.
     */
    @Override
    protected void setSeedImpl(long unused) {
        this.currentStoredBits = 0;
        this.currentNbrOfStoredBits = 0;
    }
    
    /**
     * If nbrOfBits is too large, storing overhead by not be worth it,
     * so it might be better to use nextInt() instead.
     */
    @Override
    protected int next(int nbrOfBits) {
        final int result;
        final int tmpBits;
        final int nbrOfTmpBitsNotUsed;
        final int nbrOfStoredBits = this.currentNbrOfStoredBits;
        if (nbrOfBits <= nbrOfStoredBits) {
            // Enough stored bits.
            tmpBits = this.currentStoredBits;
            nbrOfTmpBitsNotUsed = (nbrOfStoredBits-nbrOfBits);
            // Using MSBits of current bits.
            result = (tmpBits>>>nbrOfTmpBitsNotUsed);
        } else {
            // Not enough stored bits: getting more random bits.
            tmpBits = this.nextInt();
            // Number of new random bits to add to the value.
            final int nbrOfNewBitsUsed = nbrOfBits - nbrOfStoredBits;
            nbrOfTmpBitsNotUsed = (32-nbrOfNewBitsUsed);
            // Using stored bits as MSBits, and MSBits of new bits as LSBits.
            result = (this.currentStoredBits<<nbrOfNewBitsUsed) | (tmpBits>>>nbrOfTmpBitsNotUsed);
        }
        this.currentStoredBits = ((tmpBits & ((1<<nbrOfTmpBitsNotUsed)-1)));
        this.currentNbrOfStoredBits = nbrOfTmpBitsNotUsed;
        return result;
    }
    
    /*
     * For state get/set.
     */
    
    protected void setCurrentStoredBits(int currentStoredBits) {
        this.currentStoredBits = currentStoredBits;
    }

    protected void setCurrentNbrOfStoredBits(int currentNbrOfStoredBits) {
        this.currentNbrOfStoredBits = currentNbrOfStoredBits;
    }

    protected int getCurrentStoredBits() {
        return this.currentStoredBits;
    }
    
    protected int getCurrentNbrOfStoredBits() {
        return this.currentNbrOfStoredBits;
    }
}
