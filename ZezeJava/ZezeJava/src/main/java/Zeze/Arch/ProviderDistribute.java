package Zeze.Arch;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Builtin.Provider.BLoad;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.Bean;
import Zeze.Util.ConsistentHash;
import Zeze.Util.KV;
import Zeze.Util.OutLong;
import Zeze.Util.Random;
import Zeze.Util.SortedMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provider负载分发算法。
 * Linkd,Provider都需要使用。这里原则上必须是抽象的。
 */
public class ProviderDistribute extends ReentrantLock {
	public final Application zeze;
	public final LoadConfig loadConfig;
	private final Service providerService;
	private final long version;
	private final AtomicInteger feedFullOneByOneIndex = new AtomicInteger();
	private final ConcurrentHashMap<String, ConsistentHash<BServiceInfo>> consistentHashes = new ConcurrentHashMap<>(); // key:serviceName

	public ProviderDistribute(Application zeze, LoadConfig loadConfig, Service providerService, long version) {
		this.zeze = zeze;
		this.loadConfig = loadConfig;
		this.providerService = providerService;
		this.version = version;
	}

	public long getVersion() {
		return version;
	}

	private static final @NotNull SortedMap.HashFunc<Integer, BServiceInfo> hashFunc = (key, value, index) ->
			Bean.hash64(Bean.hash64(((long)key << 32) + index, value.getServiceName()), value.getServiceIdentity());

	public void addServer(@NotNull BServiceInfo s) {
		consistentHashes.computeIfAbsent(s.getServiceName(), __ -> new ConsistentHash<>(hashFunc))
				.add(s.getServiceIdentity(), s);
	}

	public void removeServer(@NotNull BServiceInfo s) {
		var consistentHash = consistentHashes.get(s.getServiceName());
		if (consistentHash != null)
			consistentHash.remove(s);
	}

	public static @NotNull String makeServiceName(@NotNull String serviceNamePrefix, int moduleId) {
		return serviceNamePrefix + moduleId;
	}

	public @Nullable ConsistentHash<BServiceInfo> getConsistentHash(@NotNull String name) {
		return consistentHashes.get(name);
	}

	// ChoiceDataIndex 用于RedirectAll或者那些已知数据分块索引的地方。
	public @Nullable BServiceInfo choiceDataIndex(@NotNull Agent.SubscribeState providers,
												  @Nullable ConsistentHash<BServiceInfo> consistentHash,
												  int dataIndex, int dataConcurrentLevel) {
		if (consistentHash == null)
			return null;
//		if (consistentHash.getNodes().size() > dataConcurrentLevel)
//			throw new IllegalStateException("ChoiceDataIndex: too many servers: "
//			+ consistentHash.getNodes().size() + " > " + dataConcurrentLevel);
		var serviceInfo = consistentHash.get(ByteBuffer.calc_hashnr(dataIndex));
		if (serviceInfo != null) {
			var providerModuleState = (ProviderModuleState)providers.getLocalStates().get(
					serviceInfo.getServiceIdentity());
			if (providerModuleState == null)
				return null;
			if (providerModuleState.sessionId == 0)
				return serviceInfo; // loop back 本机，不做过载保护。
			var providerSocket = providerService.GetSocket(providerModuleState.sessionId);
			if (providerSocket == null)
				return null;
			var ps = (ProviderSession)providerSocket.getUserState();
			if (ps.load.getOverload() == BLoad.eOverload)
				return null;
		}
		return serviceInfo;
	}

	public @Nullable BServiceInfo choiceHash(@NotNull Agent.SubscribeState providers, int hash,
											 int dataConcurrentLevel) {
		var serviceName = providers.getServiceName();
		var consistentHash = consistentHashes.get(serviceName);
		if (consistentHash == null)
			throw new IllegalStateException("ChoiceHash: not found ConsistentHash for serviceName=" + serviceName);
		if (dataConcurrentLevel <= 1)
			return consistentHash.get(hash);

		return choiceDataIndex(providers, consistentHash, (int)((hash & 0xffff_ffffL) % dataConcurrentLevel),
				dataConcurrentLevel);
	}

	public @Nullable BServiceInfo choiceHash(@NotNull Agent.SubscribeState providers, int hash) {
		return choiceHash(providers, hash, 1);
	}

	public boolean choiceHash(@NotNull Agent.SubscribeState providers, int hash, @NotNull OutLong provider) {
		provider.value = 0L;
		var serviceInfo = choiceHash(providers, hash);
		if (serviceInfo == null)
			return false;

		var providerModuleState = (ProviderModuleState)providers.getLocalStates().get(serviceInfo.getServiceIdentity());
		if (providerModuleState == null)
			return false;

		provider.value = providerModuleState.sessionId;
		return true;
	}

	public static boolean checkAppVersion(long serverAppVersion, long clientAppVersion) {
		if (clientAppVersion == 0) // 表示按以前的默认行为,不判断版本号
			return true;
		return serverAppVersion == clientAppVersion; // 暂时严格判断版本
//		return (serverAppVersion >>> 48) == (clientAppVersion >>> 48) && // 主版本必须一致
//				(serverAppVersion >>> 32) >= (clientAppVersion >>> 32); // 次版本不小于客户端次版本
	}

	public boolean choiceLoad(@NotNull Agent.SubscribeState providers, @NotNull OutLong provider) {
		provider.value = 0L;
		var serviceInfos = providers.getServiceInfos(version);
		if (serviceInfos == null)
			return false;

		var list = serviceInfos.getSortedIdentities();
		var frees = new ArrayList<KV<ProviderSession, Integer>>(list.size());
		var all = new ArrayList<ProviderSession>(list.size());
		int TotalWeight = 0;

		// 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
		for (int i = list.size() - 1; i >= 0; --i) {
			var serviceInfo = list.get(i);
			var providerModuleState = (ProviderModuleState)providers.getLocalStates().get(
					serviceInfo.getServiceIdentity());
			if (providerModuleState == null) {
				continue;
			}
			// Object tempVar2 = App.ProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
			var s = providerService.GetSocket(providerModuleState.sessionId);
			if (s == null)
				continue;
			var ps = (ProviderSession)s.getUserState();
			if (ps == null)
				continue; // 这里发现关闭的服务，仅仅忽略.

			if (ps.load.getOverload() == BLoad.eOverload)
				continue; // 忽略过载的服务器

			if (!checkAppVersion(ps.appVersion, version))
				continue;

			all.add(ps);

			if (ps.load.getOnlineNew() > loadConfig.getMaxOnlineNew())
				continue;

			int weight = ps.load.getProposeMaxOnline() - ps.load.getOnline();
			if (weight <= 0)
				continue;

			frees.add(KV.create(ps, weight));
			TotalWeight += weight;
		}
		if (TotalWeight > 0) {
			int randWeight = Random.getInstance().nextInt(TotalWeight);
			for (var ps : frees) {
				int weight = ps.getValue();
				if (randWeight < weight) {
					provider.value = ps.getKey().getSessionId();
					return true;
				}
				randWeight -= weight;
			}
		}
		// 选择失败，一般是都满载了，随机选择一个。
		if (!all.isEmpty()) {
			provider.value = all.get(Random.getInstance().nextInt(all.size())).getSessionId();
			return true;
		}
		// no providers
		return false;
	}

	public boolean choiceRequest(@NotNull Agent.SubscribeState providers, @NotNull OutLong provider) {
		provider.value = 0L;
		var serviceInfos = providers.getServiceInfos(version);
		if (serviceInfos == null)
			return false;

		var list = serviceInfos.getSortedIdentities();
		var frees = new ArrayList<KV<ProviderSession, Long>>(list.size());
		var all = new ArrayList<ProviderSession>(list.size());
		long TotalWeight = 0;
		long maxWeight = 0;

		// 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
		for (int i = list.size() - 1; i >= 0; --i) {
			var serviceInfo = list.get(i);
			var providerModuleState = (ProviderModuleState)providers.getLocalStates().get(
					serviceInfo.getServiceIdentity());
			if (providerModuleState == null) {
				continue;
			}
			// Object tempVar2 = App.ProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
			var s = providerService.GetSocket(providerModuleState.sessionId);
			if (s == null)
				continue;
			var ps = (ProviderSession)s.getUserState();
			if (ps == null)
				continue; // 这里发现关闭的服务，仅仅忽略.

			if (ps.load.getOverload() == BLoad.eOverload)
				continue; // 忽略过载的服务器

			if (!checkAppVersion(ps.appVersion, version))
				continue;

			all.add(ps);

			long weight = ps.timeCounter.count();
			if (weight > maxWeight)
				maxWeight = weight;
			frees.add(KV.create(ps, weight));
			TotalWeight += weight;
		}
		maxWeight += 10000; // 让最大的请求provider也有机会选中。
		TotalWeight = maxWeight * frees.size() - TotalWeight;
		if (TotalWeight > 0) {
			long randWeight = Random.getInstance().nextLong(TotalWeight);
			for (var ps : frees) {
				long weight = maxWeight - ps.getValue();
				if (randWeight < weight) {
					provider.value = ps.getKey().getSessionId();
					return true;
				}
				randWeight -= weight;
			}
		}
		// 选择失败，一般是都满载了，随机选择一个。
		if (!all.isEmpty()) {
			provider.value = all.get(Random.getInstance().nextInt(all.size())).getSessionId();
			return true;
		}
		// no providers
		return false;
	}

	// 查找时增加索引，和喂饱时增加索引，需要原子化。提高并发以后慢慢想，这里应该足够快了。
	public boolean choiceFeedFullOneByOne(@NotNull Agent.SubscribeState providers, @NotNull OutLong provider) {
		lock();
		try {
			provider.value = 0L;
			var serviceInfos = providers.getServiceInfos(version);
			if (serviceInfos == null)
				return false;

			var list = serviceInfos.getSortedIdentities();
			// 最多遍历一次。循环里面 continue 时，需要递增索引。
			for (int i = 0; i < list.size(); ++i, feedFullOneByOneIndex.incrementAndGet()) {
				var index = Integer.remainderUnsigned(feedFullOneByOneIndex.get(), list.size()); // current
				var serviceInfo = list.get(index);
				var providerModuleState = (ProviderModuleState)providers.getLocalStates().get(
						serviceInfo.getServiceIdentity());
				if (providerModuleState == null)
					continue;
				var providerSocket = providerService.GetSocket(providerModuleState.sessionId);
				if (providerSocket == null)
					continue;
				var ps = (ProviderSession)providerSocket.getUserState();

				// 这里发现关闭的服务，仅仅忽略.
				if (ps == null)
					continue;

				if (ps.load.getOverload() == BLoad.eOverload)
					continue; // 忽略过载服务器。

				// 这个和一个一个喂饱冲突，但是一下子给一个服务分配太多用户，可能超载。如果不想让这个生效，把MaxOnlineNew设置的很大。
				if (ps.load.getOnlineNew() > loadConfig.getMaxOnlineNew())
					continue;

				if (!checkAppVersion(ps.appVersion, version))
					continue;

				provider.value = ps.getSessionId();
				if (ps.load.getOnline() >= ps.load.getProposeMaxOnline())
					feedFullOneByOneIndex.incrementAndGet(); // 已经喂饱了一个，下一个。
				return true;
			}
			return false;
		} finally {
			unlock();
		}
	}

	public boolean choiceProviderByServerId(@NotNull String serviceNamePrefix, int moduleId, int serverId,
											@NotNull OutLong provider) {
		provider.value = 0L;
		var serviceName = makeServiceName(serviceNamePrefix, moduleId);
		var providers = zeze.getServiceManager().getSubscribeStates().get(serviceName);
		if (providers == null)
			return false;
		var serviceInfos = providers.getServiceInfos(version);
		if (serviceInfos == null)
			return false;
		var si = serviceInfos.findServiceInfoByServerId(serverId);
		if (si == null)
			return false;
		provider.value = ((ProviderModuleState)providers.getLocalStates().get(si.getServiceIdentity())).sessionId;
		return true;
	}
}
