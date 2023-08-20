import java.security.Permission;

/**
 * 自定义的Java安全管理器
 *
 * @author jackqiu
 */
public class MySecurityManager extends SecurityManager {

    /** 自定义权限
     * @param perm
     */
    @Override
    public void checkPermission(Permission perm) {
//        System.out.println("checkPermission 放开所有权限，不做任何限制:  " + perm);
//        super.checkPermission(perm);
    }

    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec 权限异常：" + cmd);
    }

    @Override
    public void checkRead(String file) {
//        System.out.println("checkRead 允许读文件:  " + file);
//        super.checkRead(file);
    }

    @Override
    public void checkWrite(String file) {
//        System.out.println("checkWrite 允许写文件:  " + file);
//        super.checkWrite(file);
    }

    @Override
    public void checkDelete(String file) {
//        System.out.println("checkDelete 允许删除文件:  " + file);
//        super.checkDelete(file);
    }

    @Override
    public void checkConnect(String host, int port) {
//        System.out.println("checkConnect 允许网络连接-->" + host + ":" + port);
//        super.checkConnect(host, port);
    }
}