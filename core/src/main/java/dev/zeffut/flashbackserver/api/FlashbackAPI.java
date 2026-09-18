package dev.zeffut.flashbackserver.api;

/**
 * Stable entry point for other plugins integrating with FlashbackServer.
 *
 * <pre>{@code
 * if (!FlashbackAPI.isAvailable()) {
 *     return; // FlashbackServer not installed / not enabled
 * }
 * FlashbackAPI.recording().start(player);
 * }</pre>
 *
 * <p>Equivalent Bukkit-style lookup:
 * {@code Bukkit.getServicesManager().load(RecordingService.class)}.
 *
 * <p>Consumer plugins should soft-depend on {@code FlashbackServer} and check
 * {@link #isAvailable()} before calling the service methods.
 */
public final class FlashbackAPI {

    private static volatile RecordingService recording;
    private static volatile ClipService clips;

    private FlashbackAPI() {}

    /** @return {@code true} when FlashbackServer is enabled and the API services are bound */
    public static boolean isAvailable() {
        return recording != null && clips != null;
    }

    /**
     * @return the recording service
     * @throws IllegalStateException if FlashbackServer is not enabled
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
     * Binds the live services. Called only by {@code FlashbackServerPlugin} on enable.
     */
    public static void bind(RecordingService recordingService, ClipService clipService) {
        recording = recordingService;
        clips = clipService;
    }

    /**
     * Clears the bound services. Called only by {@code FlashbackServerPlugin} on disable.
     */
    public static void unbind() {
        recording = null;
        clips = null;
    }
}
