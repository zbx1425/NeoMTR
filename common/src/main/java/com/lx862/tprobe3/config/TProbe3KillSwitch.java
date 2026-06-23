package com.lx862.tprobe3.config;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TODO: Change this to a config, maybe we can have auth later as well?
 */
public class TProbe3KillSwitch {
    private static boolean activated = false;

    public static void checkIfEnabled(Path configDir) {
        if(Files.exists(configDir.resolve("mtr_no_tprobe.txt"))) {
            activated = true;
        }
    }

    public static boolean activated() {
        return activated;
    }
}
