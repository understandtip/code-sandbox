package com.jackqiu.jackqiucodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

/**
 * @author jackqiu
 */
public class DockerDemo {
    public static void main(String[] args) {
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        //从dockerHub上拉取镜像
        String image = "nginx:latest";
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
            throw new RuntimeException(e);
        }
        System.out.println("拉取完毕");
        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse createContainerResponse = containerCmd
                .withCmd("echo", "hello docker")
//                .withAttachStderr(true)
//                .withAttachStdin(true)
//                .withAttachStdout(true)
//                .withTty(true)//实现交互方式执行，
                .exec();
        System.out.println(createContainerResponse);
        String containId = createContainerResponse.getId();
        //查看容器状态
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        for (Container container : listContainersCmd.withShowAll(true).exec()) {
            System.out.println(container);
        }

        //启动容器
        dockerClient.startContainerCmd(containId).exec();

        //查看日志
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containId);
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback(){
            @Override
            public void onNext(Frame item) {
                System.out.println("日志信息：" + new String(item.getPayload()));
                super.onNext(item);
            }
        };
        try {
            logContainerCmd
                    .withStdErr(true)
                    .withStdOut(true)
                    .exec(logContainerResultCallback)
                    .awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //删除容器
        dockerClient.removeContainerCmd(containId).withForce(true).exec();

        //删除镜像
        //dockerClient.removeImageCmd(image).exec();
    }
}
