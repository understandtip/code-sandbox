package com.jackqiu.jackqiucodesandbox;

import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeRequest;
import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口
 * @author jackqiu
 */
public interface CodeSandBox {
    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest);
}
