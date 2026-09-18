package dev.zeffut.flashbackserver.format;

/**
 * A single action in a Flashback chunk: a registry {@code identifier} and its opaque
 * {@code payload} bytes.
 *
 * <p>Note: {@code equals}/{@code hashCode} use array identity for {@code payload}.
 * Compare payloads with {@link java.util.Arrays#equals(byte[], byte[])}.
 *
 * <p>Plain class (not a Java record) so core can target Java 11 for Paper 1.16.1.
 */
public final class ReplayAction {
    private final String identifier;
    private final byte[] payload;

    public ReplayAction(String identifier, byte[] payload) {
        this.identifier = identifier;
        this.payload = payload;
    }

    public String identifier() { return identifier; }

    public byte[] payload() { return payload; }
}
