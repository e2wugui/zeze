package Zeze.Raft;

import Zeze.*;
import java.io.*;

public final class RaftConfig {
	private XmlDocument XmlDocument;
	private XmlDocument getXmlDocument() {
		return XmlDocument;
	}

	private String XmlFileName;
	public String getXmlFileName() {
		return XmlFileName;
	}
	private XmlElement Self;
	private XmlElement getSelf() {
		return Self;
	}

	private java.util.concurrent.ConcurrentHashMap<String, Node> Nodes = new java.util.concurrent.ConcurrentHashMap<String, Node> ();
	public java.util.concurrent.ConcurrentHashMap<String, Node> getNodes() {
		return Nodes;
	}

	// 【这个参数不保存】可以在启动的时候从参数读取并设置。
	private String Name;
	public String getName() {
		return Name;
	}
	public void setName(String value) {
		Name = value;
	}
	// 多数确认时：大于等于这个即可，因为还有自己(Leader)。
	public int getHalfCount() {
		return getNodes().size() / 2;
	}
	private String DbHome = "./";
	public String getDbHome() {
		return DbHome;
	}
	public void setDbHome(String value) {
		DbHome = value;
	}

	/** 
	 复制日志超时，以及发送失败重试超时。
	*/
	private int AppendEntriesTimeout;
	public int getAppendEntriesTimeout() {
		return AppendEntriesTimeout;
	}
	public void setAppendEntriesTimeout(int value) {
		AppendEntriesTimeout = value;
	}
	/** 
	 不精确 Heartbeat Idle 算法：
	 如果 AppendLogActive 则设为 false，然后等待下一次timer。
	 否则发送 AppendLog。
	*/
	private int LeaderHeartbeatTimer;
	public int getLeaderHeartbeatTimer() {
		return LeaderHeartbeatTimer;
	}
	public void setLeaderHeartbeatTimer(int value) {
		LeaderHeartbeatTimer = value;
	}
	/** 
	 Leader失效检测超时，超时没有从Leader得到AppendEntries及启动新的选举。
	 【注意】LeaderLostTimeout > LeaderHeartbeatTimer + AppendEntriesTimeout
	*/
	private int LeaderLostTimeout;
	public int getLeaderLostTimeout() {
		return LeaderLostTimeout;
	}
	public void setLeaderLostTimeout(int value) {
		LeaderLostTimeout = value;
	}

	/** 
	 限制每次复制日志时打包的最大数量。
	*/
	private int MaxAppendEntiresCount = 500;
	public int getMaxAppendEntiresCount() {
		return MaxAppendEntiresCount;
	}
	public void setMaxAppendEntiresCount(int value) {
		MaxAppendEntiresCount = value;
	}

	/** 
	 创建snapshot最小的日志数量。如果少于这个数，不会创建新的snapshot。
	 当然实在需要的时候可以创建。see LogSequence.StartSnapshot
	*/
	private int SnapshotMinLogCount = 10000;
	public int getSnapshotMinLogCount() {
		return SnapshotMinLogCount;
	}
	public void setSnapshotMinLogCount(int value) {
		SnapshotMinLogCount = value;
	}

	/** 
	 每天创建 snapshot 的时间，一般负载每天有个低估，
	 在这个时候创建snapshot是比较合适的。
	 如果需要其他定时模式，自己创建定时器，
	 并调用LogSequence.StartSnapshot();
	 同时把 SnapshotHourOfDay 配置成-1，关闭默认的定时器。
	*/
	private int SnapshotHourOfDay = 6;
	public int getSnapshotHourOfDay() {
		return SnapshotHourOfDay;
	}
	public void setSnapshotHourOfDay(int value) {
		SnapshotHourOfDay = value;
	}
	private int SnapshotMinute = 0;
	public int getSnapshotMinute() {
		return SnapshotMinute;
	}
	public void setSnapshotMinute(int value) {
		SnapshotMinute = value;
	}

	private RaftConfig(XmlDocument xml, String filename, XmlElement self) {
		XmlDocument = xml;
		XmlFileName = filename;
		Self = self;

		setName(self.GetAttribute("Name"));
		setDbHome(self.GetAttribute("DbHome"));
		if (tangible.StringHelper.isNullOrEmpty(getDbHome())) {
			setDbHome(".");
		}

		var attr = self.GetAttribute("AppendEntriesTimeout");
		setAppendEntriesTimeout(tangible.StringHelper.isNullOrEmpty(attr) ? 5000 : Integer.parseInt(attr));
		attr = self.GetAttribute("LeaderHeartbeatTimer");
		setLeaderHeartbeatTimer(tangible.StringHelper.isNullOrEmpty(attr) ? 6000 : Integer.parseInt(attr));
		attr = self.GetAttribute("LeaderLostTimeout");
		setLeaderLostTimeout(tangible.StringHelper.isNullOrEmpty(attr) ? 12000 : Integer.parseInt(attr));
		attr = self.GetAttribute("MaxAppendEntiresCount");
		setMaxAppendEntiresCount(tangible.StringHelper.isNullOrEmpty(attr) ? 500 : Integer.parseInt(attr));
		attr = self.GetAttribute("SnapshotMinLogCount");
		setSnapshotMinLogCount(tangible.StringHelper.isNullOrEmpty(attr) ? 10000 : Integer.parseInt(attr));
		attr = self.GetAttribute("SnapshotHourOfDay");
		setSnapshotHourOfDay(tangible.StringHelper.isNullOrEmpty(attr) ? 6 : Integer.parseInt(attr));
		attr = self.GetAttribute("SnapshotMinute");
		setSnapshotMinute(tangible.StringHelper.isNullOrEmpty(attr) ? 0 : Integer.parseInt(attr));

		XmlNodeList childNodes = self.ChildNodes;
		for (XmlNode node : childNodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			switch (e.Name) {
				case "node":
					AddNode(new Node(e));
					break;
			}
		}
	}

	public void Verify() {
		if (getAppendEntriesTimeout() < 0) {
			throw new RuntimeException("AppendEntriesTimeout < 0");
		}
		if (getLeaderHeartbeatTimer() < getAppendEntriesTimeout() + 100) {
			throw new RuntimeException("LeaderHeartbeatTimer < AppendEntriesTimeout + 100");
		}
		if (getLeaderLostTimeout() < getLeaderHeartbeatTimer() + 100) {
			throw new RuntimeException("LeaderLostTimeout < LeaderHeartbeatTimer + 100");
		}

		if (getMaxAppendEntiresCount() < 100) {
			setMaxAppendEntiresCount(100);
		}

		if (getSnapshotMinute() < 0) {
			setSnapshotMinute(0);
		}
		else if (getSnapshotMinute() > 59) {
			setSnapshotMinute(59);
		}
	}

	public void Save() {
		getSelf().SetAttribute("AppendEntriesTimeout", String.valueOf(getAppendEntriesTimeout()));
		getSelf().SetAttribute("LeaderHeartbeatTimer", String.valueOf(getLeaderHeartbeatTimer()));
		getSelf().SetAttribute("LeaderLostTimeout", String.valueOf(getLeaderLostTimeout()));
		getSelf().SetAttribute("MaxAppendEntiresCount", String.valueOf(getMaxAppendEntiresCount()));
		getSelf().SetAttribute("SnapshotMinLogCount", String.valueOf(getSnapshotMinLogCount()));
		getSelf().SetAttribute("SnapshotHourOfDay", String.valueOf(getSnapshotHourOfDay()));
		getSelf().SetAttribute("SnapshotMinute", String.valueOf(getSnapshotMinute()));

		for (var node : getNodes()) {
			node.Value.Save(getXmlDocument(), getSelf());
		}

		try (TextWriter sw = new OutputStreamWriter(getXmlFileName(), java.nio.charset.StandardCharsets.UTF_8)) {
			getXmlDocument().Save(sw);
		}
	}


	public static RaftConfig Load() {
		return Load("raft.xml");
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public static RaftConfig Load(string xmlfile = "raft.xml")
	public static RaftConfig Load(String xmlfile) {
		if ((new File(xmlfile)).isFile()) {
			XmlDocument doc = new XmlDocument();
			doc.Load(xmlfile);
			return new RaftConfig(doc, xmlfile, doc.DocumentElement);
		}

		throw new RuntimeException(String.format("Raft.Config: '%1$s' not exists.", xmlfile));
	}

	private void AddNode(Node node) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
		if (false == getNodes().TryAdd(node.getName(), node)) {
			throw new RuntimeException(String.format("duplicate node '%1$s'", node.getName()));
		}
	}

	public final static class Node {
		private String Host;
		public String getHost() {
			return Host;
		}
		public void setHost(String value) {
			Host = value;
		}
		private int Port;
		public int getPort() {
			return Port;
		}
		public void setPort(int value) {
			Port = value;
		}
		private XmlElement Self;
		public XmlElement getSelf() {
			return Self;
		}
		private void setSelf(XmlElement value) {
			Self = value;
		}
		public String getName() {
			return String.format("%1$s:%2$s", getHost(), getPort());
		}

		public Node(XmlElement self) {
			setSelf(self);
			setHost(self.GetAttribute("Host"));
			setPort(Integer.parseInt(self.GetAttribute("Port")));
		}

		public Node(String host, int port) {
			setHost(host);
			setPort(port);
		}

		public void Save(XmlDocument doc, XmlElement parent) {
			if (null == getSelf()) {
				setSelf(doc.CreateElement("node"));
				parent.AppendChild(getSelf());
			}
			getSelf().SetAttribute("HostNameOrAddress", getHost());
			getSelf().SetAttribute("Port", String.valueOf(getPort()));
		}
	}

}