package network.client.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import network.NetworkManager;
import network.communication.IMessageHandler;
import protocol.CommandMarshaller;
import protocol.commands.BaseCommand;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class NettyClientHandler extends ChannelInboundHandlerAdapter implements IMessageHandler{

    private final NetworkManager networkManager;
    private final CommandMarshaller commandMarshaller;

    private CountDownLatch readyLatch;
    private ChannelHandlerContext ctx;

    /**
     * Creates a network.client-side handler.
     */
    public NettyClientHandler(NetworkManager networkManager) {
        this.networkManager = networkManager;
        this.readyLatch = new CountDownLatch(1);
        this.commandMarshaller = new CommandMarshaller();
    }

    @Override
    public void sendCommand(BaseCommand command) {
        try {
            readyLatch.await();

            String msg = commandMarshaller.marshall(command);
            ctx.writeAndFlush(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
        } catch (JsonProcessingException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        readyLatch.countDown();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            BaseCommand command = commandMarshaller.unmarshall((String) msg);
            networkManager.receiveCommand(command);
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