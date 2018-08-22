package io.github.barnardb.infrajav.grpc.test;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Resolves names to local addresses with different ports.
 * <p>
 * Useful for integration tests that run against locally hosted services.
 */
public class LocalhostResolverFactory extends io.grpc.NameResolver.Factory {
    private volatile Map<String, List<EquivalentAddressGroup>> addressGroupsByName = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        return new NameResolver() {
            private Listener listener;

            @Override
            public String getServiceAuthority() {
                return "localhost";
            }

            @Override
            public void start(Listener listener) {
                this.listener = listener;
                refresh();
            }

            @Override
            public void refresh() {
                listener.onAddresses(addressGroupsByName.getOrDefault(targetUri.toString(), emptyList()), Attributes.EMPTY);
            }

            @Override
            public void shutdown() {
                // nothing to do
            }
        };
    }

    @Override
    public String getDefaultScheme() {
        return null;
    }

    public void setPorts(String name, Integer... ports) {
        addressGroupsByName.put(
                name,
                Arrays.stream(ports)
                        .map(p -> new EquivalentAddressGroup(new InetSocketAddress(p)))
                        .collect(Collectors.toList())
        );
    }
}
