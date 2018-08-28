package io.github.barnardb.infrajav.grpc;

import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkState;

/**
 * A NameResolver that delegates to an underlying resolver, actively refreshing if too much time has elapsed since the last refresh.
 */
public class ActiveNameResolver extends NameResolver {

    private static final Logger logger = Logger.getLogger(ActiveNameResolver.class.getName());

    private final NameResolver underlyingNameResolver;
    private final boolean isUsingSharedTimerService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int maxRefreshInterval;
    private final TimeUnit timeUnit;

    @GuardedBy("this")
    private ScheduledFuture<?> scheduledRefresh;
    @GuardedBy("this")
    private boolean shutdown;

    /**
     * Creates a new ActiveNameResolver.
     *
     * @param underlyingNameResolver   the resolver to delegate to
     * @param scheduledExecutorService the executor service to schedule refreshes on; if null, the shared GRPC {@link GrpcUtil#TIMER_SERVICE} will be used
     * @param maxRefreshInterval       the duration of time (in {@code timeUnit}s) since the last refresh after which we should trigger a new refresh
     * @param timeUnit                 the time unit for the {@code maxRefreshInterval}
     */
    public ActiveNameResolver(NameResolver underlyingNameResolver, @Nullable ScheduledExecutorService scheduledExecutorService, int maxRefreshInterval, TimeUnit timeUnit) {
        this.underlyingNameResolver = underlyingNameResolver;
        this.isUsingSharedTimerService = scheduledExecutorService == null;
        this.scheduledExecutorService = isUsingSharedTimerService
                ? SharedResourceHolder.get(GrpcUtil.TIMER_SERVICE)
                : scheduledExecutorService;
        this.maxRefreshInterval = maxRefreshInterval;
        this.timeUnit = timeUnit;
    }

    @Override
    public String getServiceAuthority() {
        return underlyingNameResolver.getServiceAuthority();
    }

    @Override
    public synchronized void start(Listener listener) {
        checkState(!shutdown, "already shutdown");
        checkState(scheduledRefresh == null, "already started");
        underlyingNameResolver.start(listener);
        scheduleRefresh();
    }

    @Override
    public synchronized void refresh() {
        checkState(!shutdown, "already shutdown");
        checkState(scheduledRefresh != null, "not yet started");

        scheduledRefresh.cancel(false);

        logger.log(Level.FINE, "Triggering explicitly requested refresh");
        underlyingNameResolver.refresh();
        scheduleRefresh();
    }

    @Override
    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(false);
        }
        if (isUsingSharedTimerService) {
            SharedResourceHolder.release(GrpcUtil.TIMER_SERVICE, scheduledExecutorService);
        }
        underlyingNameResolver.shutdown();
    }

    private void scheduleRefresh() {
        this.scheduledRefresh = scheduledExecutorService.schedule(this::performScheduledRefresh, maxRefreshInterval, timeUnit);
    }

    private synchronized void performScheduledRefresh() {
        if (shutdown) {
            return;
        }
        logger.log(Level.FINE, "Triggering scheduled refresh");
        underlyingNameResolver.refresh();
        scheduleRefresh();
    }

}
