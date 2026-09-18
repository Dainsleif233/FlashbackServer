package dev.zeffut.flashbackserver.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
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

    @Test
    void publicBindRejectsForeignOwner() {
        FlashbackAPI.bind(new StubRecording(), new StubClips());
        assertTrue(FlashbackAPI.isAvailable());

        Plugin foreign = pluginNamed("SomeOtherPlugin");
        assertThrows(IllegalArgumentException.class,
                () -> FlashbackAPI.bind(foreign, new StubRecording(), new StubClips()));
        assertThrows(IllegalArgumentException.class,
                () -> FlashbackAPI.unbind(foreign));
        assertThrows(IllegalArgumentException.class,
                () -> FlashbackAPI.bind(null, new StubRecording(), new StubClips()));
        // prior package-private bind must remain intact
        assertTrue(FlashbackAPI.isAvailable());
    }

    private static Plugin pluginNamed(String name) {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if ("getName".equals(method.getName())) return name;
                    if ("toString".equals(method.getName())) return "StubPlugin[" + name + "]";
                    if ("hashCode".equals(method.getName())) return name.hashCode();
                    if ("equals".equals(method.getName())) return proxy == args[0];
                    return null;
                });
    }

    private static final class StubRecording implements RecordingService {
        @Override public boolean start(Player player) { return true; }
        @Override public CompletableFuture<Path> stop(Player player) { return CompletableFuture.completedFuture(null); }
        @Override public CompletableFuture<Path> stop(Player player, Path outputFile) { return CompletableFuture.completedFuture(outputFile); }
        @Override public boolean isRecording(Player player) { return false; }
    }

    private static final class StubClips implements ClipService {
        @Override public boolean arm(Player player) { return true; }
        @Override public boolean disarm(Player player) { return true; }
        @Override public boolean isArmed(Player player) { return false; }
        @Override public CompletableFuture<Path> saveClip(Player player) { return CompletableFuture.completedFuture(null); }
        @Override public CompletableFuture<Path> saveClip(Player player, Path outputFile) { return CompletableFuture.completedFuture(outputFile); }
    }
}
