package cn.zbx1425.mtrsteamloco.render;

public class Oklch {

    private static final double C_MAX = 0.4;
    private static final double H_MAX = 360.0;

    private Oklch() {

    }

    public static void srgbToOklchFF(float[] color) {
        double r = color[0];
        double g = color[1];
        double b = color[2];
        r = (r <= 0.04045) ? (r / 12.92) : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.04045) ? (g / 12.92) : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.04045) ? (b / 12.92) : Math.pow((b + 0.055) / 1.055, 2.4);
        double l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b;
        double m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b;
        double s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b;
        double l_ = Math.cbrt(l);
        double m_ = Math.cbrt(m);
        double s_ = Math.cbrt(s);
        double L = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_;
        double a = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_;
        double b_ok = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_;
        double C = Math.sqrt(a * a + b_ok * b_ok);
        double H = Math.toDegrees(Math.atan2(b_ok, a));

        if (H < 0.0) {
            H += 360.0;
        } else if (H >= 360.0) {
            H -= 360.0;
        }
        if (C < 1e-8) {
            H = 0.0;
        }

        color[0] = (float) L;
        color[1] = (float) C;
        color[2] = (float) H;
    }

    public static void oklchToSrgbFF(float[] color) {
        double L = color[0];
        double C = color[1];
        double H = color[2];
        double hRad = Math.toRadians(H);
        double a = C * Math.cos(hRad);
        double b_ok = C * Math.sin(hRad);
        double l_ = L + 0.3963377774 * a + 0.2158037573 * b_ok;
        double m_ = L - 0.1055613458 * a - 0.0638541728 * b_ok;
        double s_ = L - 0.0894841775 * a - 1.2914855480 * b_ok;
        double l = l_ * l_ * l_;
        double m = m_ * m_ * m_;
        double s = s_ * s_ * s_;
        double rL = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
        double gL = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
        double bL = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;
        double r = (rL <= 0.0031308) ? (12.92 * rL) : (1.055 * Math.pow(rL, 1.0 / 2.4) - 0.055);
        double g = (gL <= 0.0031308) ? (12.92 * gL) : (1.055 * Math.pow(gL, 1.0 / 2.4) - 0.055);
        double b = (bL <= 0.0031308) ? (12.92 * bL) : (1.055 * Math.pow(bL, 1.0 / 2.4) - 0.055);
        color[0] = (float) Math.clamp(r, 0.0, 1.0);
        color[1] = (float) Math.clamp(g, 0.0, 1.0);
        color[2] = (float) Math.clamp(b, 0.0, 1.0);
    }

    public static int srgbToOklchII(int rgb) {
        float[] color = {
            ((rgb >> 16) & 0xFF) / 255.0f,
            ((rgb >> 8) & 0xFF) / 255.0f,
            (rgb & 0xFF) / 255.0f
        };

        srgbToOklchFF(color);

        int lInt = (int) Math.round(color[0] * 255.0);
        int cInt = (int) Math.round((color[1] / C_MAX) * 255.0);
        int hInt = (int) Math.round((color[2] / H_MAX) * 255.0);

        lInt = Math.clamp(lInt, 0, 255);
        cInt = Math.clamp(cInt, 0, 255);
        hInt = Math.clamp(hInt, 0, 255);

        return (lInt << 16) | (cInt << 8) | hInt;
    }

    public static int oklchToSrgbII(int lch) {
        float[] color = {
            ((lch >> 16) & 0xFF) / 255.0f,
            (float) (((lch >> 8) & 0xFF) * (C_MAX / 255.0)),
            (float) ((lch & 0xFF) * (H_MAX / 255.0))
        };

        oklchToSrgbFF(color);

        int rInt = (int) Math.round(color[0] * 255.0);
        int gInt = (int) Math.round(color[1] * 255.0);
        int bInt = (int) Math.round(color[2] * 255.0);

        rInt = Math.clamp(rInt, 0, 255);
        gInt = Math.clamp(gInt, 0, 255);
        bInt = Math.clamp(bInt, 0, 255);

        return (rInt << 16) | (gInt << 8) | bInt;
    }


    public static float[] srgbToOklchIF(int rgb) {
        float[] color = {
            ((rgb >> 16) & 0xFF) / 255.0f,
            ((rgb >> 8) & 0xFF) / 255.0f,
            (rgb & 0xFF) / 255.0f
        };

        srgbToOklchFF(color);
        return color;
    }

    public static float[] oklchToSrgbIF(int lch) {
        float[] color = {
            ((lch >> 16) & 0xFF) / 255.0f,
            (float) (((lch >> 8) & 0xFF) * (C_MAX / 255.0)),
            (float) ((lch & 0xFF) * (H_MAX / 255.0))
        };

        oklchToSrgbFF(color);
        return color;
    }

    public static int srgbToOklchFFI(float[] color) {
        srgbToOklchFF(color);

        int lInt = (int) Math.round(color[0] * 255.0);
        int cInt = (int) Math.round((color[1] / C_MAX) * 255.0);
        int hInt = (int) Math.round((color[2] / H_MAX) * 255.0);

        lInt = Math.clamp(lInt, 0, 255);
        cInt = Math.clamp(cInt, 0, 255);
        hInt = Math.clamp(hInt, 0, 255);

        return (lInt << 16) | (cInt << 8) | hInt;
    }

    public static int oklchToSrgbFFI(float[] color) {
        oklchToSrgbFF(color);

        int rInt = (int) Math.round(color[0] * 255.0);
        int gInt = (int) Math.round(color[1] * 255.0);
        int bInt = (int) Math.round(color[2] * 255.0);

        rInt = Math.clamp(rInt, 0, 255);
        gInt = Math.clamp(gInt, 0, 255);
        bInt = Math.clamp(bInt, 0, 255);

        return (rInt << 16) | (gInt << 8) | bInt;
    }
}
