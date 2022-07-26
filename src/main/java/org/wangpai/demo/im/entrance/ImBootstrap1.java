package org.wangpai.demo.im.entrance;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.wangpai.demo.im.netty.Client;
import org.wangpai.demo.im.netty.Server;
import org.wangpai.demo.im.util.CentralDatabase;
import org.wangpai.demo.im.util.Multithreading;
import org.wangpai.demo.im.view.MainFace;

/**
 * @since 2021-12-1
 */
public class ImBootstrap1 extends Application {
    @Override
    public void start(Stage stage) {
        var myServerPort = 3311;
        var server = Server.getInstance().setPort(myServerPort);
        Multithreading.execute(() -> server.start());
        var otherServerIp = "127.0.0.1";
        var otherServerPort = 3322;
        var client = Client.getInstance().setIp(otherServerIp).setPort(otherServerPort);

        var mainFace = MainFace.getInstance();
        mainFace.setClient(client);
        server.setMainFace(mainFace);

        Scene scene = new Scene(mainFace.getComponent(), 500, 500);
        stage.setTitle("ImApp1");
        stage.setScene(scene);
        stage.setX(200);
        stage.setY(120);
        stage.show();

        stage.setOnCloseRequest(event -> {
            client.destroy();
            server.destroy();
            CentralDatabase.multithreadingClosed();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch();
    }
}