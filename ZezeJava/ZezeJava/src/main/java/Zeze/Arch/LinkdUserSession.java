package Zeze.Arch;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Builtin.Provider.BLinkBroken;
import Zeze.Builtin.Provider.BUserState;
import Zeze.Builtin.Provider.LinkBroken;
import Zeze.Net.AsyncSocket;
import Zeze.Util.IntHashMap;
import Zeze.Util.Str;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Linkd 侧客户端会话：保存账号、认证状态与模块→Provider 的绑定表，处理 bind/unbind 与关闭清理。
 */
public class LinkdUserSession {
	protected static final Logger logger = LogManager.getLogger(LinkdUserSession.class);

	protected volatile @Nullable String account;
	protected volatile BUserState.Data userState = new BUserState.Data();
	protected final ReentrantReadWriteLock bindsLock = new ReentrantReadWriteLock();
	protected IntHashMap<Long> binds = new IntHashMap<>(); // 动态绑定(也会混合静态绑定) <moduleId,providerSessionId>
	// 会话关闭标志（在 bindsLock 写锁内随 binds 换出置位，拒绝迟到的 bind）。
	protected boolean closed;
	protected long sessionId; // Linkd.SessionId
	protected volatile long clientAppVersion;
	protected long lastReportUnbindDynamicModuleTime;
	protected volatile boolean authed;

	@Override
	public String toString() {
		return "(account=" + account + ",roleId=" + userState.getContext() + ",online=" + userState.getOnlineSetName()
				+ ",version=" + Str.toVersionStr(clientAppVersion) + ')';
	}

	public LinkdUserSession(long sessionId) {
		this.sessionId = sessionId;
	}

	public void setSessionId(LinkdProviderService linkdProviderService, long sessionId) {
		this.sessionId = sessionId;
	}

	public @Nullable String getAccount() {
		return account;
	}

	public void setAccount(@Nullable String value) {
		account = value;
	}

	public long getClientAppVersion() {
		return clientAppVersion;
	}

	public void setClientAppVersion(long clientAppVersion) {
		this.clientAppVersion = clientAppVersion;
	}

	public boolean trySetAccount(String newAccount) {
		var account = this.account;
		if (account == null || account.isEmpty()) {
			this.account = newAccount;
			return true;
		}

		return account.equals(newAccount);
	}

	public BUserState.Data getUserState() {
		return userState;
	}

	public void setUserState(BUserState.Data state) {
		this.userState = state;
	}

	public @Nullable Long getRoleId() {
		var context = userState.getContext();
		if (context.isEmpty())
			return null;
		try {
			return Long.parseLong(context);
		} catch (NumberFormatException e) {
			return null; // 账号在线模式下context是clientId(任意字符串,见Arch.Online登录setContext)，没有roleId
		}
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

	public @Nullable Long tryGetProvider(int moduleId) {
		var readLock = bindsLock.readLock();
		readLock.lock();
		try {
			return binds.get(moduleId);
		} finally {
			readLock.unlock();
		}
	}

	public void bind(@Nullable LinkdProviderService linkdProviderService, @Nullable AsyncSocket link,
					 Iterable<Integer> moduleIds, AsyncSocket provider) {
		var providerSessionId = Long.valueOf(provider.getSessionId());
		var writeLock = bindsLock.writeLock();
		writeLock.lock();
		try {
			if (closed) {
				logger.warn("bind after closed: account={}, link session={}", account, sessionId);
				return;
			}
			for (var moduleId : moduleIds) {
				var ps = (LinkdProviderSession)provider.getUserState();
				// 先在 provider 反向索引准入再写 binds，与 onProviderClose 保持全序。
				//noinspection DataFlowIssue
				if (ps != null && !ps.tryAddLinkSession(moduleId, link.getSessionId()))
					continue;
				var exist = binds.get(moduleId);
				if (exist != null && exist.longValue() != providerSessionId.longValue()) {
					//noinspection DataFlowIssue
					var s = linkdProviderService.GetSocket(exist);
					if (s != null && s.getUserState() instanceof LinkdProviderSession oldSession)
						//noinspection DataFlowIssue
						oldSession.removeLinkSession(moduleId, link.getSessionId());
					logger.warn("bind: replace provider moduleId={}, account={}, from={}, to={}",
							moduleId, account, s != null ? s.getRemoteAddress() : null, provider.getRemoteAddress());
				}
				binds.put(moduleId, providerSessionId);
			}
		} finally {
			writeLock.unlock();
		}
	}

	public void unbind(@Nullable LinkdProviderService linkdProviderService, @Nullable AsyncSocket link,
					   int moduleId, AsyncSocket provider) {
		unbind(linkdProviderService, link, moduleId, provider, false);
	}

	public void unbind(@Nullable LinkdProviderService linkdProviderService, @Nullable AsyncSocket link,
					   int moduleId, AsyncSocket provider, boolean isOnProviderClose) {
		unbind(linkdProviderService, link, List.of(moduleId), provider, isOnProviderClose);
	}

	public void unbind(@Nullable LinkdProviderService linkdProviderService, @Nullable AsyncSocket link,
					   Iterable<Integer> moduleIds, AsyncSocket provider) {
		unbind(linkdProviderService, link, moduleIds, provider, false);
	}

	public void unbind(@Nullable LinkdProviderService linkdProviderService, @Nullable AsyncSocket link,
					   Iterable<Integer> moduleIds, AsyncSocket provider, boolean isOnProviderClose) {
		int removeCount = 0;
		int leftCount;
		var writeLock = bindsLock.writeLock();
		writeLock.lock();
		try {
			for (var moduleId : moduleIds) {
				var exist = binds.get(moduleId);
				if (exist != null) {
					if (exist == provider.getSessionId()) { // check owner.
						if (binds.remove(moduleId) != null)
							removeCount++;
						if (!isOnProviderClose) {
							var ps = (LinkdProviderSession)provider.getUserState();
							if (ps != null)
								//noinspection DataFlowIssue
								ps.removeLinkSession(moduleId, link.getSessionId());
						}
					} else {
						//noinspection DataFlowIssue
						var s = linkdProviderService.GetSocket(exist);
						logger.warn("unbind not owner: moduleId={}, owner={}, sender={}", moduleId,
								s != null ? s.getRemoteAddress() : null, provider.getRemoteAddress());
					}
				}
			}
			leftCount = binds.size(); // onClose会在写锁内整体换出binds引用,size必须在锁内取
		} finally {
			writeLock.unlock();
		}
		logger.info("unbind: account={}, moduleIds={}, removeCount={}, leftCount={}",
				account, moduleIds, removeCount, leftCount);
	}

	public void onClose(@Nullable LinkdProviderService linkdProviderService) {
		// 未认证连接不发 LinkBroken，但仍需置 closed 并换出 binds 以清理可能已登记的反向索引。
		var authed = isAuthed();
		IntHashMap<Long> bindsSwap;
		var writeLock = bindsLock.writeLock();
		writeLock.lock();
		try {
			bindsSwap = binds;
			binds = new IntHashMap<>();
			closed = true; // 换出后拒绝迟到的动态bind。
		} finally {
			writeLock.unlock();
		}

		// 需要在锁外执行，因为如果 ProviderSocket 和 LinkdSocket 同时关闭。都需要去清理自己和对方，可能导致死锁。
		var bindProviders = new HashSet<AsyncSocket>();
		for (var it = bindsSwap.iterator(); it.moveToNext(); ) {
			//noinspection DataFlowIssue
			var provider = linkdProviderService.GetSocket(it.value());
			if (provider == null) {
				logger.warn("bind provider miss: account={}, moduleId={}, providerSessionId={}",
						account, it.key(), it.value());
				continue;
			}
			var providerSession = (LinkdProviderSession)provider.getUserState();
			if (providerSession == null) {
				logger.warn("bind provider miss session: account={}, moduleId={}, providerSessionId={}",
						account, it.key(), it.value());
				continue;
			}
			providerSession.removeLinkSession(it.key(), sessionId);
			if (authed)
				bindProviders.add(provider); // 先收集， 去重。
		}
		if (authed && !bindProviders.isEmpty()) {
			var linkBroken = new LinkBroken(new BLinkBroken.Data(
					account, sessionId, BLinkBroken.REASON_PEER_CLOSED, userState));
			for (var provider : bindProviders)
				provider.Send(linkBroken);
		}
	}
}
