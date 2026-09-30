package Zeze.Arch;

import java.util.concurrent.locks.ReentrantLock;
import Zeze.Builtin.Provider.BAnnounceProviderInfo;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashSet;

/**
 * Linkd 侧 Provider 连接会话：记录 Provider 宣告信息与其上绑定的 LinkSession、静态/动态模块集合。
 */
public class LinkdProviderSession extends ProviderSession {
	protected BAnnounceProviderInfo.Data info;

	/**
	 * 维护此Provider上绑定的LinkSession，用来在Provider关闭的时候，进行 UnBind。
	 * moduleId －＞ LinkSids
	 * 多线程：主要由LinkSession回调.  需要保护。
	 */
	protected IntHashMap<LongHashSet> linkSessionIds = new IntHashMap<>();
	protected final ReentrantLock linkSessionIdsLock = new ReentrantLock();
	private boolean linkSessionsClosed; // 与反向索引登记/关闭快照由同一把锁保护。

	/**
	 * 维护此Provider上绑定的StaticBinds，用来在Provider关闭的时候，进行 UnBind。
	 * 同时，当此Provider第一次被选中时，所有的StaticBinds都会一起被绑定到LinkSession上，
	 * 多线程：这里面的数据访问都处于 lock (Zezex.App.Instance.gnet_Provider_Module.StaticBinds) 下。
	 * see Zezex.Provider.ModuleProvider
	 */
	protected final ConcurrentHashSet<Integer> staticBinds = new ConcurrentHashSet<>(); // <moduleId>

	/**
	 * 维护此Provider通过Subscribe注册的动态模块，用来在Provider关闭的时候清理localStates
	 * （否则Subscribe写入的localStates在连接关闭后残留死sessionId状态）。
	 */
	protected final ConcurrentHashSet<Integer> dynamicSubscribes = new ConcurrentHashSet<>(); // <moduleId>

	public LinkdProviderSession(long ssid) {
		super.sessionId = ssid;
		super.disableChoice = true; // link-gs 连接新建立的时候，默认禁止选择。
	}

	public BAnnounceProviderInfo.Data getInfo() {
		return info;
	}

	public void setInfo(BAnnounceProviderInfo.Data value) {
		info = value;
	}

	public ConcurrentHashSet<Integer> getStaticBinds() {
		return staticBinds;
	}

	public ConcurrentHashSet<Integer> getDynamicSubscribes() {
		return dynamicSubscribes;
	}

	public void addLinkSession(int moduleId, long linkSessionId) {
		tryAddLinkSession(moduleId, linkSessionId);
	}

	public boolean tryAddLinkSession(int moduleId, long linkSessionId) {
		linkSessionIdsLock.lock();
		try {
			if (linkSessionsClosed)
				return false;
			linkSessionIds.computeIfAbsent(moduleId, __ -> new LongHashSet()).add(linkSessionId);
			return true;
		} finally {
			linkSessionIdsLock.unlock();
		}
	}

	/**
	 * 锁内换出整个map，O(1)临界区。onProviderClose后续的unbind与发送必须在锁外进行
	 * （unbind会获取LinkdUserSession.bindsLock，与bind路径的锁序相反，持锁调用会死锁），
	 * 因此这里只能换出快照，不能在锁内遍历处理。
	 */
	public IntHashMap<LongHashSet> swapLinkSessionIds() {
		linkSessionIdsLock.lock();
		try {
			linkSessionsClosed = true; // 换出后不再准入，迟到的bind不能写入无人清理的新map。
			var old = linkSessionIds;
			linkSessionIds = new IntHashMap<>();
			return old;
		} finally {
			linkSessionIdsLock.unlock();
		}
	}

	public void removeLinkSession(int moduleId, long linkSessionId) {
		linkSessionIdsLock.lock();
		try {
			var linkSids = linkSessionIds.get(moduleId);
			if (linkSids != null) {
				if (linkSids.remove(linkSessionId)) {
					// 下线时Provider会进行统计，这里避免二次计数，
					// 没有扣除不会有问题，本来Load应该总是由Provider报告的。
					if (linkSids.isEmpty())
						linkSessionIds.remove(moduleId);
				}
			}
		} finally {
			linkSessionIdsLock.unlock();
		}
	}

	public void updateLinkSessionId(int moduleId, long oldLinkSessionId, long newLinkSessionId) {
		linkSessionIdsLock.lock();
		try {
			var linkSids = linkSessionIds.get(moduleId);
			if (linkSids != null) {
				linkSids.remove(oldLinkSessionId);
				linkSids.add(newLinkSessionId);
			}
		} finally {
			linkSessionIdsLock.unlock();
		}
	}
}
