package org.wangpai.demo.im.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import org.wangpai.demo.im.view.MainFace;

/**
 * @since 2021-12-1
 */
public class WebsocketServerHandler extends ChannelInboundHandlerAdapter {
    private WebSocketServerHandshaker handshaker;

    private MainFace mainFace;

    private String ip;

    private int port;

    public WebsocketServerHandler(MainFace mainFace, String ip, int port) {
        this.mainFace = mainFace;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object obj) throws Exception {
        if (obj instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) obj);
        } else if (obj instanceof WebSocketFrame) {
            handleWebSocketRequest(ctx, (WebSocketFrame) obj);
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

    private String getWebsocketUrl() {
        return String.format("ws://%s:%d/ws", this.ip, this.port);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
            var rsp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            ByteBuf buf = Unpooled.copiedBuffer(rsp.status().toString(), CharsetUtil.UTF_8);
            rsp.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(rsp, rsp.content().readableBytes());
            ctx.channel().writeAndFlush(rsp).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        // 此方法的第一个参数应该是自己想要转发客户端请求的接收者，但不知为何，此处的 URL 可以为任意值
        handshaker = new WebSocketServerHandshakerFactory(this.getWebsocketUrl(), null, false)
                .newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    private void handleWebSocketRequest(ChannelHandlerContext ctx, WebSocketFrame req) throws Exception {
        if (req instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) req.retain());
            return;
        }
        if (req instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(req.content().retain()));
            return;
        }
        if (!(req instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException("不支持非文本消息"); // TODO
        }
        if (ctx == null || this.handshaker == null || ctx.isRemoved()) {
            throw new Exception("握手失败"); // TODO
        }

        this.mainFace.receive(((TextWebSocketFrame) req).text());
    }
}
