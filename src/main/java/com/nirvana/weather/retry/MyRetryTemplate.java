package com.nirvana.weather.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class MyRetryTemplate<T> {
    //重试次数
    private int retryTime;
    //重试时间
    private int sleepTime;
    //是否倍数增长
    private boolean multiple = false;

    /**
     * 执行业务方法逻辑，由实现类实现
     *
     * @return
     */
    public abstract T doBiz() throws Exception;

    public T execute() throws InterruptedException {
        for (int i = 1; i < retryTime + 1; i++) {
            try {
                return doBiz();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                if (multiple) {
                    Thread.sleep(sleepTime);
                } else {
                    Thread.sleep(sleepTime * (i));
                }
            }
        }
        return null;
    }

    public T submit(ExecutorService executorService) {
        Future submit = executorService.submit((Callable) () -> execute());
        try {
            return (T) submit.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MyRetryTemplate setRetryTime(int retryTime) {
        this.retryTime = retryTime;
        return this;
    }

    public MyRetryTemplate setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
        return this;
    }

    public MyRetryTemplate setMultiple(boolean multiple) {
        this.multiple = multiple;
        return this;
    }
}
