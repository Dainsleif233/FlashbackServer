package dev.zeffut.flashbackserver.record;

import dev.zeffut.flashbackserver.format.FlashbackValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FlashbackRecorderTest {
    @Test
    void producesOracleValidFile(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("rec.flashback");
        FlashbackRecorder recorder = new FlashbackRecorder(out, "TestPlayer", 769, 4189);
        recorder.onPacket(new byte[]{1, 2, 3});
        recorder.onTick();
        recorder.onPacket(new byte[]{4});
        recorder.onTick();
        recorder.stop();

        FlashbackValidator.Report report = FlashbackValidator.validate(out);
        assertTrue(report.valid(), report.problems().toString());
        assertEquals(2, report.totalTicks());
        assertEquals(1, report.chunkCount());
    }

    @Test
    void stopIsIdempotent(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("rec2.flashback");
        FlashbackRecorder recorder = new FlashbackRecorder(out, "P", 769, 4189);
        recorder.onTick();
        recorder.stop();
        recorder.stop(); // second stop is a no-op, does not throw
        assertTrue(FlashbackValidator.validate(out).valid());
    }

    /**
     * API contract: {@code stop(Player, Path)} may pass any destination.
     * Writes must land on that path (parents created); the recorder's default output stays unused.
     * Validated via {@link FlashbackValidator}, not via {@code /replay verify}.
     */
    @Test
    void stopWithCustomPathWritesThereAndNotDefault(@TempDir Path dir) throws Exception {
        Path defaultOut = dir.resolve("default.flashback");
        Path custom = dir.resolve("nested/season/final.flashback");
        FlashbackRecorder recorder = new FlashbackRecorder(defaultOut, "TestPlayer", 769, 4189);
        recorder.onPacket(new byte[]{1, 2, 3});
        recorder.onTick();
        recorder.stop(custom);

        assertTrue(Files.isRegularFile(custom), "custom path was not written: " + custom);
        assertFalse(Files.exists(defaultOut), "default path must not be written when custom path is set");

        FlashbackValidator.Report report = FlashbackValidator.validate(custom);
        assertTrue(report.valid(), report.problems().toString());
        assertEquals(1, report.totalTicks());
    }

    @Test
    void stopNullPathFallsBackToRecorderDefault(@TempDir Path dir) throws Exception {
        Path defaultOut = dir.resolve("fallback.flashback");
        FlashbackRecorder recorder = new FlashbackRecorder(defaultOut, "P", 769, 4189);
        recorder.onTick();
        recorder.stop(null);

        assertTrue(FlashbackValidator.validate(defaultOut).valid());
    }
}
