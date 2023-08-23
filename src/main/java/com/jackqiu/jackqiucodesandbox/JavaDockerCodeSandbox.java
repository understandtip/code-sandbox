package com.jackqiu.jackqiucodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeRequest;
import com.jackqiu.jackqiucodesandbox.model.ExecuteCodeResponse;
import com.jackqiu.jackqiucodesandbox.model.ExecuteMessage;
import com.jackqiu.jackqiucodesandbox.model.JudgeInfo;
import com.jackqiu.jackqiucodesandbox.util.ProcessUtil;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker代码沙箱
 *
 * @author jackqiu
 */
public class JavaDockerCodeSandbox implements CodeSandBox {

    public static final String GLOBAL_CODE_DIR_NAME = "tempCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 5000L;

    private static final String SECURITY_MANAGER_PATH = "D:\\BaiduNetdiskDownload\\workSpace\\jackqiu-code-sandbox\\src\\main\\resources\\security";

    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

    public static final Boolean FIRST_INIT = true;
    public static void main(String[] args) {
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setInputList(Arrays.asList("1 2", "9 10"));
        executeCodeRequest.setLanguage("java");
        System.out.println(javaNativeCodeSandbox.execute(executeCodeRequest));
    }

    /**
     * 执行代码沙箱的核心逻辑
     *
     * @param executeCodeRequest
     * @return
     */
    @Override
    public ExecuteCodeResponse execute(ExecuteCodeRequest executeCodeRequest) {
//        System.setSecurityManager(new MySecurityManager());

        String code = executeCodeRequest.getCode();
        List<String> inputList = executeCodeRequest.getInputList();
//        String language = executeCodeRequest.getLanguage();

        //1. 把用户的代码保存为文件
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;//使用File.separator是为了在不同系统环境下都能运行
        if (!FileUtil.exist(globalCodePathName)) {//如果文件名不存在，则创建文件
            FileUtil.mkdir(globalCodePathName);
        }
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        //2. 编译代码，得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        //ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            Process process = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(process, "编译");
            System.out.println(executeMessage);//输出每一次的编译结果信息
        } catch (Exception e) {
            return errorResponse(e);
        }


        //3.1 创建容器，将文件上传到容器中
        //从dockerHub上拉取镜像
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像分片，状态： " + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        System.out.println("拉取完毕");
        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        String profileConfig = ResourceUtil.readUtf8Str("profile.json");
        hostConfig.withSecurityOpts(Arrays.asList("seccomp=" + profileConfig));
        hostConfig.withMemory(100 * 1000 * 1000L);//容器大小100MB
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStderr(true)
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withTty(true)//实现交互方式执行，
                .exec();
        System.out.println(createContainerResponse);
        String containId = createContainerResponse.getId();

        //3.2 运行容器，执行代码，得到输出结果
        //启动容器
        dockerClient.startContainerCmd(containId).exec();

        //docker exec practical_buck java -cp /app Main 1 2
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String[] input = inputArgs.split(" ");
            String[] cmdString = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"},input);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containId)
                    .withCmd(cmdString)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withAttachStdin(true)
                    .exec();
            System.out.println("创建执行命令" + execCreateCmdResponse);
            String execId = execCreateCmdResponse.getId();
            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] errorMessage = {null};
            final String[] message = {null};
            final boolean[] timeOut = {true};

            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    // 如果执行完成，则表示没超时
                    timeOut[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };
            final Long[] maxMemory = {0L};
            // 获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containId);
            statsCmd.exec(new ResultCallback<Statistics>() {

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void close() throws IOException {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }
            });
//            statsCmd.exec(statisticsResultCallback);
            try {
                StopWatch stopWatch =new StopWatch();
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.SECONDS);
                stopWatch.stop();
                executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }

            executeMessage.setMassage(message[0]);
            executeMessage.setErrorMassage(errorMessage[0]);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }


        //4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        JudgeInfo judgeInfo = new JudgeInfo();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0L;
        long maxMemory = 0L;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMassage = executeMessage.getErrorMassage();
            if (StrUtil.isNotBlank(errorMassage)) {
                executeCodeResponse.setMessage(errorMassage);
                // 用户提交的代码执行中存在错误，无法执行成功
                executeCodeResponse.setStatus("1");
                break;
            }
            Long memory = executeMessage.getMemory();
            if(memory != null){
                maxMemory = Math.max(memory,maxMemory);
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


        // todo  需要在OJ在线判题系统里设置信息，使用定义好的枚举类来设置信息
//        judgeInfo.setMessage();
        judgeInfo.setTime(maxTime);//todo 优化：可以获取每个执行代码的时间
        // todo 要借助第三方库来获取内存占用，非常麻烦，此处不做实现
        judgeInfo.setMemory(maxMemory);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        executeCodeResponse.setOutput(outputList);
        //5. 文件清理，释放空间
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
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
