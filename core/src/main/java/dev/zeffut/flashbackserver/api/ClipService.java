package dev.zeffut.flashbackserver.api;

import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Public rolling-clip controls exposed by FlashbackServer.
 *
 * <p>Obtain via {@link FlashbackAPI#clips()} or Bukkit's ServicesManager:
 * {@code Bukkit.getServicesManager().load(ClipService.class)}.
 *
 * <p><b>Threading:</b> methods may be called from any thread. {@link #saveClip(Player)} completes
 * its future on an <em>async</em> scheduler thread — do not touch Bukkit API in the callback
 * without hopping back to the player's region thread
 * ({@code player.getScheduler().run(plugin, task, retired)}).
 *
 * <p>This interface is a consumer contract. Do not implement it in your own plugin except for
 * test doubles — methods may be added in future releases.
 */
public interface ClipService {

    /**
     * Starts a rolling clip buffer for {@code player} (window length from plugin config).
     *
     * @return {@code true} if armed; {@code false} if already armed
     */
    boolean arm(Player player);

    /**
     * Stops and discards the player's rolling clip buffer.
     *
     * @return {@code true} if a buffer was disarmed; {@code false} if none was armed
     */
    boolean disarm(Player player);

    /** @return {@code true} if {@code player} currently has a rolling clip buffer */
    boolean isArmed(Player player);

    /**
     * Writes the player's current clip window to disk asynchronously.
     *
     * <p>The returned future completes with the output path, or {@code null} if the player is not
     * armed or the buffer has not finished its first snapshot yet. It completes exceptionally if
     * the file could not be written.
     *
     * <p>After {@link #arm(Player)}, wait at least one server tick before calling this — the first
     * keyframe snapshot is built on the player's region thread.
     *
     * <p>Completion thread is async — see class-level threading notes.
     */
    CompletableFuture<Path> saveClip(Player player);
}
