package org.wangpai.demo.im.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.util.CharsetUtil;

/**
 * @since 2021-12-2
 */
public class WebsocketClientHandler extends ChannelInboundHandlerAdapter {
    private WebSocketClientHandshaker handshaker;

    private ChannelPromise channelPromise;

    public WebsocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object obj) {
        if (!this.handshaker.isHandshakeComplete()) { // 如果三报文握手的流程还没有走完
            // 如果现在进行的是三报文握手中的第三握手
            finishHandshake(ctx, (FullHttpResponse) obj);
        } else if (obj instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) obj;
            var msg = String.format("Unexpected FullHttpResponse, status=%s, content=%s",
                    response.status(), response.content().toString(CharsetUtil.UTF_8));
            System.out.println(msg); // FIXME：日志
        } else if (obj instanceof WebSocketFrame) {
            // TODO：如果需要服务器反馈信息，可在此添加业务
            handleWebSocketResponse(ctx, (WebSocketFrame) obj);
        } else {
            // 此分支不应该发生
        }

        try {
            super.channelRead(ctx, obj);
        } catch (Exception exception) {
            exception.printStackTrace(); // TODO：日志
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.channelPromise = ctx.newPromise();
    }

    public ChannelFuture sync() throws InterruptedException {
        return this.channelPromise.sync();
    }

    private void finishHandshake(ChannelHandlerContext ctx, FullHttpResponse response) {
        try {
            this.handshaker.finishHandshake(ctx.channel(), response);
            //设置成功
            this.channelPromise.setSuccess();
        } catch (WebSocketHandshakeException exception) {
            FullHttpResponse rsp = response;
            String errorMsg = String.format("WebSocket Client failed to connect, status=%s, reason=%s",
                    rsp.status(), rsp.content().toString(CharsetUtil.UTF_8));
            this.channelPromise.setFailure(new Exception(errorMsg)); // TODO：日志
        }
    }

    private void handleWebSocketResponse(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame; // TODO
        } else if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binFrame = (BinaryWebSocketFrame) frame; // TODO
        } else if (frame instanceof PongWebSocketFrame) {
            // TODO
        } else if (frame instanceof CloseWebSocketFrame) {
            // TODO
            ctx.channel().close();
        }
    }
}
