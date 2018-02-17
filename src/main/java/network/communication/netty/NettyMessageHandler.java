package network.communication.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import network.INetworkOperator;
import network.communication.IMessageHandler;
import protocol.CommandMarshaller;
import protocol.commands.NetworkCommand;
import protocol.commands.internal.ClientConnectionReady;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@ChannelHandler.Sharable
public class NettyMessageHandler extends ChannelInboundHandlerAdapter implements IMessageHandler {

    private final INetworkOperator networkOperator;
    private final CommandMarshaller commandMarshaller;

    private CountDownLatch readyLatch;
    private ChannelHandlerContext ctx;

    /**
     * Creates a network.client-side handler.
     */
    public NettyMessageHandler(INetworkOperator networkOperator) {
        this.networkOperator = networkOperator;
        this.readyLatch = new CountDownLatch(1);
        this.commandMarshaller = new CommandMarshaller();
    }

    @Override
    public void sendCommand(NetworkCommand command) {
        try {
            readyLatch.await();

            String msg = commandMarshaller.marshall(command);
            ctx.writeAndFlush(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
        } catch (JsonProcessingException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receiveCommand(NetworkCommand command) {
        networkOperator.notifyOperator(command, this);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        readyLatch.countDown();
        networkOperator.notifyOperator(new ClientConnectionReady(), this);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            NetworkCommand command = commandMarshaller.unmarshall((String) msg);
            receiveCommand(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
