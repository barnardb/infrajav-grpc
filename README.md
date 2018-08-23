# infrajav-grpc

Utilities for grpc-java.

[![Build Status](https://travis-ci.org/barnardb/infrajav-grpc.svg?branch=master)](https://travis-ci.org/barnardb/infrajav-grpc)
[![Download](https://api.bintray.com/packages/barnardb/maven/infrajav-grpc/images/download.svg)](https://bintray.com/barnardb/maven/infrajav-grpc/_latestVersion)

## `ActiveNameResolverFactory`

infrajav-grpc provides an `ActiveNameResolverFactory` class, which creates active GRPC `NameResolver`s that periodically retrigger name resolution. When used together with a `RoundRobinLoadBalancerFactory`, this ensures that a grpc-java channel keeps a subchannel open to each address currently being returned by a name resolver, even in the absense of channel closures of failures. This means that if you have a DNS name that resolves to one IP address per node in a cluster (e.g. such as the DNS name of a headless service in Kubernetes), you can straightforwardly do client-side load balancing with grpc-java. Even if your DNS name only resolves to one address, it also allows you to respond to changes in the DNS record even when the old address is still serving requests.

### When and How to Use

#### Client-Side Load Balancing

Use the `ActiveNameResolverFactory` when:
- The GRPC service you want to connect to has instances running multiple hosts.
- It doesn't matter which instance each GRPC call is served by.
- Your client application can connect directly to each host running the service.
- You have a (DNS) name that resolves to one address per instance.

If all of these conditions hold, you may well benefit by setting up your GRPC channel for client-side load balancing, like this:

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress("service.example.com", 8443)
        .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
        .nameResolverFactory(new ActiveNameResolverFactory(2, MINUTES))
        .build();
```

If you only used the load balancer by not the active name resolver, your GRPC channel would only re-resolve the name when one of it's existing connections dies. This would work well during rolling deployments or when a cluster is scaling down, as there will regularly be a small number of nodes closing their connections, triggering name-resolution and the establishment of new connections to any new nodes (in the deployment scenario). But what if the cluster scales up and adds new nodes? If none of the old nodes go away, your GRPC channel won't detect any events that would cause it to retrigger name resolution, so it won't be able to start sending traffic to those new nodes. That's why the `ActiveNameResolverFactory` is necessary to complete the client-side load balancing solution.

A load balancer factory of some kind is also necessary, becauae by default GRPC will just connect use the first address returned by the DNS resolver, rather than to all of them. Hence the `RoundRobinLoadBalancerFactory` above.

#### Responding to DNS Changes

Your GRPC channel may only have a single subchannel connected to a single address, such as when the GRPC service you are connecting to is running on a single host, or is behind a level 7 (HTTP/2) load balancer. In this situation you don't need a client-side load balancer, and you may be fine without an active name resolver.

If the remote service signals that it's closing the connection or if the connection dies, GRPC will trigger a name re-resolution. So if a new deployment happens by bringing node B up, changing the DNS record to point to B instead of A, and then shutting A down, when your GRPC channel loses its connection to A it will re-resolve the name and open a connection to B.

But what if you want to leave node A fully up and running, perhaps to try to understand how it got into some anomalous state.?In this case, your client application may still remain happily connected to A, and fail to notice that the DNS name for the server now exclusily resolves to B's address.

By using an `ActiveNameResolverFactory`, you ensure your GRPC channel will pick up on DNS changes an act on them in a timely manner.

```java
ManagedChannel channel = ManagedChannelBuilder.forAddress("service.example.com", 8443)
        .nameResolverFactory(new ActiveNameResolverFactory(2, MINUTES))
        .build();
```
