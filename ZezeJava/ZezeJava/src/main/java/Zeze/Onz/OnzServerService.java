package Zeze.Onz;

import Zeze.Config;
import Zeze.Net.Service;

/** OnzServer的网络服务：以"OnzServer"为名承载Onz协议的监听与远程调用处理。 */
public class OnzServerService extends Service {
	public static final String eServiceName = "OnzServer";

	public OnzServerService(Config config) {
		super(eServiceName, config);
	}
}
