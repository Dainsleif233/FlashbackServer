package dev.zeffut.flashbackserver.format;

import dev.zeffut.flashbackserver.util.Coll;

import java.util.Map;
import java.util.Optional;

/**
 * Protocol-version-keyed table of clientbound PLAY packet ids.
 *
 * <p>Add a new row to {@link #TABLE} whenever a new Minecraft protocol version is supported.
 * The table is consulted by {@link FlashbackValidator#validateRenderable} to decide which
 * id-specific checks to apply; an absent entry causes graceful degradation (agnostic-floor
 * checks only, with an informational problem noting the unknown protocol).
 */
public final class PacketIds {

    /**
     * Packet id set for a single Minecraft protocol version.
     *
     * @param login      clientbound PLAY LoginPacket id
     * @param position   clientbound PLAY PlayerPositionPacket id
     * @param chunk      clientbound PLAY LevelChunkWithLightPacket id
     * @param playerInfo clientbound PLAY PlayerInfoUpdatePacket id
     */
    public static final class Ids {
        private final int login;
        private final int position;
        private final int chunk;
        private final int playerInfo;

        public Ids(int login, int position, int chunk, int playerInfo) {
            this.login = login;
            this.position = position;
            this.chunk = chunk;
            this.playerInfo = playerInfo;
        }

        public int login() { return login; }
        public int position() { return position; }
        public int chunk() { return chunk; }
        public int playerInfo() { return playerInfo; }
    }

    /**
     * Protocol version → clientbound PLAY packet ids.
     *
     * <p>736 = MC 1.16.1 (EnumProtocol.PLAY clientbound registration on Paper build 138).
     * 770 = MC 1.21.5 (confirmed via docs/research/r3-spike.md). Add rows per supported version.
     */
    private static final Map<Integer, Ids> TABLE = Coll.mapOf(
        736, new Ids(37, 53, 33, 51),
        770, new Ids(43, 65, 39, 63)
    );

    private PacketIds() {}

    /**
     * Returns the {@link Ids} for the given Minecraft protocol version, or
     * {@link Optional#empty()} if the version is not in the table.
     */
    public static Optional<Ids> forProtocol(int protocolVersion) {
        return Optional.ofNullable(TABLE.get(protocolVersion));
    }
}
