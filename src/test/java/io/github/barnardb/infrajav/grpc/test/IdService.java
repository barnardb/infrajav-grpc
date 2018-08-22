package io.github.barnardb.infrajav.grpc.test;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.internal.IoUtils;
import io.grpc.stub.ServerCalls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static com.google.common.base.Charsets.UTF_8;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;

public class IdService {

    public static final MethodDescriptor<String, String> METHOD_DESCRIPTOR = MethodDescriptor.<String, String>newBuilder()
            .setFullMethodName("id/getId")
            .setType(MethodDescriptor.MethodType.UNARY)
            .setRequestMarshaller(StringMarshaller.INSTANCE)
            .setResponseMarshaller(StringMarshaller.INSTANCE)
            .build();

    public static void withLocalIdServer(String id, Consumer<Integer> useServerPort) {
        Server server;
        try {
            server = ServerBuilder.forPort(0)
                    .addService(serviceDescriptorForId(id))
                    .build()
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            useServerPort.accept(server.getPort());
        } finally {
            server.shutdown();
        }
    }

    public static String getId(ManagedChannel channel) {
        return blockingUnaryCall(channel, IdService.METHOD_DESCRIPTOR, CallOptions.DEFAULT, "ID please :)");
    }

    private static ServerServiceDefinition serviceDescriptorForId(String id) {
        return ServerServiceDefinition.builder("id")
                .addMethod(
                        IdService.METHOD_DESCRIPTOR,
                        ServerCalls.asyncUnaryCall((req, resp) -> {
                            resp.onNext(id);
                            resp.onCompleted();
                        })
                ).build();
    }

    private static class StringMarshaller implements MethodDescriptor.Marshaller<String> {
        public static final StringMarshaller INSTANCE = new StringMarshaller();

        @Override
        public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes(UTF_8));
        }

        @Override
        public String parse(InputStream stream) {
            try {
                return new String(IoUtils.toByteArray(stream), UTF_8);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
