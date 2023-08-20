package com.jackqiu.jackqiucodesandbox.securityManager;

import java.security.Permission;

/**
 * 默认的Java安全管理器
 *
 * @author jackqiu
 */
public class DefaultSecurityManager extends SecurityManager {

    /** 放开所有权限
     * @param perm
     */
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("放开所有权限，不做任何限制");
        System.out.println(perm);
//        super.checkPermission(perm);
    }
}
