package com.jackqiu.jackqiucodesandbox.util;

import cn.hutool.core.date.StopWatch;
import com.jackqiu.jackqiucodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 编译代码，执行代码通用工具类
 *
 * @author jackqiu
 */
public class ProcessUtil {
    public static ExecuteMessage runProcessAndGetMessage(Process process, String execCommand) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            // 等待程序执行，获取错误码
            int exitValue = process.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) { //正常退出
                System.out.println(execCommand + "成功");
                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                List<String> outputList = new ArrayList<>();
                // 逐行读取
                String successMessage;
                while ((successMessage = bufferedReader.readLine()) != null) {
                    outputList.add(successMessage);
                }
                executeMessage.setMassage(StringUtils.join(outputList,"\n"));//添加执行的信息
            } else {//异常退出
                System.out.println(execCommand + "失败");
                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                List<String> outputList = new ArrayList<>();
                // 逐行读取
                String successMessage;
                while ((successMessage = bufferedReader.readLine()) != null) {
                    outputList.add(successMessage);
                }
                executeMessage.setMassage(StringUtils.join(outputList,"\n"));//添加执行的信息
                // 分批获取进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                List<String> errorOutputList = new ArrayList<>();
                // 逐行读取
                String errorSuccessMessage;
                while ((errorSuccessMessage = errorBufferedReader.readLine()) != null) {
                    errorOutputList.add(errorSuccessMessage);
                }
                executeMessage.setErrorMassage(StringUtils.join(errorOutputList,"\n"));//添加异常执行的信息
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return executeMessage;
    }
}
