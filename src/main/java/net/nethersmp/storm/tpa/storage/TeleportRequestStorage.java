package net.nethersmp.storm.tpa.storage;


import net.nethersmp.storm.tpa.TeleportRequest;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TeleportRequestStorage {

    private final ConcurrentMap<UUID, TeleportRequest> storage = new ConcurrentHashMap<>();

    public void add(TeleportRequest teleportRequest) {
        storage.put(teleportRequest.id(), teleportRequest);
    }

    public void remove(UUID uuid) {
        storage.remove(uuid);
    }

    public Optional<TeleportRequest> getTeleportRequest(UUID uuid) {
        return Optional.ofNullable(storage.get(uuid));
    }

    public Optional<TeleportRequest> getTeleportRequest(UUID sender, UUID receiver) {

        return getTeleportRequests().stream().filter(request ->
                request.sender().compareTo(sender) == 0 && request.receiver().compareTo(receiver) == 0).findFirst();
    }

    public Collection<TeleportRequest> getTeleportRequests() {
        return storage.values();
    }


    public void clear() {
        storage.clear();
    }
}
