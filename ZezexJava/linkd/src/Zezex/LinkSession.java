package Zezex;

import java.util.*;

public class LinkSession {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private String Account;
	public final String getAccount() {
		return Account;
	}
	public final void setAccount(String value) {
		Account = value;
	}
	private ArrayList<Long> UserStates = new ArrayList<Long> ();
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

	private HashMap<Integer, Long> Binds = new HashMap<Integer, Long> ();
	private HashMap<Integer, Long> getBinds() {
		return Binds;
	}
	private void setBinds(HashMap<Integer, Long> value) {
		Binds = value;
	}

	private long SessionId;
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
				if (getBinds().containsKey(moduleId) && (var exist = getBinds().get(moduleId)) == var exist) {
					var oldSocket = App.getInstance().getProviderService().GetSocket(exist);
					logger.Warn("LinkSession.Bind replace provider {0} {1} {2}", moduleId, oldSocket.Socket.RemoteEndPoint, provider.Socket.RemoteEndPoint);
				}
				getBinds().put(moduleId, provider.SessionId);
				Object tempVar = provider.UserState;
				(tempVar instanceof ProviderSession ? (ProviderSession)tempVar : null).AddLinkSession(moduleId, link.SessionId);
			}
		}
	}


	public final void UnBind(Zeze.Net.AsyncSocket link, int moduleId, Zeze.Net.AsyncSocket provider) {
		UnBind(link, moduleId, provider, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void UnBind(Zeze.Net.AsyncSocket link, int moduleId, Zeze.Net.AsyncSocket provider, bool isOnProviderClose = false)
	public final void UnBind(Zeze.Net.AsyncSocket link, int moduleId, Zeze.Net.AsyncSocket provider, boolean isOnProviderClose) {
		UnBind(link, new HashSet<Integer>() {moduleId}, provider, isOnProviderClose);
	}


	public final void UnBind(Zeze.Net.AsyncSocket link, java.lang.Iterable<Integer> moduleIds, Zeze.Net.AsyncSocket provider) {
		UnBind(link, moduleIds, provider, false);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public void UnBind(Zeze.Net.AsyncSocket link, IEnumerable<int> moduleIds, Zeze.Net.AsyncSocket provider, bool isOnProviderClose = false)
	public final void UnBind(Zeze.Net.AsyncSocket link, java.lang.Iterable<Integer> moduleIds, Zeze.Net.AsyncSocket provider, boolean isOnProviderClose) {
		synchronized (this) {
			for (var moduleId : moduleIds) {
				if (getBinds().containsKey(moduleId) && (var exist = getBinds().get(moduleId)) == var exist) {
					if (exist == provider.SessionId) { // check owner? 也许不做这个检测更好？
						getBinds().remove(moduleId);
						if (false == isOnProviderClose) {
							Object tempVar = provider.UserState;
							if ((tempVar instanceof ProviderSession ? (ProviderSession)tempVar : null) != null) {
								Object tempVar2 = provider.UserState;
								(tempVar2 instanceof ProviderSession ? (ProviderSession)tempVar2 : null).RemoveLinkSession(moduleId, link.SessionId);
							}
						}
					}
					else {
						var oldSocket = App.getInstance().getProviderService().GetSocket(exist);
						logger.Warn("LinkSession.UnBind not owner {0} {1} {2}", moduleId, oldSocket.Socket.RemoteEndPoint, provider.Socket.RemoteEndPoint);
					}
				}
			}
		}
	}

	// 仅在网络线程中回调，并且一个时候，只会有一个回调，不线程保护了。
	private Zeze.Util.SchedulerTask KeepAliveTask;

	public final void KeepAlive() {
		if (KeepAliveTask != null) {
			KeepAliveTask.Cancel();
		}
		KeepAliveTask = Zeze.Util.Scheduler.Instance.Schedule((ThisTask) -> {
				if (App.getInstance().getLinkdService().GetSocket(getSessionId()) != null) {
					App.getInstance().getLinkdService().GetSocket(getSessionId()).Close(null);
				}
		}, 3000000, -1);
	}

	public final void OnClose() {
		if (KeepAliveTask != null) {
			KeepAliveTask.Cancel();
		}

		if (getAccount().equals(null)) {
			// 未验证通过的不通告。此时Binds肯定是空的。
			return;
		}

		HashMap<Integer, Long> bindsSwap = null;
		synchronized (this) {
			bindsSwap = getBinds();
			setBinds(new HashMap<Integer, Long>());
		}

		var linkBroken = new Zezex.Provider.LinkBroken();
		linkBroken.getArgument().Account = getAccount();
		linkBroken.getArgument().LinkSid = getSessionId();
		linkBroken.getArgument().getStates().AddRange(getUserStates());
		linkBroken.getArgument().Statex = getUserStatex();
		linkBroken.getArgument().Reason = Provider.BLinkBroken.REASON_PEERCLOSE; // 这个保留吧。现在没什么用。

		// 需要在锁外执行，因为如果 ProviderSocket 和 LinkdSocket 同时关闭。都需要去清理自己和对方，可能导致死锁。
		for (var e : bindsSwap.entrySet()) {
			var provider = App.getInstance().getProviderService().GetSocket(e.getValue());
			if (null == provider) {
				continue;
			}
			Object tempVar = provider.UserState;
			var providerSession = tempVar instanceof ProviderSession ? (ProviderSession)tempVar : null;
			if (null == providerSession) {
				continue;
			}

			provider.Send(linkBroken);
			providerSession.RemoveLinkSession(e.getKey(), getSessionId());
		}
	}
}