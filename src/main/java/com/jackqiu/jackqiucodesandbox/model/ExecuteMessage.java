package com.jackqiu.jackqiucodesandbox.model;

import lombok.Data;

/**
 * 进程执行信息
 */
@Data
public class ExecuteMessage {

    /**
     * 正确信息(也就是输出结果)
     */
    private String massage;

    /**
     * 错误信息
     */
    private String errorMassage;

    /**
     * 实际执行时间
     */
    private Long time;

    /**
     *  退出代码（exit code）
     */
    private Integer exitValue;

    /**
     * 占用的内容
     */
    private Long memory;
}
