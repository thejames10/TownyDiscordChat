package com.TownyDiscordChat.TownyDiscordChat.Scheduling;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Folia's region schedulers, reached by reflection: the single plugin JAR is
 * compiled against the oldest supported server API, which predates them.
 */
final class FoliaTaskScheduler implements TaskScheduler {
    private static final String SCHEDULER_PACKAGE = "io.papermc.paper.threadedregions.scheduler.";
    private static final long MILLISECONDS_PER_TICK = 50L;
    // Folia rejects delayed or repeating tasks whose first run is in under one tick.
    private static final long MINIMUM_DELAY_TICKS = 1L;

    private final Plugin plugin;
    private final Object globalScheduler;
    private final Object asyncScheduler;
    private final Method globalExecute;
    private final Method globalRunDelayed;
    private final Method globalRunAtFixedRate;
    private final Method globalCancelTasks;
    private final Method asyncRunNow;
    private final Method asyncRunAtFixedRate;
    private final Method asyncCancelTasks;
    private final Method entityScheduler;
    private final Method entityExecute;
    private final Method cancelTask;

    private FoliaTaskScheduler(Plugin plugin) throws ReflectiveOperationException {
        this.plugin = plugin;
        Class<?> global = Class.forName(SCHEDULER_PACKAGE + "GlobalRegionScheduler");
        Class<?> async = Class.forName(SCHEDULER_PACKAGE + "AsyncScheduler");
        Class<?> entity = Class.forName(SCHEDULER_PACKAGE + "EntityScheduler");
        globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
        asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
        globalExecute = global.getMethod("execute", Plugin.class, Runnable.class);
        globalRunDelayed = global.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
        globalRunAtFixedRate = global.getMethod("runAtFixedRate", Plugin.class, Consumer.class,
                long.class, long.class);
        globalCancelTasks = global.getMethod("cancelTasks", Plugin.class);
        asyncRunNow = async.getMethod("runNow", Plugin.class, Consumer.class);
        asyncRunAtFixedRate = async.getMethod("runAtFixedRate", Plugin.class, Consumer.class,
                long.class, long.class, TimeUnit.class);
        asyncCancelTasks = async.getMethod("cancelTasks", Plugin.class);
        entityScheduler = Entity.class.getMethod("getScheduler");
        entityExecute = entity.getMethod("execute", Plugin.class, Runnable.class, Runnable.class,
                long.class);
        cancelTask = Class.forName(SCHEDULER_PACKAGE + "ScheduledTask").getMethod("cancel");
    }

    static FoliaTaskScheduler create(Plugin plugin) {
        try {
            return new FoliaTaskScheduler(plugin);
        } catch (ReflectiveOperationException missingApi) {
            throw new IllegalStateException("Folia was detected but its scheduler API was not found",
                    missingApi);
        }
    }

    @Override
    public void runGlobal(Runnable task) {
        invoke(globalExecute, globalScheduler, plugin, task);
    }

    @Override
    public void runGlobalLater(Runnable task, long delayTicks) {
        invoke(globalRunDelayed, globalScheduler, plugin, ignoring(task), atLeastOneTick(delayTicks));
    }

    @Override
    public RepeatingTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        Object handle = invoke(globalRunAtFixedRate, globalScheduler, plugin, ignoring(task),
                atLeastOneTick(delayTicks), atLeastOneTick(periodTicks));
        return () -> invoke(cancelTask, handle);
    }

    @Override
    public void runAsync(Runnable task) {
        invoke(asyncRunNow, asyncScheduler, plugin, ignoring(task));
    }

    @Override
    public RepeatingTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        Object handle = invoke(asyncRunAtFixedRate, asyncScheduler, plugin, ignoring(task),
                atLeastOneTick(delayTicks) * MILLISECONDS_PER_TICK,
                atLeastOneTick(periodTicks) * MILLISECONDS_PER_TICK, TimeUnit.MILLISECONDS);
        return () -> invoke(cancelTask, handle);
    }

    @Override
    public void runForPlayer(Player player, Runnable task) {
        Object scheduler = invoke(entityScheduler, player);
        invoke(entityExecute, scheduler, plugin, task, null, MINIMUM_DELAY_TICKS);
    }

    @Override
    public void cancelAll() {
        invoke(globalCancelTasks, globalScheduler, plugin);
        invoke(asyncCancelTasks, asyncScheduler, plugin);
    }

    private static Consumer<Object> ignoring(Runnable task) {
        return scheduledTask -> task.run();
    }

    private static long atLeastOneTick(long ticks) {
        return Math.max(MINIMUM_DELAY_TICKS, ticks);
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (IllegalAccessException inaccessible) {
            throw new IllegalStateException("Cannot call Folia scheduler method " + method.getName(),
                    inaccessible);
        } catch (InvocationTargetException failed) {
            Throwable cause = failed.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        }
    }
}
