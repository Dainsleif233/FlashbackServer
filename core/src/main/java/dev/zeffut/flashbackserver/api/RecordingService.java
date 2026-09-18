package dev.zeffut.flashbackserver.api;

import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Public recording controls exposed by FlashbackServer.
 *
 * <p>Obtain via {@link FlashbackAPI#recording()} or Bukkit's ServicesManager:
 * {@code Bukkit.getServicesManager().load(RecordingService.class)}.
 *
 * <p><b>Threading:</b> methods may be called from any thread. {@link #stop(Player)} completes
 * its future on an <em>async</em> scheduler thread — do not touch Bukkit API in the callback
 * without hopping back to the player's region thread
 * ({@code player.getScheduler().run(plugin, task, retired)}).
 *
 * <p>This interface is a consumer contract. Do not implement it in your own plugin except for
 * test doubles — methods may be added in future releases.
 */
public interface RecordingService {

    /**
     * Starts a full recording for {@code player}.
     *
     * @return {@code true} if recording started; {@code false} if already recording
     */
    boolean start(Player player);

    /**
     * Stops the player's recording and writes the {@code .flashback} file asynchronously.
     *
     * <p>The returned future completes with the output path, or {@code null} if the player was not
     * being recorded. It completes exceptionally if the file could not be written.
     *
     * <p>Completion thread is async — see class-level threading notes.
     */
    CompletableFuture<Path> stop(Player player);

    /** @return {@code true} if {@code player} is currently being recorded */
    boolean isRecording(Player player);
}
