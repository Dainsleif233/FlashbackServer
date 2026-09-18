package dev.zeffut.flashbackserver.api;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashbackApiTest {

    @AfterEach
    void tearDown() {
        FlashbackAPI.unbind();
    }

    @Test
    void unavailableBeforeBind() {
        assertFalse(FlashbackAPI.isAvailable());
        assertThrows(IllegalStateException.class, FlashbackAPI::recording);
        assertThrows(IllegalStateException.class, FlashbackAPI::clips);
    }

    @Test
    void bindExposesServices() {
        RecordingService recording = new StubRecording();
        ClipService clips = new StubClips();
        FlashbackAPI.bind(recording, clips);

        assertTrue(FlashbackAPI.isAvailable());
        assertSame(recording, FlashbackAPI.recording());
        assertSame(clips, FlashbackAPI.clips());
    }

    @Test
    void unbindClearsServices() {
        FlashbackAPI.bind(new StubRecording(), new StubClips());
        FlashbackAPI.unbind();

        assertFalse(FlashbackAPI.isAvailable());
        assertThrows(IllegalStateException.class, FlashbackAPI::recording);
        assertThrows(IllegalStateException.class, FlashbackAPI::clips);
    }

    private static final class StubRecording implements RecordingService {
        @Override public boolean start(Player player) { return true; }
        @Override public CompletableFuture<Path> stop(Player player) { return CompletableFuture.completedFuture(null); }
        @Override public boolean isRecording(Player player) { return false; }
    }

    private static final class StubClips implements ClipService {
        @Override public boolean arm(Player player) { return true; }
        @Override public boolean disarm(Player player) { return true; }
        @Override public boolean isArmed(Player player) { return false; }
        @Override public CompletableFuture<Path> saveClip(Player player) { return CompletableFuture.completedFuture(null); }
    }
}
