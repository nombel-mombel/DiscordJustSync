package de.tronka.justsync.events.payload;

import net.minecraft.server.level.ServerPlayer;
import de.tronka.justsync.CompatUtil;

import java.util.UUID;

public class JoiningPayload {
    private final CompatUtil.Profile profile;
    private boolean canceled = false;
    private String cancelReason = "";

    public JoiningPayload(CompatUtil.Profile profile) {
        this.profile = profile;
    }

    public CompatUtil.Profile getProfile() {
        return this.profile;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public void setCanceled(String reason) {
        this.cancelReason = reason;
        this.canceled = true;
    }

    public String getCancelReason() {
        return this.cancelReason;
    }
}
