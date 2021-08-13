package pixelitor.utils;

import pd.OpenSimplex2F;

import static net.jafama.FastMath.cos;
import static net.jafama.FastMath.sin;

public class Forces {

    public static Vector2D createSinkForce(Vector2D position, Vector2D center, float magnitude, Vector2D out) {
        if (magnitude == 0) return out;

        out.set(center);
        out.subtract(position);
        out.setMagnitude(magnitude);

        return out;
    }

    public static Vector2D createRevolveForce(Vector2D position, Vector2D center, float magnitude, Vector2D out) {
        if (magnitude == 0) return out;

        out.set(center);
        out.subtract(position);
        out.set(-out.y, +out.x);
        out.setMagnitude(magnitude);
        out.normalizeIfNonzero();
        out.scale(magnitude);

        return out;
    }

    public static Vector2D createFlowFieldForce(float magnitude, float initTheta, float variantPI, double sampleX, double sampleY, double sampleZ, int turbulence, OpenSimplex2F noise, Vector2D out) {
        double value = initTheta + noise.turbulence3(sampleX, sampleY, sampleZ, turbulence) * variantPI;
        out.set((float) cos(value), (float) sin(value));
        out.setMagnitude(magnitude);
        return out;
    }

}
