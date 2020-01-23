package org.koreader.service;

import android.annotation.SuppressLint;

import java.util.HashMap;

class ColorHelper {
    private final static float NULL_ALPHA = 0.0f;
    private final static int NULL_COLOR = 0x00000000;
    private final HashMap<Integer, Integer> warmthSteps;
    private final HashMap<Integer, Float> dimSteps;

    @SuppressLint("UseSparseArrays")
    ColorHelper() {
        dimSteps = new HashMap<>();
        warmthSteps = new HashMap<>();
        setup();
    }

    private void setup() {
        // opacity of a black view, keep low to simulate dimming.
        dimSteps.put(0, NULL_ALPHA);
        dimSteps.put(1, 0.02f);
        dimSteps.put(2, 0.04f);
        dimSteps.put(3, 0.06f);
        dimSteps.put(4, 0.08f);
        dimSteps.put(5, 0.10f);
        dimSteps.put(6, 0.12f);
        dimSteps.put(7, 0.14f);
        dimSteps.put(8, 0.16f);
        dimSteps.put(9, 0.18f);
        dimSteps.put(10, 0.2f);
        dimSteps.put(11, 0.22f);
        dimSteps.put(12, 0.24f);
        dimSteps.put(13, 0.26f);
        dimSteps.put(14, 0.28f);
        dimSteps.put(15, 0.3f);
        dimSteps.put(16, 0.35f);
        dimSteps.put(17, 0.4f);
        dimSteps.put(18, 0.45f);
        dimSteps.put(19, 0.5f);
        dimSteps.put(20, 0.55f);
        dimSteps.put(21, 0.6f);
        dimSteps.put(22, 0.65f);
        dimSteps.put(23, 0.7f);
        dimSteps.put(24, 0.75f);

        // argb colors representing 5000-1500K
        // based on http://www.vendian.org/mncharity/dir3/blackbody
        warmthSteps.put(0, NULL_COLOR); // NONE
        warmthSteps.put(1, 0xffffe4ce); // 5000K
        warmthSteps.put(2, 0xffffe1c6); // 4800K
        warmthSteps.put(3, 0xffffddbe); // 4600K
        warmthSteps.put(4, 0xffffd9b6); // 4400K
        warmthSteps.put(5, 0xffffd5ad); // 4200K
        warmthSteps.put(6, 0xffffd1a3); // 4000k
        warmthSteps.put(7, 0xffffcc99); // 3800K
        warmthSteps.put(8, 0xffffc78f); // 3600K
        warmthSteps.put(9, 0xffffc184); // 3400K
        warmthSteps.put(10, 0xffffbb78); // 3200K
        warmthSteps.put(11, 0xffffb46b); // 3000k
        warmthSteps.put(12, 0xffffad5e); // 2800K
        warmthSteps.put(13, 0xffffa54f); // 2600K
        warmthSteps.put(14, 0xffff9d3f); // 2500K
        warmthSteps.put(15, 0xffff932c); // 2400K
        warmthSteps.put(16, 0xffff9836); // 2300K
        warmthSteps.put(17, 0xffff932c); // 2200K
        warmthSteps.put(18, 0xffff8e21); // 2100K
        warmthSteps.put(19, 0xffff8912); // 2000K
        warmthSteps.put(20, 0xffff8300); // 1900K
        warmthSteps.put(21, 0Xffff7e00); // 1800K
        warmthSteps.put(22, 0xffff7900); // 1700K
        warmthSteps.put(23, 0Xffff7300); // 1600K
        warmthSteps.put(24, 0xffff6f00); // 1500K
    }

    /** get the alpha value for a given step
     *
     * @param step between 0 and 24
     * @return alpha
     */
    float getAlpha(int step) {
        if (dimSteps.containsKey(step)) {
            Float value = dimSteps.get(step);
            if (value != null) {
                return value;
            } else {
                return NULL_ALPHA;
            }
        } else {
            return NULL_ALPHA;
        }
    }

    /** get the argb value for a given step
     *
     * @param step between 0 and 24
     * @return argb color
     */
    int getARGB(int step) {
        if (warmthSteps.containsKey(step)) {
            Integer value = warmthSteps.get(step);
            if (value != null) {
                return value;
            } else {
                return NULL_COLOR;
            }
        } else {
            return NULL_COLOR;
        }
    }
}
