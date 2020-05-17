package ee.ut.jbizur.network.io.tcp.netty;

import ee.ut.jbizur.network.io.AbstractClient;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.address.TCPAddress;
import ee.ut.jbizur.protocol.commands.net.NetworkCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NettyClient extends AbstractClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private final EventLoopGroup group;
    private SocketChannel channel;

    public NettyClient(String name, Address destAddress) {
        super(name, destAddress);
        this.group = new NioEventLoopGroup(1);
    }

    @Override
    protected void connect() throws IOException {
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ObjectEncoder());
                        }
                    });
            TCPAddress tcpAddress = (TCPAddress) getDestAddress();
            ChannelFuture f = b.connect(tcpAddress.getIp(), tcpAddress.getPortNumber()).sync();
            this.channel = (SocketChannel) f.channel();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    protected boolean isValid() {
        return channel != null;
    }

    @Override
    protected void send0(NetworkCommand command) {
        if (channel != null) {
            channel.writeAndFlush(command);
        }
    }

    @Override
    public void close() {
        group.shutdownGracefully();
        /*try {
            group.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }*/
        super.close();
    }
}
