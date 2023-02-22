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
	protected static final Logger logger = LogManager.getLogger(LinkdUserSession.class);

	protected String account;
	protected String context = "";
	protected Binary contextx = Binary.Empty;
	protected final ReentrantReadWriteLock bindsLock = new ReentrantReadWriteLock();
	protected IntHashMap<Long> binds = new IntHashMap<>(); // 动态绑定(也会混合静态绑定) <moduleId,providerSessionId>
	protected long sessionId; // Linkd.SessionId
	protected Future<?> keepAliveTask; // 仅在网络线程中回调，并且一个时候，只会有一个回调，不线程保护了。
	protected volatile boolean authed;

	public LinkdUserSession(long sessionId) {
		this.sessionId = sessionId;
	}

	public void setSessionId(LinkdProviderService linkdProviderService, long sessionId) {
		updateLinkSessionId(linkdProviderService, sessionId);
		this.sessionId = sessionId;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String value) {
		account = value;
	}

	public boolean trySetAccount(String newAccount) {
		if (account == null || account.isEmpty()) {
			account = newAccount;
			return true;
		}

		return account.equals(newAccount);
	}

	public String getContext() {
		return context;
	}

	public Binary getContextx() {
		return contextx;
	}

	public void setUserState(String context, Binary contextx) {
		this.context = context != null ? context : "";
		this.contextx = contextx != null ? contextx : Binary.Empty;
	}

	public Long getRoleId() {
		return context.isEmpty() ? null : Long.parseLong(context);
	}

	public long getSessionId() {
		return sessionId;
	}

	public boolean isAuthed() {
		return authed;
	}

	public void setAuthed() {
		authed = true;
	}

	public Long tryGetProvider(int moduleId) {
		var readLock = bindsLock.readLock();
		readLock.lock();
		try {
			return binds.get(moduleId);
		} finally {
			readLock.unlock();
		}
	}

	public void bind(LinkdProviderService linkdProviderService, AsyncSocket link,
					 Iterable<Integer> moduleIds, AsyncSocket provider) {
		var providerSessionId = Long.valueOf(provider.getSessionId());
		var writeLock = bindsLock.writeLock();
		writeLock.lock();
		try {
			for (var moduleId : moduleIds) {
				var exist = binds.get(moduleId);
				if (exist != null && exist.longValue() != providerSessionId.longValue()) {
					logger.warn("LinkSession.Bind replace provider {} {} {}", moduleId,
							linkdProviderService.GetSocket(exist).getRemoteAddress(), provider.getRemoteAddress());
				}
				binds.put(moduleId, providerSessionId);
				var ps = (LinkdProviderSession)provider.getUserState();
				if (ps != null)
					ps.addLinkSession(moduleId, link.getSessionId());
			}
		} finally {
			writeLock.unlock();
		}
	}

	public void unbind(LinkdProviderService linkdProviderService, AsyncSocket link,
					   int moduleId, AsyncSocket provider) {
		unbind(linkdProviderService, link, moduleId, provider, false);
	}

	public void unbind(LinkdProviderService linkdProviderService, AsyncSocket link,
					   int moduleId, AsyncSocket provider, boolean isOnProviderClose) {
		unbind(linkdProviderService, link, List.of(moduleId), provider, isOnProviderClose);
	}

	public void unbind(LinkdProviderService linkdProviderService, AsyncSocket link,
					   Iterable<Integer> moduleIds, AsyncSocket provider) {
		unbind(linkdProviderService, link, moduleIds, provider, false);
	}

	public void unbind(LinkdProviderService linkdProviderService, AsyncSocket link,
					   Iterable<Integer> moduleIds, AsyncSocket provider, boolean isOnProviderClose) {
		var writeLock = bindsLock.writeLock();
		writeLock.lock();
		try {
			for (var moduleId : moduleIds) {
				var exist = binds.get(moduleId);
				if (exist != null) {
					if (exist == provider.getSessionId()) { // check owner? 也许不做这个检测更好？
						binds.remove(moduleId);
						if (!isOnProviderClose) {
							var ps = (LinkdProviderSession)provider.getUserState();
							if (ps != null)
								ps.removeLinkSession(moduleId, link.getSessionId());
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

	public void keepAlive(Service linkdService) {
		if (keepAliveTask != null)
			keepAliveTask.cancel(false);
		keepAliveTask = Task.scheduleUnsafe(3000_000, () -> {
			var link = linkdService.GetSocket(sessionId);
			if (link != null) {
				logger.warn("KeepAlive timeout: {}", link);
				link.close();
			}
		});
	}

	private void updateLinkSessionId(LinkdProviderService linkdProviderService, long newSessionId) {
		var writeLock = bindsLock.writeLock();
		writeLock.lock();
		try {
			for (var it = binds.iterator(); it.moveToNext(); ) {
				var provider = linkdProviderService.GetSocket(it.value());
				if (provider == null)
					continue;
				var providerSession = (LinkdProviderSession)provider.getUserState();
				if (providerSession == null)
					continue;
				providerSession.updateLinkSessionId(it.key(), sessionId, newSessionId);
			}
		} finally {
			writeLock.unlock();
		}
	}

	public void onClose(LinkdProviderService linkdProviderService) {
		if (keepAliveTask != null)
			keepAliveTask.cancel(false);

		if (!isAuthed())
			return; // 未验证通过的不通告。此时Binds肯定是空的。

		IntHashMap<Long> bindsSwap;
		var writeLock = bindsLock.writeLock();
		writeLock.lock();
		try {
			bindsSwap = binds;
			binds = new IntHashMap<>();
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
			providerSession.removeLinkSession(it.key(), sessionId);
			bindProviders.add(provider); // 先收集， 去重。
		}
		if (!bindProviders.isEmpty()) {
			var linkBroken = new LinkBroken(new BLinkBroken(account, sessionId, BLinkBroken.REASON_PEERCLOSE, // 这个reason保留吧。现在没什么用。
					context, contextx));
			for (var provider : bindProviders)
				provider.Send(linkBroken);
		}
	}
}
