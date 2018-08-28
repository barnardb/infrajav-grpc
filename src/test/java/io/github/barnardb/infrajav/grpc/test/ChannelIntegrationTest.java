package io.github.barnardb.infrajav.grpc.test;

import io.github.barnardb.infrajav.grpc.ActiveNameResolverFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.util.RoundRobinLoadBalancerFactory;
import org.junit.jupiter.api.Test;

import static io.github.barnardb.infrajav.grpc.test.IdService.getId;
import static io.github.barnardb.infrajav.grpc.test.IdService.withLocalIdServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.waitAtMost;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ChannelIntegrationTest {

    @Test
    public void shouldWorkWithLocalhostWhenWrappingStandardResolver() {
        withLocalIdServer("A", aPort -> {
            ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", aPort)
                    .nameResolverFactory(new ActiveNameResolverFactory(1, SECONDS))
                    .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
                    .usePlaintext()
                    .build();
            assertThat(getId(channel), is("A"));
        });
    }

    @Test
    public void shouldActivelyRefreshSubchannels() {
        withLocalIdServer("A", aPort -> {
            withLocalIdServer("B", bPort -> {
                LocalhostResolverFactory localhostResolver = new LocalhostResolverFactory();
                localhostResolver.setPorts("test-target", aPort);
                ManagedChannel channel = ManagedChannelBuilder.forTarget("test-target")
                        .nameResolverFactory(new ActiveNameResolverFactory(localhostResolver, 1, SECONDS))
                        .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
                        .usePlaintext()
                        .build();
                assertThat(getId(channel), is("A"));
                assertThat(getId(channel), is("A"));
                assertThat(getId(channel), is("A"));

                localhostResolver.setPorts("test-target", aPort, bPort);
                waitAtMost(3, SECONDS).ignoreExceptionsInstanceOf(AssertionError.class)
                        .until(() -> getId(channel), is("B"));
                assertThat(getId(channel), is("A"));
                assertThat(getId(channel), is("B"));
                assertThat(getId(channel), is("A"));
                assertThat(getId(channel), is("B"));
            });
        });
    }

}
