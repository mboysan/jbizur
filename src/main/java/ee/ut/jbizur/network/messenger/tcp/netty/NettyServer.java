package ee.ut.jbizur.network.messenger.tcp.netty;

import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.messenger.AbstractServer;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.role.Role;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.ReferenceCountUtil;
import org.pmw.tinylog.Logger;

import java.util.concurrent.atomic.AtomicReference;

public class NettyServer extends AbstractServer {

    private NettyServerInstance nettyServerInstance;

    public NettyServer(Role roleInstance) {
        super(roleInstance);
    }

    @Override
    public void startRecv(Address address) {
        if (!(address instanceof TCPAddress)) {
            throw new IllegalArgumentException("address is not a TCP address: " + address);
        }
        TCPAddress tcpAddress = (TCPAddress) address;
        nettyServerInstance = new NettyServerInstance(tcpAddress).start();
    }

    @Override
    public void shutdown() {
        nettyServerInstance.stop();
    }

    private class NettyServerInstance {
        final AtomicReference<ChannelFuture> channel = new AtomicReference<>(null);
        final EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        final TCPAddress tcpAddress;
        final ServerHandler serverHandler = new ServerHandler();

        NettyServerInstance(TCPAddress tcpAddress) {
            this.tcpAddress = tcpAddress;
        }

        NettyServerInstance start() {
            try {
                ServerBootstrap b = new ServerBootstrap(); // (2)
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class) // (3)
                        .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
//                                ch.pipeline().addLast(new ServerHandler());
                                ch.pipeline().addLast(
                                        new ObjectDecoder(ClassResolvers.softCachingResolver(ClassLoader.getSystemClassLoader())),
                                        new ObjectEncoder(),
                                        serverHandler);
                            }
                        })
//                        .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                        .childOption(ChannelOption.SO_KEEPALIVE, GeneralConfig.tcpKeepAlive()); // (6)

                // Bind and start to accept incoming connections.
                channel.set(b.bind(tcpAddress.getPortNumber()).sync()); // (7)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return this;
        }

        void stop() {
            try {
                if (serverHandler.ctx != null) {
                    serverHandler.ctx.close();
                }
                channel.get().channel().closeFuture();
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }
    }

    private class ServerHandler extends ChannelInboundHandlerAdapter {
        ChannelHandlerContext ctx;
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object o) { // (2)
            try {
                NetworkCommand command = (NetworkCommand) o;
                roleInstance.handleNetworkCommand(command);
            } finally {
                ReferenceCountUtil.release(o);
            }
        }
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }
    }
}
