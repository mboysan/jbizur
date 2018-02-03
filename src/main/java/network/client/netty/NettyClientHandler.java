package network.client.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.communication.IMessageHandler;

public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    private final IMessageHandler messageHandler;
    private final int messageSize;
    private final ByteBuf firstMessage;

    private ChannelHandlerContext ctx;

    /**
     * Creates a network.client-side handler.
     */
    public NettyClientHandler(IMessageHandler messageHandler) {
        this(messageHandler,256);
    }

    public NettyClientHandler(IMessageHandler messageHandler, int messageSize) {
        this.messageHandler = messageHandler;

        this.messageSize = messageSize;

        firstMessage = Unpooled.buffer(messageSize);
        for (int i = 0; i < firstMessage.capacity(); i ++) {
            firstMessage.writeByte((byte) i);
        }
    }

    

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(firstMessage);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.write(msg);
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