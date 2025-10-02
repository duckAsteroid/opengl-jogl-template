package com.asteroid.duck.opengl.util;

public enum OperatingSystem {
    LINUX, MAC, WINDOWS, UNKNOWN;
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static final OperatingSystem CURRENT = getCurrent();

    private static OperatingSystem getCurrent() {
        if (OS_NAME.contains("mac")) {
            return MAC;
        }
        else if (OS_NAME.contains("win")) {
            return WINDOWS;
        }
        else if (OS_NAME.contains("nix") || OS_NAME.contains("nux")) {
            return LINUX;
        }
        return UNKNOWN;
    }
}
