package Zeze.Arch;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.BAnnounceProviderInfo;
import Zeze.Util.IntHashMap;
import Zeze.Util.LongHashSet;

public class LinkdProviderSession extends ProviderSession {
	private BAnnounceProviderInfo Info;

	/**
	 * 维护此Provider上绑定的LinkSession，用来在Provider关闭的时候，进行 UnBind。
	 * moduleId -> LinkSids
	 * 多线程：主要由LinkSession回调.  需要保护。
	 */
	private final IntHashMap<LongHashSet> LinkSessionIds = new IntHashMap<>();

	/**
	 * 维护此Provider上绑定的StaticBinds，用来在Provider关闭的时候，进行 UnBind。
	 * 同时，当此Provider第一次被选中时，所有的StaticBinds都会一起被绑定到LinkSession上，
	 * 多线程：这里面的数据访问都处于 lock (Zezex.App.Instance.gnet_Provider_Module.StaticBinds) 下
	 * see Zezex.Provider.ModuleProvider
	 */
	private final ConcurrentHashMap<Integer, Integer> StaticBinds = new ConcurrentHashMap<>();

	public LinkdProviderSession(long ssid) {
		super(ssid);
	}

	public final BAnnounceProviderInfo getInfo() {
		return Info;
	}

	public final void setInfo(BAnnounceProviderInfo value) {
		Info = value;
	}

	public final IntHashMap<LongHashSet> getLinkSessionIds() {
		return LinkSessionIds;
	}

	public final ConcurrentHashMap<Integer, Integer> getStaticBinds() {
		return StaticBinds;
	}

	public final void AddLinkSession(int moduleId, long linkSessionId) {
		synchronized (LinkSessionIds) {
			LinkSessionIds.computeIfAbsent(moduleId, __ -> new LongHashSet()).add(linkSessionId);
		}
	}

	public final void RemoveLinkSession(int moduleId, long linkSessionId) {
		synchronized (LinkSessionIds) {
			var linkSids = LinkSessionIds.get(moduleId);
			if (linkSids != null) {
				if (linkSids.remove(linkSessionId)) {
					// 下线时Provider会进行统计，这里避免二次计数，
					// 没有扣除不会有问题，本来Load应该总是由Provider报告的。
					// --Load.Online;
					if (linkSids.isEmpty())
						LinkSessionIds.remove(moduleId);
				}
			}
		}
	}
}
