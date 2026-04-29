package com.frontleaves.plugins.lib.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * gRPC 客户端认证拦截器，在每个请求的 metadata 中注入 plugin-name 和 plugin-secret-key。
 *
 * @author xiao_lfeng
 * @version 1.0.0
 */
public class ClientAuthInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> PLUGIN_NAME_KEY =
            Metadata.Key.of("plugin-name", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> PLUGIN_SECRET_KEY =
            Metadata.Key.of("plugin-secret-key", Metadata.ASCII_STRING_MARSHALLER);

    private final String pluginName;
    private final String secretKey;

    public ClientAuthInterceptor(@NotNull String pluginName, @NotNull String secretKey) {
        this.pluginName = pluginName;
        this.secretKey = secretKey;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(PLUGIN_NAME_KEY, pluginName);
                headers.put(PLUGIN_SECRET_KEY, secretKey);
                super.start(responseListener, headers);
            }
        };
    }
}
