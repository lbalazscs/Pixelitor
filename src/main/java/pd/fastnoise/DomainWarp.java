package pd.fastnoise;

import static pd.fastnoise.FastNoiseLite.*;

public class DomainWarp {
    private static FastNoiseLite fastNoiseLite = new FastNoiseLite();
    private int mSeed = 1337;
    private float mFrequency = 0.01f;
    private NoiseType mNoiseType = NoiseType.OpenSimplex2;
    private RotationType3D mRotationType3D = RotationType3D.None;

    private FractalType mFractalType = FractalType.None;
    private DomainWarpFractalType mDomainWarpFractalType = DomainWarpFractalType.None;

    private int mOctaves = 3;
    private float mLacunarity = 2.0f;
    private float mGain = 0.5f;
    private float mWeightedStrength = 0.0f;
    private float mPingPongStrength = 2.0f;

    private float mFractalBounding = 1 / 1.75f;

    private CellularDistanceFunction mCellularDistanceFunction = CellularDistanceFunction.EuclideanSq;
    private CellularReturnType mCellularReturnType = CellularReturnType.Distance;
    private float mCellularJitterModifier = 1.0f;

    private DomainWarpType mDomainWarpType = DomainWarpType.OpenSimplex2;

    private float mDomainWarpAmp = 1.0f;

    public DomainWarp(FastNoiseLite fastNoiseLite) {
        DomainWarp.fastNoiseLite = fastNoiseLite;
    }

    public void DomainWarp(Vectors.Vector2 coord) {
        switch (mDomainWarpFractalType) {
            case None -> DomainWarpSingle(coord);
            case Progressive -> DomainWarpFractalProgressive(coord);
            case Independent -> DomainWarpFractalIndependent(coord);
        }
    }

    public void SetDomainWarpType(DomainWarpType domainWarpType) {
        mDomainWarpType = domainWarpType;
        fastNoiseLite.UpdateWarpTransformType3D();
    }

    public void SetDomainWarpAmp(float domainWarpAmp) {
        mDomainWarpAmp = domainWarpAmp;
    }

    public void SetDomainFractalType(DomainWarpFractalType fractalType) {
        mDomainWarpFractalType = fractalType;
    }



    protected void DomainWarpSingle(Vectors.Vector2 coord) {
        int seed = mSeed;
        float amp = mDomainWarpAmp * mFractalBounding;
        float freq = mFrequency;

        /*FNLfloat*/
        double xs = coord.x;
        /*FNLfloat*/
        double ys = coord.y;
        switch (mDomainWarpType) {
            case OpenSimplex2:
            case OpenSimplex2Reduced:
                final double SQRT3 = (double) 1.7320508075688772935274463415059;
                final double F2 = 0.5f * (SQRT3 - 1);
                /*FNLfloat*/
                double t = (xs + ys) * F2;
                xs += t;
                ys += t;
                break;
            default:
                break;
        }

        DoSingleDomainWarp(seed, amp, freq, xs, ys, coord);
    }
     protected void DoSingleDomainWarp(int seed, float amp, float freq, double x, double y, Vectors.Vector2 coord) {
         switch (mDomainWarpType) {
             case OpenSimplex2 ->
                     SingleDomainWarpSimplexGradient(seed, amp * 38.283687591552734375f, freq, x, y, coord, false);
             case OpenSimplex2Reduced -> SingleDomainWarpSimplexGradient(seed, amp * 16.0f, freq, x, y, coord, true);
             case BasicGrid -> SingleDomainWarpBasicGrid(seed, amp, freq, x, y, coord);
         }
    }

     protected static void SingleDomainWarpBasicGrid(int seed, float warpAmp, float frequency, double x, double y, Vectors.Vector2 coord) {
        /*FNLfloat*/
        double xf = x * frequency;
        /*FNLfloat*/
        double yf = y * frequency;

        int x0 = FastVals.FastFloor(xf);
        int y0 = FastVals.FastFloor(yf);

        float xs = InterpHermite((float) (xf - x0));
        float ys = InterpHermite((float) (yf - y0));

        x0 *= PrimeX;
        y0 *= PrimeY;
        int x1 = x0 + PrimeX;
        int y1 = y0 + PrimeY;

        int hash0 = Hash(seed, x0, y0) & (255 << 1);
        int hash1 = Hash(seed, x1, y0) & (255 << 1);

        float lx0x = Lerp(RandVecs2D[hash0], RandVecs2D[hash1], xs);
        float ly0x = Lerp(RandVecs2D[hash0 | 1], RandVecs2D[hash1 | 1], xs);

        hash0 = Hash(seed, x0, y1) & (255 << 1);
        hash1 = Hash(seed, x1, y1) & (255 << 1);

        float lx1x = Lerp(RandVecs2D[hash0], RandVecs2D[hash1], xs);
        float ly1x = Lerp(RandVecs2D[hash0 | 1], RandVecs2D[hash1 | 1], xs);

        coord.x += Lerp(lx0x, lx1x, ys) * warpAmp;
        coord.y += Lerp(ly0x, ly1x, ys) * warpAmp;
    }

    protected static void SingleDomainWarpBasicGrid(int seed, float warpAmp, float frequency, double x, double y, double z, Vectors.Vector3 coord) {
        /*FNLfloat*/
        double xf = x * frequency;
        /*FNLfloat*/
        double yf = y * frequency;
        /*FNLfloat*/
        double zf = z * frequency;

        int x0 = FastVals.FastFloor(xf);
        int y0 = FastVals.FastFloor(yf);
        int z0 = FastVals.FastFloor(zf);

        float xs = InterpHermite((float) (xf - x0));
        float ys = InterpHermite((float) (yf - y0));
        float zs = InterpHermite((float) (zf - z0));

        x0 *= PrimeX;
        y0 *= PrimeY;
        z0 *= PrimeZ;
        int x1 = x0 + PrimeX;
        int y1 = y0 + PrimeY;
        int z1 = z0 + PrimeZ;

        int hash0 = Hash(seed, x0, y0, z0) & (255 << 2);
        int hash1 = Hash(seed, x1, y0, z0) & (255 << 2);

        float lx0x = Lerp(RandVecs3D[hash0], RandVecs3D[hash1], xs);
        float ly0x = Lerp(RandVecs3D[hash0 | 1], RandVecs3D[hash1 | 1], xs);
        float lz0x = Lerp(RandVecs3D[hash0 | 2], RandVecs3D[hash1 | 2], xs);

        hash0 = Hash(seed, x0, y1, z0) & (255 << 2);
        hash1 = Hash(seed, x1, y1, z0) & (255 << 2);

        float lx1x = Lerp(RandVecs3D[hash0], RandVecs3D[hash1], xs);
        float ly1x = Lerp(RandVecs3D[hash0 | 1], RandVecs3D[hash1 | 1], xs);
        float lz1x = Lerp(RandVecs3D[hash0 | 2], RandVecs3D[hash1 | 2], xs);

        float lx0y = Lerp(lx0x, lx1x, ys);
        float ly0y = Lerp(ly0x, ly1x, ys);
        float lz0y = Lerp(lz0x, lz1x, ys);

        hash0 = Hash(seed, x0, y0, z1) & (255 << 2);
        hash1 = Hash(seed, x1, y0, z1) & (255 << 2);

        lx0x = Lerp(RandVecs3D[hash0], RandVecs3D[hash1], xs);
        ly0x = Lerp(RandVecs3D[hash0 | 1], RandVecs3D[hash1 | 1], xs);
        lz0x = Lerp(RandVecs3D[hash0 | 2], RandVecs3D[hash1 | 2], xs);

        hash0 = Hash(seed, x0, y1, z1) & (255 << 2);
        hash1 = Hash(seed, x1, y1, z1) & (255 << 2);

        lx1x = Lerp(RandVecs3D[hash0], RandVecs3D[hash1], xs);
        ly1x = Lerp(RandVecs3D[hash0 | 1], RandVecs3D[hash1 | 1], xs);
        lz1x = Lerp(RandVecs3D[hash0 | 2], RandVecs3D[hash1 | 2], xs);

        coord.x += Lerp(lx0y, Lerp(lx0x, lx1x, ys), zs) * warpAmp;
        coord.y += Lerp(ly0y, Lerp(ly0x, ly1x, ys), zs) * warpAmp;
        coord.z += Lerp(lz0y, Lerp(lz0x, lz1x, ys), zs) * warpAmp;
    }


    // Domain Warp Simplex/OpenSimplex2
     protected static void SingleDomainWarpSimplexGradient(int seed, float warpAmp, float frequency, double x, double y, Vectors.Vector2 coord, boolean outGradOnly) {
        final float SQRT3 = 1.7320508075688772935274463415059f;
        final float G2 = (3 - SQRT3) / 6;

        x *= frequency;
        y *= frequency;

        /*
         * --- Skew moved to switch statements before fractal evaluation  ---
         * final FNLfloat F2 = 0.5f * (SQRT3 - 1);
         * FNLfloat s = (x + y) * F2;
         * x += s; y += s;
         */

        int i = FastVals.FastFloor(x);
        int j = FastVals.FastFloor(y);
        float xi = (float) (x - i);
        float yi = (float) (y - j);

        float t = (xi + yi) * G2;
        float x0 = xi - t;
        float y0 = yi - t;

        i *= PrimeX;
        j *= PrimeY;

        float vx, vy;
        vx = vy = 0;

        float a = 0.5f - x0 * x0 - y0 * y0;
        if (a > 0) {
            float aaaa = (a * a) * (a * a);
            float xo, yo;
            if (outGradOnly) {
                int hash = Hash(seed, i, j) & (255 << 1);
                xo = RandVecs2D[hash];
                yo = RandVecs2D[hash | 1];
            } else {
                int hash = Hash(seed, i, j);
                int index1 = hash & (127 << 1);
                int index2 = (hash >> 7) & (255 << 1);
                float xg = Gradients2D[index1];
                float yg = Gradients2D[index1 | 1];
                float value = x0 * xg + y0 * yg;
                float xgo = RandVecs2D[index2];
                float ygo = RandVecs2D[index2 | 1];
                xo = value * xgo;
                yo = value * ygo;
            }
            vx += aaaa * xo;
            vy += aaaa * yo;
        }

        float c = 2 * (1 - 2 * G2) * (1 / G2 - 2) * t + ((-2 * (1 - 2 * G2) * (1 - 2 * G2)) + a);
        if (c > 0) {
            float x2 = x0 + (2 * G2 - 1);
            float y2 = y0 + (2 * G2 - 1);
            float cccc = (c * c) * (c * c);
            float xo, yo;
            if (outGradOnly) {
                int hash = Hash(seed, i + PrimeX, j + PrimeY) & (255 << 1);
                xo = RandVecs2D[hash];
                yo = RandVecs2D[hash | 1];
            } else {
                int hash = Hash(seed, i + PrimeX, j + PrimeY);
                int index1 = hash & (127 << 1);
                int index2 = (hash >> 7) & (255 << 1);
                float xg = Gradients2D[index1];
                float yg = Gradients2D[index1 | 1];
                float value = x2 * xg + y2 * yg;
                float xgo = RandVecs2D[index2];
                float ygo = RandVecs2D[index2 | 1];
                xo = value * xgo;
                yo = value * ygo;
            }
            vx += cccc * xo;
            vy += cccc * yo;
        }

        if (y0 > x0) {
            float x1 = x0 + G2;
            float y1 = y0 + (G2 - 1);
            float b = 0.5f - x1 * x1 - y1 * y1;
            if (b > 0) {
                float bbbb = (b * b) * (b * b);
                float xo, yo;
                if (outGradOnly) {
                    int hash = Hash(seed, i, j + PrimeY) & (255 << 1);
                    xo = RandVecs2D[hash];
                    yo = RandVecs2D[hash | 1];
                } else {
                    int hash = Hash(seed, i, j + PrimeY);
                    int index1 = hash & (127 << 1);
                    int index2 = (hash >> 7) & (255 << 1);
                    float xg = Gradients2D[index1];
                    float yg = Gradients2D[index1 | 1];
                    float value = x1 * xg + y1 * yg;
                    float xgo = RandVecs2D[index2];
                    float ygo = RandVecs2D[index2 | 1];
                    xo = value * xgo;
                    yo = value * ygo;
                }
                vx += bbbb * xo;
                vy += bbbb * yo;
            }
        } else {
            float x1 = x0 + (G2 - 1);
            float y1 = y0 + G2;
            float b = 0.5f - x1 * x1 - y1 * y1;
            if (b > 0) {
                float bbbb = (b * b) * (b * b);
                float xo, yo;
                if (outGradOnly) {
                    int hash = Hash(seed, i + PrimeX, j) & (255 << 1);
                    xo = RandVecs2D[hash];
                    yo = RandVecs2D[hash | 1];
                } else {
                    int hash = Hash(seed, i + PrimeX, j);
                    int index1 = hash & (127 << 1);
                    int index2 = (hash >> 7) & (255 << 1);
                    float xg = Gradients2D[index1];
                    float yg = Gradients2D[index1 | 1];
                    float value = x1 * xg + y1 * yg;
                    float xgo = RandVecs2D[index2];
                    float ygo = RandVecs2D[index2 | 1];
                    xo = value * xgo;
                    yo = value * ygo;
                }
                vx += bbbb * xo;
                vy += bbbb * yo;
            }
        }

        coord.x += vx * warpAmp;
        coord.y += vy * warpAmp;
    }
    protected void DomainWarpFractalIndependent(Vectors.Vector2 coord) {
        /*FNLfloat*/
        double xs = coord.x;
        /*FNLfloat*/
        double ys = coord.y;
        switch (mDomainWarpType) {
            case OpenSimplex2, OpenSimplex2Reduced -> {
                final double SQRT3 = 1.7320508075688772935274463415059;
                final double F2 = 0.5f * (SQRT3 - 1);
                /*FNLfloat*/
                double t = (xs + ys) * F2;
                xs += t;
                ys += t;
            }
            default -> {
            }
        }

        int seed = mSeed;
        float amp = mDomainWarpAmp * mFractalBounding;
        float freq = mFrequency;

        for (int i = 0; i < mOctaves; i++) {
            DoSingleDomainWarp(seed, amp, freq, xs, ys, coord);

            seed++;
            amp *= mGain;
            freq *= mLacunarity;
        }
    }
   protected void DomainWarpFractalProgressive(Vectors.Vector2 coord) {
        int seed = mSeed;
        float amp = mDomainWarpAmp * mFractalBounding;
        float freq = mFrequency;

        for (int i = 0; i < mOctaves; i++) {
            /*FNLfloat*/
            double xs = coord.x;
            /*FNLfloat*/
            double ys = coord.y;
            switch (mDomainWarpType) {
                case OpenSimplex2:
                case OpenSimplex2Reduced:
                    final double SQRT3 = (double) 1.7320508075688772935274463415059;
                    final double F2 = 0.5f * (SQRT3 - 1);
                    /*FNLfloat*/
                    double t = (xs + ys) * F2;
                    xs += t;
                    ys += t;
                    break;
                default:
                    break;
            }

            DoSingleDomainWarp(seed, amp, freq, xs, ys, coord);

            seed++;
            amp *= mGain;
            freq *= mLacunarity;
        }
    }

}
