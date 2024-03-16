package pd.fastnoise;

public class FastVals {
    protected static float FastMin(float a, float b) {
        return a < b ? a : b;
    }

    protected static float FastMax(float a, float b) {
        return a > b ? a : b;
    }

    protected static float FastAbs(float f) {
        return f < 0 ? -f : f;
    }

    protected static float FastSqrt(float f) {
        return (float) Math.sqrt(f);
    }

    protected static int FastFloor(double f) {
        return f >= 0 ? (int) f : (int) f - 1;
    }

    protected static int FastRound(double f) {
        return f >= 0 ? (int) (f + 0.5f) : (int) (f - 0.5f);
    }
}
