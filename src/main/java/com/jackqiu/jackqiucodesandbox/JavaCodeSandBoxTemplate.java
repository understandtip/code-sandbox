package com.jackqiu.jackqiucodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeRequest;
import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeResponse;
import com.jackqiu.jackqiucodesandbox.model.ExecuteMessage;
import com.jackqiu.jackqiucodesandbox.model.JudgeInfo;
import com.jackqiu.jackqiucodesandbox.util.ProcessUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jackqiu
 */
public class JavaCodeSandBoxTemplate implements CodeSandBox {
    public static final String GLOBAL_CODE_DIR_NAME = "tempCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 5000L;

    private static final String SECURITY_MANAGER_PATH = "D:\\BaiduNetdiskDownload\\workSpace\\jackqiu-code-sandbox\\src\\main\\resources\\security";

    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

    /**
     * //1. 把用户的代码保存为文件
     *
     * @param code 需要执行的代码
     * @return 代码所在的路径（文件对象）
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;//使用File.separator是为了在不同系统环境下都能运行
        if (!FileUtil.exist(globalCodePathName)) {//如果文件名不存在，则创建文件
            FileUtil.mkdir(globalCodePathName);
        }
        //todo 可以将其保存为静态方法
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2. 编译代码，得到class文件
     *
     * @param userCodeFile 代码所在的路径（文件对象）
     * @return 编译代码后的信息
     */
    public ExecuteMessage compileCode(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        //ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            Process process = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(process, "编译");
            System.out.println(executeMessage);//输出每一次的编译结果信息
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException("编译错误", e);
        }
    }

    /**
     * 3. 执行代码，得到输出结果
     *
     * @param inputList    测试用例
     * @param userCodeFile 代码所在的路径（文件对象）
     * @return 执行代码的结果信息集合
     */
    public List<ExecuteMessage> executeCode(List<String> inputList, File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
//            String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            String execCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process process = Runtime.getRuntime().exec(execCmd);
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("执行超时");
                        process.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(process, "执行");
                System.out.println(executeMessage);//输出每一次的运行结果信息
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行代码错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4. 收集整理输出结果
     *
     * @param executeMessageList 执行代码的结果信息集合
     * @return 返回给服务调用方的信息
     */
    public ExecuteCodeResponse collectOutcome(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0L;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMassage = executeMessage.getErrorMassage();
            if (StrUtil.isNotBlank(errorMassage)) {
                executeCodeResponse.setMessage(errorMassage);
                // 用户提交的代码执行中存在错误，无法执行成功
                executeCodeResponse.setStatus("1");
                break;
            }
            outputList.add(executeMessage.getMassage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
//        if (outputList.size() == executeMessageList.size()) {
//            //某些用例运行失败  todo 定义代码沙箱执行信息的状态枚举类
//            executeCodeResponse.setStatus("3");
//        }

        JudgeInfo judgeInfo = new JudgeInfo();
        // todo  需要在OJ在线判题系统里设置信息，使用定义好的枚举类来设置信息
//        judgeInfo.setMessage();
        judgeInfo.setTime(maxTime);//todo 优化：可以获取每个执行代码的时间
        // todo 要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//      judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        executeCodeResponse.setOutput(outputList);
        return executeCodeResponse;
    }

    /**
     * 5. 文件清理，释放空间
     *
     * @param userCodeFile 代码所在的路径（文件对象）
     * @return 用户代码文件是否删除成功
     */
    public boolean clearFile(File userCodeFile) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 执行代码沙箱的核心逻辑
     *
     * @param executeCodeRequest
     * @return
     */
    @Override
    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();
        String language = executeCodeRequest.getLanguage();
        //1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);

        //2. 编译代码，得到class文件
        ExecuteMessage compileMessage = compileCode(userCodeFile);
        System.out.println(compileMessage);

        //3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = executeCode(inputList, userCodeFile);

        //4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = collectOutcome(executeMessageList);

        //5. 文件清理，释放空间
        boolean del = clearFile(userCodeFile);
        if (!del) {
            throw new RuntimeException("用户代码文件删除失败");
        }

        //6. 错误处理，提升程序健壮性
        return executeCodeResponse;
    }

    /**
     * 打印异常信息
     *
     * @param e
     */
    private ExecuteCodeResponse errorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        //代码沙箱错误
        executeCodeResponse.setStatus("2");
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        executeCodeResponse.setOutput(new ArrayList<>());
        return executeCodeResponse;
    }
}
