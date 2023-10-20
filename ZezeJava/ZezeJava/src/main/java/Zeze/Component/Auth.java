package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.Auth.BAuthKey;
import Zeze.Net.Protocol;

public class Auth extends AbstractAuth {
	public Auth(Application zeze) {
		RegisterZezeTables(zeze);
	}

	public void setProtocolAuth(String account, int moduleId, int protocolId, String flags) {
		_tAuth.getOrAdd(account).getAuths().put(new BAuthKey(moduleId, protocolId), flags);
	}

	public void setModuleAuth(String account, int moduleId, String flags) {
		setProtocolAuth(account, moduleId, 0, flags);
	}

	public void setProtocolAuth(String account, long typeId, String flags) {
		setProtocolAuth(account, Protocol.getModuleId(typeId), Protocol.getProtocolId(typeId), flags);
	}

	public String getAuth(String account, long typeId) {
		return getAuth(account, Protocol.getModuleId(typeId), Protocol.getProtocolId(typeId));
	}

	public String getAuth(String account, int moduleId, int protocolId) {
		var value = _tAuth.selectDirty(account);
		if (null == value)
			return null;

		// 首先查找具体协议权限
		var flags = value.getAuths().get(new BAuthKey(moduleId, protocolId));
		if (null != flags)
			return flags;

		// 不存在，查找模块权限
		return value.getAuths().get(new BAuthKey(moduleId, 0));
	}

	public void start() throws Exception {

	}

	public void stop() throws Exception {
		UnRegister();
	}
}
