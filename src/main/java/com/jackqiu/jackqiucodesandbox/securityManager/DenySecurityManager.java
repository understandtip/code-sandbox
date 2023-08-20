package com.jackqiu.jackqiucodesandbox.securityManager;

import java.security.Permission;

/**
 * 全部权限都拒绝的Java安全管理器
 *
 * @author jackqiu
 */
public class DenySecurityManager extends SecurityManager {

    /** 禁用所有权限
     * @param perm
     */
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("禁用所有权限");
        System.out.println(perm);
        super.checkPermission(perm);
    }
}
