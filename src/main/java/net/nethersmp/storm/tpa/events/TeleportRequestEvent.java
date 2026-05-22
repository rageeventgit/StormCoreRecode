package net.nethersmp.storm.tpa.events;

import lombok.Getter;
import net.nethersmp.storm.tpa.TeleportRequest;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TeleportRequestEvent extends Event {

    @Getter
    private final TeleportRequest request;
    @Getter
    private final Status status;

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    public TeleportRequestEvent(@NotNull TeleportRequest request, Status status) {
        this.request = request;
        this.status = status;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public enum Status {
        ACCEPTED,
        DENIED,

    }
}
