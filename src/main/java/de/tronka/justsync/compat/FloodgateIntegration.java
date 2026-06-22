package de.tronka.justsync.compat;

import com.mojang.authlib.GameProfile;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.minecraft.server.level.ServerPlayer;
import org.geysermc.floodgate.api.FloodgateApi;
import de.tronka.justsync.JustSyncApplication;
import de.tronka.justsync.events.CoreEvents;
import de.tronka.justsync.events.payload.JoiningPayload;
import de.tronka.justsync.linking.LinkManager;
import de.tronka.justsync.linking.PlayerLink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FloodgateIntegration {
    private FloodgateApi floodgateApi;
    private JustSyncApplication integration;

    public FloodgateIntegration(JustSyncApplication integration) {
        if (integration.getPlatform().isModLoaded("floodgate")) {
            this.floodgateApi = FloodgateApi.getInstance();
        }
        this.integration = integration;
        CoreEvents.PLAYER_JOINING.subscribe(this::onPlayerJoining);
    }

    private void onPlayerJoining(JoiningPayload payload) {
        if (this.floodgateApi == null) {
            return;
        }

        UUID uuid = payload.getProfile().id();
        if (!this.canJoinMixedAccountType(uuid)) {
            payload.setCanceled(this.integration.getConfig().integrations.floodgate.joiningMixedAccountTypesKickMessage);
        }
    }

    public boolean isBedrock(UUID uuid) {
        return this.floodgateApi != null && this.floodgateApi.isFloodgateId(uuid);
    }

    public String getUsername(UUID uuid) {
        if (this.floodgateApi != null) {
            try {
                return this.floodgateApi.getPlayerPrefix()
                    + this.floodgateApi.getGamertagFor(uuid.getLeastSignificantBits()).get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }
        return "unknown";
    }

    public boolean isFloodGateName(String name) {
        return this.floodgateApi != null && name.startsWith(this.floodgateApi.getPlayerPrefix());
    }

    public GameProfile getGameProfileFor(String name) {
        String gamerTag = name.replaceFirst(this.floodgateApi.getPlayerPrefix(), "");
        try {
            UUID id = this.floodgateApi.getUuidFor(gamerTag).get();
            String username = this.getUsername(id);
            return new GameProfile(id, username);
        } catch (InterruptedException | ExecutionException ignored) {
        }
        return null;
    }

    private boolean canJoinMixedAccountType(UUID id) {
        if (this.integration.getConfig().integrations.floodgate.allowJoiningMixedAccountTypes
            || this.floodgateApi == null) {
            return true;
        }

        LinkManager linkManager = this.integration.getLinkManager();

        Optional<PlayerLink> linkOptional = linkManager.getDataOf(id);
        if (linkOptional.isEmpty()) {
            return true;
        }

        PlayerLink playerLink = linkOptional.get();

        Optional<Member> member = linkManager.getDiscordOf(playerLink);
        if (member.isEmpty()) {
            // should never occure
            return false;
        }
        List<Role> roles = new ArrayList<>(member.get().getRoles());

        roles.retainAll(linkManager.getAllowJoiningMixedAccountTypesBypass());

        if (!roles.isEmpty()) {
            return true;
        }

        List<UUID> uuids = playerLink.getAllUuids();
        boolean currentIsBedrock = this.isBedrock(id);

        for (UUID uuid : uuids) {
            ServerPlayer player = this.integration.getServer().getPlayerList().getPlayer(uuid);
            if ((player != null) && (this.isBedrock(uuid) != currentIsBedrock)) {
                return false;
            }
        }

        return true;
    }

}

