package io.github.barnardb.infrajav.grpc.test;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;

import java.util.List;

/**
 * Captures the most recent events emitted by a NameResolver.
 */
class CapturingListener implements NameResolver.Listener {
    volatile List<EquivalentAddressGroup> servers;
    volatile Attributes attributes;
    volatile Status error;

    @Override
    public void onAddresses(List<EquivalentAddressGroup> servers, Attributes attributes) {
        this.servers = servers;
        this.attributes = attributes;
    }

    @Override
    public void onError(Status error) {
        this.error = error;
    }
}
