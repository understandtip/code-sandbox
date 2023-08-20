package com.jackqiu.jackqiucodesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试使用的控制类
 * @author jackqiu
 */
@RestController
public class TestController {

    @GetMapping("/health")
    public String test(){
        return "ok";
    }
}
