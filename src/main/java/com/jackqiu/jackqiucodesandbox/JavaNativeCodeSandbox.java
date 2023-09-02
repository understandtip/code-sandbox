package com.jackqiu.jackqiucodesandbox;

import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeRequest;
import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * 原生Java代码沙箱
 *
 * @author jackqiu
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandBoxTemplate {
    @Override
    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {
        return super.execute(executeCodeRequest);
    }
}
