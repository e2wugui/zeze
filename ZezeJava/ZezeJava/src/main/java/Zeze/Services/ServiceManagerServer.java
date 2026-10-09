package Zeze.Services;

import java.io.Closeable;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import Zeze.Component.ThreadingServer;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Raft.RaftConfig;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.*;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FastLock;
import Zeze.Util.LongHashMap;
import Zeze.Util.LongHashSet;
import Zeze.Util.LongList;
import Zeze.Util.Random;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import Zeze.Util.TaskOneByOneByKey;
import Zeze.Util.TaskSpec;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDBException;
import org.w3c.dom.Element;

import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;

/**
 * 服务管理：注册和订阅
 * 【名词】
 * 动态服务(gs)
 * 动态服务器一般指启用cache-sync的逻辑服务器。比如gs。
 * 注册服务器（ServiceManager）
 * 支持更新服务器，这个服务一开始是为了启用cache-sync的服务器的查找。
 * 动态服务器列表使用者(linkd)
 * 当前使用动态服务的客户端主要是Game2/linkd，linkd在hash分配请求的时候需要一致动态服务器列表。
 * <p>
 * 【下面的流程都是用现有的服务名字（上面括号中的名字）】
 * <p>
 * 【本控制功能的目标】
 * 所有的linkd的可用动态服务列表的更新并不是原子的。
 * 1. 让所有的linkd的列表保持最新；
 * 2. 尽可能减少linkd上的服务列表不一致的时间（通过ready-commit机制）；
 * 3. 列表不一致时，分发请求可能引起cache不命中，但不影响正确性（cache-sync保证了正确性）；
 * <p>
 * 【主要事件和流程】
 * 1. gs停止时调用 registerService,unRegisterService 向ServiceManager声明自己服务状态。
 * 2. linkd启动时调用 subscribeService, unSubscribeService 向ServiceManager申请使用gs-list。
 * 3. ServiceManager在 registerService,unRegisterService 处理时发送 NotifyServiceList 给所有的 linkd。
 * 4. linkd收到NotifyServiceList先记录到本地，同时持续关注自己和gs之间的连接，
 * 当列表中的所有service都准备完成时调用 ReadyServiceList。
 * 5. ServiceManager收到所有的linkd的ReadyServiceList后，向所有的linkd广播 CommitServiceList。
 * 6. linkd 收到 CommitServiceList 时，启用新的服务列表。
 * <p>
 * 【特别规则和错误处理】
 * 1. linkd 异常停止，ServiceManager 按 unSubscribeService 处理，仅仅简单移除use-list。相当于减少了以后请求来源。
 * 2. gs 异常停止，ServiceManager 按 unRegisterService 处理，移除可用服务，并启动列表更新流程（NotifyServiceList）。
 * 3. linkd 处理 gs 关闭（在NotifyServiceList之前），仅仅更新本地服务列表状态，让该服务暂时不可用，但不改变列表。
 * linkd总是使用ServiceManager提交给他的服务列表，自己不主动增删。
 * linkd在NotifyServiceList的列表减少的处理：一般总是立即进入ready（因为其他gs都是可用状态）。
 * 4. ServiceManager 异常关闭：
 * a) 启用raft以后，新的master会有正确列表数据，但服务状态（连接）未知，此时等待gs的registerService一段时间,
 * 然后开启新的一轮NotifyServiceList，等待时间内没有再次注册的gs以后当作新的处理。
 * b) 启用raft的好处是raft的非master服务器会识别这种状态，并重定向请求到master，使得系统内只有一个master启用服务。
 * 实际上raft不需要维护相同数据状态（gs-list），从空的开始即可，启用raft的话仅使用他的选举功能。
 * #) Raft版实现见ServiceManagerWithRaft（main -raft启动）。
 * 5. ServiceManager开启一轮变更通告过程中，有新的gs启动停止，将开启新的通告(NotifyServiceList)。
 * ReadyServiceList时会检查ready中的列表是否和当前ServiceManagerList一致，不一致直接忽略。
 * 新的通告流程会促使linkd继续发送ready。
 * 另外为了更健壮的处理通告，通告加一个超时机制。超时没有全部ready，就启动一次新的通告。
 * 原则是：总按最新的gs-list通告。中间不一致的ready全部忽略。
 */
public final class ServiceManagerServer extends ReentrantLock implements Closeable {
	// 显式启动动作（构造器调用），仅当显式指定logLevel属性才重置root logger级别——
	// 不在类加载时静默篡改全JVM日志配置（测试/工具/同JVM引用会受影响）。WithRaft版同源调用。
	static void applyLogLevelProperty() {
		var levelProperty = System.getProperty("logLevel");
		if (levelProperty != null)
			((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger()
				.setLevel(Level.toLevel(levelProperty, Level.INFO));
	}

	private static final @NotNull Logger logger = LogManager.getLogger(ServiceManagerServer.class);

	// ServiceInfo.Name -> ServiceState
	private final ConcurrentHashMap<String, ServiceState> serviceStates = new ConcurrentHashMap<>();

	// 简单负载广播，
	// 在registerService/updateService时自动订阅，会话关闭的时候删除。
	// ProcessSetLoad时广播，本来不需要记录负载数据的，但为了以后可能的查询，保存一份。
	private final ConcurrentHashMap<String, LoadObservers> loads = new ConcurrentHashMap<>();

	private static final class LoadObservers extends FastLock {
		private final @NotNull ServiceManagerServer serviceManager;
		private final LongHashSet observers = new LongHashSet();

		public LoadObservers(@NotNull ServiceManagerServer m) {
			serviceManager = m;
		}

		public void addObserver(long sessionId) {
			lock();
			try {
				observers.add(sessionId);
			} finally {
				unlock();
			}
		}

		public void setLoad(@NotNull BServerLoad load) {
			lock();
			try {
				var set = new SetServerLoad(load);
				LongList removed = null;
				for (var it = observers.iterator(); it.moveToNext(); ) {
					long observer = it.value();
					try {
						if (set.Send(serviceManager.server.GetSocket(observer)))
							continue;
					} catch (Throwable ignored) {
					}
					if (removed == null)
						removed = new LongList();
					removed.add(observer);
				}
				if (removed != null)
					removed.foreach(observers::remove);
			} finally {
				unlock();
			}
		}

		// 会话关闭联动剔除该会话登记的观察者（仅靠setLoad转发失败的惰性剔除，
		// 死观察者会滞留到该地址下一次上报）。返回是否已空（地址行随之回收）。
		public boolean removeObserver(long sessionId) {
			lock();
			try {
				observers.remove(sessionId);
				return observers.isEmpty();
			} finally {
				unlock();
			}
		}
	}

	// 需要从配置文件中读取，把这个引用加入：Zeze.Config.AddCustomize
	private final Conf conf = new Conf();
	private NetServer server;
	private final @NotNull AsyncSocket serverSocket;
	private final @NotNull RocksDatabase autoKeysDb;
	private final @NotNull RocksDatabase.Table autoKeyTable;
	private final ConcurrentHashMap<String, AutoKey> autoKeys = new ConcurrentHashMap<>();
	private final Id128UdpServer id128Server;

	public static final class Conf implements Config.ICustomize {
		public int keepAlivePeriod = -1;
		public int retryNotifyDelayWhenNotAllReady = 30 * 1000;
		public @NotNull String dbHome = ".";

		public long threadingReleaseTimeout = 30 * 60 * 1000;

		@Override
		public @NotNull String getName() {
			return "Zeze.Services.ServiceManager";
		}

		@Override
		public void parse(@NotNull Element self) {
			String attr = self.getAttribute("KeepAlivePeriod");
			if (!attr.isEmpty())
				keepAlivePeriod = Integer.parseInt(attr);
			attr = self.getAttribute("RetryNotifyDelayWhenNotAllReady");
			if (!attr.isEmpty())
				retryNotifyDelayWhenNotAllReady = Integer.parseInt(attr);
			dbHome = self.getAttribute("DbHome");
			if (dbHome.isEmpty())
				dbHome = ".";
			attr = self.getAttribute("ThreadingReleaseTimeout");
			if (!attr.isBlank())
				threadingReleaseTimeout = Long.parseLong(attr);
		}
	}

	// 每个服务的状态
	public static final class ServiceState {
		private final @NotNull ServiceManagerServer serviceManager;
		private final @NotNull String serviceName;
		// version -> map<identity, serviceInfo>
		// 记录一下SessionId，方便以后找到服务所在的连接。
		private final HashMap<Long, HashMap<String, BServiceInfo>> serviceInfos = new HashMap<>(); // <version,<serverId,info>>
		private final LongHashMap<BSubscribeInfo> simple = new LongHashMap<>(); // key:sessionId

		public ServiceState(@NotNull ServiceManagerServer sm, @NotNull String serviceName) {
			serviceManager = sm;
			this.serviceName = serviceName;
		}

		public HashMap<Long, HashMap<String, BServiceInfo>> getServiceInfos() {
			return serviceInfos;
		}

		// 通知订阅了info版本的会话（version==0订阅全部版本）。info的版本决定通知过滤。
		private void collectNotify(@NotNull BServiceInfo info, boolean isAdd,
								   @NotNull HashMap<AsyncSocket, EditService> result) {
			for (var it = simple.iterator(); it.moveToNext(); ) {
				var itVersion = it.value().getVersion();
				if (itVersion == 0 || itVersion == info.getVersion()) {
					var peer = serviceManager.server.GetSocket(it.key());
					if (peer != null) {
						var notify = result.computeIfAbsent(peer, __ -> new EditService());
						if (isAdd)
							notify.Argument.getAdd().add(info);
						else
							notify.Argument.getRemove().add(info);
					}
				}
			}
		}

		public void addAndCollectNotify(@NotNull BServiceInfo info, @NotNull HashMap<AsyncSocket, EditService> result) {
			// AddOrUpdate，否则重连重新注册很难恢复到正确的状态。
			// 更新以name+id为key（BEditService.add声明的语义）：同identity重注册到新版本时，
			// 先从其他版本桶移除旧记录并通知其版本订阅者remove，
			// 否则实例下线后旧版本桶残留幽灵地址（会话registers只保留最后一次注册）。
			for (var e : serviceInfos.entrySet()) {
				if (e.getKey() == info.getVersion())
					continue;
				var old = e.getValue().remove(info.getServiceIdentity());
				if (old != null)
					collectNotify(old, false, result);
			}
			serviceInfos.values().removeIf(HashMap::isEmpty);
			serviceInfos.computeIfAbsent(info.getVersion(), __ -> new HashMap<>()).put(info.getServiceIdentity(), info);
			collectNotify(info, true, result);
			// 新注册实例同样要为现有订阅者登记负载观察者：addLoadObserver若只在订阅时
			// 登记，观察者集合是订阅时刻的快照——订阅之后注册的实例，其负载上报永不
			// 转发给订阅者（权重缺失直到重连重订阅）。simple的key即订阅者sessionId。
			// 本方法与simple的修改都在editLock内，迭代安全。
			for (var it = simple.iterator(); it.moveToNext(); )
				serviceManager.addLoadObserver(info.getPassiveIp(), info.getPassivePort(), it.key());
		}

		public void removeAndCollectNotify(@NotNull BServiceInfo info, long sessionId,
										   @NotNull HashMap<AsyncSocket, EditService> result) {
			// 注销同样以name+id为key跨全部版本桶收敛（与addAndCollectNotify、客户端onUnRegister一致）。
			for (var e : serviceInfos.entrySet()) {
				var exist = e.getValue().get(info.getServiceIdentity());
				// 有可能当前连接没有注销，新的注册已经AddOrUpdate，此时忽略当前连接的注销。
				if (exist == null || exist.getSessionId() == null || exist.getSessionId() != sessionId)
					continue;
				e.getValue().remove(info.getServiceIdentity());
				collectNotify(exist, false, result);
			}
			serviceInfos.values().removeIf(HashMap::isEmpty);
		}

		public void subscribeAndCollectResult(@NotNull Subscribe r, @NotNull BSubscribeInfo subInfo, long sessionId) {
			// 外面会话的 TryAdd 加入成功，下面TryAdd肯定也成功。
			simple.put(sessionId, subInfo);
			r.Result.map.put(serviceName, new BServiceInfosVersion(subInfo.getVersion(), this));
			for (var versions : serviceInfos.values()) {
				for (var info : versions.values())
					serviceManager.addLoadObserver(info.getPassiveIp(), info.getPassivePort(), r.getSender());
			}
		}
	}

	// 每个server连接的状态
	public static final class Session {
		private final @NotNull ServiceManagerServer serviceManager;
		private final long sessionId;
		private final ConcurrentHashSet<BServiceInfo> registers = new ConcurrentHashSet<>(); // 以'服务名+ID'作为key的set
		// key is ServiceName: 会话订阅
		private final ConcurrentHashMap<String, BSubscribeInfo> subscribes = new ConcurrentHashMap<>();
		private final @Nullable Future<?> keepAliveTimerTask;
		private volatile int identifyServerId = -1; // Identify上报的serverId；-1=未上报。断线时据此广播Suspect。

		public Session(@NotNull ServiceManagerServer sm, long sid) {
			serviceManager = sm;
			sessionId = sid;
			if (serviceManager.conf.keepAlivePeriod > 0) {
				keepAliveTimerTask = TaskSpec.ofAction(() -> {
					AsyncSocket s = null;
					try {
						s = serviceManager.server.GetSocket(sessionId);
						if (s == null)
							return; // 会话已关闭/已被清理，KeepAlive 无事可做
						var r = new KeepAlive();
						// 异步等待应答：SendAndWaitCheckResultCode在调度池线程上同步阻塞
						// 等待，半开连接（无FIN）堆积时每个KeepAlive各占一个rpc超时时长，数百会话
						// 即可耗尽调度池，拖停同JVM全部周期任务（含死会话检测本身，恶性循环）。
						// 回调判活：超时/失败码在回调中关闭连接触发重连——对端假死检测语义
						// 不变（只发不等同样测不出假死），调度线程不再被占用。
						final var sock = s;
						if (!r.Send(s, response -> {
							if (response.isTimeout() || response.getResultCode() != KeepAlive.Success)
								sock.close(new java.io.IOException("KeepAlive fail: " + response));
							return 0;
						}))
							sock.close(new java.io.IOException("KeepAlive send fail"));
					} catch (Throwable ex) { // resource close. logger.error
						if (s != null)
							s.close(ex);
						else
							logger.error("ServiceManager.KeepAlive", ex);
					}
				}).schedulePeriodNow(
					Random.getInstance().nextInt(serviceManager.conf.keepAlivePeriod),
					serviceManager.conf.keepAlivePeriod);
			} else
				keepAliveTimerTask = null;
		}

		// 底层确保只会回调一次
		public void onClose() {
			if (keepAliveTimerTask != null)
				keepAliveTimerTask.cancel(false);

			// Suspect广播：立即、不延迟、不挑选、不取SM锁（避免锁序问题）。仅是提示，
			// 由租约表裁决：未过期租约会被接收方安排到过期时刻精确重试。
			var suspectServerId = identifyServerId;
			if (suspectServerId >= 0) {
				try {
					serviceManager.server.foreach(so -> {
						if (so.getSessionId() == sessionId)
							return; // 刚断线的会话本身不报信（发给它会得到submitAction错误日志）
						var suspect = new Suspect();
						suspect.Argument.serverId = suspectServerId;
						so.Send(suspect);
					});
				} catch (Exception e) {
					logger.warn("Suspect broadcast for serverId={} failed", suspectServerId, e);
				}
			}

			var notifies = new HashMap<AsyncSocket, EditService>();
			serviceManager.editLock.lock();

			try {
				// 联动清理该会话登记的全部负载观察者（地址行随之回收）。
				serviceManager.removeLoadObservers(sessionId);

				for (var info : subscribes.values())
					serviceManager.unSubscribeNow(sessionId, info.getServiceName());

				for (var unReg : registers) {
					var state = serviceManager.serviceStates.get(unReg.getServiceName());
					if (state != null)
						state.removeAndCollectNotify(unReg, sessionId, notifies);
				}
				ServiceManagerServer.sendNotifies(notifies);
			} finally {
				serviceManager.editLock.unlock();
			}
		}

	}

	private void addLoadObserver(@NotNull String ip, int port, @NotNull AsyncSocket sender) {
		addLoadObserver(ip, port, sender.getSessionId());
	}

	// 观察者按sessionId登记（sessionId在连接存活期内稳定；重连产生新id，死观察者由
	// LoadObservers.setLoad转发失败时惰性剔除）。
	private void addLoadObserver(@NotNull String ip, int port, long sessionId) {
		if (!ip.isEmpty() && port != 0)
			loads.computeIfAbsent(ip + "_" + port, __ -> new LoadObservers(this)).addObserver(sessionId);
	}

	// 会话关闭联动清理该会话登记的全部负载观察者；地址行在观察者清空时移除——
	// 仅靠转发失败惰性剔除，服务下线后该地址再无上报则观察者集合与地址行永久残留。
	private void removeLoadObservers(long sessionId) {
		// 死地址回收
		loads.entrySet().removeIf(entry -> entry.getValue().removeObserver(sessionId));
	}

	private final ReentrantLock editLock = new ReentrantLock(); // 整个edit使用一把锁。不并发了。

	// Critical协议经oneByOne池执行，可能晚于OnSocketClose的会话清理到达（注册报文
	// 与RST几乎同时到达是常态）。判活必须在editLock内调用：NetServer.OnSocketClose先从
	// socketMap摘除再清理（清理持editLock），故锁内GetSocket==sender⟹摘除未发生⟹清理未开始，
	// 本次处理的写入会被随后的清理收走；GetSocket!=sender⟹会话已死，拒绝即不产生死会话残留。
	// sessionId由全局AtomicLong发号不复用，不存在同号新连接误判。
	private boolean isSenderAlive(@NotNull AsyncSocket sender) {
		return server.GetSocket(sender.getSessionId()) == sender;
	}

	private static void sendNotifies(HashMap<AsyncSocket, EditService> notifies) {
		// todo 增加一些发送错误的日志。
		for (var e : notifies.entrySet()) {
			e.getValue().Send(e.getKey());
		}
	}

	// 服务端注册入口校验identity（对齐客户端AbstractAgent.verify与BServiceInfos.comparer的
	// 排序前提）：非'@'/'#'前缀必须是可Long.parseLong的数字，否则订阅者侧
	// insert的binarySearch在Long.parseLong上抛NumberFormatException，打断同批全部合法
	// 变更的处理。畸形请求整批拒绝（错误码经派发层onError应答，客户端SendAndWait感知失败）。
	private static boolean isLegalServiceIdentity(@NotNull String identity) {
		if (identity.startsWith("@") || identity.startsWith("#"))
			return true;
		try {
			Long.parseLong(identity);
			return true;
		} catch (NumberFormatException e) {
			logger.warn("illegal service identity: '{}'", identity);
			return false;
		}
	}

	// 阈值：serviceName/identity 128B、
	// passiveIp 64B、
	// extraInfo 256B（合法形态最坏值余量充分）；
	// 每请求批 128；
	// 每会话 registers/subscribes 各 64（合法基数实证 1~3，防一条消息占满全局名额锁死合法订阅）；
	// 全局唯一 serviceName 1024（复用Id128UdpServer.MAX_UNIQUE_NAMES）满员时逐出空壳行自愈（内容可由重发恢复）。
	private static final int SVC_NAME_MAX_BYTES = 128;
	private static final int SVC_IDENTITY_MAX_BYTES = 128;
	private static final int SVC_IP_MAX_BYTES = 64;
	private static final int SVC_EXTRA_MAX_BYTES = 256;
	private static final int SVC_EDIT_BATCH_MAX = 128;
	private static final int SVC_PER_SESSION_MAX = 64;

	private static boolean isOverUtf8Bytes(@Nullable String s, int maxBytes) {
		return s != null && s.getBytes(StandardCharsets.UTF_8).length > maxBytes;
	}

	// 拒绝告警限频（同Id128UdpServer.warnRejected，60秒一条防日志刷屏DoS）。
	private volatile long lastSvcRejectLogMs;

	private void warnSvcRejected(@NotNull String reason) {
		var now = System.currentTimeMillis();
		var last = lastSvcRejectLogMs;
		if (now - last < 60_000)
			return; // 限频窗口内静默拒绝（race下至多多记几条）
		lastSvcRejectLogMs = now;
		logger.error("SM edit/subscribe rejected (possible attack or misbehaving client), serviceStates={}: {}",
			serviceStates.size(), reason);
	}

	// 满员时逐出一个空壳行（无注册无订阅）腾位。editLock内调用（Edit/Subscribe/清理
	// 全序串行，无并发），excludeName为本请求正要使用的名字。非raft行是内存态、内容
	// 可由客户端重发恢复，逐出无损。
	private boolean evictIdleServiceState(@NotNull String excludeName) {
		for (var it = serviceStates.entrySet().iterator(); it.hasNext(); ) {
			var e = it.next();
			if (!e.getKey().equals(excludeName)
				&& e.getValue().getServiceInfos().isEmpty() && e.getValue().simple.isEmpty()) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	// 只规划、不修改：整批验证成功后，先注销，再应用逐出计划，最后注册/订阅并通知。
	private ArrayList<String> planServiceNames(Set<String> requested, Set<BServiceInfo> removes, long sessionId) {
		int needed = 0;
		for (var name : requested) {
			if (!serviceStates.containsKey(name))
				++needed;
		}
		int deficit = serviceStates.size() + needed - Id128UdpServer.MAX_UNIQUE_NAMES;
		var evictions = new ArrayList<String>();
		if (deficit <= 0)
			return evictions;
		for (var entry : serviceStates.entrySet()) {
			var state = entry.getValue();
			if (requested.contains(entry.getKey()) || !state.simple.isEmpty())
				continue;
			boolean idle = true;
			for (var bucket : state.serviceInfos.values()) {
				for (var info : bucket.values()) {
					if (!Long.valueOf(sessionId).equals(info.getSessionId()) || !removes.contains(info)) {
						idle = false;
						break;
					}
				}
				if (!idle)
					break;
			}
			if (idle) {
				evictions.add(entry.getKey());
				if (evictions.size() == deficit)
					return evictions;
			}
		}
		return null;
	}

	private long processEditService(@NotNull EditService r) {
		var add = r.Argument.getAdd();
		var remove = r.Argument.getRemove();
		if (add.size() + remove.size() > SVC_EDIT_BATCH_MAX) {
			warnSvcRejected("edit batch exceeded " + SVC_EDIT_BATCH_MAX + ": " + (add.size() + remove.size()));
			return Procedure.ErrorRequestId;
		}
		for (var info : add) {
			if (!isLegalServiceIdentity(info.getServiceIdentity()))
				return Procedure.ErrorRequestId;
				// 字段长度上限（identity非'@'/'#'通道已被isLegalServiceIdentity数字化封顶）。
			if (isOverUtf8Bytes(info.getServiceName(), SVC_NAME_MAX_BYTES) ||
				isOverUtf8Bytes(info.getServiceIdentity(), SVC_IDENTITY_MAX_BYTES) ||
				isOverUtf8Bytes(info.getPassiveIp(), SVC_IP_MAX_BYTES) ||
				info.getExtraInfo().size() > SVC_EXTRA_MAX_BYTES) {
				warnSvcRejected("edit field over size: " + info.getServiceName());
				return Procedure.ErrorRequestId;
			}
		}
		for (var info : r.Argument.getRemove())
			if (!isLegalServiceIdentity(info.getServiceIdentity()))
				return Procedure.ErrorRequestId;
		var session = (Session)r.getSender().getUserState();
		var notifies = new HashMap<AsyncSocket, EditService>();
		// 原子的完成所有编辑的修改和通知。
		editLock.lock();
		try {
			if (!isSenderAlive(r.getSender())) { // 迟到协议，会话已清理——拒绝防死注册复活
				r.SendResultCode(ServiceManagerWithRaft.ErrorNotLogin);
				return Procedure.Success;
			}
			var finalRegisters = new HashSet<BServiceInfo>();
			for (var info : session.registers)
				finalRegisters.add(info);
			finalRegisters.removeAll(remove);
			finalRegisters.addAll(add);
			var requestedNames = new HashSet<String>();
			for (var info : add)
				requestedNames.add(info.getServiceName());
			var evictions = planServiceNames(requestedNames, new HashSet<>(remove), session.sessionId);
			if (finalRegisters.size() > SVC_PER_SESSION_MAX || evictions == null) {
				warnSvcRejected("edit final capacity exceeded");
				r.SendResultCode(Procedure.ErrorRequestId);
				return Procedure.Success;
			}
			// step 1: remove
			for (var unReg : r.Argument.getRemove()) {
				var info = session.registers.remove(unReg);
				if (info != null) {
					logger.info("{}: UnRegister {} version={} serverId={} ip={} port={}",
						r.getSender(), info.getServiceName(), info.getVersion(), info.getServiceIdentity(),
						info.getPassiveIp(), info.getPassivePort());
					var state = serviceStates.get(info.getServiceName());
					if (state != null)
						state.removeAndCollectNotify(info, r.getSender().getSessionId(), notifies);
				} else {
					logger.info("{}: Ignore UnRegister {} serverId={}",
						r.getSender(), unReg.getServiceName(), unReg.getServiceIdentity());
				}
			}

			for (var name : evictions)
				serviceStates.remove(name);

			// step 2: add
			// 允许重复登录，断线重连Agent不好原子实现重发。
			for (var reg : r.Argument.getAdd()) {
				if (session.registers.remove(reg) == null) { // 先删除再加入,确保key也更新成新的
					logger.info("{}: Register {} version={} serverId={} ip={} port={}",
						r.getSender(), reg.getServiceName(), reg.getVersion(), reg.getServiceIdentity(),
						reg.getPassiveIp(), reg.getPassivePort());
				} else {
					logger.info("{}: Overwrite Registered {} version={} serverId={} ip={} port={}",
						r.getSender(), reg.getServiceName(), reg.getVersion(), reg.getServiceIdentity(),
						reg.getPassiveIp(), reg.getPassivePort());
				}
				session.registers.add(reg);
				var state = serviceStates.computeIfAbsent(reg.getServiceName(), name -> new ServiceState(this, name));

				// 【警告】
				// 为了简单，这里没有创建新的对象，直接修改并引用了r.Argument。
				// 这个破坏了r.Argument只读的属性。另外引用同一个对象，也有点风险。
				// 在目前没有问题，因为r.Argument主要记录在state.ServiceInfos中，
				// 另外它也被Session引用（用于连接关闭时，自动注销）。
				// 这是专用程序，不是一个库，以后有修改时，小心就是了。
				reg.setSessionId(r.getSender().getSessionId());
				state.addAndCollectNotify(reg, notifies);
			}

			sendNotifies(notifies);
			r.SendResult();
		} finally {
			editLock.unlock();
		}
		return Procedure.Success;
	}

	private long processSubscribe(@NotNull Subscribe r) {
		logger.info("{}: Subscribe {}", r.getSender(), r.Argument);
		var session = (Session)r.getSender().getUserState();

		if (r.Argument.subs.size() > SVC_EDIT_BATCH_MAX) {
			warnSvcRejected("subscribe batch exceeded " + SVC_EDIT_BATCH_MAX + ": " + r.Argument.subs.size());
			return Procedure.ErrorRequestId;
		}
		for (var sub : r.Argument.subs)
			if (isOverUtf8Bytes(sub.getServiceName(), SVC_NAME_MAX_BYTES)) {
				warnSvcRejected("subscribe name over size");
				return Procedure.ErrorRequestId;
			}
		editLock.lock();
		try {
			if (!isSenderAlive(r.getSender())) { // 迟到协议，会话已清理——拒绝防死订阅残留
				r.SendResultCode(ServiceManagerWithRaft.ErrorNotLogin);
				return Procedure.Success;
			}
			var finalSubscriptions = new HashSet<>(session.subscribes.keySet());
			var requestedNames = new HashSet<String>();
			for (var sub : r.Argument.subs) {
				finalSubscriptions.add(sub.getServiceName());
				requestedNames.add(sub.getServiceName());
			}
			var evictions = planServiceNames(requestedNames, Set.of(), session.sessionId);
			if (finalSubscriptions.size() > SVC_PER_SESSION_MAX || evictions == null) {
				warnSvcRejected("subscribe final capacity exceeded");
				r.SendResultCode(Procedure.ErrorRequestId);
				return Procedure.Success;
			}
			for (var name : evictions)
				serviceStates.remove(name);
			for (var sub : r.Argument.subs) {
				session.subscribes.put(sub.getServiceName(), sub);
				serviceStates.computeIfAbsent(sub.getServiceName(), name -> new ServiceState(this, name))
					.subscribeAndCollectResult(r, sub, session.sessionId);
			}
			r.SendResult();
		} finally {
			editLock.unlock();
		}
		return Procedure.Success;
	}

	private void unSubscribeNow(long sessionId, @NotNull String serviceName) {
		var state = serviceStates.get(serviceName);
		if (state != null)
			state.simple.remove(sessionId);
	}

	private long processUnSubscribe(@NotNull UnSubscribe r) {
		logger.info("{}: UnSubscribe {}", r.getSender(), r.Argument);
		var session = (Session)r.getSender().getUserState();

		editLock.lock();
		try {
			if (!isSenderAlive(r.getSender())) { // 迟到协议，会话已清理——拒绝防死退订写脏状态
				r.SendResultCode(ServiceManagerWithRaft.ErrorNotLogin);
				return Procedure.Success;
			}
			for (var serviceName : r.Argument.serviceNames) {
				session.subscribes.remove(serviceName); // continue if not exist
				unSubscribeNow(session.sessionId, serviceName);
			}
			r.SendResult();
		} finally {
			editLock.unlock();
		}
		return Procedure.Success;
	}

	private long processSetLoad(@NotNull SetServerLoad setServerLoad) {
		// loads键（name）客户端可控——长度校验（空行自愈已有：removeLoadObservers
		// 在任意会话关闭时按isEmpty扫除）。
		if (isOverUtf8Bytes(setServerLoad.Argument.getName(), SVC_NAME_MAX_BYTES)
			|| isOverUtf8Bytes(setServerLoad.Argument.ip, SVC_IP_MAX_BYTES)) {
			warnSvcRejected("setLoad field over size");
			return 0; // 非Rpc：拒绝即静默丢弃
		}
		editLock.lock();
		try {
			// 迟到的SetLoad会为死会话重建零观察者地址行（绕过会话联动清理）；
			// 判活与removeLoadObservers（onClose，editLock内）串行。SetServerLoad非Rpc，
			// 死连接本就收不到应答，拒绝即静默丢弃。
			if (!isSenderAlive(setServerLoad.getSender()))
				return 0;
			loads.computeIfAbsent(setServerLoad.Argument.getName(), __ -> new LoadObservers(this))
				.setLoad(setServerLoad.Argument);
			return 0;
		} finally {
			editLock.unlock();
		}
	}

	// 只写session上一个int（Direct派发内完成、无锁、无取消语义）。
	@SuppressWarnings("MethodMayBeStatic")
	private long processIdentify(@NotNull Identify r) {
		var session = (Session)r.getSender().getUserState();
		if (session != null) {
			session.identifyServerId = r.Argument.serverId;
			logger.info("{}: Identify serverId={}", r.getSender(), r.Argument.serverId);
		}
		return 0;
	}

	@Override
	public void close() {
		try {
			stop();
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	private final @NotNull ThreadingServer threading;

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port,
								@NotNull Config config) throws Exception {
		this(ipaddress, port, config, "autokeys");
	}

	public ServiceManagerServer(@Nullable InetAddress ipaddress, int port,
								@NotNull Config config,
								@NotNull String autokeys) throws Exception {
		ZezeCounter.tryInit();
		applyLogLevelProperty(); // 显式启动动作（仅显式指定logLevel属性才动配置）
		config.parseCustomize(this.conf);

		// 全部资源先在局部变量上构造，成功后才发布到字段：构造失败（如UDP端口被占）时
		// 调用方拿不到实例、无法调stop——遗留的TCP监听与RocksDB目录锁会让同进程重试永久
		// 受阻（部分成功状态与实际服务状态分叉）。失败按逆序清理，清理异常作suppressed保留。
		var localServer = new NetServer(this, config);
		var localThreading = new ThreadingServer(localServer, conf);
		RocksDatabase localDb = null;
		AsyncSocket localSocket = null;
		Id128UdpServer localId128 = null;
		try {
			localServer.AddFactoryHandle(EditService.TypeId_, new Service.ProtocolFactoryHandle<>(
				EditService::new, this::processEditService, TransactionLevel.None, DispatchMode.Critical));
			localServer.AddFactoryHandle(Subscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				Subscribe::new, this::processSubscribe, TransactionLevel.None, DispatchMode.Critical));
			localServer.AddFactoryHandle(UnSubscribe.TypeId_, new Service.ProtocolFactoryHandle<>(
				UnSubscribe::new, this::processUnSubscribe, TransactionLevel.None, DispatchMode.Critical));
			localServer.AddFactoryHandle(KeepAlive.TypeId_, new Service.ProtocolFactoryHandle<>(
				KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
			localServer.AddFactoryHandle(AllocateId.TypeId_, new Service.ProtocolFactoryHandle<>(
				AllocateId::new, this::processAllocateId, TransactionLevel.None, DispatchMode.Direct));
			localServer.AddFactoryHandle(SetServerLoad.TypeId_, new Service.ProtocolFactoryHandle<>(
				SetServerLoad::new, this::processSetLoad, TransactionLevel.None, DispatchMode.Critical));
			localServer.AddFactoryHandle(Identify.TypeId_, new Service.ProtocolFactoryHandle<>(
				Identify::new, this::processIdentify, TransactionLevel.None, DispatchMode.Direct));

			localThreading.RegisterProtocols(localServer);

			localDb = new RocksDatabase(Path.of(this.conf.dbHome, autokeys).toString());
			var localAutoKeyTable = localDb.getOrAddTable("autokey");
			var id128Table = localDb.getOrAddTable("id128");

			// 允许配置多个acceptor，如果有冲突，通过日志查看。
			localSocket = localServer.newServerSocket(ipaddress, port,
				new Acceptor(port, ipaddress != null ? ipaddress.getHostAddress() : null));
			localServer.start();
			localId128 = new Id128UdpServer(id128Table, null, port); // todo 先使用和tcp一样的端口.自动选择下一步.
			localId128.start();

			// 全部成功，发布完成态到字段
			server = localServer;
			threading = localThreading;
			autoKeysDb = localDb;
			autoKeyTable = localAutoKeyTable;
			serverSocket = localSocket;
			id128Server = localId128;
		} catch (Throwable e) {
			if (localId128 != null) {
				try {
					localId128.stop();
				} catch (Throwable ce) {
					e.addSuppressed(ce);
				}
			}
			if (localSocket != null) {
				try {
					localSocket.close();
				} catch (Throwable ce) {
					e.addSuppressed(ce);
				}
			}
			try {
				localServer.stop();
			} catch (Throwable ce) {
				e.addSuppressed(ce);
			}
			if (localDb != null) {
				try {
					localDb.close();
				} catch (Throwable ce) {
					e.addSuppressed(ce);
				}
			}
			try {
				localThreading.close();
			} catch (Throwable ce) {
				e.addSuppressed(ce);
			}
			throw e;
		}
	}

	private static final class AutoKey extends FastLock {
		private final @NotNull ServiceManagerServer sms;
		private final byte @NotNull [] key;
		private final AtomicLong current = new AtomicLong();
		private volatile long max; // 当前可分配的上限(不含)

		public AutoKey(@NotNull String name, @NotNull ServiceManagerServer sms) {
			this.sms = sms;
			var nameBytes = name.getBytes(StandardCharsets.UTF_8);
			var bb = ByteBuffer.Allocate(ByteBuffer.WriteUIntSize(nameBytes.length) + nameBytes.length);
			bb.WriteBytes(nameBytes);
			key = bb.Bytes;
			try {
				var value = sms.autoKeyTable.get(key);
				current.set(max = (value != null ? ByteBuffer.Wrap(value).ReadLong() : 1)); // 默认从1开始
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		// 分配全程持锁（调用前提：已lock且通过"持锁且在册"复核，见processAllocateId）：
		// 无锁CAS快路径与满员逐出（evictIdleAutoKey的tryLock）不相容——CAS推进不持锁，
		// 逐出可越过在途分配移除条目，重建后current从持久max前移，与孤儿AutoKey的越界
		// 慢路径交付重叠重号。持锁后读-推进线性化，CAS循环不再需要。
		public void allocateLocked(@NotNull AllocateId rpc, int count) {
			var c = current.get();
			if (c + count > max) {
				var m = max + count * 10L;
				var bb = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(m));
				bb.WriteLong(m);
				try {
					sms.autoKeyTable.put(key, bb.Bytes);
				} catch (RocksDBException e) {
					throw Task.forceThrow(e);
				}
				max = m; // 确保数据库记下了再更新max
			}
			current.set(c + count);
			rpc.Result.setStartId(c);
			rpc.Result.setCount(count);
		}
	}

	private long processAllocateId(@NotNull AllocateId r) {
		var name = r.Argument.getName();
		var count = r.Argument.getCount();
		// 入口校验（同端口UDP面Id128UdpServer同口径）：该TCP端口
		// 同样无认证（四种EncryptType均密钥协商，默认Disable明文帧直达），name/count
		// 均对端可控。count<=0或超上限会巨幅烧号洞（慢路径count*10抬水位）；超长name
		// 无界驻留CHM与RocksDB。非法返回错误码（诚实客户端按失败重试）。
		if (count < 1 || count > Tid128Cache.ALLOCATE_COUNT_MAX) {
			warnAllocateIdRejected("invalid count=" + count);
			r.SendResultCode(Procedure.ErrorRequestId);
			return Procedure.Success;
		}
		if (name.getBytes(StandardCharsets.UTF_8).length > 128) {
			warnAllocateIdRejected("name too long: chars=" + name.length());
			r.SendResultCode(Procedure.ErrorRequestId);
			return Procedure.Success;
		}
		// 唯一名满员：逐出一个闲置条目自愈（RocksDB高水位行保留，重建安全，见
		// evictIdleAutoKey）；全部条目持锁（病态并发）时拒绝。竞态窗口内可能略超
		// 上限（多线程同时computeIfAbsent），有界即可。
		if (!autoKeys.containsKey(name) && autoKeys.size() >= Id128UdpServer.MAX_UNIQUE_NAMES
			&& !evictIdleAutoKey()) {
			warnAllocateIdRejected("unique names exceeded " + Id128UdpServer.MAX_UNIQUE_NAMES);
			r.SendResultCode(Procedure.ErrorRequestId);
			return Procedure.Success;
		}
		for (; ; ) {
			var autoKey = autoKeys.computeIfAbsent(name, key -> new AutoKey(key, this));
			autoKey.lock();
			if (autoKeys.get(name) == autoKey) { // 持锁且在册：临界区内逐出的tryLock拿不到本锁
				try {
					autoKey.allocateLocked(r, count);
				} finally {
					autoKey.unlock();
				}
				break;
			}
			autoKey.unlock(); // 孤儿（逐出-重建竞态窗口内拿到的旧条目）：重取重试
		}
		r.SendResult();
		return 0;
	}

	// 满员自愈。逐出未持锁的闲置条目腾出槽位；RocksDB高水位行保留，重建
	// （computeIfAbsent）时current向前重置到持久max——在途已分配区间烧成号洞而不
	// 重发，与进程重启加载同一损失类别（同Id128UdpServer.evictIdleContext的论证）。
	// 在途分配持锁不可逐出；全部持锁（病态并发）时失败。
	private boolean evictIdleAutoKey() {
		for (var e : autoKeys.entrySet()) {
			var autoKey = e.getValue();
			if (autoKey.tryLock()) {
				try {
					if (autoKeys.remove(e.getKey(), autoKey))
						return true;
				} finally {
					autoKey.unlock();
				}
			}
		}
		return false;
	}

	// 拒绝告警限频（同Id128UdpServer.warnRejected）：无认证端口上高频非法
	// 请求按包记日志可耗尽日志盘/CPU（日志刷屏DoS），60秒一条。
	private volatile long lastAllocateIdRejectLogMs;

	private void warnAllocateIdRejected(@NotNull String reason) {
		var now = System.currentTimeMillis();
		var last = lastAllocateIdRejectLogMs;
		if (now - last < 60_000)
			return; // 限频窗口内静默拒绝（race下至多多记几条）
		lastAllocateIdRejectLogMs = now;
		logger.error("AllocateId rejected (possible attack or misbehaving client), cached names={}: {}",
			autoKeys.size(), reason);
	}

	public void stop() throws Exception {
		lock();
		try {
			id128Server.stop();
			if (server == null)
				return;
			serverSocket.close();
			server.stop();
			server = null;
			logger.info("closeDb: {}, autokeys", this.conf.dbHome);
			autoKeysDb.close();
			threading.close();
		} finally {
			unlock();
		}
	}

	public static final class NetServer extends HandshakeServer {
		private final @NotNull ServiceManagerServer serviceManager;
		private final TaskOneByOneByKey oneByOneByKey = new TaskOneByOneByKey();

		public NetServer(@NotNull ServiceManagerServer sm, Config config) {
			super("Zeze.Services.ServiceManager", config);
			serviceManager = sm;
		}

		@Override
		public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
			logger.info("OnSocketAccept: {} sessionId={}", so, so.getSessionId());
			so.setUserState(new Session(serviceManager, so.getSessionId()));
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			logger.info("OnSocketClose: {} sessionId={}", so, so.getSessionId());
			// 先经基类从socketMap摘除，再做会话清理——摘除成为关闭的第一可见步骤，
			// process*的锁内判活（isSenderAlive）才有全序：判活通过⟹清理尚未开始（本次处理
			// 的写入随后会被清理收走）；判活失败⟹拒绝，死会话状态不会复活。
			super.OnSocketClose(so, e);
			var session = (Session)so.getUserState();
			if (session != null)
				session.onClose();
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so) {
			var p = decodeProtocol(typeId, bb, factoryHandle, so);
			if (factoryHandle.Mode == DispatchMode.Direct) {
				// 有几个direct方式的协议,为了性能就不考虑和其它非direct协议的处理顺序了,但因为在IO线程串行处理,这些协议本身的处理还是有顺序的
				TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode).call();
			} else {
				TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p, Protocol::trySendResultCode)
					.dispatchMode(factoryHandle.Mode)
					.executeOneByOne(p.getSender(), oneByOneByKey);
			}
			// 不支持事务，由于这里直接OneByOne执行，所以下面两个方法就不重载了。
		}
	}

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String ip = null;
		int port = 5001;

		String raftName = null;
		String raftConf = "servicemanager.raft.xml";
		String autokeys = "autokeys";

		Task.tryInitThreadPool();

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				ip = requireValue(args, ++i, "-ip");
				break;
			case "-port":
				port = requireInt(args, ++i, "-port");
				break;
			case "-raft":
				raftName = requireValue(args, ++i, "-raft");
				break;
			case "-raftConf":
				raftConf = requireValue(args, ++i, "-raftConf");
				break;
			case "-threads":
				i++;
				break;
			case "-autokeys":
				autokeys = requireValue(args, ++i, "-autokeys");
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}
		if (raftName == null || raftName.isEmpty()) {
			logger.info("Start {}:{}", ip != null ? ip : "any", port);
			InetAddress address = (ip != null && !ip.isBlank()) ? InetAddress.getByName(ip) : null;
			var config = Config.load();
			try (var ignored = new ServiceManagerServer(address, port, config, autokeys)) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		} else if (raftName.equals("RunAllNodes")) {
			logger.info("Start Raft=RunAllNodes");
			//noinspection unused
			try (var raft1 = new ServiceManagerWithRaft("127.0.0.1:6556", RaftConfig.load(raftConf));
				 var raft2 = new ServiceManagerWithRaft("127.0.0.1:6557", RaftConfig.load(raftConf));
				 var raft3 = new ServiceManagerWithRaft("127.0.0.1:6558", RaftConfig.load(raftConf))) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		} else {
			logger.info("Start Raft={},{}", raftName, raftConf);
			//noinspection unused
			try (var raft = new ServiceManagerWithRaft(raftName, RaftConfig.load(raftConf))) {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait();
				}
			}
		}
	}
}
