package org.wangpai.demo.im.util;

import java.util.concurrent.Future;
import javafx.concurrent.Task;

/**
 * @since 2021-10-3
 */
public class Multithreading {
    /**
     * 无结果反馈的版本
     *
     * @since 2021-10-3
     * @lastModified 2021-10-10
     */
    public static void execute(Function function) {
        /**
         * 开新线程来完成下面的操作
         */
        Task<Object> task = new Task<>() {
            @Override
            protected Integer call() {
                function.run();
                return null; // 因为此处不需要结果反馈，所以返回 null
            }
        };
        CentralDatabase.getTasks().add(task);
        CentralDatabase.getExecutor().execute(task);
    }

    /**
     * 有结果反馈的版本
     *
     * @since 2021-10-10
     */
    public static Future<?> submit(Function function) {
        /**
         * 开新线程来完成下面的操作
         */
        Task<Object> task = new Task<>() {
            @Override
            protected Object call() {
                function.run();
                return null; // 因为此处暂时没定好应该反馈什么东西，所以返回 null
            }
        };
        CentralDatabase.getTasks().add(task);
        return CentralDatabase.getExecutor().submit(task);
    }
}
