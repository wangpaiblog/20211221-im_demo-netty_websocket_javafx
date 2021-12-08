package org.wangpai.demo.im.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @since 2021-12-1
 */
public class Client {
    private String otherServerIp;

    private int otherServerPort;

    public Client setIp(String otherServerIp) {
        this.otherServerIp = otherServerIp;
        return this;
    }

    public Client setPort(int otherServerPort) {
        this.otherServerPort = otherServerPort;
        return this;
    }

    private Channel channel;

    private EventLoopGroup workerLoopGroup = new NioEventLoopGroup();

    public Client start() {
        var handshaker = this.getWebSocketClientHandshaker();
        var businessHandler = new WebsocketClientHandler(handshaker);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        // 设置接收端的 IP 和端口号，但实际上，自己作为发送端也会为自己自动生成一个端口号
        bootstrap.remoteAddress(otherServerIp, otherServerPort);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                var pipeline = ch.pipeline();
                // 定义客户端 HTTP 编解码器
                pipeline.addLast(new HttpClientCodec());
                // 定义分段请求聚合时，字节的最大长度
                pipeline.addLast(new HttpObjectAggregator(65535));
                // 定义块写处理器
                pipeline.addLast(new ChunkedWriteHandler());
                // 定义业务处理器
                pipeline.addLast("businessHandler", businessHandler);
            }
        });

        ChannelFuture future = bootstrap.connect();
        future.addListener((ChannelFuture futureListener) -> {
            if (futureListener.isSuccess()) {
                System.out.println("客户端连接成功"); // FIXME：日志
            } else {
                System.out.println("客户端连接失败"); // FIXME：日志
            }
        });
        try {
            future.sync();
        } catch (Exception exception) {
            exception.printStackTrace(); // FIXME：日志
        }

        this.channel = future.channel();
        handshaker.handshake(channel);
        try {
            businessHandler.sync();
        } catch (InterruptedException exception) {
            exception.printStackTrace(); // FIXME：日志
        }
        return this;
    }

    private String generateWebsocketUrl(String ip, int port, String relativePath) {
        return String.format("ws://%s:%d/%s", ip, port, relativePath);
    }

    /**
     * 进行三报文握手中的第一握手，由客户端发起
     *
     * @since 2021-12-2
     */
    private WebSocketClientHandshaker getWebSocketClientHandshaker() {
        URI websocketUri = null;
        try {
            // 对于这个 URL，只有 relativePath 是起作用的，ip、port 不起使用。原因不明
            websocketUri = new URI(this.generateWebsocketUrl(this.otherServerIp, this.otherServerPort,
                    Protocol.WEBSOCKET_PREFIX_PATH));
        } catch (URISyntaxException exception) {
            exception.printStackTrace(); // FIXME：日志
        }
        var httpHeaders = new DefaultHttpHeaders();
        var handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                websocketUri, WebSocketVersion.V13, null, false, httpHeaders);
        return handshaker;
    }

    public void send(String msg) {
        channel.writeAndFlush(new TextWebSocketFrame(msg));
    }

    public void destroy() {
        this.workerLoopGroup.shutdownGracefully();
    }

    private Client() {
        super();
    }

    public static Client getInstance() {
        return new Client();
    }
}