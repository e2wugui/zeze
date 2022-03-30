package Zezex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;
import java.util.concurrent.Future;

public class LinkSession {
	private static final Logger logger = LogManager.getLogger(LinkSession.class);

	private String Account;
	public final String getAccount() {
		return Account;
	}
	public final void setAccount(String value) {
		Account = value;
	}
	private final ArrayList<Long> UserStates = new ArrayList<>();
	public final ArrayList<Long> getUserStates() {
		return UserStates;
	}
	private Zeze.Net.Binary UserStatex = Zeze.Net.Binary.Empty;
	public final Zeze.Net.Binary getUserStatex() {
		return UserStatex;
	}
	private void setUserStatex(Zeze.Net.Binary value) {
		UserStatex = value;
	}

	private HashMap<Integer, Long> Binds = new HashMap<>();
	private HashMap<Integer, Long> getBinds() {
		return Binds;
	}
	private void setBinds(HashMap<Integer, Long> value) {
		Binds = value;
	}

	private final long SessionId;
	public final long getSessionId() {
		return SessionId;
	}

	public LinkSession(long sessionId) {
		SessionId = sessionId;
	}

	public final void SetUserState(Collection<Long> states, Zeze.Net.Binary statex) {
		synchronized (this) { // 简单使用一下这个锁。
			getUserStates().clear();
			getUserStates().addAll(states);
			setUserStatex(statex);
		}
	}

	public final boolean TryGetProvider(int moduleId, Zeze.Util.OutObject<Long> provider) {
		synchronized (this) {
			var binded = Binds.get(moduleId);
			if (null != binded) {
				provider.Value = binded;
				return true;
			}
			provider.Value = 0L;
			return false;
		}
	}

	public final void Bind(Zeze.Net.AsyncSocket link, java.lang.Iterable<Integer> moduleIds, Zeze.Net.AsyncSocket provider) {
		synchronized (this) {
			for (var moduleId : moduleIds) {
				var exist = Binds.get(moduleId);
				if (null != exist) {
					var oldSocket = App.getInstance().ProviderService.GetSocket(exist);
					logger.warn("LinkSession.Bind replace provider {} {} {}",
							moduleId, oldSocket.getRemoteAddress(), provider.getRemoteAddress());
				}
				getBinds().put(moduleId, provider.getSessionId());
				var ps = (ProviderSession)provider.getUserState();
				if (null != ps)
					ps.AddLinkSession(moduleId, link.getSessionId());
			}
		}
	}


	public final void UnBind(Zeze.Net.AsyncSocket link, int moduleId, Zeze.Net.AsyncSocket provider) {
		UnBind(link, moduleId, provider, false);
	}

	public final void UnBind(Zeze.Net.AsyncSocket link, int moduleId, Zeze.Net.AsyncSocket provider, boolean isOnProviderClose) {
		var moduleIds = new HashSet<Integer>();
		moduleIds.add(moduleId);
		UnBind(link, moduleIds, provider, isOnProviderClose);
	}


	public final void UnBind(Zeze.Net.AsyncSocket link, java.lang.Iterable<Integer> moduleIds, Zeze.Net.AsyncSocket provider) {
		UnBind(link, moduleIds, provider, false);
	}

	public final void UnBind(Zeze.Net.AsyncSocket link, java.lang.Iterable<Integer> moduleIds, Zeze.Net.AsyncSocket provider, boolean isOnProviderClose) {
		synchronized (this) {
			for (var moduleId : moduleIds) {
				var exist = Binds.get(moduleId);
				if (null != exist) {
					if (exist == provider.getSessionId()) { // check owner? 也许不做这个检测更好？
						getBinds().remove(moduleId);
						if (!isOnProviderClose) {
							var ps = (ProviderSession)provider.getUserState();
							if (null != ps)
								ps.RemoveLinkSession(moduleId, link.getSessionId());
						}
					}
					else {
						var oldSocket = App.getInstance().ProviderService.GetSocket(exist);
						logger.warn("LinkSession.UnBind not owner {} {} {}",
								moduleId, oldSocket.getRemoteAddress(), provider.getRemoteAddress());
					}
				}
			}
		}
	}

	// 仅在网络线程中回调，并且一个时候，只会有一个回调，不线程保护了。
	private Future<?> KeepAliveTask;

	public final void KeepAlive() {
		if (KeepAliveTask != null) {
			KeepAliveTask.cancel(false);
		}
		KeepAliveTask = Zeze.Util.Task.schedule(3000000, () -> {
			var link = App.getInstance().LinkdService.GetSocket(getSessionId());
				if (link != null) {
					link.Close(null);
				}
		});
	}

	public final void OnClose() {
		if (KeepAliveTask != null) {
			KeepAliveTask.cancel(false);
		}

		if (getAccount() == null) {
			// 未验证通过的不通告。此时Binds肯定是空的。
			return;
		}

		HashMap<Integer, Long> bindsSwap;
		synchronized (this) {
			bindsSwap = getBinds();
			setBinds(new HashMap<>());
		}

		var linkBroken = new Zezex.Provider.LinkBroken();
		linkBroken.Argument.setAccount(Account);
		linkBroken.Argument.setLinkSid(SessionId);
		linkBroken.Argument.getStates().addAll(getUserStates());
		linkBroken.Argument.setStatex(UserStatex);
		linkBroken.Argument.setReason(Zezex.Provider.BLinkBroken.REASON_PEERCLOSE); // 这个保留吧。现在没什么用。

		// 需要在锁外执行，因为如果 ProviderSocket 和 LinkdSocket 同时关闭。都需要去清理自己和对方，可能导致死锁。
		HashSet<Zeze.Net.AsyncSocket> bindProviders = new HashSet<>();
		for (var e : bindsSwap.entrySet()) {
			var provider = App.getInstance().ProviderService.GetSocket(e.getValue());
			if (null == provider) {
				continue;
			}
			var providerSession = (ProviderSession)provider.getUserState();
			if (null == providerSession) {
				continue;
			}
			providerSession.RemoveLinkSession(e.getKey(), getSessionId());
			bindProviders.add(provider); // 先收集， 去重。
		}
		for (var provider : bindProviders) {
			provider.Send(linkBroken);
		}
	}
}
