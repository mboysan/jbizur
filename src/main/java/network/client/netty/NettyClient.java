package network.client.netty;

import network.client.ClientMessageHandler;
import network.client.IClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import network.communication.IMessageHandler;

public class NettyClient implements IClient {

    private transient ClientState state = ClientState.initializing;

    private enum ClientState {
        initializing,
        initialized,
        started,
        stopped
    }

    private final IMessageHandler messageHandler;

    private final boolean ssl;
    private final String hostAddress;
    private final int hostPort;
    private final int messageSize;

    private EventLoopGroup group;
    private Bootstrap b;

    public NettyClient(boolean ssl, String hostAddress, int hostPort, int messageSize) throws Exception {
        this.ssl = ssl;
        this.hostAddress = hostAddress;
        this.hostPort = hostPort;
        this.messageSize = messageSize;

        this.messageHandler = new ClientMessageHandler(this);

        init();
    }

    @Override
    public void init() throws Exception {
        if (state.ordinal() >= ClientState.initialized.ordinal()) {
            return;
        }

        // Configure SSL.git
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        // Configure the network.client.
        this.group = new NioEventLoopGroup();
        try {
            this.b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), hostAddress, hostPort));
                            }
                            //p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new NettyClientHandler(messageHandler));
                        }
                    });

            // Start the network.client.
            ChannelFuture f = b.connect(hostAddress, hostPort).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

    @Override
    public void start() throws Exception {
        if (state == ClientState.started) {
            return;
        }
        if (b != null) {
            // Start the network.client.
            ChannelFuture f = b.connect(hostAddress, hostPort).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        }
    }

    @Override
    public void stop() throws Exception {
        if (state != ClientState.started) {
            return;
        }
        if (this.group != null) {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();

            state = ClientState.stopped;
        }
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IMessageHandler getMessageHandler() {
        return messageHandler;
    }
}
