package Zeze.Arch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.ServiceInfo;
import Zeze.Util.ConsistentHash;
import Zeze.Util.OutLong;
import Zeze.Util.Random;

/**
 * Provider负载分发算法。
 * Linkd,Provider都需要使用。这里原则上必须是抽象的。
 * 目前LoadConfig仅用于Linkd。Provider可以传null，并不使用相关算法(ChoiceLoad)。
 */
public class ProviderDistribute {
	public Zeze.Application Zeze;
	public LoadConfig LoadConfig;
	public Service ProviderService;
	private final AtomicInteger FeedFullOneByOneIndex = new AtomicInteger();

	public ConcurrentHashMap<String, Zeze.Util.ConsistentHash<ServiceInfo>> ConsistentHashs = new ConcurrentHashMap<>();

	public void AddServer(Agent.SubscribeState state, ServiceInfo s) {
		var consistentHash = ConsistentHashs.computeIfAbsent(s.getServiceName(), key -> new ConsistentHash<>());
		consistentHash.add(s.getServiceIdentity(), s);
	}

	public void RemoveServer(Agent.SubscribeState state, ServiceInfo s) {
		var consistentHash = ConsistentHashs.get(s.getServiceName());
		if (null != consistentHash)
			consistentHash.remove(s.getServiceIdentity(), s);
	}

	public void ApplyServers(Agent.SubscribeState ass) {
		var consistentHash = ConsistentHashs.computeIfAbsent(ass.getServiceName(), key -> new ConsistentHash<>());
		var nodes = consistentHash.getNodes();
		var current = new HashSet<ServiceInfo>();
		for (var node : ass.getServiceInfos().getServiceInfoListSortedByIdentity()) {
			consistentHash.add(node.getServiceIdentity(), node);
			current.add(node);
		}
		for (var node : nodes) {
			if (!current.contains(node))
				consistentHash.remove(node.getServiceIdentity(), node);
		}
	}

	public static String MakeServiceName(String serviceNamePrefix, int moduleId) {
		return serviceNamePrefix + moduleId;
	}

	public Zeze.Util.ConsistentHash<ServiceInfo> getConsistentHash(String name) {
		return ConsistentHashs.get(name);
	}

	private int calc_hash(int src) {
		return ByteBuffer.calc_hashnr(src);
	}

	// ChoiceDataIndex 用于RedirectAll或者那些已知数据分块索引的地方。
	public ServiceInfo ChoiceDataIndex(Zeze.Util.ConsistentHash<ServiceInfo> consistentHash, int dataIndex, int dataConcurrentLevel) {
		if (consistentHash.getNodes().size() > dataConcurrentLevel)
			throw new RuntimeException("too many server");
		return consistentHash.get(calc_hash(dataIndex));
	}

	public ServiceInfo ChoiceHash(Agent.SubscribeState providers, int hash, int dataConcurrentLevel) {
		var consistentHash = ConsistentHashs.get(providers.getServiceName());
		if (null == consistentHash)
			return null;
		if (dataConcurrentLevel == 1)
			return consistentHash.get(hash);

		if (consistentHash.getNodes().size() > dataConcurrentLevel)
			throw new RuntimeException("too many server");

		var dataIndex = hash % dataConcurrentLevel;
		return consistentHash.get(calc_hash(dataIndex));
	}

	public ServiceInfo ChoiceHash(Agent.SubscribeState providers, int hash) {
		return ChoiceHash(providers, hash,1);
	}

	public boolean ChoiceHash(Agent.SubscribeState providers, int hash, OutLong provider) {
		provider.Value = 0L;
		var serviceInfo = ChoiceHash(providers, hash);
		if (serviceInfo == null)
			return false;

		var providerModuleState = (ProviderModuleState)serviceInfo.getLocalState();
		if (providerModuleState == null)
			return false;

		provider.Value = providerModuleState.SessionId;
		return true;
	}

	public boolean ChoiceLoad(Agent.SubscribeState providers, OutLong provider) {
		provider.Value = 0L;

		var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
		var frees = new ArrayList<ProviderSession>(list.size());
		var all = new ArrayList<ProviderSession>(list.size());
		int TotalWeight = 0;

		// 新的provider在后面，从后面开始搜索。后面的可能是新的provider。
		for (int i = list.size() - 1; i >= 0; --i) {
			var serviceInfo = list.get(i);
			var providerModuleState = (ProviderModuleState)serviceInfo.getLocalState();
			if (providerModuleState == null) {
				continue;
			}
			// Object tempVar2 = App.ProviderService.GetSocket(providerModuleState.getSessionId()).getUserState();
			var ps = (ProviderSession)ProviderService.GetSocket(providerModuleState.SessionId).getUserState();
			if (ps == null) {
				continue; // 这里发现关闭的服务，仅仅忽略.
			}
			all.add(ps);

			if (ps.Load.getOnlineNew() > LoadConfig.getMaxOnlineNew()) {
				continue;
			}
			int weight = ps.Load.getProposeMaxOnline() - ps.Load.getOnline();
			if (weight <= 0) {
				continue;
			}
			frees.add(ps);
			TotalWeight += weight;
		}
		if (TotalWeight > 0) {
			int randWeight = Random.getInstance().nextInt(TotalWeight);
			for (var ps : frees) {
				int weight = ps.Load.getProposeMaxOnline() - ps.Load.getOnline();
				if (randWeight < weight) {
					provider.Value = ps.getSessionId();
					return true;
				}
				randWeight -= weight;
			}
		}
		// 选择失败，一般是都满载了，随机选择一个。
		if (!all.isEmpty()) {
			provider.Value = all.get(Random.getInstance().nextInt(all.size())).getSessionId();
			return true;
		}
		// no providers
		return false;
	}

	public boolean ChoiceFeedFullOneByOne(Agent.SubscribeState providers, OutLong provider) {
		// 查找时增加索引，和喂饱时增加索引，需要原子化。提高并发以后慢慢想，这里应该足够快了。
		synchronized (this) {
			provider.Value = 0L;

			var list = providers.getServiceInfos().getServiceInfoListSortedByIdentity();
			// 最多遍历一次。循环里面 continue 时，需要递增索引。
			for (int i = 0; i < list.size(); ++i, FeedFullOneByOneIndex.incrementAndGet()) {
				var index = Integer.remainderUnsigned(FeedFullOneByOneIndex.get(), list.size()); // current
				var serviceInfo = list.get(index);
				var providerModuleState = (ProviderModuleState)serviceInfo.getLocalState();
				if (providerModuleState == null)
					continue;
				var providerSocket = ProviderService.GetSocket(providerModuleState.SessionId);
				if (providerSocket == null)
					continue;
				var ps = (LinkdProviderSession)providerSocket.getUserState();
				// 这里发现关闭的服务，仅仅忽略.
				if (ps == null)
					continue;

				// 这个和一个一个喂饱冲突，但是一下子给一个服务分配太多用户，可能超载。如果不想让这个生效，把MaxOnlineNew设置的很大。
				if (ps.Load.getOnlineNew() > LoadConfig.getMaxOnlineNew())
					continue;

				provider.Value = ps.getSessionId();
				if (ps.Load.getOnline() >= ps.Load.getProposeMaxOnline())
					FeedFullOneByOneIndex.incrementAndGet(); // 已经喂饱了一个，下一个。
				return true;
			}
			return false;
		}
	}

	public boolean ChoiceProviderByServerId(String serviceNamePrefix, int moduleId, int serverId, OutLong provider) {
		var serviceName = MakeServiceName(serviceNamePrefix, moduleId);

		var volatileProviders = Zeze.getServiceManagerAgent().getSubscribeStates().get(serviceName);
		if (volatileProviders == null) {
			provider.Value = 0L;
			return false;
		}
		var si = volatileProviders.getServiceInfos().findServiceInfoByServerId(serverId);
		if (si != null) {
			var state = (ProviderModuleState)si.getLocalState();
			provider.Value = state.SessionId;
			return true;
		}
		return false;
	}
}
