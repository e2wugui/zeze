package Zeze.Raft;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import Zeze.Net.Binary;
import Zeze.Util.Random;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class RaftConfig {
	public static final int DefaultAppendEntriesTimeout = 2000;
	public static final int DefaultLeaderHeartbeatTimer = DefaultAppendEntriesTimeout + 200;

	private final Document xmlDocument;
	private final String xmlFileName;
	private final Element self;
	private final ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<>();

	// 整个node排序编码以后，可以作为key。
	private final String sortedNames;
	private final byte[] sortedNamesUtf8;
	private final Binary sortedNamesBinary;

	private String name; // 【这个参数不保存】可以在启动的时候从参数读取并设置
	private String dbHome;
	private int appendEntriesTimeout = DefaultAppendEntriesTimeout; // 复制日志超时，以及发送失败重试超时
	private int leaderHeartbeatTimer = DefaultLeaderHeartbeatTimer; // 不精确 Heartbeat Idle 算法
	private int electionRandomMax = 1000; // 默认给足抖动预算：heartbeat+100+rand(1000)，避免单次心跳抖动引发选举
	private int maxAppendEntriesCount = 500; // 限制每次复制日志时打包的最大数量

	private int snapshotLogCount = 100_0000; // -1 disable snapshot
	private boolean snapshotCommitDelayed = false;
	// 预投票：真正选举前先询问多数派，防止分区后term膨胀的节点回归时打断健康集群。
	// 滚动升级期间新旧节点混布时旧节点不认识PreVote协议，会导致选不出Leader，此时应临时配置为false。
	private boolean preVote = true;
	// snapshot时回退数量，某些系统不能任意截断日志，需要回退一定数量进行保护。回退现在用延时提交实现。
	private int backgroundApplyCount = 500; // 需要的时间应小于LeaderHeartbeatTimer
	private int uniqueRequestExpiredDays = 7;

	public String getXmlFileName() {
		return xmlFileName;
	}

	public ConcurrentHashMap<String, Node> getNodes() {
		return nodes;
	}

	public String getSortedNames() {
		return sortedNames;
	}

	public byte[] getSortedNamesUtf8() {
		return sortedNamesUtf8;
	}

	public Binary getSortedNamesBinary() {
		return sortedNamesBinary;
	}

	private String makeSortedNames() {
		var sorted = new String[nodes.size()];
		var i = 0;
		for (var node : nodes.values())
			sorted[i++] = node.getName();
		Arrays.sort(sorted);
		var sb = new StringBuilder();
		for (var s : sorted)
			sb.append('-').append(s);
		return sb.toString();
	}

	public String getName() {
		return name;
	}

	void setName(String value) {
		name = value.replace(":", "_");
	}

	/**
	 * raft 所在服务器的ip地址，从name中提取。
	 */
	public String getHostIp() {
		var p = name.indexOf('_');
		if (p < 0)
			return name;
		return name.substring(0, p);
	}

	/**
	 * raft 所在服务器的端口，从name中提取。
	 */
	public int getHostPort() {
		var p = name.indexOf('_');
		if (p < 0)
			throw new RuntimeException("port not found.");
		return Integer.parseInt(name.substring(p + 1));
	}

	// 多数确认时：大于等于这个即可，因为还有自己(Leader)。
	public int getHalfCount() {
		return nodes.size() >>> 1;
	}

	public int getMajorityCount() {
		return (nodes.size() >>> 1) + 1;
	}

	public String getDbHome() {
		return dbHome;
	}

	public void setDbHome(String value) {
		dbHome = value;
	}

	public int getAppendEntriesTimeout() {
		return appendEntriesTimeout;
	}

	public int getAgentTimeout() {
		return appendEntriesTimeout + 2000;
	}

	public void setAppendEntriesTimeout(int value) {
		appendEntriesTimeout = value;
	}

	public int getLeaderHeartbeatTimer() {
		return leaderHeartbeatTimer;
	}

	public void setLeaderHeartbeatTimer(int value) {
		leaderHeartbeatTimer = value;
	}

	public int getElectionRandomMax() {
		return electionRandomMax;
	}

	public void setElectionRandomMax(int value) {
		electionRandomMax = value;
	}

	public int getElectionTimeout() {
		return getLeaderHeartbeatTimer() + 100 + Random.getInstance().nextInt(getElectionRandomMax());
	}

	public int getElectionTimeoutMax() {
		return getLeaderHeartbeatTimer() + 100 + getElectionRandomMax() * 2;
	}

	public int getMaxAppendEntriesCount() {
		return maxAppendEntriesCount;
	}

	public void setMaxAppendEntriesCount(int value) {
		maxAppendEntriesCount = value;
	}

	public int getSnapshotLogCount() {
		return snapshotLogCount;
	}

	public boolean isSnapshotCommitDelayed() {
		return snapshotCommitDelayed;
	}

	public boolean isPreVote() {
		return preVote;
	}

	public void setPreVote(boolean value) {
		preVote = value;
	}

	public void setSnapshotLogCount(int value) {
		snapshotLogCount = value;
	}

	public void setSnapshotCommitDelayed(boolean value) {
		snapshotCommitDelayed = value;
	}

	public int getBackgroundApplyCount() {
		return backgroundApplyCount;
	}

	public void setBackgroundApplyCount(int value) {
		backgroundApplyCount = value;
	}

	public int getUniqueRequestExpiredDays() {
		return uniqueRequestExpiredDays;
	}

	public void setUniqueRequestExpiredDays(int value) {
		uniqueRequestExpiredDays = value;
	}

	private RaftConfig(String sortedNames) {
		xmlDocument = null;
		xmlFileName = null;
		self = null;

		this.sortedNames = sortedNames;
		this.sortedNamesUtf8 = sortedNames.getBytes(StandardCharsets.UTF_8);
		this.sortedNamesBinary = new Binary(sortedNamesUtf8);

		var nodes = sortedNames.split("-");
		for (var node : nodes) {
			if (node.isEmpty())
				continue;
			var ipPort = node.split("_");
			var proxyIp = ipPort.length > 2 ? ipPort[2] : "";
			var proxyPort = ipPort.length > 3 ? Integer.parseInt(ipPort[3]) : 0;
			addNode(new Node(ipPort[0], Integer.parseInt(ipPort[1]), proxyIp, proxyPort));
		}
	}

	// 【FND8-42】按RaftName派生私有副本：Raft构造器不得变异调用方传入的配置对象——
	// 共享同一RaftConfig实例的多Raft（多库/多桶共用一份集群配置的便捷用法）会互相
	// 污染Name/DbHome，getSnapshotFullName等路径错位造成跨实例数据覆盖。
	// 派生副本共享不可变部分（xmlDocument/nodes条目/sortedNames），复制全部可变标量；
	// 保留原联动语义：仅当DbHome与Name相关（未特别配置）时才跟随RaftName改写。
	RaftConfig derive(String raftName) {
		var copy = new RaftConfig(xmlDocument, xmlFileName, self, nodes,
				sortedNames, sortedNamesUtf8, sortedNamesBinary);
		copy.name = name;
		copy.dbHome = dbHome;
		copy.appendEntriesTimeout = appendEntriesTimeout;
		copy.leaderHeartbeatTimer = leaderHeartbeatTimer;
		copy.electionRandomMax = electionRandomMax;
		copy.maxAppendEntriesCount = maxAppendEntriesCount;
		copy.snapshotLogCount = snapshotLogCount;
		copy.snapshotCommitDelayed = snapshotCommitDelayed;
		copy.preVote = preVote;
		copy.backgroundApplyCount = backgroundApplyCount;
		copy.uniqueRequestExpiredDays = uniqueRequestExpiredDays;
		if (raftName != null && !raftName.isEmpty()) {
			// 如果 DbHome 和 Name 相关，一般表示没有特别配置。
			// 此处特别设置 Raft.Name 时，需要一起更新。
			if (copy.dbHome.equals(copy.name.replace(':', '_')))
				copy.dbHome = raftName.replace(':', '_');
			copy.setName(raftName);
		}
		return copy;
	}

	// 【FND8-42】derive的拷贝构造：共享只读的xml与节点（构造后不再变化），nodes
	// 复制进自己的map（addNode只在装载期发生，条目本身全final不可变）。
	private RaftConfig(Document xmlDocument, String xmlFileName, Element self,
					   ConcurrentHashMap<String, Node> nodes,
					   String sortedNames, byte[] sortedNamesUtf8, Binary sortedNamesBinary) {
		this.xmlDocument = xmlDocument;
		this.xmlFileName = xmlFileName;
		this.self = self;
		this.nodes.putAll(nodes);
		this.sortedNames = sortedNames;
		this.sortedNamesUtf8 = sortedNamesUtf8;
		this.sortedNamesBinary = sortedNamesBinary;
	}

	private RaftConfig(Document xml, String filename, Element self) {
		xmlDocument = xml;
		xmlFileName = filename;
		this.self = self;
		name = self.getAttribute("Name");
		if (name.indexOf('_') < 0) {
			int p = name.lastIndexOf(':');
			if (p >= 0)
				name = name.substring(0, p) + '_' + name.substring(p + 1);
		}

		var attr = self.getAttribute("DbHome");
		if (!attr.isEmpty())
			dbHome = attr;
		if (dbHome == null)
			dbHome = name.replace(':', '_');

		attr = self.getAttribute("AppendEntriesTimeout");
		if (!attr.isEmpty())
			appendEntriesTimeout = Integer.parseInt(attr);
		attr = self.getAttribute("LeaderHeartbeatTimer");
		if (!attr.isEmpty())
			leaderHeartbeatTimer = Integer.parseInt(attr);
		attr = self.getAttribute("MaxAppendEntriesCount");
		if (!attr.isEmpty())
			maxAppendEntriesCount = Integer.parseInt(attr);
		attr = self.getAttribute("SnapshotLogCount");
		if (!attr.isEmpty())
			snapshotLogCount = Integer.parseInt(attr);
		attr = self.getAttribute("SnapshotCommitDelayed");
		if (!attr.isEmpty())
			snapshotCommitDelayed = Boolean.parseBoolean(attr);
		attr = self.getAttribute("PreVote");
		if (!attr.isEmpty())
			preVote = Boolean.parseBoolean(attr);
		attr = self.getAttribute("ElectionRandomMax");
		if (!attr.isEmpty())
			electionRandomMax = Integer.parseInt(attr);
		attr = self.getAttribute("BackgroundApplyCount");
		if (!attr.isEmpty())
			backgroundApplyCount = Integer.parseInt(attr);
		attr = self.getAttribute("UniqueRequestExpiredDays");
		if (!attr.isEmpty())
			uniqueRequestExpiredDays = Integer.parseInt(attr);

		NodeList childNodes = self.getChildNodes();
		for (int i = 0, n = childNodes.getLength(); i < n; i++) {
			var node = childNodes.item(i);
			if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
				continue;
			Element e = (Element)node;
			if ("node".equals(e.getTagName()))
				addNode(new Node(e));
		}
		sortedNames = makeSortedNames();
		sortedNamesUtf8 = sortedNames.getBytes(StandardCharsets.UTF_8);
		sortedNamesBinary = new Binary(sortedNamesUtf8);
	}

	public void verify() {
		if (appendEntriesTimeout < 1000)
			throw new IllegalStateException("AppendEntriesTimeout < 1000");
		if (leaderHeartbeatTimer < appendEntriesTimeout + 200)
			throw new IllegalStateException("LeaderHeartbeatTimer < AppendEntriesTimeout + 200");
		if (backgroundApplyCount < 1)
			// FND5-14：0/负数使后台apply的tryApply(count=0)一条不应用且退出条件永不成，
			// 无限yield忙轮询、applyFuture永不完成、shutdown的await挂死。fail-fast对齐verify口径。
			throw new IllegalStateException("BackgroundApplyCount < 1");
		if (electionRandomMax < 1)
			// FND6-09（FND5-14姊妹）：getElectionTimeout的Random.nextInt(n<=0)抛IllegalArgumentException，
			// 被定时器吞掉仅记日志——onTimer的Follower分支永不选举、followerOnAppendEntries的
			// setLeaderActiveTime同抛致AppendEntries无应答，节点静默僵死只剩error日志。
			throw new IllegalStateException("ElectionRandomMax < 1");
		if (uniqueRequestExpiredDays < 1)
			// FND6-09姊妹：isUniqueRequestCreateTimeValid对expiredDays<=0把所有请求判RaftExpired
			// （0时(now-create)/86400_000>=0恒真，负数连未来时间也拒绝）——集群功能瘫痪但
			// 无异常无日志（每请求确定性拒绝），同属「非法值静默失效」靶型。
			throw new IllegalStateException("UniqueRequestExpiredDays < 1");
		if (maxAppendEntriesCount < 100)
			maxAppendEntriesCount = 100;
	}

	public void save() throws TransformerException {
		if (null == self || null == xmlDocument)
			return; // client 可能不是从xml装载，不需要保存。

		// skip default
		if (appendEntriesTimeout != DefaultAppendEntriesTimeout)
			self.setAttribute("AppendEntriesTimeout", String.valueOf(appendEntriesTimeout));
		if (leaderHeartbeatTimer != DefaultLeaderHeartbeatTimer)
			self.setAttribute("LeaderHeartbeatTimer", String.valueOf(leaderHeartbeatTimer));
		if (electionRandomMax != 1000)
			self.setAttribute("ElectionRandomMax", String.valueOf(electionRandomMax));
		if (maxAppendEntriesCount != 500)
			self.setAttribute("MaxAppendEntriesCount", String.valueOf(maxAppendEntriesCount));
		if (snapshotLogCount != 100_0000)
			self.setAttribute("SnapshotLogCount", String.valueOf(snapshotLogCount));
		if (snapshotCommitDelayed)
			self.setAttribute("SnapshotCommitDelayed", "true");
		if (!preVote)
			self.setAttribute("PreVote", "false");
		if (backgroundApplyCount != 500)
			self.setAttribute("BackgroundApplyCount", String.valueOf(backgroundApplyCount));
		if (uniqueRequestExpiredDays != 7)
			self.setAttribute("UniqueRequestExpiredDays", String.valueOf(uniqueRequestExpiredDays));

		for (var node : nodes.values())
			node.save(xmlDocument, self);

		if (null != xmlFileName) {
			var factory = TransformerFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			var transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(xmlDocument), new StreamResult(new File(xmlFileName)));
		}
	}

	public static @NotNull RaftConfig load() throws Exception {
		return load("raft.xml");
	}

	public static @NotNull RaftConfig load(String xmlFile) throws Exception {
		if (new File(xmlFile).isFile()) {
			var factory = DocumentBuilderFactory.newInstance();
			// 与 save() 安全设置一致；合法配置不含 DOCTYPE，行为不变
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(new File(xmlFile));
			return new RaftConfig(doc, xmlFile, doc.getDocumentElement());
		}

		throw new FileNotFoundException(String.format("Raft.Config: '%s' not exists.", xmlFile));
	}

	public static @NotNull RaftConfig loadFromSortedNames(String names) {
		return new RaftConfig(names);
	}

	public static @NotNull RaftConfig loadFromString(String content) {
		try {
			var is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
			var factory = DocumentBuilderFactory.newInstance();
			// 与 save() 安全设置一致；合法配置不含 DOCTYPE，行为不变
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			var doc = factory.newDocumentBuilder().parse(is);
			return new RaftConfig(doc, null, doc.getDocumentElement());
		} catch (Exception e) {
			throw Task.forceThrow(e);
		}
	}

	private void addNode(Node node) {
		if (nodes.putIfAbsent(node.getName(), node) != null)
			throw new IllegalStateException(String.format("duplicate node '%s'", node.getName()));
	}

	public static final class Node {
		private final String host;
		private final int port;

		// 当多个raft运行在一个进程内，可以设置Agent只链接这个进程，这几个raft共享连接。
		// 这个需要一定的开发，这里仅提供基本的扩展配置点。
		private final String proxyHost;
		private final int proxyPort;

		// 多数。
		// 当raft把节点分布到异地机房中，但是在主机房保留足够的多数派，推荐Leader都在主机房中产生。
		// 这个配置用来区分是否主机房。
		// 默认true，所有的节点都在主机房。
		private final boolean suggestMajority;
		private Element self;

		Node(Element self) {
			this.self = self;
			host = self.getAttribute("Host");
			port = Integer.parseInt(self.getAttribute("Port"));
			proxyHost = self.getAttribute("ProxyHost");
			var attr = self.getAttribute("ProxyPort");
			proxyPort = attr.isBlank() ? 0 : Integer.parseInt(attr);
			attr = self.getAttribute("SuggestMajority");
			suggestMajority = attr.isBlank() || Boolean.parseBoolean(attr);
		}

		Node(String host, int port, String proxyHost, int proxyPort) {
			this.host = host;
			this.port = port;
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;
			suggestMajority = true;
		}

		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public String getProxyHost() {
			return proxyHost;
		}

		public int getProxyPort() {
			return proxyPort;
		}

		public String getName() {
			return host + '_' + port;
		}

		public boolean isSuggestMajority() {
			return suggestMajority;
		}

		void save(Document doc, Element parent) {
			if (self == null) {
				self = doc.createElement("node");
				parent.appendChild(self);
			}
			self.setAttribute("Host", host);
			self.setAttribute("Port", String.valueOf(port));
			self.setAttribute("ProxyHost", proxyHost);
			self.setAttribute("ProxyPort", String.valueOf(proxyPort));
			self.setAttribute("SuggestMajority", String.valueOf(suggestMajority));
		}

		@Override
		public String toString() {
			return host + "_" + port + "_" + proxyHost + "_" + proxyPort;
		}
	}

	public int getSuggestMajorityCount() {
		var count = 0;
		for (var node : nodes.values())
			if (node.isSuggestMajority())
				count++;
		return count;
	}

	public static void main(String[] args) {
		var conf = RaftConfig.loadFromSortedNames("-127.0.0.1_10004-127.0.0.1_10005-127.0.0.1_10006");
		System.out.println(conf.getNodes());
	}
}
