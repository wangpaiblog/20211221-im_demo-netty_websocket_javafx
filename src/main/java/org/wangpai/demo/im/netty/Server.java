package org.wangpai.demo.im.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
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
    private String ip;

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
                pipeline.addLast("http-codec", new HttpServerCodec());
                // 定义分段请求聚合时，字节的最大长度
                pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                // 定义块写处理器
                pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                // 定义业务处理器
                pipeline.addLast("businessHandler", new WebsocketServerHandler(mainFace, ip, port));
            }
        });

        try {
            ChannelFuture channelFuture = bootstrap.bind().sync();
            ChannelFuture closeFuture = channelFuture.channel().closeFuture();
            closeFuture.sync();
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