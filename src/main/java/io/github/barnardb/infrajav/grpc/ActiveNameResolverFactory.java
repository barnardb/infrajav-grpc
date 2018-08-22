package io.github.barnardb.infrajav.grpc;

import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.internal.GrpcUtil;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A factory for {@link ActiveNameResolver}s.
 */
public class ActiveNameResolverFactory extends NameResolver.Factory {

    private final NameResolver.Factory underlyingFactory;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int maxRefreshInterval;
    private final TimeUnit timeUnit;

    /**
     * Creates a new ActiveNameResolver using the shared GRPC {@link GrpcUtil#TIMER_SERVICE}.
     *
     * @param underlyingFactory  the factory to use to create delegates for the {@link ActiveNameResolver}s that will be created
     * @param maxRefreshInterval the duration of time (in {@code timeUnit}s) since the last refresh after which we should trigger a new refresh
     * @param timeUnit           the time unit for the {@code maxRefreshInterval}
     */
    public ActiveNameResolverFactory(NameResolver.Factory underlyingFactory, int maxRefreshInterval, TimeUnit timeUnit) {
        this(underlyingFactory, null, maxRefreshInterval, timeUnit);
    }

    /**
     * Creates a new ActiveNameResolver.
     *
     * @param underlyingFactory        the factory to use to create delegates for the {@link ActiveNameResolver}s that will be created
     * @param scheduledExecutorService the executor service to schedule refreshes on; if null, the shared GRPC {@link GrpcUtil#TIMER_SERVICE} will be used
     * @param maxRefreshInterval       the duration of time (in {@code timeUnit}s) since the last refresh after which we should trigger a new refresh
     * @param timeUnit                 the time unit for the {@code maxRefreshInterval}
     */
    public ActiveNameResolverFactory(NameResolver.Factory underlyingFactory, @Nullable ScheduledExecutorService scheduledExecutorService, int maxRefreshInterval, TimeUnit timeUnit) {
        this.underlyingFactory = underlyingFactory;
        this.scheduledExecutorService = scheduledExecutorService;
        this.maxRefreshInterval = maxRefreshInterval;
        this.timeUnit = timeUnit;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        NameResolver underlyingNameResolver = underlyingFactory.newNameResolver(targetUri, params);
        return underlyingNameResolver == null
                ? null
                : new ActiveNameResolver(underlyingNameResolver, scheduledExecutorService, maxRefreshInterval, timeUnit);
    }

    @Override
    public String getDefaultScheme() {
        return underlyingFactory.getDefaultScheme();
    }

}
