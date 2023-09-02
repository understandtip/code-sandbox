package com.jackqiu.jackqiucodesandbox.controller;

import com.jackqiu.jackqiucodesandbox.JavaNativeCodeSandbox;
import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeRequest;
import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 测试使用的控制类
 * @author jackqiu
 */
@RestController
public class TestController {

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @GetMapping("/health")
    public String test(){
        return "ok";
    }

    /**
     * 执行代码
     * @param executeCodeRequest 调用此服务的请求信息 （json格式）
     * @return 执行代码的结果信息
     */
    @PostMapping("/executeCode")
    public ExecuteCodeResponse execute(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request, HttpServletResponse response){
        String header = request.getHeader(AUTH_REQUEST_HEADER);
        if(!AUTH_REQUEST_SECRET.equals(header)){
            response.setStatus(403);
            return null;
        }
        if(executeCodeRequest == null) {
            throw new RuntimeException("请求参数不允许为空");
        }
        return javaNativeCodeSandbox.execute(executeCodeRequest);
    }
}
