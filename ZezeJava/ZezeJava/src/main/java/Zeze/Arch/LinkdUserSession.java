package Zeze.Arch;

import java.util.HashSet;
import java.util.concurrent.Future;
import Zeze.Builtin.Provider.BLinkBroken;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Util.IntHashMap;
import Zeze.Util.OutLong;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkdUserSession {
	private static final Logger logger = LogManager.getLogger(LinkdUserSession.class);

	private String Account;
	private String Context = "";
	private Binary Contextx = Binary.Empty;
	private IntHashMap<Long> Binds = new IntHashMap<>();
	private final long SessionId; // Linkd.SessionId
	private Future<?> KeepAliveTask; // 仅在网络线程中回调，并且一个时候，只会有一个回调，不线程保护了。
	private boolean authed = false;

	public final String getAccount() {
		return Account;
	}

	public final void setAccount(String value) {
		Account = value;
	}

	public final boolean isAuthed() {
		synchronized (this) {
			return authed;
		}
	}

	public final void setAuthed() {
		synchronized (this) {
			authed = true;
		}
	}

	public boolean TrySetAccount(String newAccount)
	{
		synchronized (this) {
			if (null == Account || Account.isEmpty()) {
				Account = newAccount;
				return true;
			}

			return Account.equals(newAccount);
		}
	}

	public final String getContext() {
		return Context;
	}

	public final Binary getContextx() {
		return Contextx;
	}

	public Long getRoleId() {
		return Context.isEmpty() ? null : Long.parseLong(Context);
	}

	private IntHashMap<Long> getBinds() {
		return Binds;
	}

	private void setBinds(IntHashMap<Long> value) {
		Binds = value;
	}

	public final long getSessionId() {
		return SessionId;
	}

	public LinkdUserSession(long sessionId) {
		SessionId = sessionId;
	}

	public final void SetUserState(String context, Binary contextx) {
		synchronized (this) { // 简单使用一下这个锁。
			Context = context;
			Contextx = contextx;
		}
	}

	public final boolean TryGetProvider(int moduleId, OutLong provider) {
		synchronized (this) {
			var bound = Binds.get(moduleId);
			if (bound != null) {
				provider.Value = bound;
				return true;
			}
			provider.Value = 0L;
			return false;
		}
	}

	public final void Bind(LinkdProviderService linkdProviderService, AsyncSocket link,
						   Iterable<Integer> moduleIds, AsyncSocket provider) {
		synchronized (this) {
			for (var moduleId : moduleIds) {
				var exist = Binds.get(moduleId);
				if (exist != null) {
					var oldSocket = linkdProviderService.GetSocket(exist);
					logger.warn("LinkSession.Bind replace provider {} {} {}",
							moduleId, oldSocket.getRemoteAddress(), provider.getRemoteAddress());
				}
				getBinds().put(moduleId, provider.getSessionId());
				var ps = (LinkdProviderSession)provider.getUserState();
				if (ps != null)
					ps.AddLinkSession(moduleId, link.getSessionId());
			}
		}
	}

	public final void UnBind(LinkdProviderService linkdProviderService, AsyncSocket link,
							 int moduleId, AsyncSocket provider) {
		UnBind(linkdProviderService, link, moduleId, provider, false);
	}

	public final void UnBind(LinkdProviderService linkdProviderService, AsyncSocket link,
							 int moduleId, AsyncSocket provider, boolean isOnProviderClose) {
		var moduleIds = new HashSet<Integer>();
		moduleIds.add(moduleId);
		UnBind(linkdProviderService, link, moduleIds, provider, isOnProviderClose);
	}

	public final void UnBind(LinkdProviderService linkdProviderService, AsyncSocket link,
							 Iterable<Integer> moduleIds, AsyncSocket provider) {
		UnBind(linkdProviderService, link, moduleIds, provider, false);
	}

	public final void UnBind(LinkdProviderService linkdProviderService, AsyncSocket link,
							 Iterable<Integer> moduleIds, AsyncSocket provider, boolean isOnProviderClose) {
		synchronized (this) {
			for (var moduleId : moduleIds) {
				var exist = Binds.get(moduleId);
				if (exist != null) {
					if (exist == provider.getSessionId()) { // check owner? 也许不做这个检测更好？
						getBinds().remove(moduleId);
						if (!isOnProviderClose) {
							var ps = (LinkdProviderSession)provider.getUserState();
							if (ps != null)
								ps.RemoveLinkSession(moduleId, link.getSessionId());
						}
					} else {
						var oldSocket = linkdProviderService.GetSocket(exist);
						logger.warn("LinkSession.UnBind not owner {} {} {}",
								moduleId, oldSocket.getRemoteAddress(), provider.getRemoteAddress());
					}
				}
			}
		}
	}

	public final void KeepAlive(Service linkdService) {
		if (KeepAliveTask != null) {
			KeepAliveTask.cancel(false);
		}
		KeepAliveTask = Task.schedule(3000_000, () -> {
			var link = linkdService.GetSocket(getSessionId());
			if (link != null)
				link.close();
		});
	}

	public final void OnClose(LinkdProviderService linkdProviderService) {
		if (KeepAliveTask != null) {
			KeepAliveTask.cancel(false);
		}

		if (!isAuthed()) {
			// 未验证通过的不通告。此时Binds肯定是空的。
			return;
		}

		IntHashMap<Long> bindsSwap;
		synchronized (this) {
			bindsSwap = getBinds();
			setBinds(new IntHashMap<>());
		}

		var linkBroken = new LinkBroken();
		linkBroken.Argument.setAccount(Account);
		linkBroken.Argument.setLinkSid(SessionId);
		linkBroken.Argument.setContext(Context);
		linkBroken.Argument.setContextx(Contextx);
		linkBroken.Argument.setReason(BLinkBroken.REASON_PEERCLOSE); // 这个保留吧。现在没什么用。

		// 需要在锁外执行，因为如果 ProviderSocket 和 LinkdSocket 同时关闭。都需要去清理自己和对方，可能导致死锁。
		HashSet<AsyncSocket> bindProviders = new HashSet<>();
		for (var it = bindsSwap.iterator(); it.moveToNext(); ) {
			var provider = linkdProviderService.GetSocket(it.value());
			if (provider == null) {
				continue;
			}
			var providerSession = (LinkdProviderSession)provider.getUserState();
			if (providerSession == null) {
				continue;
			}
			providerSession.RemoveLinkSession(it.key(), getSessionId());
			bindProviders.add(provider); // 先收集， 去重。
		}
		for (var provider : bindProviders) {
			provider.Send(linkBroken);
		}
	}
}
