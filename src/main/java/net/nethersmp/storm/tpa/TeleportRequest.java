package net.nethersmp.storm.tpa;

import java.util.UUID;


public record TeleportRequest(UUID id, TeleportType type, long expiry, UUID sender, UUID receiver) {

    public boolean expired() {
        return System.currentTimeMillis() >= expiry;
    }


    public static TeleportRequest create(TeleportType type, long expiry, UUID sender, UUID receiver) {
        return new TeleportRequest(UUID.randomUUID(), type, expiry, sender, receiver);
    }
}
