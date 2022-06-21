package Zeze.Raft;

import java.io.File;
import java.io.FileNotFoundException;
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

	private final Document XmlDocument;
	private final String XmlFileName;
	private final Element Self;
	private final ConcurrentHashMap<String, Node> Nodes = new ConcurrentHashMap<>();
	private String Name; // 【这个参数不保存】可以在启动的时候从参数读取并设置
	private String DbHome;
	private int AppendEntriesTimeout = DefaultAppendEntriesTimeout; // 复制日志超时，以及发送失败重试超时
	private int LeaderHeartbeatTimer = DefaultLeaderHeartbeatTimer; // 不精确 Heartbeat Idle 算法
	private int ElectionRandomMax = 300;
	private int MaxAppendEntriesCount = 500; // 限制每次复制日志时打包的最大数量

	private int SnapshotLogCount = 100_0000; // -1 disable snapshot
	private int BackgroundApplyCount = 500; // 需要的时间应小于LeaderHeartbeatTimer
	private int UniqueRequestExpiredDays = 7;

	public String getXmlFileName() {
		return XmlFileName;
	}

	ConcurrentHashMap<String, Node> getNodes() {
		return Nodes;
	}

	public String getName() {
		return Name;
	}

	void setName(String value) {
		Name = value;
	}

	// 多数确认时：大于等于这个即可，因为还有自己(Leader)。
	public int getHalfCount() {
		return Nodes.size() / 2;
	}

	public String getDbHome() {
		return DbHome;
	}

	public void setDbHome(String value) {
		DbHome = value;
	}

	public int getAppendEntriesTimeout() {
		return AppendEntriesTimeout;
	}

	public void setAppendEntriesTimeout(int value) {
		AppendEntriesTimeout = value;
	}

	public int getLeaderHeartbeatTimer() {
		return LeaderHeartbeatTimer;
	}

	public void setLeaderHeartbeatTimer(int value) {
		LeaderHeartbeatTimer = value;
	}

	public int getElectionRandomMax() {
		return ElectionRandomMax;
	}

	public void setElectionRandomMax(int value) {
		ElectionRandomMax = value;
	}

	public int getElectionTimeout() {
		return getLeaderHeartbeatTimer() + 100 + Random.getInstance().nextInt(getElectionRandomMax());
	}

	public int getElectionTimeoutMax() {
		return getLeaderHeartbeatTimer() + 100 + getElectionRandomMax() * 2;
	}

	public int getMaxAppendEntriesCount() {
		return MaxAppendEntriesCount;
	}

	public void setMaxAppendEntriesCount(int value) {
		MaxAppendEntriesCount = value;
	}

	public int getSnapshotLogCount() {
		return SnapshotLogCount;
	}

	public void setSnapshotLogCount(int value) {
		SnapshotLogCount = value;
	}

	public int getBackgroundApplyCount() {
		return BackgroundApplyCount;
	}

	public void setBackgroundApplyCount(int value) {
		BackgroundApplyCount = value;
	}

	public int getUniqueRequestExpiredDays() {
		return UniqueRequestExpiredDays;
	}

	public void setUniqueRequestExpiredDays(int value) {
		UniqueRequestExpiredDays = value;
	}

	private RaftConfig(Document xml, String filename, Element self) {
		XmlDocument = xml;
		XmlFileName = filename;
		Self = self;
		Name = self.getAttribute("Name");

		var attr = self.getAttribute("DbHome");
		if (!attr.isEmpty())
			DbHome = attr;
		if (DbHome == null)
			DbHome = Name.replace(':', '_');

		attr = self.getAttribute("AppendEntriesTimeout");
		if (!attr.isEmpty())
			AppendEntriesTimeout = Integer.parseInt(attr);
		attr = self.getAttribute("LeaderHeartbeatTimer");
		if (!attr.isEmpty())
			LeaderHeartbeatTimer = Integer.parseInt(attr);
		attr = self.getAttribute("MaxAppendEntriesCount");
		if (!attr.isEmpty())
			MaxAppendEntriesCount = Integer.parseInt(attr);
		attr = self.getAttribute("SnapshotLogCount");
		if (!attr.isEmpty())
			SnapshotLogCount = Integer.parseInt(attr);
		attr = self.getAttribute("ElectionRandomMax");
		if (!attr.isEmpty())
			ElectionRandomMax = Integer.parseInt(attr);
		attr = self.getAttribute("BackgroundApplyCount");
		if (!attr.isEmpty())
			BackgroundApplyCount = Integer.parseInt(attr);
		attr = self.getAttribute("UniqueRequestExpiredDays");
		if (!attr.isEmpty())
			UniqueRequestExpiredDays = Integer.parseInt(attr);

		NodeList childNodes = self.getChildNodes();
		for (int i = 0, n = childNodes.getLength(); i < n; i++) {
			var node = childNodes.item(i);
			if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
				continue;
			Element e = (Element)node;
			if ("node".equals(e.getTagName()))
				AddNode(new Node(e));
		}
	}

	public void Verify() {
		if (AppendEntriesTimeout < 1000)
			throw new IllegalStateException("AppendEntriesTimeout < 1000");
		if (LeaderHeartbeatTimer < AppendEntriesTimeout + 200)
			throw new IllegalStateException("LeaderHeartbeatTimer < AppendEntriesTimeout + 200");
		if (MaxAppendEntriesCount < 100)
			MaxAppendEntriesCount = 100;
	}

	public void Save() throws TransformerException {
		// skip default
		if (AppendEntriesTimeout != DefaultAppendEntriesTimeout)
			Self.setAttribute("AppendEntriesTimeout", String.valueOf(AppendEntriesTimeout));
		if (LeaderHeartbeatTimer != DefaultLeaderHeartbeatTimer)
			Self.setAttribute("LeaderHeartbeatTimer", String.valueOf(LeaderHeartbeatTimer));
		if (ElectionRandomMax != 300)
			Self.setAttribute("ElectionRandomMax", String.valueOf(ElectionRandomMax));
		if (MaxAppendEntriesCount != 500)
			Self.setAttribute("MaxAppendEntriesCount", String.valueOf(MaxAppendEntriesCount));
		if (SnapshotLogCount != 100_0000)
			Self.setAttribute("SnapshotLogCount", String.valueOf(SnapshotLogCount));
		if (BackgroundApplyCount != 500)
			Self.setAttribute("BackgroundApplyCount", String.valueOf(BackgroundApplyCount));
		if (UniqueRequestExpiredDays != 7)
			Self.setAttribute("UniqueRequestExpiredDays", String.valueOf(UniqueRequestExpiredDays));

		for (var node : Nodes.values())
			node.Save(XmlDocument, Self);

		TransformerFactory.newInstance().newTransformer().transform(
				new DOMSource(XmlDocument), new StreamResult(new File(XmlFileName)));
	}

	public static RaftConfig Load() throws Exception {
		return Load("raft.xml");
	}

	public static RaftConfig Load(String xmlFile) throws Exception {
		if (new File(xmlFile).isFile()) {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xmlFile));
			return new RaftConfig(doc, xmlFile, doc.getDocumentElement());
		}

		throw new FileNotFoundException(String.format("Raft.Config: '%s' not exists.", xmlFile));
	}

	private void AddNode(Node node) {
		if (Nodes.putIfAbsent(node.getName(), node) != null)
			throw new IllegalStateException(String.format("duplicate node '%s'", node.getName()));
	}

	static final class Node {
		private final String Host;
		private final int Port;
		private Element Self;

		Node(Element self) {
			Self = self;
			Host = self.getAttribute("Host");
			Port = Integer.parseInt(self.getAttribute("Port"));
		}

		String getHost() {
			return Host;
		}

		int getPort() {
			return Port;
		}

		String getName() {
			return Host + ':' + Port;
		}

		void Save(Document doc, Element parent) {
			if (Self == null) {
				Self = doc.createElement("node");
				parent.appendChild(Self);
			}
			Self.setAttribute("HostNameOrAddress", Host);
			Self.setAttribute("Port", String.valueOf(Port));
		}
	}
}
