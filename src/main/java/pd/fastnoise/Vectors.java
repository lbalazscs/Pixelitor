package pd.fastnoise;

import static pd.fastnoise.FastNoiseLite.*;

public class Vectors {

    public static class Vector2 {
        public double x;
        public double y;

        public Vector2(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class Vector3 {
        public double x;
        public double y;
        public double z;

        public Vector3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    protected static float ValCoord(int seed, int xPrimed, int yPrimed) {
        int hash = Hash(seed, xPrimed, yPrimed);

        hash *= hash;
        hash ^= hash << 19;
        return hash * (1 / 2147483648.0f);
    }

    protected static float ValCoord(int seed, int xPrimed, int yPrimed, int zPrimed) {
        int hash = Hash(seed, xPrimed, yPrimed, zPrimed);

        hash *= hash;
        hash ^= hash << 19;
        return hash * (1 / 2147483648.0f);
    }

    protected static float GradCoord(int seed, int xPrimed, int yPrimed, float xd, float yd) {
        int hash = Hash(seed, xPrimed, yPrimed);
        hash ^= hash >> 15;
        hash &= 127 << 1;

        float xg = Gradients2D[hash];
        float yg = Gradients2D[hash | 1];

        return xd * xg + yd * yg;
    }

    protected static float GradCoord(int seed, int xPrimed, int yPrimed, int zPrimed, float xd, float yd, float zd) {
        int hash = Hash(seed, xPrimed, yPrimed, zPrimed);
        hash ^= hash >> 15;
        hash &= 63 << 2;

        float xg = Gradients3D[hash];
        float yg = Gradients3D[hash | 1];
        float zg = Gradients3D[hash | 2];

        return xd * xg + yd * yg + zd * zg;
    }
}
