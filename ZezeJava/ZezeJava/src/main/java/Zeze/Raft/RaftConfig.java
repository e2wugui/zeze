package Zeze.Raft;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import Zeze.Util.Random;
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
	private final String sortedNames;
	private final byte[] sortedNamesUtf8;
	private String name; // 【这个参数不保存】可以在启动的时候从参数读取并设置
	private String dbHome;
	private int appendEntriesTimeout = DefaultAppendEntriesTimeout; // 复制日志超时，以及发送失败重试超时
	private int leaderHeartbeatTimer = DefaultLeaderHeartbeatTimer; // 不精确 Heartbeat Idle 算法
	private int electionRandomMax = 300;
	private int maxAppendEntriesCount = 500; // 限制每次复制日志时打包的最大数量

	private int snapshotLogCount = 100_0000; // -1 disable snapshot
	private boolean snapshotCommitDelayed = false;
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

	private String makeSortedNames() {
		var sorted = new String[nodes.size()];
		var i = 0;
		for (var node : nodes.values())
			sorted[i++] = node.getName();
		Arrays.sort(sorted);
		var sb = new StringBuilder();
		for (var s : sorted)
			sb.append("-").append(s);
		return sb.toString();
	}

	public String getName() {
		return name;
	}

	void setName(String value) {
		name = value;
	}

	// 多数确认时：大于等于这个即可，因为还有自己(Leader)。
	public int getHalfCount() {
		return nodes.size() / 2;
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

	private RaftConfig(Document xml, String filename, Element self) {
		xmlDocument = xml;
		xmlFileName = filename;
		this.self = self;
		name = self.getAttribute("Name");

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
	}

	public void verify() {
		if (appendEntriesTimeout < 1000)
			throw new IllegalStateException("AppendEntriesTimeout < 1000");
		if (leaderHeartbeatTimer < appendEntriesTimeout + 200)
			throw new IllegalStateException("LeaderHeartbeatTimer < AppendEntriesTimeout + 200");
		if (maxAppendEntriesCount < 100)
			maxAppendEntriesCount = 100;
	}

	public void save() throws TransformerException {
		// skip default
		if (appendEntriesTimeout != DefaultAppendEntriesTimeout)
			self.setAttribute("AppendEntriesTimeout", String.valueOf(appendEntriesTimeout));
		if (leaderHeartbeatTimer != DefaultLeaderHeartbeatTimer)
			self.setAttribute("LeaderHeartbeatTimer", String.valueOf(leaderHeartbeatTimer));
		if (electionRandomMax != 300)
			self.setAttribute("ElectionRandomMax", String.valueOf(electionRandomMax));
		if (maxAppendEntriesCount != 500)
			self.setAttribute("MaxAppendEntriesCount", String.valueOf(maxAppendEntriesCount));
		if (snapshotLogCount != 100_0000)
			self.setAttribute("SnapshotLogCount", String.valueOf(snapshotLogCount));
		if (snapshotCommitDelayed)
			self.setAttribute("SnapshotCommitDelayed", "true");
		if (backgroundApplyCount != 500)
			self.setAttribute("BackgroundApplyCount", String.valueOf(backgroundApplyCount));
		if (uniqueRequestExpiredDays != 7)
			self.setAttribute("UniqueRequestExpiredDays", String.valueOf(uniqueRequestExpiredDays));

		for (var node : nodes.values())
			node.save(xmlDocument, self);

		if (null != xmlFileName) {
			TransformerFactory.newInstance().newTransformer().transform(
					new DOMSource(xmlDocument), new StreamResult(new File(xmlFileName)));
		}
	}

	public static RaftConfig load() throws Exception {
		return load("raft.xml");
	}

	public static RaftConfig load(String xmlFile) throws Exception {
		if (new File(xmlFile).isFile()) {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xmlFile));
			return new RaftConfig(doc, xmlFile, doc.getDocumentElement());
		}

		throw new FileNotFoundException(String.format("Raft.Config: '%s' not exists.", xmlFile));
	}

	public static RaftConfig loadFromString(String content) throws Exception {
		var is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
		return new RaftConfig(doc, null, doc.getDocumentElement());
	}

	private void addNode(Node node) {
		if (nodes.putIfAbsent(node.getName(), node) != null)
			throw new IllegalStateException(String.format("duplicate node '%s'", node.getName()));
	}

	public static final class Node {
		private final String host;
		private final int port;
		private Element self;

		Node(Element self) {
			this.self = self;
			host = self.getAttribute("Host");
			port = Integer.parseInt(self.getAttribute("Port"));
		}

		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public String getName() {
			return host + ':' + port;
		}

		void save(Document doc, Element parent) {
			if (self == null) {
				self = doc.createElement("node");
				parent.appendChild(self);
			}
			self.setAttribute("HostNameOrAddress", host);
			self.setAttribute("Port", String.valueOf(port));
		}
	}
}
