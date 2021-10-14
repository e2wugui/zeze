package Game.Login;

import Game.*;

// auto-generated


public abstract class AbstractModule implements Zeze.IModule {
	@Override
	public String getFullName() {
		return "Game.Login";
	}
	@Override
	public String getName() {
		return "Login";
	}
	@Override
	public int getId() {
		return 1;
	}

	public static final int ResultCodeSuccess = 0;
	public static final int ResultCodeCreateRoleDuplicateRoleName = 1;
	public static final int ResultCodeAccountNotExist = 2;
	public static final int ResultCodeRoleNotExist = 3;
	public static final int ResultCodeNotLastLoginRoleId = 4;
	public static final int ResultCodeOnlineDataNotFound = 5;
	public static final int ResultCodeReliableNotifyConfirmCountOutOfRange = 6;
	public static final int ResultCodeNotLogin = 7;

	public abstract int ProcessCreateRoleRequest(CreateRole rpc);

	public abstract int ProcessGetRoleListRequest(GetRoleList rpc);

	public abstract int ProcessLoginRequest(Login rpc);

	public abstract int ProcessLogoutRequest(Logout rpc);

	public abstract int ProcessReliableNotifyConfirmRequest(ReliableNotifyConfirm rpc);

	public abstract int ProcessReLoginRequest(ReLogin rpc);

}