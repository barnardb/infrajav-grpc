# infrajav-grpc

Utilities for grpc-java.

## `ActiveNameResolverFactory`

Provides an active `NameResolver` that can be used to ensure that a GRPC channel target keeps its subchannels in sync with resolved addresses.

For example, suppose you want to connect to a GRPC server available at `service.example.com` on port 8443.
A common way would be to create a channel like this:

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress("service.example.com", 8443)
        .build();
```

If there is a single GRPC server,
or if `service.example.com` resolves to a single IP address for a level 7 (HTTP/2) load balancer,
this is an appropriate way to set up the channel.

However, if `service.example.com` resolves to a list of IP addresses,
we may want to do client-side load balancing, distributing our requests across the addresses
and recovering gracefully from problems with a connection to one of the addresses.
By default GRPC will just connect use the first address returned by the DNS resolver,
but we can tell it to open a subchannel (connection) for each address and do round-robin load balancing of outgoing requests:

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress("service.example.com", 8443)
        .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
        .build();
```

Great! But what happens when the list of addresses returned by DNS changes?
GRPC won't detect this automatically.
If there is a problem with one of the connections, it will re-resolve,
and update the set of subchannels, closing and creating them as necessary.
So if new addresses are used by new servers after deploying a new version,
GRPC will start to notice these as the old servers stop serving requests.
Also if the cluster scales down, it will notice this as the old servers stop serving requests.
But what if your cluster scales up due to increased load?
The old servers continue to work, so there's nothing to signal to GRPC that new servers are available.

This is where `ActiveNameResolverFactory` come in.
It periodically triggers a DNS resolution, ensuring that GRPC opens new subchannels and starts using new servers.

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress("service.example.com", 8443)
        .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
        .nameResolverFactory(new ActiveNameResolverFactory(2, MINUTES))
        .build();
```
