package dev.zeffut.flashbackserver.api;

import java.util.List;

/**
 * Result of {@link FlashbackAPI#verify(java.nio.file.Path)}.
 *
 * @param valid          {@code true} when format checks pass and, if the decode layer ran, it reported no errors
 * @param formatValid    container/format validation only
 * @param decodeClean    packet-decode result; {@code null} if decode was skipped (e.g. version adapter unavailable)
 * @param totalTicks     total ticks from container metadata (0 if the file could not be opened)
 * @param chunkCount     number of chunks in the container
 * @param decodedPackets number of game/config packets successfully decoded (0 if decode skipped)
 * @param errorCount     format problems + decode errors (decode errors only counted when decode ran)
 * @param problems       human-readable problem lines (possibly truncated)
 */
public record ReplayCheckResult(
        boolean valid,
        boolean formatValid,
        Boolean decodeClean,
        int totalTicks,
        int chunkCount,
        int decodedPackets,
        int errorCount,
        List<String> problems) {

    public ReplayCheckResult {
        problems = problems == null ? List.of() : List.copyOf(problems);
    }

    /** Convenience alias for {@link #valid()}. */
    public boolean ok() {
        return valid;
    }
}
