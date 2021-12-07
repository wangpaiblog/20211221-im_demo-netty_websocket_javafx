package org.wangpai.demo.im.view;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.wangpai.demo.im.netty.Client;
import org.wangpai.demo.im.util.Multithreading;

/**
 * @since 2021-11-30
 */
@Accessors(chain = true)
public class MainFace {
    @FXML
    private VBox mainFace;

    @FXML
    private TextArea receiver;

    @FXML
    private TextArea input;

    @Setter
    private Client client;

    /**
     * 本客户端后台是否已启动
     *
     * @since 2021-12-1
     */
    private boolean isStarted;

    @FXML
    public void onKeyPressedInput(KeyEvent keyEvent) {
        // 如果按下了回车键
        if (keyEvent.getCode() == KeyCode.ENTER) {
            // 获得此时的光标位置。此位置为刚刚输入的换行符之后
            var caretPosition = this.input.getCaretPosition();

            // 如果已经按下的按键中包含 Control 键
            if (!keyEvent.isControlDown()) { // 如果输入的不是组合键 `Ctrl+Enter`，去掉换行符，然后将文本发送
                // 获得输入文本，此文本包含刚刚输入的换行符
                var text = this.input.getText();
                // 获得换行符两边的文本
                var front = text.substring(0, caretPosition - 1);
                var end = text.substring(caretPosition);
                this.input.setText(front + end);
                this.onActionSend(null); // 模拟发送

                /*----- 如果希望发送后保留输入框文本，需要只使用下面这行代码，然后去掉清除文本框的代码 -------*/
                // this.textArea.positionCaret(caretPosition - 1); // TODO
            } else {
                // 获得输入文本，此文本不包含刚刚输入的换行符
                var text = this.input.getText();
                // 获得光标两边的文本
                var front = text.substring(0, caretPosition);
                var end = text.substring(caretPosition);
                // 在光标处插入换行符
                this.input.setText(front + System.lineSeparator() + end);
                // 将光标移至换行符
                this.input.positionCaret(caretPosition + 1);
            }
        }
    }

    @FXML
    public void onActionSend(ActionEvent event) {
        System.out.println("正在发送信息...");
        System.out.println(this.input.getText());
        var msg = this.input.getText();

        Multithreading.execute(() -> {
            // 让客户端后台懒启动。避免先于服务端启动而引发异常
            if (!isStarted) {
                this.client.start();
                isStarted = true;
            }

            Platform.runLater(() -> {
                this.client.send(msg);
            });
        });

        this.input.requestFocus();
        /*----- 如果希望发送后清除输入框文本，使用下面这行代码 -------*/
        this.input.clear(); // TODO
    }

    public void receive(String msg) {
        Platform.runLater(() -> {
            this.receiver.appendText(System.lineSeparator());
            this.receiver.appendText(msg);
        });
    }

    /**
     * 此方法不能手动调用触发，且只能声明为 public
     */
    public MainFace() {
        super();
    }

    public static MainFace getInstance() {
        /**
         * 注意：此路径是以 resources 下 XXX.class 的类所在 模块 及 包 中的文件路径为相对路径。
         * 例如，如果类 XXX 所在模块的包为 xxx，此相对路径的基路径为该模块 resources/xxx/
         */
        FXMLLoader fxmlLoader = new FXMLLoader(MainFace.class.getResource("MainFace.fxml"));
        Node node = null;
        try {
            node = fxmlLoader.load(); // 如果不先调用方法 load，则不能使用方法 getController
        } catch (Exception exception) {
            System.out.println("文件不存在...." + MainFace.class); // FIXME：日志
        }
        MainFace fxmlController = fxmlLoader.getController();
        node.setUserData(fxmlController);
        return fxmlController;
    }

    public VBox getComponent() {
        return this.mainFace;
    }
}