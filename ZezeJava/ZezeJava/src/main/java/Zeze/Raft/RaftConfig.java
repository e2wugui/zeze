package Zeze.Raft;

import java.util.Objects;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import Zeze.Util.AtomicOutputFile;
import Zeze.Util.Random;
import Zeze.Util.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.jspecify.annotations.Nullable;

/**
 * Raft 集群配置：xml/字符串装载、节点表、选举/复制/快照等参数及其校验，支持按 RaftName 派生副本。
 */
public final class RaftConfig {
	public static final int DefaultAppendEntriesTimeout = 2000;
	public static final int DefaultLeaderHeartbeatTimer = DefaultAppendEntriesTimeout + 200;
	// 选举超时基础余量：预留心跳调度抖动空间，避免误触发选举。
	public static final int ElectionTimeoutBase = 1000;

	private final @Nullable Document xmlDocument;
	private final @Nullable String xmlFileName;
	private final @Nullable Element self;
	private final ConcurrentHashMap<String, Node> nodes = new ConcurrentHashMap<>();

	// 整个node排序编码以后，可以作为key。
	private final String sortedNames;
	private final byte[] sortedNamesUtf8;
	private final Binary sortedNamesBinary;

	private @Nullable String name; // 不持久化，启动时可从参数覆盖
	private @Nullable String dbHome;
	private int appendEntriesTimeout = DefaultAppendEntriesTimeout; // 复制日志及发送失败重试超时
	private int leaderHeartbeatTimer = DefaultLeaderHeartbeatTimer; // 心跳周期（Idle 估算）
	private int electionRandomMax = 1000; // 选举随机超时上限，错开多节点同时发起选举
	private int maxAppendEntriesCount = 500; // 单次日志复制打包的最大条数

	private int snapshotLogCount = 100_0000; // -1 表示禁用自动快照
	private boolean snapshotCommitDelayed = false; // 延迟一代提交快照，保留上一代快照以保护日志回退窗口
	// 预投票：选举前先向多数派探测，防止分区节点回归打断集群；滚动升级混布旧节点时可临时关闭。
	private boolean preVote = true;
	// 同步写：日志与状态机存储是否启用 sync write（默认 false，依赖多副本持久性）。
	private boolean syncWrite = false;
	private int backgroundApplyCount = 500; // 单批后台 apply 日志数，耗时应小于 LeaderHeartbeatTimer
	private int uniqueRequestExpiredDays = 7;

	public @Nullable String getXmlFileName() {
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

	public @Nullable String getName() {
		return name;
	}

	void setName(String value) {
		name = value.replace(":", "_");
	}

	/**
	 * raft 所在服务器的ip地址，从name中提取。
	 */
	public String getHostIp() {
		var name = Objects.requireNonNull(this.name);
		var p = name.indexOf('_');
		if (p < 0)
			return name;
		return name.substring(0, p);
	}

	/**
	 * raft 所在服务器的端口，从name中提取。
	 */
	public int getHostPort() {
		var name = Objects.requireNonNull(this.name);
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

	public @Nullable String getDbHome() {
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
		return getLeaderHeartbeatTimer() + ElectionTimeoutBase + Random.getInstance().nextInt(getElectionRandomMax());
	}

	public int getElectionTimeoutMax() {
		return getLeaderHeartbeatTimer() + ElectionTimeoutBase + getElectionRandomMax() * 2;
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

	public boolean isSyncWrite() {
		return syncWrite;
	}

	public void setSyncWrite(boolean value) {
		syncWrite = value;
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

	// 按 raftName 派生私有副本，避免多 Raft 共用配置时互相污染 Name/DbHome；
	// 仅当 DbHome 未单独配置（与原 Name 一致）时跟随 raftName 联动更新。
	RaftConfig derive(@Nullable String raftName) {
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
		copy.syncWrite = syncWrite;
		copy.backgroundApplyCount = backgroundApplyCount;
		copy.uniqueRequestExpiredDays = uniqueRequestExpiredDays;
		if (raftName != null && !raftName.isEmpty()) {
			// DbHome 与 Name 一致时视为未单独配置，随 raftName 联动更新。
			//noinspection DataFlowIssue
			if (Objects.requireNonNull(copy.dbHome).equals(copy.name.replace(':', '_')))
				copy.dbHome = raftName.replace(':', '_');
			copy.setName(raftName);
		}
		return copy;
	}

	// derive 拷贝构造：共享只读的 XML 与不可变节点对象，浅拷贝 nodes 映射。
	private RaftConfig(@Nullable Document xmlDocument, @Nullable String xmlFileName, @Nullable Element self,
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

	private RaftConfig(@Nullable Document xml, @Nullable String filename, @Nullable Element self) {
		xmlDocument = xml;
		xmlFileName = filename;
		this.self = self;
		//noinspection DataFlowIssue
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
		attr = self.getAttribute("SyncWrite");
		if (!attr.isEmpty())
			syncWrite = Boolean.parseBoolean(attr);
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
			throw new IllegalStateException("BackgroundApplyCount < 1");
		if (electionRandomMax < 1)
			throw new IllegalStateException("ElectionRandomMax < 1");
		if (uniqueRequestExpiredDays < 1)
			throw new IllegalStateException("UniqueRequestExpiredDays < 1");
		if (maxAppendEntriesCount < 100)
			maxAppendEntriesCount = 100;
	}

	public void save() throws TransformerException {
		if (null == self || null == xmlDocument)
			return; // client 可能不是从xml装载，不需要保存。

		// 包括恢复默认值：沿用装载时的 DOM，跳过默认值会把旧属性再次保存。
		self.setAttribute("AppendEntriesTimeout", String.valueOf(appendEntriesTimeout));
		self.setAttribute("LeaderHeartbeatTimer", String.valueOf(leaderHeartbeatTimer));
		self.setAttribute("ElectionRandomMax", String.valueOf(electionRandomMax));
		self.setAttribute("MaxAppendEntriesCount", String.valueOf(maxAppendEntriesCount));
		self.setAttribute("SnapshotLogCount", String.valueOf(snapshotLogCount));
		self.setAttribute("SnapshotCommitDelayed", String.valueOf(snapshotCommitDelayed));
		self.setAttribute("PreVote", String.valueOf(preVote));
		self.setAttribute("SyncWrite", String.valueOf(syncWrite));
		self.setAttribute("BackgroundApplyCount", String.valueOf(backgroundApplyCount));
		self.setAttribute("UniqueRequestExpiredDays", String.valueOf(uniqueRequestExpiredDays));

		for (var node : nodes.values())
			node.save(xmlDocument, self);

		if (null != xmlFileName) {
			var factory = TransformerFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			var transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			var output = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(xmlDocument), new StreamResult(output));
			try {
				AtomicOutputFile.writeAtomically(new File(xmlFileName).toPath(), output.toByteArray());
			} catch (IOException e) {
				throw new TransformerException(e);
			}
		}
	}

	public static RaftConfig load() throws Exception {
		return load("raft.xml");
	}

	public static RaftConfig load(String xmlFile) throws Exception {
		if (new File(xmlFile).isFile()) {
			var factory = DocumentBuilderFactory.newInstance();
			// 禁用 DOCTYPE 防止 XXE，与 save() 安全配置一致
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			Document doc = factory.newDocumentBuilder().parse(new File(xmlFile));
			return new RaftConfig(doc, xmlFile, doc.getDocumentElement());
		}

		throw new FileNotFoundException(String.format("Raft.Config: '%s' not exists.", xmlFile));
	}

	public static RaftConfig loadFromSortedNames(String names) {
		return new RaftConfig(names);
	}

	public static RaftConfig loadFromString(String content) {
		try {
			var is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
			var factory = DocumentBuilderFactory.newInstance();
			// 禁用 DOCTYPE 防止 XXE，与 save() 安全配置一致
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

		// 多 Raft 共用进程代理连接的地址配置。
		private final String proxyHost;
		private final int proxyPort;

		// 是否位于推荐产生 Leader 的主机房（默认 true）。
		private final boolean suggestMajority;
		private @Nullable Element self;

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
			//noinspection DataFlowIssue
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
