package org.wangpai.demo.im.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.wangpai.demo.im.view.MainFace;

/**
 * @since 2021-12-8
 */
public class TextServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private MainFace mainFace;

    public TextServerHandler(MainFace mainFace) {
        super();
        this.mainFace = mainFace;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        this.mainFace.receive(msg.text());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TextWebSocketFrame) {
            super.channelRead(ctx, msg);
        } else { // 如果 msg 不是文本，交给流水线上后续的处理器来处理
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
