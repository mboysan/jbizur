package ee.ut.jbizur.network.messenger.tcp.netty;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.messenger.AbstractClient;
import ee.ut.jbizur.protocol.commands.NetworkCommand;
import ee.ut.jbizur.protocol.commands.ping.SignalEnd_NC;
import ee.ut.jbizur.role.Role;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.pmw.tinylog.Logger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class NettyClient extends AbstractClient {

    private final Map<String,NettyClientInstance> clientMap = new ConcurrentHashMap<>();
    private final ExecutorService clientShutdownService;


    public NettyClient(Role roleInstance) {
        super(roleInstance);
        this.clientShutdownService = Executors.newCachedThreadPool();
    }

    @Override
    protected <T> T connect(Address address) {
        if (!(address instanceof TCPAddress)) {
            throw new IllegalArgumentException("address must be a TCP address");
        }
        TCPAddress tcpAddress = (TCPAddress) address;
        NettyClientInstance client;
        if (keepAlive) {
            client = clientMap.get(address.toString());
            if (client == null) {
                client = new NettyClientInstance(tcpAddress).start();
                clientMap.put(tcpAddress.toString(), client);
            }
        } else {
            client = new NettyClientInstance(tcpAddress).start();
        }
        return (T) client;
    }

    @Override
    public void send(NetworkCommand command) {
        NettyClientInstance client = connect(command.getReceiverAddress());
        client.send(command);
        if (command instanceof SignalEnd_NC) {
            if (keepAlive) {
                clientMap.forEach((s, nettyClientInstance) -> {
                    nettyClientInstance.stop();
                });
            }
            clientShutdownService.shutdown();
        }
        if (!keepAlive) {
            try {
                clientShutdownService.execute(() -> client.stop());
            } catch (RejectedExecutionException e) {
                Logger.warn(e, "");
                client.stop();
            }
        }
    }

    private static class NettyClientInstance {
        TCPAddress tcpAddress;
        EventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture channelFuture;
        ClientHandler clientHandler = new ClientHandler(this);

        boolean isChannelActive = false;
        final Object channelLock = new Object();

        public NettyClientInstance(TCPAddress tcpAddress) {
            this.tcpAddress = tcpAddress;
        }

        NettyClientInstance start() {
            Bootstrap clientBootstrap = new Bootstrap();

            clientBootstrap.group(group);
            clientBootstrap.channel(NioSocketChannel.class);
            clientBootstrap.remoteAddress(new InetSocketAddress(tcpAddress.getIp(), tcpAddress.getPortNumber()));
            clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
//                    socketChannel.pipeline().addLast(clientHandler);
                    socketChannel.pipeline().addLast(
                            new ObjectDecoder(ClassResolvers.softCachingResolver(ClassLoader.getSystemClassLoader())),
                            new ObjectEncoder(),
                            clientHandler);

                }
            });
            try {
                channelFuture = clientBootstrap.connect().sync();
            } catch (InterruptedException e) {
                Logger.error(e, "");
            }
            return this;
        }

        void stop() {
            try {
                clientHandler.stop();
            } catch (Exception e) {
                Logger.error(e, "");
            }
            try {
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Logger.error(e, "");
            } finally {
                try {
                    group.shutdownGracefully().sync();
                } catch (InterruptedException e) {
                    Logger.error(e, "");
                }
            }
        }

        void send(NetworkCommand command) {
            waitForChannel();
            clientHandler.send(command);
        }

        void waitForChannel() {
            synchronized (channelLock) {
                while (!isChannelActive) {
                    try {
                        channelLock.wait();
                    } catch (InterruptedException e) {
                        Logger.error(e, "");
                    }
                }
            }
        }

        void signalChannelActive() {
            synchronized (channelLock) {
                isChannelActive = true;
                channelLock.notifyAll();
            }
        }
    }

    public static class ClientHandler extends SimpleChannelInboundHandler {
        ChannelHandlerContext ctx;

        final NettyClientInstance client;

        ClientHandler(NettyClientInstance client) {
            this.client = client;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            this.ctx = ctx;
            client.signalChannelActive();
        }

        void send(NetworkCommand command) {
            ctx.writeAndFlush(command);
        }

        void stop() {
            if (ctx != null) {
                ctx.close();
            }
        }

        @Override
        public void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) {
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable cause) {
            Logger.error(cause, "");
            channelHandlerContext.close();
        }
    }
}
