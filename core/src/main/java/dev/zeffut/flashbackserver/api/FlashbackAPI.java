package dev.zeffut.flashbackserver.api;

import org.bukkit.plugin.Plugin;

/**
 * Stable entry point for other plugins integrating with FlashbackServer.
 *
 * <p><b>Classloading:</b> this class lives in the FlashbackServer jar. If the plugin is
 * <em>not installed</em>, merely referencing {@link FlashbackAPI} throws
 * {@link NoClassDefFoundError} — {@link #isAvailable()} cannot protect you. Check installation
 * first:
 *
 * <pre>{@code
 * if (Bukkit.getPluginManager().getPlugin("FlashbackServer") == null) {
 *     return; // not installed — do not touch FlashbackAPI
 * }
 * if (!FlashbackAPI.isAvailable()) {
 *     return; // installed but not enabled
 * }
 * FlashbackAPI.recording().start(player);
 * }</pre>
 *
 * <p>Compile against the plugin jar with {@code compileOnly}. <b>Do not shade</b> the API
 * classes into your plugin: a second copy of {@code FlashbackAPI} is never bound and
 * {@link #isAvailable()} stays {@code false}.
 *
 * <p>Equivalent Bukkit-style lookup (same classloading caveat):
 * {@code Bukkit.getServicesManager().load(RecordingService.class)}.
 *
 * <p>Consumer plugins should declare {@code softdepend: [FlashbackServer]} so enable order and
 * class-loader visibility are correct.
 *
 * <p>Do not cache {@link #recording()} / {@link #clips()} across plugin (re)loads — after
 * FlashbackServer disables, a held reference is dead. Re-fetch on each use; the cost is a
 * volatile read.
 */
public final class FlashbackAPI {

    private static final String OWNER = "FlashbackServer";

    private static volatile RecordingService recording;
    private static volatile ClipService clips;

    private FlashbackAPI() {}

    /**
     * @return {@code true} when FlashbackServer is enabled and the API services are bound
     * @throws NoClassDefFoundError if the FlashbackServer jar is not installed (check
     *         {@code Bukkit.getPluginManager().getPlugin("FlashbackServer")} first)
     */
    public static boolean isAvailable() {
        return recording != null && clips != null;
    }

    /**
     * @return the recording service
     * @throws IllegalStateException if FlashbackServer is not enabled
     * @throws NoClassDefFoundError if the FlashbackServer jar is not installed
     */
    public static RecordingService recording() {
        RecordingService service = recording;
        if (service == null) {
            throw new IllegalStateException(
                    "FlashbackServer API is not available (plugin missing or not enabled)");
        }
        return service;
    }

    /**
     * @return the rolling-clip service
     * @throws IllegalStateException if FlashbackServer is not enabled
     * @throws NoClassDefFoundError if the FlashbackServer jar is not installed
     */
    public static ClipService clips() {
        ClipService service = clips;
        if (service == null) {
            throw new IllegalStateException(
                    "FlashbackServer API is not available (plugin missing or not enabled)");
        }
        return service;
    }

    /**
     * Binds the live services. Only the FlashbackServer plugin may call this.
     *
     * @param owner must be the FlashbackServer plugin instance
     */
    public static void bind(Plugin owner, RecordingService recordingService, ClipService clipService) {
        requireOwner(owner);
        recording = recordingService;
        clips = clipService;
    }

    /**
     * Clears the bound services. Only the FlashbackServer plugin may call this.
     *
     * @param owner must be the FlashbackServer plugin instance
     */
    public static void unbind(Plugin owner) {
        requireOwner(owner);
        recording = null;
        clips = null;
    }

    /** Package-private bind for unit tests in this package. */
    static void bind(RecordingService recordingService, ClipService clipService) {
        recording = recordingService;
        clips = clipService;
    }

    /** Package-private unbind for unit tests in this package. */
    static void unbind() {
        recording = null;
        clips = null;
    }

    private static void requireOwner(Plugin owner) {
        if (owner == null || !OWNER.equals(owner.getName())) {
            throw new IllegalArgumentException(
                    "FlashbackAPI.bind/unbind is internal to FlashbackServer");
        }
    }
}
