package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.Auth.BRoleAuth;
import Zeze.Net.Protocol;
import Zeze.Transaction.TableWalkHandle;

public class Auth extends AbstractAuth {
	public Auth(Application zeze) {
		RegisterZezeTables(zeze);
	}

	// 事务内，角色权限设置
	public void setRoleProtocolAuth(String role, int moduleId, int protocolId, String flags) {
		setRoleProtocolAuth(role, Protocol.makeTypeId(moduleId, protocolId), flags);
	}

	public void setRoleModuleAuth(String role, int moduleId, String flags) {
		setRoleProtocolAuth(role, Protocol.makeTypeId(moduleId, 0), flags);
	}

	public void setRoleProtocolAuth(String role, long typeId, String flags) {
		_tRoleAuth.getOrAdd(role).getAuths().put(typeId, flags);
	}

	// 角色查询，事务外
	public BRoleAuth selectRoleAuth(String role) {
		return _tRoleAuth.selectDirty(role);
	}

	public void walkRoles(TableWalkHandle<String, BRoleAuth> handle) throws Exception {
		_tRoleAuth.walk(handle);
	}

	// 账号角色管理
	public void addAccountRole(String account, String role) {
		_tAccountAuth.getOrAdd(account).getRoles().add(role);
	}

	public void removeAccountRole(String account, String role) {
		_tAccountAuth.getOrAdd(account).getRoles().remove(role);
	}

	// 账号角色查询
	public String getAccountAuth(String account, int moduleId, int protocolId) {
		return getAccountAuth(account, Protocol.makeTypeId(moduleId, protocolId));
	}

	public String getAccountAuth(String account, long typeId) {
		var roleBean = _tAccountAuth.selectDirty(account);
		if (null == roleBean)
			return null;

		for (var role : roleBean.getRoles()) {
			var value = selectRoleAuth(role);

			// 首先查找具体协议权限
			var flags = value.getAuths().get(typeId);
			if (null != flags)
				return flags;

			// 不存在，查找模块权限
			var moduleId = Protocol.getModuleId(typeId);
			flags = value.getAuths().get(Protocol.makeTypeId(moduleId, 0));
			if (null != flags)
				return flags;
		}
		return null;
	}

	public void start() throws Exception {

	}

	public void stop() throws Exception {
		UnRegister();
	}
}
