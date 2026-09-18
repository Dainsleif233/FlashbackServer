package dev.zeffut.flashbackserver.version.v1_16_1;

import dev.zeffut.flashbackserver.format.ReplayAction;
import dev.zeffut.flashbackserver.version.PacketTranslator;
import dev.zeffut.flashbackserver.version.VersionAdapter;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * {@link VersionAdapter} for Minecraft 1.16.1 (Paper, Spigot NMS {@code v1_16_R1}).
 *
 * <p>1.16.1 has no CONFIGURATION phase and no StreamCodec API. Snapshot config actions are
 * empty; PLAY packets are encoded via {@link PacketDataSerializer} plus
 * {@link EnumProtocol#PLAY} packet-id lookup. All NMS access stays in this package.
 */
public final class V1_16_1Adapter implements VersionAdapter {

    private static final int SNAPSHOT_CHUNK_RADIUS = 8;
    private static final String GAME_PACKET = "flashback:action/game_packet";
    private static final String CONFIG_PACKET = "flashback:action/configuration_packet";
    private static final int MAX_PROBLEMS = 20;
    private static final Logger LOG = Logger.getLogger("FlashbackServer");

    @Override
    public Channel channelOf(Player player) {
        EntityPlayer sp = handle(player);
        NetworkManager connection = sp.playerConnection != null
                ? sp.playerConnection.networkManager
                : sp.networkManager;
        if (connection == null || connection.channel == null) {
            throw new IllegalStateException("Netty channel was null for player " + player.getName());
        }
        return connection.channel;
    }

    @Override
    public int protocolVersion() {
        return SharedConstants.getGameVersion().getProtocolVersion();
    }

    @Override
    public int dataVersion() {
        return org.bukkit.Bukkit.getUnsafe().getDataVersion();
    }

    /**
     * 1.16.1 has no configuration phase; registries/tags ride along in the login packet.
     */
    @Override
    public List<ReplayAction> configActions(Player player) {
        return java.util.Collections.emptyList();
    }

    @Override
    public List<ReplayAction> loginAction(Player player) {
        EntityPlayer sp = handle(player);
        WorldServer world = sp.getWorldServer();
        MinecraftServer server = sp.server;
        PlayerList playerList = server.getPlayerList();
        GeneratorSettings settings = world.worldDataServer.getGeneratorSettings();

        Set<ResourceKey<World>> levels = new HashSet<>();
        for (WorldServer ws : server.getWorlds()) {
            levels.add(ws.getDimensionKey());
        }

        PacketPlayOutLogin loginPacket = new PacketPlayOutLogin(
                sp.getId(),
                sp.playerInteractManager.getGameMode(),
                EnumGamemode.NOT_SET,
                world.getSeed(),
                server.isHardcore(),
                levels,
                server.f,
                world.getTypeKey(),
                world.getDimensionKey(),
                playerList.getMaxPlayers(),
                playerList.getViewDistance(),
                false, // reducedDebugInfo
                true,  // enableRespawnScreen
                settings.isDebugWorld(),
                settings.isFlatWorld()
        );
        return java.util.Collections.singletonList(new ReplayAction(GAME_PACKET, encodePacket(sp, loginPacket)));
    }

    @Override
    public List<ReplayAction> postLoginActions(Player player) {
        EntityPlayer sp = handle(player);
        WorldServer world = sp.getWorldServer();
        MinecraftServer server = sp.server;
        List<ReplayAction> actions = new ArrayList<>();

        // No ClientboundPlayerPositionPacket: Flashback's replay client refuses it.
        PacketPlayOutPlayerInfo playerInfoPacket = new PacketPlayOutPlayerInfo(
                PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, sp);
        actions.add(new ReplayAction(GAME_PACKET, encodePacket(sp, playerInfoPacket)));

        int viewDist = server.getPlayerList().getViewDistance();
        int radius = Math.min(viewDist, SNAPSHOT_CHUNK_RADIUS);
        int cx0 = sp.chunkX;
        int cz0 = sp.chunkZ;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk chunk = world.getChunkIfLoaded(cx0 + dx, cz0 + dz);
                if (chunk == null) continue;
                PacketPlayOutMapChunk chunkPacket = new PacketPlayOutMapChunk(chunk, 0xFFFF, true);
                actions.add(new ReplayAction(GAME_PACKET, encodePacket(sp, chunkPacket)));
            }
        }
        return actions;
    }

    @Override
    public DecodeResult decode(List<ReplayAction> snapshotActions, List<ReplayAction> streamActions) {
        int decoded = 0;
        int errors = 0;
        List<String> problems = new ArrayList<>();
        List<ReplayAction> all = new ArrayList<>(snapshotActions.size() + streamActions.size());
        all.addAll(snapshotActions);
        all.addAll(streamActions);

        for (ReplayAction action : all) {
            if (!GAME_PACKET.equals(action.identifier())
                    && !CONFIG_PACKET.equals(action.identifier())) {
                continue;
            }
            byte[] payload = action.payload();
            if (payload == null || payload.length == 0) {
                errors++;
                if (problems.size() < MAX_PROBLEMS) {
                    problems.add("Empty payload (" + action.identifier() + ")");
                }
                continue;
            }
            PacketDataSerializer buf = new PacketDataSerializer(Unpooled.wrappedBuffer(payload));
            try {
                int id = buf.readVarInt();
                Packet<?> packet = EnumProtocol.PLAY.a(EnumProtocolDirection.CLIENTBOUND, id);
                if (packet == null) {
                    errors++;
                    if (problems.size() < MAX_PROBLEMS) {
                        problems.add("Unknown clientbound PLAY packet id " + id
                                + " (" + action.identifier() + ")");
                    }
                    continue;
                }
                packet.a(buf);
                if (buf.isReadable()) {
                    errors++;
                    if (problems.size() < MAX_PROBLEMS) {
                        problems.add("Trailing bytes after decode (" + action.identifier() + "): "
                                + buf.readableBytes() + " byte(s) unconsumed");
                    }
                } else {
                    decoded++;
                }
            } catch (Exception e) {
                errors++;
                if (problems.size() < MAX_PROBLEMS) {
                    problems.add("Decode error (" + action.identifier() + "): "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            } finally {
                buf.release();
            }
        }
        return new DecodeResult(decoded, errors, java.util.Collections.unmodifiableList(problems));
    }

    @Override
    public PacketTranslator translatorFor(Player player) {
        EntityPlayer sp = handle(player);
        String playerName = player.getName();
        return new PacketTranslator() {
            @Override
            public List<Object> expand(Object message) {
                if (message instanceof Packet<?>) return java.util.Collections.singletonList(message);
                return java.util.Collections.emptyList();
            }

            @Override
            public byte[] encode(Object packet) {
                if (!(packet instanceof Packet<?>)) return null;
                try {
                    return encodePacket(sp, (Packet<?>) packet);
                } catch (Exception e) {
                    LOG.warning("Could not encode " + packet.getClass().getSimpleName()
                            + " for " + playerName + ": " + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
                    return null;
                }
            }
        };
    }

    @Override
    public String dimensionKey(Player player) {
        EntityPlayer sp = handle(player);
        return sp.getWorldServer().getDimensionKey().a().toString();
    }

    @Override
    public List<EntityPosition> visibleEntityPositions(Player player) {
        EntityPlayer sp = handle(player);
        WorldServer world = sp.getWorldServer();
        MinecraftServer server = sp.server;
        double range = Math.min(server.getPlayerList().getViewDistance(), SNAPSHOT_CHUNK_RADIUS) * 16.0;
        AxisAlignedBB box = sp.getBoundingBox().grow(range);

        List<EntityPosition> out = new ArrayList<>();
        out.add(positionOf(sp));
        for (Entity entity : world.getEntities(sp, box, e -> true)) {
            out.add(positionOf(entity));
        }
        return out;
    }

    private static EntityPlayer handle(Player player) {
        if (!(player instanceof CraftPlayer)) {
            throw new IllegalArgumentException(
                    "Expected CraftPlayer but got " + player.getClass().getName());
        }
        return ((CraftPlayer) player).getHandle();
    }

    private static EntityPosition positionOf(Entity entity) {
        float headYaw = (entity instanceof EntityLiving)
                ? ((EntityLiving) entity).getHeadRotation()
                : entity.yaw;
        return new EntityPosition(
                entity.getId(),
                entity.locX(), entity.locY(), entity.locZ(),
                entity.yaw, entity.pitch,
                headYaw,
                entity.isOnGround());
    }

    @SuppressWarnings("unchecked")
    private static byte[] encodePacket(EntityPlayer sp, Packet<?> packet) {
        Integer id = EnumProtocol.PLAY.a(EnumProtocolDirection.CLIENTBOUND, packet);
        if (id == null) {
            throw new IllegalStateException(
                    "Packet " + packet.getClass().getSimpleName() + " is not registered clientbound PLAY");
        }
        PacketDataSerializer buf = new PacketDataSerializer(Unpooled.buffer());
        try {
            buf.d(id);
            ((Packet<PacketListener>) packet).b(buf);
            return ByteBufUtil.getBytes(buf);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to encode packet " + packet.getClass().getSimpleName()
                            + " for player " + sp.getBukkitEntity().getName(), e);
        } finally {
            buf.release();
        }
    }
}
