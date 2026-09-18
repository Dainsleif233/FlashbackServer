package dev.zeffut.flashbackserver.platform;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Scheduler helpers that work on both modern Paper/Folia (entity + async schedulers) and
 * Paper 1.16.1 (classic {@code BukkitScheduler} only).
 *
 * <p>Modern Paper APIs are invoked reflectively so this class never links against types
 * that are missing on 1.16.1 — otherwise class verification would fail at load time.
 */
public final class PlatformScheduler {
    private PlatformScheduler() {}

    /**
     * Runs {@code task} once on the entity's region thread (Folia / modern Paper) or the
     * main server thread (Paper 1.16.1).
     */
    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (tryEntityScheduler(plugin, entity, task)) return;
        Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Runs {@code task} every {@code periodTicks} ticks on the entity's region thread or the
     * main server thread. Returns a handle that cancels the repeating task.
     */
    public static Runnable repeatForEntity(Plugin plugin, Entity entity, long periodTicks, Runnable task) {
        try {
            Method getScheduler = entity.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(entity);
            Method runAtFixedRate = scheduler.getClass().getMethod(
                    "runAtFixedRate",
                    Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
            Object handle = runAtFixedRate.invoke(
                    scheduler, plugin, (Consumer<Object>) t -> task.run(), null, 1L, periodTicks);
            return () -> cancelQuietly(handle);
        } catch (Throwable ignored) {
            // Fall through to classic scheduler (Paper 1.16.1).
        }
        org.bukkit.scheduler.BukkitTask scheduled =
                Bukkit.getScheduler().runTaskTimer(plugin, task, 1L, periodTicks);
        return scheduled::cancel;
    }

    /** Runs {@code task} once, off the server threads. */
    public static void async(Plugin plugin, Runnable task) {
        try {
            Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(Bukkit.getServer());
            if (asyncScheduler == null) {
                asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            }
            Method runNow = asyncScheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
            runNow.invoke(asyncScheduler, plugin, (Consumer<Object>) t -> task.run());
            return;
        } catch (Throwable ignored) {
            // Fall through.
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    /**
     * Runs {@code task} once on the global region / main thread after an async completion.
     */
    public static void syncGlobal(Plugin plugin, Runnable task) {
        try {
            Object server = Bukkit.getServer();
            Object global = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
            if (global != null) {
                try {
                    Method run2 = global.getClass().getMethod("run", Plugin.class, Consumer.class);
                    run2.invoke(global, plugin, (Consumer<Object>) t -> task.run());
                    return;
                } catch (NoSuchMethodException e) {
                    Method run3 = global.getClass().getMethod(
                            "run", Plugin.class, Consumer.class, Runnable.class);
                    run3.invoke(global, plugin, (Consumer<Object>) t -> task.run(), null);
                    return;
                }
            }
        } catch (Throwable ignored) {
            // Fall through.
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private static boolean tryEntityScheduler(Plugin plugin, Entity entity, Runnable task) {
        try {
            Method getScheduler = entity.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(entity);
            Method run = scheduler.getClass().getMethod(
                    "run", Plugin.class, Consumer.class, Runnable.class);
            run.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void cancelQuietly(Object handle) {
        if (handle == null) return;
        try {
            handle.getClass().getMethod("cancel").invoke(handle);
        } catch (Throwable ignored) {
            // Already cancelled or classic BukkitTask.cancel() path.
        }
    }
}
