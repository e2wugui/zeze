package Zeze.Services.Log4jQuery;

import java.util.HashMap;
import Zeze.Config;
import Zeze.Util.Str;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

public class LogServiceConf implements Config.ICustomize {
	public String logActive;
	public String logRotateDir;
	public String logTimeFormat;
	public String serviceIdentity = "#LogService_{serverId}_{host}_{port}";

	@Override
	public @NotNull String getName() {
		return "LogServiceConf";
	}

	@Override
	public void parse(@NotNull Element self) {
		logActive = self.getAttribute("LogActive");
		logRotateDir = self.getAttribute("LogRotateDir");
		logTimeFormat = self.getAttribute("LogTimeFormat");
		if (!logTimeFormat.isBlank())
			Log4jLog.LogTimeFormat = logTimeFormat;
		var attr = self.getAttribute("ServiceIdentity");
		if (!attr.isBlank())
			serviceIdentity = attr;
	}

	public void formatServiceIdentity(int serverId, String host, int port) {
		var params = new HashMap<String, Object>();
		params.put("serverId", serverId);
		params.put("host", host);
		params.put("port", port);
		serviceIdentity = Str.format(serviceIdentity, params);
	}

}
