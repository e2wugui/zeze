package Zeze.Arch;

import Zeze.Builtin.Provider.BLoad;
import Zeze.Services.LoginQueueAgent;

public class LinkdLoad extends LoadBase {
	private final LinkdApp linkdApp;
	private final String linkdIp;
	private final int linkdPort;

	public LinkdLoad(LinkdApp linkdApp) {
		super(linkdApp.zeze);
		this.linkdApp = linkdApp;
		var kv = linkdApp.linkdService.getOnePassiveAddress();
		linkdIp = kv.getKey();
		linkdPort = kv.getValue();
		// 构造即创建，保持时序（choiceProvider随时可能读secret）；配方收口在LoadBase.ensure。
		// 隐含契约：LinkdApp仅在本服务节配置存在时才构造本类。
		ensureLoginQueueAgent();
	}

	public String getLinkdIp() {
		return linkdIp;
	}

	public int getLinkdPort() {
		return linkdPort;
	}

	@Override
	public int getOnlineLocalCount() {
		return linkdApp.linkdService.getSocketCount();
	}

	@Override
	public long getOnlineLoginTimes() {
		return linkdApp.linkdService.getLoginTimes();
	}

	@Override
	public LoadConfig getLoadConfig() {
		return linkdApp.linkdProvider.distributes.loadConfig;
	}

	@Override
	public String getServiceIp() {
		return linkdIp;
	}

	@Override
	public int getServicePort() {
		return linkdPort;
	}

	@Override
	protected void reportLoginQueueLoad(LoginQueueAgent loginQueueAgent, BLoad.Data load) {
		loginQueueAgent.reportLinkLoad(load);
	}
}
