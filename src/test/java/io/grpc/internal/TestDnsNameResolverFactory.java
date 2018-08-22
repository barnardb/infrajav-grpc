package io.grpc.internal;

import io.grpc.Attributes;
import io.grpc.NameResolver;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;

/**
 * A DnsNameResolverProvider with a stubbed AddressResolver.
 * <p>
 * Allows the addresses a name will resolve to in the created resolvers to be set at will during a test.
 * <p>
 * This class needs to be in the io.grpc.internal so that it can set the AddressResolver, which is package-private.
 */
public class TestDnsNameResolverFactory extends NameResolver.Factory {

    private final Map<String, List<InetAddress>> addresses = new ConcurrentHashMap<>();
    private final DnsNameResolverProvider dnsNameResolverProvider = new DnsNameResolverProvider();

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        DnsNameResolver nameResolver = dnsNameResolverProvider.newNameResolver(targetUri, params);
        if (nameResolver != null)
            nameResolver.setAddressResolver(name -> {
                List<InetAddress> resolvedAddresses = addresses.get(name);
                if (resolvedAddresses == null)
                    throw new UnknownHostException("Can't resolve " + name);
                return resolvedAddresses;
            });
        return nameResolver;
    }

    @Override
    public String getDefaultScheme() {
        return dnsNameResolverProvider.getDefaultScheme();
    }

    public void setAddresses(String name, InetAddress... addresses) {
        this.addresses.put(name, asList(addresses));
    }

}
