package pd.fastnoise;

import static pd.fastnoise.FastNoiseLite.Lerp;
import static pd.fastnoise.FastNoiseLite.PingPong;
import static pd.fastnoise.FastVals.FastMin;

public class Fractal {
    private int mOctaves = 3;
    private float mGain = 0.5f;
    private int mSeed = 1337;
    private float mLacunarity = 2.0f;
    private float mWeightedStrength = 0.0f;
    private float mPingPongStrength = 2.0f;
    private FastNoiseLite.DomainWarpType mDomainWarpType = FastNoiseLite.DomainWarpType.OpenSimplex2;
    private FastNoiseLite.FractalType mFractalType = FastNoiseLite.FractalType.None;
    SingleValues singleValues;
    private float mFractalBounding = 1 / 1.75f;
    private static float FastAbs(float f) {
        return f < 0 ? -f : f;
    }
    public void SetFractalType(FastNoiseLite.FractalType fractalType) {
        mFractalType = fractalType;
    }
    public void SetFractalOctaves(int octaves) {
        mOctaves = octaves;
        CalculateFractalBounding();
    }
    private void CalculateFractalBounding() {
        float gain = FastAbs(mGain);
        float amp = gain;
        float ampFractal = 1.0f;
        for (int i = 1; i < mOctaves; i++) {
            ampFractal += amp;
            amp *= gain;
        }
        mFractalBounding = 1 / ampFractal;
    }

    public void SetFractalLacunarity(float lacunarity) {
        mLacunarity = lacunarity;
    }
    public void SetFractalGain(float gain) {
        mGain = gain;
        CalculateFractalBounding();
    }
    public void SetFractalWeightedStrength(float weightedStrength) {
        mWeightedStrength = weightedStrength;
    }
    public void SetFractalPingPongStrength(float pingPongStrength) {
        mPingPongStrength = pingPongStrength;
    }


    protected float GenFractalRidged(double x, double y) {
        int seed = mSeed;
        float sum = 0;
        float amp = mFractalBounding;

        for (int i = 0; i < mOctaves; i++) {
            float noise = FastAbs(singleValues.GenNoiseSingle(seed++, x, y));
            sum += (noise * -2 + 1) * amp;
            amp *= Lerp(1.0f, 1 - noise, mWeightedStrength);

            x *= mLacunarity;
            y *= mLacunarity;
            amp *= mGain;
        }

        return sum;
    }

    // Fractal PingPong

    protected float GenFractalPingPong(double x, double y) {
        int seed = mSeed;
        float sum = 0;
        float amp = mFractalBounding;

        for (int i = 0; i < mOctaves; i++) {
            float noise = PingPong((singleValues.GenNoiseSingle(seed++, x, y) + 1) * mPingPongStrength);
            sum += (noise - 0.5f) * 2 * amp;
            amp *= Lerp(1.0f, noise, mWeightedStrength);

            x *= mLacunarity;
            y *= mLacunarity;
            amp *= mGain;
        }

        return sum;
    }
    protected float GenFractalFBm(double x, double y) {
        int seed = mSeed;
        float sum = 0;
        float amp = mFractalBounding;

        for (int i = 0; i < mOctaves; i++) {
            float noise = singleValues.GenNoiseSingle(seed++, x, y);
            sum += noise * amp;
            amp *= Lerp(1.0f, FastMin(noise + 1, 2) * 0.5f, mWeightedStrength);

            x *= mLacunarity;
            y *= mLacunarity;
            amp *= mGain;
        }

        return sum;
    }

}
