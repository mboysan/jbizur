package network.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;
import network.NetworkManager;
import network.communication.IMessageHandler;
import network.server.IServer;
import network.server.ServerConfig;

public class NettyServer implements IServer {

    private transient ServerState state = ServerState.initializing;

    private enum ServerState {
        initializing,
        initialized,
        started,
        stopped
    }

    private final NetworkManager networkManager;
    private final IMessageHandler messageHandler;

    private final ServerConfig serverConfig;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap b;

    public NettyServer(NetworkManager networkManager, ServerConfig serverConfig) throws Exception {
        this.networkManager = networkManager;
        this.serverConfig = serverConfig;
        this.messageHandler = new NettyServerHandler(networkManager);
        init();
    }

    @Override
    public void init() throws Exception {
        if (state.ordinal() >= ServerState.initialized.ordinal()) {
            return;
        }

        // Configure SSL.
        final SslContext sslCtx;
        if (serverConfig.isSsl()) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the network.server.
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        try {
            this.b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc()));
                            }
                            // Decoders
                            p.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                            // Encoders
                            p.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));
                            // Network Handlers
                            p.addLast((NettyServerHandler) messageHandler);
                        }
                    });

            state = ServerState.initialized;

        } catch (Exception e) {
            e.printStackTrace();
            stop();
            throw e;
        }
    }

    @Override
    public void start() throws Exception {
        if (state == ServerState.started) {
            return;
        }
        if (b != null) {
            // Start the network.server.
            ChannelFuture f = b.bind(serverConfig.getPort()).sync();

            state = ServerState.started;

            // Wait until the network.server socket is closed.
            f.channel().closeFuture().sync();
        }
    }

    @Override
    public void stop() {
        if (state != ServerState.started) {
            return;
        }
        // Shut down all event loops to terminate all threads.
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        state = ServerState.stopped;
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
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    @Override
    public IMessageHandler getMessageHandler() {
        return messageHandler;
    }
}
