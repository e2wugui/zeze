package Zeze.Component;

import Zeze.Application;
import Zeze.Net.Protocol;

public class Auth extends AbstractAuth {
	public Auth(Application zeze) {
		RegisterZezeTables(zeze);
	}

	public void setProtocolAuth(String account, int moduleId, int protocolId, String flags) {
		setProtocolAuth(account, Protocol.makeTypeId(moduleId, protocolId), flags);
	}

	public void setModuleAuth(String account, int moduleId, String flags) {
		setProtocolAuth(account, Protocol.makeTypeId(moduleId, 0), flags);
	}

	public void setProtocolAuth(String account, long typeId, String flags) {
		_tAuth.getOrAdd(account).getAuths().put(typeId, flags);
	}

	public String getAuth(String account, int moduleId, int protocolId) {
		return getAuth(account, Protocol.makeTypeId(moduleId, protocolId));
	}

	public String getAuth(String account, long typeId) {
		var value = _tAuth.selectDirty(account);
		if (null == value)
			return null;

		// 首先查找具体协议权限
		var flags = value.getAuths().get(typeId);
		if (null != flags)
			return flags;

		// 不存在，查找模块权限
		var moduleId = Protocol.getModuleId(typeId);
		return value.getAuths().get(Protocol.makeTypeId(moduleId, 0));
	}

	public void start() throws Exception {

	}

	public void stop() throws Exception {
		UnRegister();
	}
}
