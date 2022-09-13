package Zeze.Arch;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Builtin.Provider.BLinkBroken;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Service;
import Zeze.Util.IntHashMap;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkdUserSession {
	private static final Logger logger = LogManager.getLogger(LinkdUserSession.class);

	private String Account;
	private String Context = "";
	private Binary Contextx = Binary.Empty;
	private final ReentrantReadWriteLock BindsLock = new ReentrantReadWriteLock();
	private IntHashMap<Long> Binds = new IntHashMap<>(); // 动态绑定(也会混合静态绑定) <moduleId,providerSessionId>
	private final long SessionId; // Linkd.SessionId
	private Future<?> KeepAliveTask; // 仅在网络线程中回调，并且一个时候，只会有一个回调，不线程保护了。
	private volatile boolean authed;

	public LinkdUserSession(long sessionId) {
		SessionId = sessionId;
	}

	public final String getAccount() {
		return Account;
	}

	public final void setAccount(String value) {
		Account = value;
	}

	public final boolean TrySetAccount(String newAccount) {
		if (Account == null || Account.isEmpty()) {
			Account = newAccount;
			return true;
		}

		return Account.equals(newAccount);
	}

	public final String getContext() {
		return Context;
	}

	public final Binary getContextx() {
		return Contextx;
	}

	public final void SetUserState(String context, Binary contextx) {
		Context = context != null ? context : "";
		Contextx = contextx != null ? contextx : Binary.Empty;
	}

	public final Long getRoleId() {
		return Context.isEmpty() ? null : Long.parseLong(Context);
	}

	public final long getSessionId() {
		return SessionId;
	}

	public final boolean isAuthed() {
		return authed;
	}

	public final void setAuthed() {
		authed = true;
	}

	public final Long TryGetProvider(int moduleId) {
		var readLock = BindsLock.readLock();
		readLock.lock();
		try {
			return Binds.get(moduleId);
		} finally {
			readLock.unlock();
		}
	}

	public final void Bind(LinkdProviderService linkdProviderService, AsyncSocket link,
						   Iterable<Integer> moduleIds, AsyncSocket provider) {
		var providerSessionId = Long.valueOf(provider.getSessionId());
		var writeLock = BindsLock.writeLock();
		writeLock.lock();
		try {
			for (var moduleId : moduleIds) {
				var exist = Binds.get(moduleId);
				if (exist != null && exist.longValue() != providerSessionId.longValue()) {
					logger.warn("LinkSession.Bind replace provider {} {} {}", moduleId,
							linkdProviderService.GetSocket(exist).getRemoteAddress(), provider.getRemoteAddress());
				}
				Binds.put(moduleId, providerSessionId);
				var ps = (LinkdProviderSession)provider.getUserState();
				if (ps != null)
					ps.AddLinkSession(moduleId, link.getSessionId());
			}
		} finally {
			writeLock.unlock();
		}
	}

	public final void UnBind(LinkdProviderService linkdProviderService, AsyncSocket link,
							 int moduleId, AsyncSocket provider) {
		UnBind(linkdProviderService, link, moduleId, provider, false);
	}

	public final void UnBind(LinkdProviderService linkdProviderService, AsyncSocket link,
							 int moduleId, AsyncSocket provider, boolean isOnProviderClose) {
		UnBind(linkdProviderService, link, List.of(moduleId), provider, isOnProviderClose);
	}

	public final void UnBind(LinkdProviderService linkdProviderService, AsyncSocket link,
							 Iterable<Integer> moduleIds, AsyncSocket provider) {
		UnBind(linkdProviderService, link, moduleIds, provider, false);
	}

	public final void UnBind(LinkdProviderService linkdProviderService, AsyncSocket link,
							 Iterable<Integer> moduleIds, AsyncSocket provider, boolean isOnProviderClose) {
		var writeLock = BindsLock.writeLock();
		writeLock.lock();
		try {
			for (var moduleId : moduleIds) {
				var exist = Binds.get(moduleId);
				if (exist != null) {
					if (exist == provider.getSessionId()) { // check owner? 也许不做这个检测更好？
						Binds.remove(moduleId);
						if (!isOnProviderClose) {
							var ps = (LinkdProviderSession)provider.getUserState();
							if (ps != null)
								ps.RemoveLinkSession(moduleId, link.getSessionId());
						}
					} else {
						logger.warn("LinkSession.UnBind not owner {} {} {}", moduleId,
								linkdProviderService.GetSocket(exist).getRemoteAddress(), provider.getRemoteAddress());
					}
				}
			}
		} finally {
			writeLock.unlock();
		}
	}

	public final void KeepAlive(Service linkdService) {
		if (KeepAliveTask != null)
			KeepAliveTask.cancel(false);
		KeepAliveTask = Task.scheduleUnsafe(3000_000, () -> {
			var link = linkdService.GetSocket(SessionId);
			if (link != null)
				link.close();
		});
	}

	public final void OnClose(LinkdProviderService linkdProviderService) {
		if (KeepAliveTask != null)
			KeepAliveTask.cancel(false);

		if (!isAuthed())
			return; // 未验证通过的不通告。此时Binds肯定是空的。

		IntHashMap<Long> bindsSwap;
		var writeLock = BindsLock.writeLock();
		writeLock.lock();
		try {
			bindsSwap = Binds;
			Binds = new IntHashMap<>();
		} finally {
			writeLock.unlock();
		}

		// 需要在锁外执行，因为如果 ProviderSocket 和 LinkdSocket 同时关闭。都需要去清理自己和对方，可能导致死锁。
		var bindProviders = new HashSet<AsyncSocket>();
		for (var it = bindsSwap.iterator(); it.moveToNext(); ) {
			var provider = linkdProviderService.GetSocket(it.value());
			if (provider == null)
				continue;
			var providerSession = (LinkdProviderSession)provider.getUserState();
			if (providerSession == null)
				continue;
			providerSession.RemoveLinkSession(it.key(), SessionId);
			bindProviders.add(provider); // 先收集， 去重。
		}
		if (!bindProviders.isEmpty()) {
			var linkBroken = new LinkBroken(new BLinkBroken(Account, SessionId, BLinkBroken.REASON_PEERCLOSE, // 这个reason保留吧。现在没什么用。
					Context, Contextx));
			for (var provider : bindProviders)
				provider.Send(linkBroken);
		}
	}
}
