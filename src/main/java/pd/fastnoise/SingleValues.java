package pd.fastnoise;

import static pd.fastnoise.FastNoiseLite.*;
import static pd.fastnoise.FastNoiseLite.CellularReturnType.CellValue;
import static pd.fastnoise.FastNoiseLite.CellularReturnType.Distance;
import static pd.fastnoise.FastNoiseLite.NoiseType.*;
import static pd.fastnoise.FastVals.*;
import static pd.fastnoise.Vectors.GradCoord;
import static pd.fastnoise.Vectors.ValCoord;

public class SingleValues {
    private CellularDistanceFunction mCellularDistanceFunction = CellularDistanceFunction.EuclideanSq;
    private CellularReturnType mCellularReturnType = CellularReturnType.Distance;
    private float mCellularJitterModifier = 1.0f;
    private NoiseType mNoiseType = NoiseType.OpenSimplex2;

    protected static float SingleValue(int seed, double x, double y, double z) {
        int x0 = FastFloor(x);
        int y0 = FastFloor(y);
        int z0 = FastFloor(z);

        float xs = InterpHermite((float) (x - x0));
        float ys = InterpHermite((float) (y - y0));
        float zs = InterpHermite((float) (z - z0));

        x0 *= PrimeX;
        y0 *= PrimeY;
        z0 *= PrimeZ;
        int x1 = x0 + PrimeX;
        int y1 = y0 + PrimeY;
        int z1 = z0 + PrimeZ;

        float xf00 = Lerp(ValCoord(seed, x0, y0, z0), ValCoord(seed, x1, y0, z0), xs);
        float xf10 = Lerp(ValCoord(seed, x0, y1, z0), ValCoord(seed, x1, y1, z0), xs);
        float xf01 = Lerp(ValCoord(seed, x0, y0, z1), ValCoord(seed, x1, y0, z1), xs);
        float xf11 = Lerp(ValCoord(seed, x0, y1, z1), ValCoord(seed, x1, y1, z1), xs);

        float yf0 = Lerp(xf00, xf10, ys);
        float yf1 = Lerp(xf01, xf11, ys);

        return Lerp(yf0, yf1, zs);
    }

     protected static float SingleValue(int seed, double x, double y) {
        int x0 = FastFloor(x);
        int y0 = FastFloor(y);

        float xs = InterpHermite((float) (x - x0));
        float ys = InterpHermite((float) (y - y0));

        x0 *= PrimeX;
        y0 *= PrimeY;
        int x1 = x0 + PrimeX;
        int y1 = y0 + PrimeY;

        float xf0 = Lerp(ValCoord(seed, x0, y0), ValCoord(seed, x1, y0), xs);
        float xf1 = Lerp(ValCoord(seed, x0, y1), ValCoord(seed, x1, y1), xs);

        return Lerp(xf0, xf1, ys);
    }

    protected float GenNoiseSingle(int seed, double x, double y) {
        return switch (mNoiseType) {
            case OpenSimplex2 -> SingleSimplex(seed, x, y);
            case OpenSimplex2S -> SingleOpenSimplex2S(seed, x, y);
            case Cellular -> SingleCellular(seed, x, y);
            case Perlin -> SinglePerlin(seed, x, y);
            case ValueCubic -> SingleValueCubic(seed, x, y);
            case Value -> SingleValue(seed, x, y);
            default -> 0;
        };
    }


   protected float SingleCellular(int seed, double x, double y) {
        int xr = FastRound(x);
        int yr = FastRound(y);

        float distance0 = Float.MAX_VALUE;
        float distance1 = Float.MAX_VALUE;
        int closestHash = 0;

        float cellularJitter = 0.43701595f * mCellularJitterModifier;

        int xPrimed = (xr - 1) * PrimeX;
        int yPrimedBase = (yr - 1) * PrimeY;

       switch (mCellularDistanceFunction) {
           default -> {
               for (int xi = xr - 1; xi <= xr + 1; xi++) {
                   int yPrimed = yPrimedBase;

                   for (int yi = yr - 1; yi <= yr + 1; yi++) {
                       int hash = Hash(seed, xPrimed, yPrimed);
                       int idx = hash & (255 << 1);

                       float vecX = (float) (xi - x) + RandVecs2D[idx] * cellularJitter;
                       float vecY = (float) (yi - y) + RandVecs2D[idx | 1] * cellularJitter;

                       float newDistance = vecX * vecX + vecY * vecY;

                       distance1 = FastMax(FastMin(distance1, newDistance), distance0);
                       if (newDistance < distance0) {
                           distance0 = newDistance;
                           closestHash = hash;
                       }
                       yPrimed += PrimeY;
                   }
                   xPrimed += PrimeX;
               }
           }
           case Manhattan -> {
               for (int xi = xr - 1; xi <= xr + 1; xi++) {
                   int yPrimed = yPrimedBase;

                   for (int yi = yr - 1; yi <= yr + 1; yi++) {
                       int hash = Hash(seed, xPrimed, yPrimed);
                       int idx = hash & (255 << 1);

                       float vecX = (float) (xi - x) + RandVecs2D[idx] * cellularJitter;
                       float vecY = (float) (yi - y) + RandVecs2D[idx | 1] * cellularJitter;

                       float newDistance = FastAbs(vecX) + FastAbs(vecY);

                       distance1 = FastMax(FastMin(distance1, newDistance), distance0);
                       if (newDistance < distance0) {
                           distance0 = newDistance;
                           closestHash = hash;
                       }
                       yPrimed += PrimeY;
                   }
                   xPrimed += PrimeX;
               }
           }
           case Hybrid -> {
               for (int xi = xr - 1; xi <= xr + 1; xi++) {
                   int yPrimed = yPrimedBase;

                   for (int yi = yr - 1; yi <= yr + 1; yi++) {
                       int hash = Hash(seed, xPrimed, yPrimed);
                       int idx = hash & (255 << 1);

                       float vecX = (float) (xi - x) + RandVecs2D[idx] * cellularJitter;
                       float vecY = (float) (yi - y) + RandVecs2D[idx | 1] * cellularJitter;

                       float newDistance = (FastAbs(vecX) + FastAbs(vecY)) + (vecX * vecX + vecY * vecY);

                       distance1 = FastMax(FastMin(distance1, newDistance), distance0);
                       if (newDistance < distance0) {
                           distance0 = newDistance;
                           closestHash = hash;
                       }
                       yPrimed += PrimeY;
                   }
                   xPrimed += PrimeX;
               }
           }
       }

        if (mCellularDistanceFunction == CellularDistanceFunction.Euclidean && mCellularReturnType != CellValue) {
            distance0 = FastSqrt(distance0);

            if (mCellularReturnType != Distance) {
                distance1 = FastSqrt(distance1);
            }
        }

        return switch (mCellularReturnType) {
            case CellValue -> closestHash * (1 / 2147483648.0f);
            case Distance -> distance0 - 1;
            case Distance2 -> distance1 - 1;
            case Distance2Add -> (distance1 + distance0) * 0.5f - 1;
            case Distance2Sub -> distance1 - distance0 - 1;
            case Distance2Mul -> distance1 * distance0 * 0.5f - 1;
            case Distance2Div -> distance0 / distance1 - 1;
            default -> 0;
        };
    }

     protected float SingleCellular(int seed, double x, double y, double z) {
        int xr = FastRound(x);
        int yr = FastRound(y);
        int zr = FastRound(z);

        float distance0 = Float.MAX_VALUE;
        float distance1 = Float.MAX_VALUE;
        int closestHash = 0;

        float cellularJitter = 0.39614353f * mCellularJitterModifier;

        int xPrimed = (xr - 1) * PrimeX;
        int yPrimedBase = (yr - 1) * PrimeY;
        int zPrimedBase = (zr - 1) * PrimeZ;

        switch (mCellularDistanceFunction) {
            case Euclidean:
            case EuclideanSq:
                for (int xi = xr - 1; xi <= xr + 1; xi++) {
                    int yPrimed = yPrimedBase;

                    for (int yi = yr - 1; yi <= yr + 1; yi++) {
                        int zPrimed = zPrimedBase;

                        for (int zi = zr - 1; zi <= zr + 1; zi++) {
                            int hash = Hash(seed, xPrimed, yPrimed, zPrimed);
                            int idx = hash & (255 << 2);

                            float vecX = (float) (xi - x) + RandVecs3D[idx] * cellularJitter;
                            float vecY = (float) (yi - y) + RandVecs3D[idx | 1] * cellularJitter;
                            float vecZ = (float) (zi - z) + RandVecs3D[idx | 2] * cellularJitter;

                            float newDistance = vecX * vecX + vecY * vecY + vecZ * vecZ;

                            distance1 = FastMax(FastMin(distance1, newDistance), distance0);
                            if (newDistance < distance0) {
                                distance0 = newDistance;
                                closestHash = hash;
                            }
                            zPrimed += PrimeZ;
                        }
                        yPrimed += PrimeY;
                    }
                    xPrimed += PrimeX;
                }
                break;
            case Manhattan:
                for (int xi = xr - 1; xi <= xr + 1; xi++) {
                    int yPrimed = yPrimedBase;

                    for (int yi = yr - 1; yi <= yr + 1; yi++) {
                        int zPrimed = zPrimedBase;

                        for (int zi = zr - 1; zi <= zr + 1; zi++) {
                            int hash = Hash(seed, xPrimed, yPrimed, zPrimed);
                            int idx = hash & (255 << 2);

                            float vecX = (float) (xi - x) + RandVecs3D[idx] * cellularJitter;
                            float vecY = (float) (yi - y) + RandVecs3D[idx | 1] * cellularJitter;
                            float vecZ = (float) (zi - z) + RandVecs3D[idx | 2] * cellularJitter;

                            float newDistance = FastAbs(vecX) + FastAbs(vecY) + FastAbs(vecZ);

                            distance1 = FastMax(FastMin(distance1, newDistance), distance0);
                            if (newDistance < distance0) {
                                distance0 = newDistance;
                                closestHash = hash;
                            }
                            zPrimed += PrimeZ;
                        }
                        yPrimed += PrimeY;
                    }
                    xPrimed += PrimeX;
                }
                break;
            case Hybrid:
                for (int xi = xr - 1; xi <= xr + 1; xi++) {
                    int yPrimed = yPrimedBase;

                    for (int yi = yr - 1; yi <= yr + 1; yi++) {
                        int zPrimed = zPrimedBase;

                        for (int zi = zr - 1; zi <= zr + 1; zi++) {
                            int hash = Hash(seed, xPrimed, yPrimed, zPrimed);
                            int idx = hash & (255 << 2);

                            float vecX = (float) (xi - x) + RandVecs3D[idx] * cellularJitter;
                            float vecY = (float) (yi - y) + RandVecs3D[idx | 1] * cellularJitter;
                            float vecZ = (float) (zi - z) + RandVecs3D[idx | 2] * cellularJitter;

                            float newDistance = (FastAbs(vecX) + FastAbs(vecY) + FastAbs(vecZ)) + (vecX * vecX + vecY * vecY + vecZ * vecZ);

                            distance1 = FastMax(FastMin(distance1, newDistance), distance0);
                            if (newDistance < distance0) {
                                distance0 = newDistance;
                                closestHash = hash;
                            }
                            zPrimed += PrimeZ;
                        }
                        yPrimed += PrimeY;
                    }
                    xPrimed += PrimeX;
                }
                break;
            default:
                break;
        }

        if (mCellularDistanceFunction == CellularDistanceFunction.Euclidean && mCellularReturnType != CellValue) {
            distance0 = FastSqrt(distance0);

            if (mCellularReturnType != Distance) {
                distance1 = FastSqrt(distance1);
            }
        }

        return switch (mCellularReturnType) {
            case CellValue -> closestHash * (1 / 2147483648.0f);
            case Distance -> distance0 - 1;
            case Distance2 -> distance1 - 1;
            case Distance2Add -> (distance1 + distance0) * 0.5f - 1;
            case Distance2Sub -> distance1 - distance0 - 1;
            case Distance2Mul -> distance1 * distance0 * 0.5f - 1;
            case Distance2Div -> distance0 / distance1 - 1;
            default -> 0;
        };
    }

    // Simplex/OpenSimplex2 Noise

    protected static float SingleSimplex(int seed, double x, double y) {
        // 2D OpenSimplex2 case uses the same algorithm as ordinary Simplex.

        final float SQRT3 = 1.7320508075688772935274463415059f;
        final float G2 = (3 - SQRT3) / 6;

        /*
         * --- Skew moved to switch statements before fractal evaluation ---
         * final FNLfloat F2 = 0.5f * (SQRT3 - 1);
         * FNLfloat s = (x + y) * F2;
         * x += s; y += s;
         */

        int i = FastFloor(x);
        int j = FastFloor(y);
        float xi = (float) (x - i);
        float yi = (float) (y - j);

        float t = (xi + yi) * G2;
        float x0 = xi - t;
        float y0 = yi - t;

        i *= PrimeX;
        j *= PrimeY;

        float n0, n1, n2;

        float a = 0.5f - x0 * x0 - y0 * y0;
        if (a <= 0) {
            n0 = 0;
        } else {
            n0 = (a * a) * (a * a) * GradCoord(seed, i, j, x0, y0);
        }

        float c = 2 * (1 - 2 * G2) * (1 / G2 - 2) * t + ((-2 * (1 - 2 * G2) * (1 - 2 * G2)) + a);
        if (c <= 0) {
            n2 = 0;
        } else {
            float x2 = x0 + (2 * G2 - 1);
            float y2 = y0 + (2 * G2 - 1);
            n2 = (c * c) * (c * c) * GradCoord(seed, i + PrimeX, j + PrimeY, x2, y2);
        }

        if (y0 > x0) {
            float x1 = x0 + G2;
            float y1 = y0 + (G2 - 1);
            float b = 0.5f - x1 * x1 - y1 * y1;
            if (b <= 0) {
                n1 = 0;
            } else {
                n1 = (b * b) * (b * b) * GradCoord(seed, i, j + PrimeY, x1, y1);
            }
        } else {
            float x1 = x0 + (G2 - 1);
            float y1 = y0 + G2;
            float b = 0.5f - x1 * x1 - y1 * y1;
            if (b <= 0) {
                n1 = 0;
            } else {
                n1 = (b * b) * (b * b) * GradCoord(seed, i + PrimeX, j, x1, y1);
            }
        }

        return (n0 + n1 + n2) * 99.83685446303647f;
    }

     protected static float SingleOpenSimplex2(int seed, double x, double y, double z) {
        // 3D OpenSimplex2 case uses two offset rotated cube grids.

        /*
         * --- Rotation moved to switch statements before fractal evaluation ---
         * final FNLfloat R3 = (FNLfloat)(2.0 / 3.0);
         * FNLfloat r = (x + y + z) * R3; // Rotation, not skew
         * x = r - x; y = r - y; z = r - z;
         */

        int i = FastRound(x);
        int j = FastRound(y);
        int k = FastRound(z);
        float x0 = (float) (x - i);
        float y0 = (float) (y - j);
        float z0 = (float) (z - k);

        int xNSign = (int) (-1.0f - x0) | 1;
        int yNSign = (int) (-1.0f - y0) | 1;
        int zNSign = (int) (-1.0f - z0) | 1;

        float ax0 = xNSign * -x0;
        float ay0 = yNSign * -y0;
        float az0 = zNSign * -z0;

        i *= PrimeX;
        j *= PrimeY;
        k *= PrimeZ;

        float value = 0;
        float a = (0.6f - x0 * x0) - (y0 * y0 + z0 * z0);

        for (int l = 0; ; l++) {
            if (a > 0) {
                value += (a * a) * (a * a) * GradCoord(seed, i, j, k, x0, y0, z0);
            }

            if (ax0 >= ay0 && ax0 >= az0) {
                float b = a + ax0 + ax0;
                if (b > 1) {
                    b -= 1;
                    value += (b * b) * (b * b) * GradCoord(seed, i - xNSign * PrimeX, j, k, x0 + xNSign, y0, z0);
                }
            } else if (ay0 > ax0 && ay0 >= az0) {
                float b = a + ay0 + ay0;
                if (b > 1) {
                    b -= 1;
                    value += (b * b) * (b * b) * GradCoord(seed, i, j - yNSign * PrimeY, k, x0, y0 + yNSign, z0);
                }
            } else {
                float b = a + az0 + az0;
                if (b > 1) {
                    b -= 1;
                    value += (b * b) * (b * b) * GradCoord(seed, i, j, k - zNSign * PrimeZ, x0, y0, z0 + zNSign);
                }
            }

            if (l == 1) {
                break;
            }

            ax0 = 0.5f - ax0;
            ay0 = 0.5f - ay0;
            az0 = 0.5f - az0;

            x0 = xNSign * ax0;
            y0 = yNSign * ay0;
            z0 = zNSign * az0;

            a += (0.75f - ax0) - (ay0 + az0);

            i += (xNSign >> 1) & PrimeX;
            j += (yNSign >> 1) & PrimeY;
            k += (zNSign >> 1) & PrimeZ;

            xNSign = -xNSign;
            yNSign = -yNSign;
            zNSign = -zNSign;

            seed = ~seed;
        }

        return value * 32.69428253173828125f;
    }


    // OpenSimplex2S Noise

  protected static float SingleOpenSimplex2S(int seed, double x, double y) {
        // 2D OpenSimplex2S case is a modified 2D simplex noise.

        final double SQRT3 = 1.7320508075688772935274463415059;
        final double G2 = (3 - SQRT3) / 6;

        /*
         * --- Skew moved to TransformNoiseCoordinate method ---
         * final FNLfloat F2 = 0.5f * (SQRT3 - 1);
         * FNLfloat s = (x + y) * F2;
         * x += s; y += s;
         */

        int i = FastFloor(x);
        int j = FastFloor(y);
        float xi = (float) (x - i);
        float yi = (float) (y - j);

        i *= PrimeX;
        j *= PrimeY;
        int i1 = i + PrimeX;
        int j1 = j + PrimeY;

        float t = (xi + yi) * (float) G2;
        float x0 = xi - t;
        float y0 = yi - t;

        float a0 = (2.0f / 3.0f) - x0 * x0 - y0 * y0;
        float value = (a0 * a0) * (a0 * a0) * GradCoord(seed, i, j, x0, y0);

        float a1 = (float) (2 * (1 - 2 * G2) * (1 / G2 - 2)) * t + ((float) (-2 * (1 - 2 * G2) * (1 - 2 * G2)) + a0);
        float x1 = x0 - (float) (1 - 2 * G2);
        float y1 = y0 - (float) (1 - 2 * G2);
        value += (a1 * a1) * (a1 * a1) * GradCoord(seed, i1, j1, x1, y1);

        // Nested conditionals were faster than compact bit logic/arithmetic.
        float xmyi = xi - yi;
        if (t > G2) {
            if (xi + xmyi > 1) {
                float x2 = x0 + (float) (3 * G2 - 2);
                float y2 = y0 + (float) (3 * G2 - 1);
                float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
                if (a2 > 0) {
                    value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i + (PrimeX << 1), j + PrimeY, x2, y2);
                }
            } else {
                float x2 = x0 + (float) G2;
                float y2 = y0 + (float) (G2 - 1);
                float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
                if (a2 > 0) {
                    value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i, j + PrimeY, x2, y2);
                }
            }

            if (yi - xmyi > 1) {
                float x3 = x0 + (float) (3 * G2 - 1);
                float y3 = y0 + (float) (3 * G2 - 2);
                float a3 = (2.0f / 3.0f) - x3 * x3 - y3 * y3;
                if (a3 > 0) {
                    value += (a3 * a3) * (a3 * a3) * GradCoord(seed, i + PrimeX, j + (PrimeY << 1), x3, y3);
                }
            } else {
                float x3 = x0 + (float) (G2 - 1);
                float y3 = y0 + (float) G2;
                float a3 = (2.0f / 3.0f) - x3 * x3 - y3 * y3;
                if (a3 > 0) {
                    value += (a3 * a3) * (a3 * a3) * GradCoord(seed, i + PrimeX, j, x3, y3);
                }
            }
        } else {
            if (xi + xmyi < 0) {
                float x2 = x0 + (float) (1 - G2);
                float y2 = y0 - (float) G2;
                float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
                if (a2 > 0) {
                    value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i - PrimeX, j, x2, y2);
                }
            } else {
                float x2 = x0 + (float) (G2 - 1);
                float y2 = y0 + (float) G2;
                float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
                if (a2 > 0) {
                    value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i + PrimeX, j, x2, y2);
                }
            }

            if (yi < xmyi) {
                float x2 = x0 - (float) G2;
                float y2 = y0 - (float) (G2 - 1);
                float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
                if (a2 > 0) {
                    value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i, j - PrimeY, x2, y2);
                }
            } else {
                float x2 = x0 + (float) G2;
                float y2 = y0 + (float) (G2 - 1);
                float a2 = (2.0f / 3.0f) - x2 * x2 - y2 * y2;
                if (a2 > 0) {
                    value += (a2 * a2) * (a2 * a2) * GradCoord(seed, i, j + PrimeY, x2, y2);
                }
            }
        }

        return value * 18.24196194486065f;
    }

    protected static float SingleOpenSimplex2S(int seed, double x, double y, double z) {
        // 3D OpenSimplex2S case uses two offset rotated cube grids.

        /*
         * --- Rotation moved to TransformNoiseCoordinate method ---
         * final FNLfloat R3 = (FNLfloat)(2.0 / 3.0);
         * FNLfloat r = (x + y + z) * R3; // Rotation, not skew
         * x = r - x; y = r - y; z = r - z;
         */

        int i = FastFloor(x);
        int j = FastFloor(y);
        int k = FastFloor(z);
        float xi = (float) (x - i);
        float yi = (float) (y - j);
        float zi = (float) (z - k);

        i *= PrimeX;
        j *= PrimeY;
        k *= PrimeZ;
        int seed2 = seed + 1293373;

        int xNMask = (int) (-0.5f - xi);
        int yNMask = (int) (-0.5f - yi);
        int zNMask = (int) (-0.5f - zi);

        float x0 = xi + xNMask;
        float y0 = yi + yNMask;
        float z0 = zi + zNMask;
        float a0 = 0.75f - x0 * x0 - y0 * y0 - z0 * z0;
        float value = (a0 * a0) * (a0 * a0) * GradCoord(seed,
                i + (xNMask & PrimeX), j + (yNMask & PrimeY), k + (zNMask & PrimeZ), x0, y0, z0);

        float x1 = xi - 0.5f;
        float y1 = yi - 0.5f;
        float z1 = zi - 0.5f;
        float a1 = 0.75f - x1 * x1 - y1 * y1 - z1 * z1;
        value += (a1 * a1) * (a1 * a1) * GradCoord(seed2,
                i + PrimeX, j + PrimeY, k + PrimeZ, x1, y1, z1);

        float xAFlipMask0 = ((xNMask | 1) << 1) * x1;
        float yAFlipMask0 = ((yNMask | 1) << 1) * y1;
        float zAFlipMask0 = ((zNMask | 1) << 1) * z1;
        float xAFlipMask1 = (-2 - (xNMask << 2)) * x1 - 1.0f;
        float yAFlipMask1 = (-2 - (yNMask << 2)) * y1 - 1.0f;
        float zAFlipMask1 = (-2 - (zNMask << 2)) * z1 - 1.0f;

        boolean skip5 = false;
        float a2 = xAFlipMask0 + a0;
        if (a2 > 0) {
            float x2 = x0 - (xNMask | 1);
            float y2 = y0;
            float z2 = z0;
            value += (a2 * a2) * (a2 * a2) * GradCoord(seed,
                    i + (~xNMask & PrimeX), j + (yNMask & PrimeY), k + (zNMask & PrimeZ), x2, y2, z2);
        } else {
            float a3 = yAFlipMask0 + zAFlipMask0 + a0;
            if (a3 > 0) {
                float x3 = x0;
                float y3 = y0 - (yNMask | 1);
                float z3 = z0 - (zNMask | 1);
                value += (a3 * a3) * (a3 * a3) * GradCoord(seed,
                        i + (xNMask & PrimeX), j + (~yNMask & PrimeY), k + (~zNMask & PrimeZ), x3, y3, z3);
            }

            float a4 = xAFlipMask1 + a1;
            if (a4 > 0) {
                float x4 = (xNMask | 1) + x1;
                float y4 = y1;
                float z4 = z1;
                value += (a4 * a4) * (a4 * a4) * GradCoord(seed2,
                        i + (xNMask & (PrimeX * 2)), j + PrimeY, k + PrimeZ, x4, y4, z4);
                skip5 = true;
            }
        }

        boolean skip9 = false;
        float a6 = yAFlipMask0 + a0;
        if (a6 > 0) {
            float x6 = x0;
            float y6 = y0 - (yNMask | 1);
            float z6 = z0;
            value += (a6 * a6) * (a6 * a6) * GradCoord(seed,
                    i + (xNMask & PrimeX), j + (~yNMask & PrimeY), k + (zNMask & PrimeZ), x6, y6, z6);
        } else {
            float a7 = xAFlipMask0 + zAFlipMask0 + a0;
            if (a7 > 0) {
                float x7 = x0 - (xNMask | 1);
                float y7 = y0;
                float z7 = z0 - (zNMask | 1);
                value += (a7 * a7) * (a7 * a7) * GradCoord(seed,
                        i + (~xNMask & PrimeX), j + (yNMask & PrimeY), k + (~zNMask & PrimeZ), x7, y7, z7);
            }

            float a8 = yAFlipMask1 + a1;
            if (a8 > 0) {
                float x8 = x1;
                float y8 = (yNMask | 1) + y1;
                float z8 = z1;
                value += (a8 * a8) * (a8 * a8) * GradCoord(seed2,
                        i + PrimeX, j + (yNMask & (PrimeY << 1)), k + PrimeZ, x8, y8, z8);
                skip9 = true;
            }
        }

        boolean skipD = false;
        float aA = zAFlipMask0 + a0;
        if (aA > 0) {
            float xA = x0;
            float yA = y0;
            float zA = z0 - (zNMask | 1);
            value += (aA * aA) * (aA * aA) * GradCoord(seed,
                    i + (xNMask & PrimeX), j + (yNMask & PrimeY), k + (~zNMask & PrimeZ), xA, yA, zA);
        } else {
            float aB = xAFlipMask0 + yAFlipMask0 + a0;
            if (aB > 0) {
                float xB = x0 - (xNMask | 1);
                float yB = y0 - (yNMask | 1);
                float zB = z0;
                value += (aB * aB) * (aB * aB) * GradCoord(seed,
                        i + (~xNMask & PrimeX), j + (~yNMask & PrimeY), k + (zNMask & PrimeZ), xB, yB, zB);
            }

            float aC = zAFlipMask1 + a1;
            if (aC > 0) {
                float xC = x1;
                float yC = y1;
                float zC = (zNMask | 1) + z1;
                value += (aC * aC) * (aC * aC) * GradCoord(seed2,
                        i + PrimeX, j + PrimeY, k + (zNMask & (PrimeZ << 1)), xC, yC, zC);
                skipD = true;
            }
        }

        if (!skip5) {
            float a5 = yAFlipMask1 + zAFlipMask1 + a1;
            if (a5 > 0) {
                float x5 = x1;
                float y5 = (yNMask | 1) + y1;
                float z5 = (zNMask | 1) + z1;
                value += (a5 * a5) * (a5 * a5) * GradCoord(seed2,
                        i + PrimeX, j + (yNMask & (PrimeY << 1)), k + (zNMask & (PrimeZ << 1)), x5, y5, z5);
            }
        }

        if (!skip9) {
            float a9 = xAFlipMask1 + zAFlipMask1 + a1;
            if (a9 > 0) {
                float x9 = (xNMask | 1) + x1;
                float y9 = y1;
                float z9 = (zNMask | 1) + z1;
                value += (a9 * a9) * (a9 * a9) * GradCoord(seed2,
                        i + (xNMask & (PrimeX * 2)), j + PrimeY, k + (zNMask & (PrimeZ << 1)), x9, y9, z9);
            }
        }

        if (!skipD) {
            float aD = xAFlipMask1 + yAFlipMask1 + a1;
            if (aD > 0) {
                float xD = (xNMask | 1) + x1;
                float yD = (yNMask | 1) + y1;
                float zD = z1;
                value += (aD * aD) * (aD * aD) * GradCoord(seed2,
                        i + (xNMask & (PrimeX << 1)), j + (yNMask & (PrimeY << 1)), k + PrimeZ, xD, yD, zD);
            }
        }

        return value * 9.046026385208288f;
    }

    protected static float SinglePerlin(int seed, double x, double y) {
        int x0 = FastFloor(x);
        int y0 = FastFloor(y);

        float xd0 = (float) (x - x0);
        float yd0 = (float) (y - y0);
        float xd1 = xd0 - 1;
        float yd1 = yd0 - 1;

        float xs = InterpQuintic(xd0);
        float ys = InterpQuintic(yd0);

        x0 *= PrimeX;
        y0 *= PrimeY;
        int x1 = x0 + PrimeX;
        int y1 = y0 + PrimeY;

        float xf0 = Lerp(GradCoord(seed, x0, y0, xd0, yd0), GradCoord(seed, x1, y0, xd1, yd0), xs);
        float xf1 = Lerp(GradCoord(seed, x0, y1, xd0, yd1), GradCoord(seed, x1, y1, xd1, yd1), xs);

        return Lerp(xf0, xf1, ys) * 1.4247691104677813f;
    }

     protected static float SinglePerlin(int seed, double x, double y, double z) {
        int x0 = FastFloor(x);
        int y0 = FastFloor(y);
        int z0 = FastFloor(z);

        float xd0 = (float) (x - x0);
        float yd0 = (float) (y - y0);
        float zd0 = (float) (z - z0);
        float xd1 = xd0 - 1;
        float yd1 = yd0 - 1;
        float zd1 = zd0 - 1;

        float xs = InterpQuintic(xd0);
        float ys = InterpQuintic(yd0);
        float zs = InterpQuintic(zd0);

        x0 *= PrimeX;
        y0 *= PrimeY;
        z0 *= PrimeZ;
        int x1 = x0 + PrimeX;
        int y1 = y0 + PrimeY;
        int z1 = z0 + PrimeZ;

        float xf00 = Lerp(GradCoord(seed, x0, y0, z0, xd0, yd0, zd0), GradCoord(seed, x1, y0, z0, xd1, yd0, zd0), xs);
        float xf10 = Lerp(GradCoord(seed, x0, y1, z0, xd0, yd1, zd0), GradCoord(seed, x1, y1, z0, xd1, yd1, zd0), xs);
        float xf01 = Lerp(GradCoord(seed, x0, y0, z1, xd0, yd0, zd1), GradCoord(seed, x1, y0, z1, xd1, yd0, zd1), xs);
        float xf11 = Lerp(GradCoord(seed, x0, y1, z1, xd0, yd1, zd1), GradCoord(seed, x1, y1, z1, xd1, yd1, zd1), xs);

        float yf0 = Lerp(xf00, xf10, ys);
        float yf1 = Lerp(xf01, xf11, ys);

        return Lerp(yf0, yf1, zs) * 0.964921414852142333984375f;
    }


    // Value Cubic Noise

    protected static float SingleValueCubic(int seed, double x, double y) {
        int x1 = FastFloor(x);
        int y1 = FastFloor(y);

        float xs = (float) (x - x1);
        float ys = (float) (y - y1);

        x1 *= PrimeX;
        y1 *= PrimeY;
        int x0 = x1 - PrimeX;
        int y0 = y1 - PrimeY;
        int x2 = x1 + PrimeX;
        int y2 = y1 + PrimeY;
        int x3 = x1 + (PrimeX << 1);
        int y3 = y1 + (PrimeY << 1);

        return CubicLerp(
                CubicLerp(ValCoord(seed, x0, y0), ValCoord(seed, x1, y0), ValCoord(seed, x2, y0), ValCoord(seed, x3, y0),
                        xs),
                CubicLerp(ValCoord(seed, x0, y1), ValCoord(seed, x1, y1), ValCoord(seed, x2, y1), ValCoord(seed, x3, y1),
                        xs),
                CubicLerp(ValCoord(seed, x0, y2), ValCoord(seed, x1, y2), ValCoord(seed, x2, y2), ValCoord(seed, x3, y2),
                        xs),
                CubicLerp(ValCoord(seed, x0, y3), ValCoord(seed, x1, y3), ValCoord(seed, x2, y3), ValCoord(seed, x3, y3),
                        xs),
                ys) * (1 / (1.5f * 1.5f));
    }

   protected static float SingleValueCubic(int seed, double x, double y, double z) {
        int x1 = FastFloor(x);
        int y1 = FastFloor(y);
        int z1 = FastFloor(z);

        float xs = (float) (x - x1);
        float ys = (float) (y - y1);
        float zs = (float) (z - z1);

        x1 *= PrimeX;
        y1 *= PrimeY;
        z1 *= PrimeZ;

        int x0 = x1 - PrimeX;
        int y0 = y1 - PrimeY;
        int z0 = z1 - PrimeZ;
        int x2 = x1 + PrimeX;
        int y2 = y1 + PrimeY;
        int z2 = z1 + PrimeZ;
        int x3 = x1 + (PrimeX << 1);
        int y3 = y1 + (PrimeY << 1);
        int z3 = z1 + (PrimeZ << 1);


        return CubicLerp(
                CubicLerp(
                        CubicLerp(ValCoord(seed, x0, y0, z0), ValCoord(seed, x1, y0, z0), ValCoord(seed, x2, y0, z0), ValCoord(seed, x3, y0, z0), xs),
                        CubicLerp(ValCoord(seed, x0, y1, z0), ValCoord(seed, x1, y1, z0), ValCoord(seed, x2, y1, z0), ValCoord(seed, x3, y1, z0), xs),
                        CubicLerp(ValCoord(seed, x0, y2, z0), ValCoord(seed, x1, y2, z0), ValCoord(seed, x2, y2, z0), ValCoord(seed, x3, y2, z0), xs),
                        CubicLerp(ValCoord(seed, x0, y3, z0), ValCoord(seed, x1, y3, z0), ValCoord(seed, x2, y3, z0), ValCoord(seed, x3, y3, z0), xs),
                        ys),
                CubicLerp(
                        CubicLerp(ValCoord(seed, x0, y0, z1), ValCoord(seed, x1, y0, z1), ValCoord(seed, x2, y0, z1), ValCoord(seed, x3, y0, z1), xs),
                        CubicLerp(ValCoord(seed, x0, y1, z1), ValCoord(seed, x1, y1, z1), ValCoord(seed, x2, y1, z1), ValCoord(seed, x3, y1, z1), xs),
                        CubicLerp(ValCoord(seed, x0, y2, z1), ValCoord(seed, x1, y2, z1), ValCoord(seed, x2, y2, z1), ValCoord(seed, x3, y2, z1), xs),
                        CubicLerp(ValCoord(seed, x0, y3, z1), ValCoord(seed, x1, y3, z1), ValCoord(seed, x2, y3, z1), ValCoord(seed, x3, y3, z1), xs),
                        ys),
                CubicLerp(
                        CubicLerp(ValCoord(seed, x0, y0, z2), ValCoord(seed, x1, y0, z2), ValCoord(seed, x2, y0, z2), ValCoord(seed, x3, y0, z2), xs),
                        CubicLerp(ValCoord(seed, x0, y1, z2), ValCoord(seed, x1, y1, z2), ValCoord(seed, x2, y1, z2), ValCoord(seed, x3, y1, z2), xs),
                        CubicLerp(ValCoord(seed, x0, y2, z2), ValCoord(seed, x1, y2, z2), ValCoord(seed, x2, y2, z2), ValCoord(seed, x3, y2, z2), xs),
                        CubicLerp(ValCoord(seed, x0, y3, z2), ValCoord(seed, x1, y3, z2), ValCoord(seed, x2, y3, z2), ValCoord(seed, x3, y3, z2), xs),
                        ys),
                CubicLerp(
                        CubicLerp(ValCoord(seed, x0, y0, z3), ValCoord(seed, x1, y0, z3), ValCoord(seed, x2, y0, z3), ValCoord(seed, x3, y0, z3), xs),
                        CubicLerp(ValCoord(seed, x0, y1, z3), ValCoord(seed, x1, y1, z3), ValCoord(seed, x2, y1, z3), ValCoord(seed, x3, y1, z3), xs),
                        CubicLerp(ValCoord(seed, x0, y2, z3), ValCoord(seed, x1, y2, z3), ValCoord(seed, x2, y2, z3), ValCoord(seed, x3, y2, z3), xs),
                        CubicLerp(ValCoord(seed, x0, y3, z3), ValCoord(seed, x1, y3, z3), ValCoord(seed, x2, y3, z3), ValCoord(seed, x3, y3, z3), xs),
                        ys),
                zs) * (1 / (1.5f * 1.5f * 1.5f));
    }

}
