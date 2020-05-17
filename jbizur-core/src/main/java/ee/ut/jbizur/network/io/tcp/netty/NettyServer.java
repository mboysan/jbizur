package ee.ut.jbizur.network.io.tcp.netty;

import ee.ut.jbizur.network.io.AbstractServer;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.address.TCPAddress;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NettyServer extends AbstractServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private Channel channel;

    public NettyServer(String name, Address serverAddress) {
        super(name, serverAddress);
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
    }

    @Override
    public void start() throws IOException {
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(getClass().getClassLoader())));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<NetworkCommand>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, NetworkCommand msg) {
                                    recv(msg);
                                }
                            });
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            channel = b.bind(((TCPAddress) getServerAddress()).getPortNumber()).sync().channel();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        super.start();
    }

    @Override
    public void close() {
        try {
            workerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        try {
            bossGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (channel != null) {
            channel.close();
        }
        super.close();
    }
}
