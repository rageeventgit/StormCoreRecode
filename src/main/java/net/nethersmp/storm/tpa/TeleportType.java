package net.nethersmp.storm.tpa;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum TeleportType {

    AWAY,
    HERE;

    public static Set<String> keys() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.toSet());
    }
}
