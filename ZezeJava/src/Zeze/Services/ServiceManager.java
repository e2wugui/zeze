package Zeze.Services;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceManager implements Closeable {
	public final static class Conf implements Zeze.Config.ICustomize {
		public String getName() {
			return "Zeze.Services.ServiceManager";
		}

		private int KeepAlivePeriod = 300 * 1000;
		public int getKeepAlivePeriod() {
			return KeepAlivePeriod;
		}
		public void setKeepAlivePeriod(int value) {
			KeepAlivePeriod = value;
		}

		/** 
		 启动以后接收注册和订阅，一段时间内不进行通知。
		 用来处理ServiceManager异常重启导致服务列表重置的问题。
		 在Delay时间内，希望所有的服务都重新连接上来并注册和订阅。
		 Delay到达时，全部通知一遍，以后正常工作。
		*/
		private int StartNotifyDelay = 12 * 1000;
		public int getStartNotifyDelay() {
			return StartNotifyDelay;
		}
		public void setStartNotifyDelay(int value) {
			StartNotifyDelay = value;
		}

		private int RetryNotifyDelayWhenNotAllReady = 30 * 1000;
		public int getRetryNotifyDelayWhenNotAllReady() {
			return RetryNotifyDelayWhenNotAllReady;
		}
		public void setRetryNotifyDelayWhenNotAllReady(int value) {
			RetryNotifyDelayWhenNotAllReady = value;
		}
		private String DbHome = ".";
		public String getDbHome() {
			return DbHome;
		}
		private void setDbHome(String value) {
			DbHome = value;
		}

		public void Parse(XmlElement self) {
			String attr = self.GetAttribute("KeepAlivePeriod");
			if (!tangible.StringHelper.isNullOrEmpty(attr)) {
				setKeepAlivePeriod(Integer.parseInt(attr));
			}
			attr = self.GetAttribute("StartNotifyDelay");
			if (!tangible.StringHelper.isNullOrEmpty(attr)) {
				setStartNotifyDelay(Integer.parseInt(attr));
			}
			attr = self.GetAttribute("RetryNotifyDelayWhenNotAllReady");
			if (!tangible.StringHelper.isNullOrEmpty(attr)) {
				setRetryNotifyDelayWhenNotAllReady(Integer.parseInt(attr));
			}
			setDbHome(self.GetAttribute("DbHome"));
			if (tangible.StringHelper.isNullOrEmpty(getDbHome())) {
				setDbHome(".");
			}
		}
	}

	public final static class ServiceInfo extends Zeze.Transaction.Bean {
		/** 
		 服务名，比如"GameServer"
		*/
		private String ServiceName;
		public String getServiceName() {
			return ServiceName;
		}
		private void setServiceName(String value) {
			ServiceName = value;
		}

		/** 
		 服务id，对于 Zeze.Application，一般就是 Config.AutoKeyLocalId.
		 这里使用类型 string 是为了更好的支持扩展。
		*/
		private String ServiceIdentity;
		public String getServiceIdentity() {
			return ServiceIdentity;
		}
		private void setServiceIdentity(String value) {
			ServiceIdentity = value;
		}

		/** 
		 服务ip-port，如果没有，保持空和0.
		*/
		private String PassiveIp = "";
		public String getPassiveIp() {
			return PassiveIp;
		}
		private void setPassiveIp(String value) {
			PassiveIp = value;
		}
		private int PassivePort = 0;
		public int getPassivePort() {
			return PassivePort;
		}
		private void setPassivePort(int value) {
			PassivePort = value;
		}

		// 服务扩展信息，可选。
		private Binary ExtraInfo = Binary.Empty;
		public Binary getExtraInfo() {
			return ExtraInfo;
		}
		private void setExtraInfo(Binary value) {
			ExtraInfo = value;
		}

		// ServiceManager或者ServiceManager.Agent用来保存本地状态，不是协议一部分，不会被系列化。
		// 算是一个简单的策略，不怎么优美。一般仅设置一次，线程保护由使用者自己管理。
		private Object LocalState;
		public Object getLocalState() {
			return LocalState;
		}
		public void setLocalState(Object value) {
			LocalState = value;
		}

		public ServiceInfo() {
		}


		public ServiceInfo(String name, String identity, String ip, int port) {
			this(name, identity, ip, port, null);
		}

		public ServiceInfo(String name, String identity, String ip) {
			this(name, identity, ip, 0, null);
		}

		public ServiceInfo(String name, String identity) {
			this(name, identity, null, 0, null);
		}

		public ServiceInfo(String name, String identity, String ip, int port, Binary extrainfo) {
			setServiceName(name);
			setServiceIdentity(identity);
			if (!ip.equals(null)) {
				setPassiveIp(ip);
			}
			setPassivePort(port);
			if (extrainfo != null) {
				setExtraInfo(extrainfo);
			}
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setServiceName(bb.ReadString());
			setServiceIdentity(bb.ReadString());
			setPassiveIp(bb.ReadString());
			setPassivePort(bb.ReadInt());
			setExtraInfo(bb.ReadBinary());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getServiceName());
			bb.WriteString(getServiceIdentity());
			bb.WriteString(getPassiveIp());
			bb.WriteInt(getPassivePort());
			bb.WriteBinary(getExtraInfo());
		}

		@Override
		protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 17;
			result = prime * result + getServiceName().hashCode();
			result = prime * result + getServiceIdentity().hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			boolean tempVar = obj instanceof ServiceInfo;
			ServiceInfo other = tempVar ? (ServiceInfo)obj : null;
			if (tempVar) {
				return getServiceName().equals(other.getServiceName()) && getServiceIdentity().equals(other.getServiceIdentity());
			}
			return false;
		}
	}

	/** 
	 动态服务启动时通过这个rpc注册自己。
	*/
	public final static class Register extends Rpc<ServiceInfo, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(Register.class.FullName);

		public static final int Success = 0;
		public static final int DuplicateRegister = 1;

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}

	}

	/** 
	 动态服务关闭时，注销自己，当与本服务器的连接关闭时，默认也会注销。
	 最好主动注销，方便以后错误处理。
	*/
	public final static class UnRegister extends Rpc<ServiceInfo, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(UnRegister.class.FullName);

		public static final int Success = 0;
		public static final int NotExist = 1;

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class SubscribeInfo extends Bean {
		public static final int SubscribeTypeSimple = 0;
		public static final int SubscribeTypeReadyCommit = 1;

		private String ServiceName;
		public String getServiceName() {
			return ServiceName;
		}
		public void setServiceName(String value) {
			ServiceName = value;
		}
		private int SubscribeType;
		public int getSubscribeType() {
			return SubscribeType;
		}
		public void setSubscribeType(int value) {
			SubscribeType = value;
		}
		private Object LocalState;
		public Object getLocalState() {
			return LocalState;
		}
		public void setLocalState(Object value) {
			LocalState = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setServiceName(bb.ReadString());
			setSubscribeType(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getServiceName());
			bb.WriteInt(getSubscribeType());
		}

		@Override
		protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return String.format("%1$s:%2$s", getServiceName(), getSubscribeType());
		}
	}

	public final static class Subscribe extends Rpc<SubscribeInfo, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(Subscribe.class.getName());

		public static final int Success = 0;
		public static final int DuplicateSubscribe = 1;
		public static final int UnknownSubscribeType = 2;

		public Subscribe() {
			Argument = new SubscribeInfo();
			Result = new EmptyBean();
		}

		@Override
		public int getModuleId() {
			return 0;
		}

		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class UnSubscribe extends Rpc<SubscribeInfo, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(UnSubscribe.class.getName());

		public static final int Success = 0;
		public static final int NotExist = 1;

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class ServiceInfos extends Bean {
		// ServiceList maybe empty. need a ServiceName
		private String ServiceName;
		public String getServiceName() {
			return ServiceName;
		}
		private void setServiceName(String value) {
			ServiceName = value;
		}
		// sorted by ServiceIdentity
		private ArrayList<ServiceInfo> _ServiceInfoListSortedByIdentity = new ArrayList<ServiceInfo> ();
		public ArrayList<ServiceInfo> getServiceInfoListSortedByIdentity() {
			return _ServiceInfoListSortedByIdentity;
		}
		private long SerialId;
		public long getSerialId() {
			return SerialId;
		}
		public void setSerialId(long value) {
			SerialId = value;
		}

		public ServiceInfos() {
		}

		public ServiceInfos(String serviceName) {
			setServiceName(serviceName);
		}

		public ServiceInfo get(String identity) {
			var cur = new ServiceInfo(getServiceName(), identity);
			int index = getServiceInfoListSortedByIdentity().BinarySearch(cur, new ServiceInfoIdentityComparer());
			if (index >= 0) {
				info.outArgValue = getServiceInfoListSortedByIdentity().get(index);
				return true;
			}
			info.outArgValue = null;
			return false;
		}
		@Override
		public void Decode(ByteBuffer bb) {
			setServiceName(bb.ReadString());
			getServiceInfoListSortedByIdentity().clear();
			for (int c = bb.ReadInt(); c > 0; --c) {
				var service = new ServiceInfo();
				service.Decode(bb);
				getServiceInfoListSortedByIdentity().add(service);
			}
			setSerialId(bb.ReadLong());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getServiceName());
			bb.WriteInt(getServiceInfoListSortedByIdentity().size());
			for (var service : getServiceInfoListSortedByIdentity()) {
				service.Encode(bb);
			}
			bb.WriteLong(getSerialId());
		}

		@Override
		protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();
			sb.append(getServiceName()).append("=");
			sb.append("[");
			for (var e : getServiceInfoListSortedByIdentity()) {
				sb.append(e.getServiceIdentity());
				sb.append(",");
			}
			sb.append("]");
			return sb.toString();
		}
	}

	public final static class NotifyServiceList extends Protocol1<ServiceInfos> {
		public final static int ProtocolId_ = Bean.Hash16(NotifyServiceList.class.getName());

		public NotifyServiceList() {
			Argument = new ServiceInfos();
		}

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class ReadyServiceList extends Protocol1<ServiceInfos> {
		public final static int ProtocolId_ = Bean.Hash16(ReadyServiceList.class.getName());

		public ReadyServiceList() {
			Argument = new ServiceInfos();
		}

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class CommitServiceList extends Protocol1<ServiceInfos> {
		public final static int ProtocolId_ = Bean.Hash16(CommitServiceList.class.getName());

		public CommitServiceList() {
			Argument = new ServiceInfos();
		}

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class ServiceInfoIdentityComparer implements Comparator<ServiceInfo> {
		public int compare(ServiceInfo x, ServiceInfo y) {
			return x.getServiceIdentity().compareTo(y.getServiceIdentity());
		}
	}

	public final static class Keepalive extends Rpc<EmptyBean, EmptyBean> {
		public final static int ProtocolId_ = Bean.Hash16(Keepalive.class.getName());

		public static final int Success = 0;

		public Keepalive() {
			Argument = new EmptyBean();
			Result = new EmptyBean();
		}
		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class SubscribeFirstCommit extends Protocol1<ServiceInfos> {
		public final static int ProtocolId_ = Bean.Hash16(SubscribeFirstCommit.class.getName());

		public SubscribeFirstCommit() {
			Argument = new ServiceInfos();
		}

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class AllocateIdArgument extends Bean {
		private String Name;
		public String getName() {
			return Name;
		}
		public void setName(String value) {
			Name = value;
		}
		private int Count;
		public int getCount() {
			return Count;
		}
		public void setCount(int value) {
			Count = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setName(bb.ReadString());
			setCount(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getName());
			bb.WriteInt(getCount());
		}

		@Override
		protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}
	}

	public final static class AllocateIdResult extends Bean {
		private String Name;
		public String getName() {
			return Name;
		}
		public void setName(String value) {
			Name = value;
		}
		private long StartId;
		public long getStartId() {
			return StartId;
		}
		public void setStartId(long value) {
			StartId = value;
		}
		private int Count;
		public int getCount() {
			return Count;
		}
		public void setCount(int value) {
			Count = value;
		}

		@Override
		public void Decode(ByteBuffer bb) {
			setName(bb.ReadString());
			setStartId(bb.ReadLong());
			setCount(bb.ReadInt());
		}

		@Override
		public void Encode(ByteBuffer bb) {
			bb.WriteString(getName());
			bb.WriteLong(getStartId());
			bb.WriteInt(getCount());
		}

		@Override
		protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
			throw new UnsupportedOperationException();
		}
	}

	public final static class AllocateId extends Rpc<AllocateIdArgument, AllocateIdResult> {
		public final static int ProtocolId_ = Bean.Hash16(AllocateId.class.getName());

		@Override
		public int getModuleId() {
			return 0;
		}
		@Override
		public int getProtocolId() {
			return ProtocolId_;
		}
	}

	public final static class Agent implements Closeable {
		// key is ServiceName。对于一个Agent，一个服务只能有一个订阅。
		// ServiceName ->
		private ConcurrentHashMap<String, SubscribeState> SubscribeStates = new ConcurrentHashMap<> ();
		public ConcurrentHashMap<String, SubscribeState> getSubscribeStates() {
			return SubscribeStates;
		}
		private NetClient Client;
		public NetClient getClient() {
			return Client;
		}
		private void setClient(NetClient value) {
			Client = value;
		}

		/** 
		 订阅服务状态发生变化时回调。
		 如果需要处理这个事件，请在订阅前设置回调。
		*/
		private Zeze.Util.Action1<SubscribeState> OnChanged;
		public Zeze.Util.Action1<SubscribeState> getOnChanged() {
			return OnChanged;
		}
		public void setOnChanged(Zeze.Util.Action1<SubscribeState> value) {
			OnChanged = value;
		}

		// 应用可以在这个Action内起一个测试事务并执行一次。也可以实现其他检测。
		// ServiceManager 定时发送KeepAlive给Agent，并等待结果。超时则认为服务失效。
		private java.lang.Runnable OnKeepAlive;
		public java.lang.Runnable getOnKeepAlive() {
			return OnKeepAlive;
		}
		public void setOnKeepAlive(java.lang.Runnable value) {
			OnKeepAlive = value;
		}

		private ConcurrentHashMap<ServiceInfo, ServiceInfo> Registers = new ConcurrentHashMap<> ();
		private ConcurrentHashMap<ServiceInfo, ServiceInfo> getRegisters() {
			return Registers;
		}

		// 【警告】
		// 记住当前已经注册和订阅信息，当ServiceManager连接发生重连时，重新发送请求。
		// 维护这些状态数据都是先更新本地再发送远程请求，在失败的时候rollback。
		// 当同一个Key(比如ServiceName)存在并发时，现在处理所有情况，但不保证都是合理的。
		public final static class SubscribeState {
			private Agent Agent;
			public Agent getAgent() {
				return Agent;
			}
			private SubscribeInfo SubscribeInfo;
			public SubscribeInfo getSubscribeInfo() {
				return SubscribeInfo;
			}
			public int getSubscribeType() {
				return getSubscribeInfo().getSubscribeType();
			}
			public String getServiceName() {
				return getSubscribeInfo().getServiceName();
			}

			private ServiceInfos ServiceInfos;
			public ServiceInfos getServiceInfos() {
				return ServiceInfos;
			}
			private void setServiceInfos(ServiceInfos value) {
				ServiceInfos = value;
			}
			private ServiceInfos ServiceInfosPending;
			public ServiceInfos getServiceInfosPending() {
				return ServiceInfosPending;
			}
			private void setServiceInfosPending(ServiceInfos value) {
				ServiceInfosPending = value;
			}

			/** 
			 刚初始化时为false，任何修改ServiceInfos都会设置成true。
			 用来处理Subscribe返回的第一份数据和Commit可能乱序的问题。
			 目前的实现不会发生乱序。
			*/
			private boolean Committed = false;
			public boolean getCommitted() {
				return Committed;
			}
			public void setCommitted(boolean value) {
				Committed = value;
			}

			// 服务准备好。
			private java.util.concurrent.ConcurrentHashMap<String, Object> ServiceIdentityReadyStates = new java.util.concurrent.ConcurrentHashMap<String, Object> ();
			public java.util.concurrent.ConcurrentHashMap<String, Object> getServiceIdentityReadyStates() {
				return ServiceIdentityReadyStates;
			}

			public SubscribeState(Agent ag, SubscribeInfo info) {
				Agent = ag;
				SubscribeInfo = info;
				setServiceInfos(new ServiceInfos(info.getServiceName()));
			}

			// NOT UNDER LOCK
			private boolean TrySendReadyServiceList() {
				if (null == getServiceInfosPending()) {
					return false;
				}

				for (var pending : getServiceInfosPending().getServiceInfoListSortedByIdentity()) {
					if (!getServiceIdentityReadyStates().containsKey(pending.getServiceIdentity())) {
						return false;
					}
				}
				var r = new ReadyServiceList();
				r.Argument = getServiceInfosPending();
				if (getAgent().getClient().Socket != null) {
					getAgent().getClient().Socket.Send(r);
				}
				return true;
			}

			public void SetServiceIdentityReadyState(String identity, Object state) {
				if (null == state) {
					ServiceIdentityReadyStates.remove(identity);
				}
				else {
					getServiceIdentityReadyStates().put(identity, state);
				}

				synchronized (this) {
					// 把 state 复制到当前版本的服务列表中。允许列表不变，服务状态改变。
					if (null != ServiceInfos) {
						ServiceInfo info = ServiceInfos.get(identity);
						if (null != info)
							info.setLocalState(state);
						}
					}
					// 尝试发送Ready，如果有pending.
					TrySendReadyServiceList();
				}
			}

			private void PrepareAndTriggerOnchanged() {
				for (var info : getServiceInfos().getServiceInfoListSortedByIdentity()) {
					TValue state;
					tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					if (getServiceIdentityReadyStates().TryGetValue(info.getServiceIdentity(), tempOut_state)) {
					state = tempOut_state.outArgValue;
						info.setLocalState(state);
					}
				else {
					state = tempOut_state.outArgValue;
				}
				}
				if (getAgent().OnChanged != null) {
					getAgent().OnChanged.Invoke(this);
				}
			}

			public void OnNotify(ServiceInfos infos) {
				synchronized (this) {
					switch (getSubscribeType()) {
						case SubscribeInfo.SubscribeTypeSimple:
							setServiceInfos(infos);
							setCommitted(true);
							PrepareAndTriggerOnchanged();
							break;

						case SubscribeInfo.SubscribeTypeReadyCommit:
							if (null == getServiceInfosPending() || infos.getSerialId() > getServiceInfosPending().getSerialId()) {
								setServiceInfosPending(infos);
								TrySendReadyServiceList();
							}
							break;
					}
				}
			}

			public void OnCommit(ServiceInfos infos) {
				synchronized (this) {
					// ServiceInfosPending 和 Commit.infos 应该一样，否则肯定哪里出错了。
					// 这里总是使用最新的 Commit.infos，检查记录日志。
					if (!Enumerable.SequenceEqual(infos.getServiceInfoListSortedByIdentity(), getServiceInfosPending().getServiceInfoListSortedByIdentity())) {
						Agent.logger.Warn("OnCommit: ServiceInfosPending Miss Match.");
					}
					setServiceInfos(infos);
					setServiceInfosPending(null);
					setCommitted(true);
					PrepareAndTriggerOnchanged();
				}
			}

			public void OnFirstCommit(ServiceInfos infos) {
				synchronized (this) {
					if (getCommitted()) {
						return;
					}
					setCommitted(true);
					setServiceInfos(infos);
					setServiceInfosPending(null);
					PrepareAndTriggerOnchanged();
				}
			}
		}

		private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();


		public ServiceInfo RegisterService(String name, String identity, String ip, int port) {
			return RegisterService(name, identity, ip, port, null);
		}

		public ServiceInfo RegisterService(String name, String identity, String ip) {
			return RegisterService(name, identity, ip, 0, null);
		}

		public ServiceInfo RegisterService(String name, String identity) {
			return RegisterService(name, identity, null, 0, null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public ServiceInfo RegisterService(string name, string identity, string ip = null, int port = 0, Binary extrainfo = null)
		public ServiceInfo RegisterService(String name, String identity, String ip, int port, Binary extrainfo) {
			return RegisterService(new ServiceInfo(name, identity, ip, port, extrainfo));
		}

		public void WaitConnectorReady() {
			// 实际上只有一个连接，这样就不用查找了。
			getClient().getConfig().ForEachConnector((c) -> c.WaitReady());
		}

		private ServiceInfo RegisterService(ServiceInfo info) {
			WaitConnectorReady();

			boolean regNew = false;
			var regServInfo = getRegisters().putIfAbsent(info, (key) -> {
						regNew = true;
						return info;
			});

			if (regNew) {
				try {
					var r = new Register();
					r.setArgument(info);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException e) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					getRegisters().TryRemove(KeyValuePair.Create(info, info)); // rollback
					throw e;
				}
			}
			return regServInfo;
		}

		public void UnRegisterService(String name, String identity) {
			UnRegisterService(new ServiceInfo(name, identity));
		}

		private void UnRegisterService(ServiceInfo info) {
			WaitConnectorReady();

			TValue exist;
			tangible.OutObject<ServiceInfo> tempOut_exist = new tangible.OutObject<ServiceInfo>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getRegisters().TryRemove(info, tempOut_exist)) {
			exist = tempOut_exist.outArgValue;
				try {
					var r = new UnRegister();
					r.setArgument(info);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException e) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					getRegisters().TryAdd(exist, exist); // rollback
					throw e;
				}
			}
		else {
			exist = tempOut_exist.outArgValue;
		}
		}


		public SubscribeState SubscribeService(String serviceName, int type) {
			return SubscribeService(serviceName, type, null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public SubscribeState SubscribeService(string serviceName, int type, object state = null)
		public SubscribeState SubscribeService(String serviceName, int type, Object state) {
			if (type != SubscribeInfo.SubscribeTypeSimple && type != SubscribeInfo.SubscribeTypeReadyCommit) {
				throw new RuntimeException("Unkown SubscribeType");
			}

			SubscribeInfo tempVar = new SubscribeInfo();
			tempVar.setServiceName(serviceName);
			tempVar.setSubscribeType(type);
			tempVar.setLocalState(state);
			return SubscribeService(tempVar);
		}

		private SubscribeState SubscribeService(SubscribeInfo info) {
			WaitConnectorReady();

			boolean newAdd = false;
			var subState = getSubscribeStates().putIfAbsent(info.getServiceName(), (_) -> {
						newAdd = true;
						return new SubscribeState(this, info);
			});

			if (newAdd) {
				var r = new Subscribe();
				r.setArgument(info);
				r.SendAndWaitCheckResultCode(getClient().getSocket());
			}
			return subState;
		}

		private int ProcessSubscribeFirstCommit(Protocol p) {
			var r = p instanceof SubscribeFirstCommit ? (SubscribeFirstCommit)p : null;
			TValue state;
			tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSubscribeStates().TryGetValue(r.getArgument().getServiceName(), tempOut_state)) {
			state = tempOut_state.outArgValue;
				state.OnFirstCommit(r.getArgument());
			}
		else {
			state = tempOut_state.outArgValue;
		}
			return Procedure.Success;
		}

		public void UnSubscribeService(String serviceName) {
			WaitConnectorReady();

			TValue state;
			tangible.OutObject<SubscribeState> tempOut_state = new tangible.OutObject<SubscribeState>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSubscribeStates().TryRemove(serviceName, tempOut_state)) {
			state = tempOut_state.outArgValue;
				try {
					var r = new UnSubscribe();
					r.setArgument(state.SubscribeInfo);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException e) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
					getSubscribeStates().TryAdd(serviceName, state); // rollback
					throw e;
				}
			}
		else {
			state = tempOut_state.outArgValue;
		}
		}

		private int ProcessNotifyServiceList(Protocol p) {
			var r = p instanceof NotifyServiceList ? (NotifyServiceList)p : null;
			TValue state;
			tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSubscribeStates().TryGetValue(r.getArgument().getServiceName(), tempOut_state)) {
			state = tempOut_state.outArgValue;
				state.OnNotify(r.getArgument());
			}
			else {
			state = tempOut_state.outArgValue;
				Agent.logger.Warn("NotifyServiceList But SubscribeState Not Found.");
			}
			return Procedure.Success;
		}

		private int ProcessCommitServiceList(Protocol p) {
			var r = p instanceof CommitServiceList ? (CommitServiceList)p : null;
			TValue state;
			tangible.OutObject<TValue> tempOut_state = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSubscribeStates().TryGetValue(r.getArgument().getServiceName(), tempOut_state)) {
			state = tempOut_state.outArgValue;
				state.OnCommit(r.getArgument());
			}
			else {
			state = tempOut_state.outArgValue;
				Agent.logger.Warn("CommitServiceList But SubscribeState Not Found.");
			}
			return Procedure.Success;
		}

		private int ProcessKeepalive(Protocol p) {
			var r = p instanceof Keepalive ? (Keepalive)p : null;
			if (getOnKeepAlive() != null) {
				getOnKeepAlive().run();
			}
			r.SendResultCode(Keepalive.Success);
			return Procedure.Success;
		}

		public final static class AutoKey {
			private String Name;
			public String getName() {
				return Name;
			}
			private long Current;
			public long getCurrent() {
				return Current;
			}
			private void setCurrent(long value) {
				Current = value;
			}
			private int Count;
			public int getCount() {
				return Count;
			}
			private void setCount(int value) {
				Count = value;
			}
			private Agent Agent;
			public Agent getAgent() {
				return Agent;
			}

			public AutoKey(String name, Agent agent) {
				Name = name;
				Agent = agent;
			}

			public long Next() {
				synchronized (this) {
					if (getCount() <= 0) {
						Allocate();
					}

					if (getCount() <= 0) {
						throw new RuntimeException(String.format("AllocateId failed for %1$s", getName()));
					}

					var tmp = getCurrent();
					setCount(getCount() - 1);
					setCurrent(getCurrent() + 1);
					return tmp;
				}
			}

			private void Allocate() {
				var r = new AllocateId();
				r.getArgument().Name = getName();
				r.getArgument().setCount(1024);
				r.SendAndWaitCheckResultCode(getAgent().getClient().Socket);
				setCurrent(r.getResult().getStartId());
				setCount(r.getResult().getCount());
			}
		}

		private java.util.concurrent.ConcurrentHashMap<String, AutoKey> AutoKeys = new java.util.concurrent.ConcurrentHashMap<String, AutoKey> ();
		private java.util.concurrent.ConcurrentHashMap<String, AutoKey> getAutoKeys() {
			return AutoKeys;
		}

		public AutoKey GetAutoKey(String name) {
			return getAutoKeys().putIfAbsent(name, (k) -> new AutoKey(k, this));
		}

		public void Stop() {
			synchronized (this) {
				if (null == getClient()) {
					return;
				}
				getClient().Stop();
				setClient(null);
			}
		}

		public void OnConnected() {
			for (var e : getRegisters()) {
				try {
					var r = new Register();
					r.setArgument(e.Value);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException ex) {
					logger.Debug(ex, "OnConnected.Register={0}", e.Value);
				}
			}
			for (var e : getSubscribeStates()) {
				try {
					e.Value.Committed = false;
					var r = new Subscribe();
					r.setArgument(e.Value.SubscribeInfo);
					r.SendAndWaitCheckResultCode(getClient().getSocket());
				}
				catch (RuntimeException ex) {
					logger.Debug(ex, "OnConnected.Subscribe={0}", e.Value.SubscribeInfo);
				}
			}
		}

		/** 
		 使用Config配置连接信息，可以配置是否支持重连。
		 用于测试：Agent.Client.NewClientSocket(...)，不会自动重连，不要和Config混用。
		*/

		public Agent(Config config) {
			this(config, null);
		}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public Agent(Config config, string netServiceName = null)
		public Agent(Config config, String netServiceName) {
			if (null == config) {
				throw new RuntimeException("Config is null");
			}

			setClient(tangible.StringHelper.isNullOrEmpty(netServiceName) ? new NetClient(this, config) : new NetClient(this, config, netServiceName));

			getClient().AddFactoryHandle((new Register()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Register()});

			getClient().AddFactoryHandle((new UnRegister()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new UnRegister()});

			getClient().AddFactoryHandle((new Subscribe()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Subscribe()});

			getClient().AddFactoryHandle((new UnSubscribe()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new UnSubscribe()});

			getClient().AddFactoryHandle((new NotifyServiceList()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new NotifyServiceList(), Handle = ProcessNotifyServiceList});

			getClient().AddFactoryHandle((new CommitServiceList()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new CommitServiceList(), Handle = ProcessCommitServiceList});

			getClient().AddFactoryHandle((new Keepalive()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new Keepalive(), Handle = ProcessKeepalive});

			getClient().AddFactoryHandle((new SubscribeFirstCommit()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new SubscribeFirstCommit(), Handle = ProcessSubscribeFirstCommit});

			getClient().AddFactoryHandle((new AllocateId()).getTypeId(), new Service.ProtocolFactoryHandle() {Factory = () -> new AllocateId()});

			getClient().Start();
		}

		public void close() throws IOException {
			Stop();
		}

		public final static class NetClient extends HandshakeClient {
			private Agent Agent;
			public Agent getAgent() {
				return Agent;
			}
			/** 
			 和注册服务器之间只保持一个连接。并且不处理任何协议状态。
			*/
			private AsyncSocket Socket;
			public AsyncSocket getSocket() {
				return Socket;
			}
			private void setSocket(AsyncSocket value) {
				Socket = value;
			}

			public NetClient(Agent agent, Config config) {
				super("Zeze.Services.ServiceManager.Agent", config);
				Agent = agent;
			}
			public NetClient(Agent agent, Config config, String name) {
				super(name, config);
				Agent = agent;
			}

			@Override
			public void OnHandshakeDone(AsyncSocket sender) {
				super.OnHandshakeDone(sender);
				if (null == getSocket()) {
					setSocket(sender);
					Util.Task.Run(getAgent().OnConnected, "ServiceManager.Agent.OnConnected");
				}
				else {
					Agent.logger.Error("Has Connected.");
				}
			}

			@Override
			public void OnSocketClose(AsyncSocket so, Throwable e) {
				if (getSocket() == so) {
					setSocket(null);
				}
				super.OnSocketClose(so, e);
			}
		}
	}
}