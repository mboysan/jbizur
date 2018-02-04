/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package network.server.netty;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import network.communication.IMessageHandler;
import protocol.CommandMarshaller;
import protocol.commands.GenericCommand;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Handler implementation for the echo network.server.
 */
@Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter implements IMessageHandler{

    private final CommandMarshaller commandMarshaller;

    private CountDownLatch readyLatch;
    private ChannelHandlerContext ctx;

    public NettyServerHandler() {
        System.out.println("NettyServerHandler + " + UUID.randomUUID().toString());
        this.readyLatch = new CountDownLatch(1);
        this.commandMarshaller = new CommandMarshaller();
    }

    @Override
    public void sendCommand(GenericCommand command) {
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
            GenericCommand command = commandMarshaller.unmarshall((String) msg);
//            new CommandProcessor().processCommand(command);
            //TODO: process command
            String testId = UUID.randomUUID().toString();
            System.out.println("FROM CLIENT: serverId: " + testId + " | "+ command.toString());
            command.setSenderId(testId);
            sendCommand(command);
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
