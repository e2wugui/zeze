// auto-generated
package Game.Login;

public abstract class AbstractModule extends Zeze.IModule {
    public String getFullName() { return "Game.Login"; }
    public String getName() { return "Login"; }
    public int getId() { return 1; }

    public final static int ResultCodeSuccess = 0;
    public final static int ResultCodeCreateRoleDuplicateRoleName = 1;
    public final static int ResultCodeAccountNotExist = 2;
    public final static int ResultCodeRoleNotExist = 3;
    public final static int ResultCodeNotLastLoginRoleId = 4;
    public final static int ResultCodeOnlineDataNotFound = 5;
    public final static int ResultCodeReliableNotifyConfirmCountOutOfRange = 6;
    public final static int ResultCodeNotLogin = 7;

    public abstract int ProcessCreateRoleRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessGetRoleListRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessLoginRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessLogoutRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessReliableNotifyConfirmRequest(Zeze.Net.Protocol _p);

    public abstract int ProcessReLoginRequest(Zeze.Net.Protocol _p);

}
