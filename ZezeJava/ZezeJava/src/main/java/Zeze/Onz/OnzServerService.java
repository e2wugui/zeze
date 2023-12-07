package Zeze.Onz;

import Zeze.Config;
import Zeze.Net.Service;

public class OnzServerService extends Service {
	public static final String eServiceName = "OnzServer";

	public OnzServerService(Config config) {
		super(eServiceName, config);
	}
}
