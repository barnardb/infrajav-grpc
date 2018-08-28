package io.github.barnardb.infrajav.grpc.test;

import io.github.barnardb.infrajav.grpc.ActiveNameResolver;
import io.github.barnardb.infrajav.grpc.ActiveNameResolverFactory;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.internal.TestDnsNameResolverFactory;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.waitAtMost;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ActiveNameResolverTest {

    @Test
    public void shouldResolveWhenExplicitlyRequested() throws Exception {
        TestDnsNameResolverFactory underlyingFactory = new TestDnsNameResolverFactory();
        InetAddress initialAddress = InetAddress.getByAddress(new byte[]{1, 1, 1, 1});
        underlyingFactory.setAddresses("foo", initialAddress);

        NameResolver.Factory factory = new ActiveNameResolverFactory(underlyingFactory, 100, TimeUnit.SECONDS);

        try (TestLogHandler log = TestLogHandler.forClass(ActiveNameResolver.class)) {
            NameResolver nameResolver = factory.newNameResolver(new URI("dns:///foo:1234"), null);
            try {
                CapturingListener listener = new CapturingListener();
                nameResolver.start(listener);

                waitAtMost(50, TimeUnit.MILLISECONDS)
                        .pollDelay(10, TimeUnit.MILLISECONDS)
                        .ignoreExceptionsInstanceOf(AssertionError.class)
                        .until(() -> {
                            assertAll("Receives initial host",
                                    () -> assertThat("servers", listener.servers, hasItems(new EquivalentAddressGroup(new InetSocketAddress(initialAddress, 1234)))),
                                    () -> assertThat("attributes", listener.attributes, is(Attributes.EMPTY)),
                                    () -> assertThat("error", listener.error, nullValue())
                            );
                            return true;
                        });

                InetAddress updatedAddress = InetAddress.getByAddress(new byte[]{2, 2, 2, 2});
                underlyingFactory.setAddresses("foo", updatedAddress);

                assertThat(log.records, hasSize(0));

                nameResolver.refresh();

                assertThat(log.records.remove(0).getMessage(), is("Triggering explicitly requested refresh"));

                waitAtMost(50, TimeUnit.MILLISECONDS)
                        .pollDelay(10, TimeUnit.MILLISECONDS)
                        .ignoreExceptionsInstanceOf(AssertionError.class)
                        .until(() -> {
                            assertAll("Receives updated host",
                                    () -> assertThat("servers", listener.servers, hasItems(new EquivalentAddressGroup(new InetSocketAddress(updatedAddress, 1234)))),
                                    () -> assertThat("attributes", listener.attributes, is(Attributes.EMPTY)),
                                    () -> assertThat("error", listener.error, nullValue())
                            );
                            return true;
                        });

            } finally {
                nameResolver.shutdown();
            }
            assertThat(log.records, hasSize(0));
        }
    }

    @Test
    public void shouldActivelyResolve() throws Exception {
        TestDnsNameResolverFactory underlyingFactory = new TestDnsNameResolverFactory();
        InetAddress initialAddress = InetAddress.getByAddress(new byte[]{1, 1, 1, 1});
        underlyingFactory.setAddresses("foo", initialAddress);

        NameResolver.Factory factory = new ActiveNameResolverFactory(underlyingFactory, 300, TimeUnit.MILLISECONDS);

        try (TestLogHandler log = TestLogHandler.forClass(ActiveNameResolver.class)) {
            NameResolver nameResolver = factory.newNameResolver(new URI("dns:///foo:1234"), null);
            try {
                CapturingListener listener = new CapturingListener();
                nameResolver.start(listener);

                waitAtMost(50, TimeUnit.MILLISECONDS)
                        .pollDelay(10, TimeUnit.MILLISECONDS)
                        .ignoreExceptionsInstanceOf(AssertionError.class)
                        .until(() -> {
                            assertAll("Receives initial host",
                                    () -> assertThat("servers", listener.servers, hasItems(new EquivalentAddressGroup(new InetSocketAddress(initialAddress, 1234)))),
                                    () -> assertThat("attributes", listener.attributes, is(Attributes.EMPTY)),
                                    () -> assertThat("error", listener.error, nullValue())
                            );
                            return true;
                        });

                InetAddress updatedAddress = InetAddress.getByAddress(new byte[]{2, 2, 2, 2});
                underlyingFactory.setAddresses("foo", updatedAddress);

                assertThat(log.records, hasSize(0));

                waitAtMost(350, TimeUnit.MILLISECONDS)
                        .pollDelay(10, TimeUnit.MILLISECONDS)
                        .ignoreExceptionsInstanceOf(IndexOutOfBoundsException.class)
                        .until(() -> log.records.remove(0).getMessage(), is("Triggering scheduled refresh"));

                waitAtMost(50, TimeUnit.MILLISECONDS)
                        .pollDelay(10, TimeUnit.MILLISECONDS)
                        .ignoreExceptionsInstanceOf(AssertionError.class)
                        .until(() -> {
                            assertAll("Receives updated host",
                                    () -> assertThat("servers", listener.servers, hasItems(new EquivalentAddressGroup(new InetSocketAddress(updatedAddress, 1234)))),
                                    () -> assertThat("attributes", listener.attributes, is(Attributes.EMPTY)),
                                    () -> assertThat("error", listener.error, nullValue())
                            );
                            return true;
                        });

            } finally {
                nameResolver.shutdown();
            }
            assertThat(log.records, hasSize(0));
        }
    }

    @Test
    public void shouldHandleNullResolversFromTheUnderlyingFactory() throws Exception {
        DnsNameResolverProvider underlyingFactory = new DnsNameResolverProvider();
        assumeTrue(underlyingFactory.newNameResolver(new URI("foo:2134"), null) == null);

        NameResolver.Factory factory = new ActiveNameResolverFactory(underlyingFactory, 1, TimeUnit.HOURS);
        assertThat(factory.newNameResolver(new URI("foo:2134"), null), nullValue());
    }

}
