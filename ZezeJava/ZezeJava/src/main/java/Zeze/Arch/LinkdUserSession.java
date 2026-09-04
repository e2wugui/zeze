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

public class LinkdUserSession {
	protected static final Logger logger = LogManager.getLogger(LinkdUserSession.class);

	// FND2-A1-3：写者应用Auth处理（如Zezex ModuleLinkd.ProcessAuthRequest，跑在LinkdService客户端
	// 连接EL），读者ProcessBindRequest动态分支日志（Task线程）跨线程，无happens-before时显示null/陈旧账号。
	protected volatile String account;
	// FND-A1-3：写者LinkdProvider.ProcessSetUserState跑在LinkdProviderService的IO线程，
	// 读者LinkdService.createDispatch跑在LinkdService的IO线程（两套EventLoop），无volatile时
	// 引用替换与读取之间无happens-before。对照ProviderSession.load等同场景字段均标volatile。
	protected volatile BUserState.Data userState = new BUserState.Data();
	protected final ReentrantReadWriteLock bindsLock = new ReentrantReadWriteLock();
	protected IntHashMap<Long> binds = new IntHashMap<>(); // 动态绑定(也会混合静态绑定) <moduleId,providerSessionId>
	protected long sessionId; // Linkd.SessionId
	// FND2-A1-2：写者Auth处理（客户端连接EL），读者ProcessBroadcast（provider连接EL），
	// 两套EventLoop无同步边；读到陈旧0时checkAppVersion(server,0)==true（0表示不检查），
	// onlySameVersion广播的版本过滤静默失效。同族先例：userState（e4a488194）、authed。
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
		// updateLinkSessionId(linkdProviderService, sessionId);
		this.sessionId = sessionId;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String value) {
		account = value;
	}

	public long getClientAppVersion() {
		return clientAppVersion;
	}

	public void setClientAppVersion(long clientAppVersion) {
		this.clientAppVersion = clientAppVersion;
	}

	public boolean trySetAccount(String newAccount) {
		if (account == null || account.isEmpty()) {
			account = newAccount;
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

	public Long getRoleId() {
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
					var s = linkdProviderService.GetSocket(exist);
					logger.warn("bind: replace provider moduleId={}, account={}, from={}, to={}",
							moduleId, account, s != null ? s.getRemoteAddress() : null, provider.getRemoteAddress());
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
		int removeCount = 0;
		int leftCount;
		var writeLock = bindsLock.writeLock();
		writeLock.lock();
		try {
			for (var moduleId : moduleIds) {
				var exist = binds.get(moduleId);
				if (exist != null) {
					if (exist == provider.getSessionId()) { // check owner? 也许不做这个检测更好？
						if (binds.remove(moduleId) != null)
							removeCount++;
						if (!isOnProviderClose) {
							var ps = (LinkdProviderSession)provider.getUserState();
							if (ps != null)
								ps.removeLinkSession(moduleId, link.getSessionId());
						}
					} else {
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

	/*
	protected void updateLinkSessionId(LinkdProviderService linkdProviderService, long newSessionId) {
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
	*/

	public void onClose(LinkdProviderService linkdProviderService) {
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
			bindProviders.add(provider); // 先收集， 去重。
		}
		if (!bindProviders.isEmpty()) {
			var linkBroken = new LinkBroken(new BLinkBroken.Data(
					account, sessionId, BLinkBroken.REASON_PEER_CLOSED, userState));
			for (var provider : bindProviders)
				provider.Send(linkBroken);
		}
	}
}
