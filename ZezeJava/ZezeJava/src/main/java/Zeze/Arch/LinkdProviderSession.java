package Zeze.Arch;

import Zeze.Builtin.Provider.BAnnounceProviderInfo;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashSet;

public class LinkdProviderSession extends ProviderSession {
	private BAnnounceProviderInfo info;

	/**
	 * 维护此Provider上绑定的LinkSession，用来在Provider关闭的时候，进行 UnBind。
	 * moduleId -> LinkSids
	 * 多线程：主要由LinkSession回调.  需要保护。
	 */
	private final IntHashMap<LongHashSet> linkSessionIds = new IntHashMap<>();

	/**
	 * 维护此Provider上绑定的StaticBinds，用来在Provider关闭的时候，进行 UnBind。
	 * 同时，当此Provider第一次被选中时，所有的StaticBinds都会一起被绑定到LinkSession上，
	 * 多线程：这里面的数据访问都处于 lock (Zezex.App.Instance.gnet_Provider_Module.StaticBinds) 下
	 * see Zezex.Provider.ModuleProvider
	 */
	private final ConcurrentHashSet<Integer> staticBinds = new ConcurrentHashSet<>(); // <moduleId>

	public LinkdProviderSession(long ssid) {
		super.sessionId = ssid;
	}

	public final BAnnounceProviderInfo getInfo() {
		return info;
	}

	public final void setInfo(BAnnounceProviderInfo value) {
		info = value;
	}

	public final IntHashMap<LongHashSet> getLinkSessionIds() {
		return linkSessionIds;
	}

	public final ConcurrentHashSet<Integer> getStaticBinds() {
		return staticBinds;
	}

	public final void addLinkSession(int moduleId, long linkSessionId) {
		synchronized (linkSessionIds) {
			linkSessionIds.computeIfAbsent(moduleId, __ -> new LongHashSet()).add(linkSessionId);
		}
	}

	public final void removeLinkSession(int moduleId, long linkSessionId) {
		synchronized (linkSessionIds) {
			var linkSids = linkSessionIds.get(moduleId);
			if (linkSids != null) {
				if (linkSids.remove(linkSessionId)) {
					// 下线时Provider会进行统计，这里避免二次计数，
					// 没有扣除不会有问题，本来Load应该总是由Provider报告的。
					// --Load.Online;
					if (linkSids.isEmpty())
						linkSessionIds.remove(moduleId);
				}
			}
		}
	}
}
