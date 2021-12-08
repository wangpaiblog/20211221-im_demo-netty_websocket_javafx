package org.wangpai.demo.im.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.wangpai.demo.im.view.MainFace;

/**
 * @since 2021-12-1
 */
@Accessors(chain = true)
public class Server {
    @Setter
    private int port;

    @Setter
    private MainFace mainFace;

    private EventLoopGroup bossLoopGroup = new NioEventLoopGroup(1);

    private EventLoopGroup workerLoopGroup = new NioEventLoopGroup();

    public Server start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossLoopGroup, this.workerLoopGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.localAddress(port);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                var pipeline = ch.pipeline();
                // 定义服务端 HTTP 编解码器
                pipeline.addLast(new HttpServerCodec());
                // 定义分段请求聚合时，字节的最大长度
                pipeline.addLast(new HttpObjectAggregator(65535));
                // 定义块写处理器
                pipeline.addLast(new ChunkedWriteHandler());
                // 设置监听的路径
                pipeline.addLast(new WebSocketServerProtocolHandler("/" + Protocol.WEBSOCKET_PREFIX_PATH));

                // 定义业务处理器-文本处理器
                pipeline.addLast(new TextServerHandler(mainFace));
            }
        });

        try {
            bootstrap.bind().sync()
                    .channel().closeFuture().sync();
        } catch (Exception exception) {
            exception.printStackTrace(); // FIXME：日志
        } finally {
            this.workerLoopGroup.shutdownGracefully();
            this.bossLoopGroup.shutdownGracefully();
        }

        return this;
    }

    public void destroy() {
        this.workerLoopGroup.shutdownGracefully();
        this.bossLoopGroup.shutdownGracefully();
    }

    private Server() {
        super();
    }

    public static Server getInstance() {
        return new Server();
    }
}