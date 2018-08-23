package io.github.barnardb.infrajav.grpc;

import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.SharedResourceHolder;

import javax.annotation.Nullable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A NameResolver that delegates to an underlying resolver, actively refreshing if too much time has elapsed since the last refresh.
 */
public class ActiveNameResolver extends NameResolver {

    private final NameResolver underlyingNameResolver;
    private final boolean isUsingSharedTimerService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int maxRefreshInterval;
    private final TimeUnit timeUnit;
    private final AtomicReference<ScheduledFuture<?>> scheduledRefresh;
    private boolean closed;

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
        this.scheduledRefresh = new AtomicReference<>();
    }

    @Override
    public String getServiceAuthority() {
        return underlyingNameResolver.getServiceAuthority();
    }

    @Override
    public void start(Listener listener) {
        if (closed) {
            throw new IllegalStateException("closed");
        }
        underlyingNameResolver.start(listener);
        scheduleRefresh();
    }

    @Override
    public void refresh() {
        if (closed) {
            throw new IllegalStateException("closed");
        }
        setScheduledRefreshFuture(null);
        underlyingNameResolver.refresh();
        scheduleRefresh();
    }

    @Override
    public void shutdown() {
        if (closed) {
            return;
        }
        closed = true;
        if (isUsingSharedTimerService) {
            SharedResourceHolder.release(GrpcUtil.TIMER_SERVICE, scheduledExecutorService);
        }
        underlyingNameResolver.shutdown();
    }

    private void scheduleRefresh() {
        setScheduledRefreshFuture(scheduledExecutorService.schedule(this::refresh, maxRefreshInterval, timeUnit));
    }

    private void setScheduledRefreshFuture(ScheduledFuture<?> scheduledRefresh) {
        ScheduledFuture<?> previouslyScheduledRefresh = this.scheduledRefresh.getAndSet(scheduledRefresh);
        if (previouslyScheduledRefresh != null)
            previouslyScheduledRefresh.cancel(false);
    }

}
