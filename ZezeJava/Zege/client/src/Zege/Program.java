package Zege;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import Zege.Friend.*;
import Zege.Message.*;
import Zeze.Serialize.ByteBuffer;

public class Program {
	public static Program Instance = new Program();

	private static String getComputerName() throws UnknownHostException {
		InetAddress addr;
		addr = InetAddress.getLocalHost();
		return addr.getHostName();
	}

	public synchronized static void main(String[] args) throws Throwable {
		Instance.run(args);
	}

	private ArrayList<Window> Windows = new ArrayList<>();
	private MainWindow Main;
	public String Self;

	public void run(String[] args) throws Throwable {
		var app = App.Instance;
		var linkIp = "127.0.0.1";
		var linkPort = 5100;
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				linkIp = args[++i];
				break;
			case "-port":
				linkPort = Integer.parseInt(args[++i]);
				break;
			}
		}
		app.Start(linkIp, linkPort);
		try {
			app.Connector.WaitReady();
			var account = getComputerName().toLowerCase(Locale.ROOT);
			app.Zege_Linkd.auth(account).await();
			app.Zeze_Builtin_Online.login("PC").await();
			Main = new MainWindow();
			Windows.add(Main);
			Self = account;
			refresh();
			var console = new BufferedReader(new InputStreamReader(System.in));
			String line = "";
			do {
				line = console.readLine();
				try {
					if (!Windows.get(Windows.size() - 1).process(line))
						System.out.println("Unknown Command!");
				} catch (Throwable e) {
					e.printStackTrace();
				}
			} while (!line.equals("exit"));
		} finally {
			app.Stop();
		}
	}

	public class Window {
		public String Name;

		public long tryParseLong(String value) {
			try {
				return Long.parseLong(value);
			} catch (Exception ex) {
				return -1L;
			}
		}
		public void back() {
			Windows.remove(Windows.size() - 1);
			Program.this.refresh();
		}

		public boolean process(String line) {
			if (line.isEmpty()) {
				Program.this.refresh();
				return true;
			}

			var cmd = line.split(" ");
			switch (cmd[0])
			{
			case "b":
				if (cmd.length > 1) {
					var find = Windows.indexOf(cmd[1]);
					if (find >= 0) {
						for (int i = find + 1; i < Windows.size(); ++i)
							Windows.remove(i);
						Program.this.refresh();
						return true;
					}
				} else if (Windows.size() > 1) {
					back();
					return true;
				}
				break;
			case "af":
				App.Instance.Zege_Friend.add(cmd[1]).await();
				Program.this.refresh();
				return true;
			}
			return false;
		}

		public void refresh() {
		}

		public boolean processNotifyMessage(BMessage notify) {
			return false;
		}
	}

	public void OnMessage(NotifyMessage r) {
		var current = Windows.get(Windows.size() - 1);
		if (current.processNotifyMessage(r.Argument))
			return;
		Main.processNotifyMessage(r.Argument);
	}

	public class DepartmentWindow extends Window {
		public String Group;
		public String DepartmentName;
		public long Id;
		public BDepartmentNode Department;
		public long MemberNodeId;
		public BDepartmentMemberNode MemberNode;

		public DepartmentWindow(String group, long id, String name) {
			this.Group = group;
			this.Id = id;
			this.DepartmentName = name;
			Name = name + "(" + Id + ")";
		}

		@Override
		public boolean process(String line) {
			// 聊天消息先处理基本命令。
			if (super.process(line))
				return true;

			var cmd = line.split(" ");
			if (cmd.length == 0)
				return true;

			switch (cmd[0]) {
			case "create":
				var newId = App.Instance.Zege_Friend.createDepartment(Group, Id, cmd[1]);
				addWindow(new DepartmentWindow(Group, newId.getId(), cmd[1]));
				Program.this.refresh();
				break;
			case "delete":
				App.Instance.Zege_Friend.deleteDepartment(Group, Id);
				back();
				break;
			case "move":
				App.Instance.Zege_Friend.moveDepartment(Group, Id, Long.parseLong(cmd[1]));
				System.out.println("Move Success. Window Not Change Because This Is A Simple Program.");
				break;
			case "open":
				var id = tryParseLong(cmd[1]);
				if (id > 0) {
					addWindow(new DepartmentWindow(Group, id, findChildName(id)));
				} else {
					addWindow(new DepartmentWindow(Group, findChildId(cmd[1]), cmd[1]));
				}
				return true;
			}
			App.Instance.Zege_Message.send(Group, line, Id).await();
			return true;
		}

		public String findChildName(long id) {
			for (var child : Department.getChilds()) {
				if (child.getValue() == id)
					return child.getKey();
			}
			throw new RuntimeException("child not found with id=" + id);
		}

		public long findChildId(String name) {
			return Department.getChilds().get(name);
		}

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			if (notify.getGroup().equals(Group) && notify.getDeparmentId() == Id) {
				var bb = ByteBuffer.Wrap(notify.getSecureMessage());
				var bMsg = new BTextMessage();
				bMsg.Decode(bb);
				System.out.println(bMsg.getMessage());
				return true;
			}
			return false;
		}

		@Override
		public void refresh() {
			Department = App.Instance.Zege_Friend.getDepartmentNode(Group, Id);
			for (var child : Department.getChilds()) {
				System.out.println("[" + child.getKey() + "(" + child.getValue() + ")]");
			}
			MemberNode = App.Instance.Zege_Friend.getDepartmentMemberNode(Group, Id, MemberNodeId);
			for (var member : MemberNode.getDepartmentMembers()) {
				System.out.println(member.getAccount());
			}

			var list = Main.ReceivedMessages.remove(new MessageTarget(Group, Id));
			if (null != list) {
				for (var notify : list)
					processNotifyMessage(notify);
			}
		}
	}

	public class GroupWindow extends Window {
		public String Group;
		public BGroup Root;
		public long MemberNodeId;
		public BMemberNode MemberNode;

		public GroupWindow(String group) {
			this.Group = group;
			Name = Group;
		}

		@Override
		public boolean process(String line) {
			// 聊天消息先处理基本命令。
			if (super.process(line))
				return true;

			var cmd = line.split(" ");
			if (cmd.length == 0)
				return true;

			switch (cmd[0]) {
			case "create":
				var newId = App.Instance.Zege_Friend.createDepartment(Group, 0, cmd[1]);
				addWindow(new DepartmentWindow(Group, newId.getId(), cmd[1]));
				Program.this.refresh();
				break;
			case "open":
				var id = tryParseLong(cmd[1]);
				if (id > 0) {
					addWindow(new DepartmentWindow(Group, id, findChildName(id)));
				} else {
					addWindow(new DepartmentWindow(Group, findChildId(cmd[1]), cmd[1]));
				}
				return true;
			}
			App.Instance.Zege_Message.send(Group, line, 0).await();
			return true;
		}

		public String findChildName(long id) {
			for (var child : Root.getChilds()) {
				if (child.getValue() == id)
					return child.getKey();
			}
			throw new RuntimeException("child not found with id=" + id);
		}

		public long findChildId(String name) {
			return Root.getChilds().get(name);
		}

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			if (notify.getGroup().equals(Group) && notify.getDeparmentId() == 0) {
				var bb = ByteBuffer.Wrap(notify.getSecureMessage());
				var bMsg = new BTextMessage();
				bMsg.Decode(bb);
				System.out.println(bMsg.getMessage());
				return true;
			}
			return false;
		}

		@Override
		public void refresh() {
			Root = App.Instance.Zege_Friend.getGroupRoot(Group);
			for (var child : Root.getChilds()) {
				System.out.println("[" + child.getKey() + "(" + child.getValue() + ")]");
			}
			MemberNode = App.Instance.Zege_Friend.getGroupMemberNode(Group, MemberNodeId);
			for (var member : MemberNode.getMembers()) {
				System.out.println(member.getAccount());
			}

			var list = Main.ReceivedMessages.remove(new MessageTarget(Group, 0));
			if (null != list) {
				for (var notify : list)
					processNotifyMessage(notify);
			}
		}
	}

	public class ChatWindow extends Window {
		public String Target;
		public ChatWindow(String target) {
			this.Target = target;
			Name = "chat:" + target;
		}

		@Override
		public boolean process(String line) {
			// 聊天消息先处理基本命令。
			if (super.process(line))
				return true;

			App.Instance.Zege_Message.send(Target, line, 0).await();
			return true;
		}

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			if (notify.getGroup().isEmpty() && notify.getFrom().equals(Target)) {
				var bb = ByteBuffer.Wrap(notify.getSecureMessage());
				var bMsg = new BTextMessage();
				bMsg.Decode(bb);
				System.out.println(bMsg.getMessage());
				return true;
			}
			return false;
		}

		@Override
		public void refresh() {
			var list = Main.ReceivedMessages.remove(new MessageTarget(Target, 0));
			if (null != list) {
				for (var notify : list)
					processNotifyMessage(notify);
			}
		}
	}

	public static class MessageTarget {
		public String Name;
		public long   Id;

		@Override
		public int hashCode() {
			return Name.hashCode() ^ Long.hashCode(Id);
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (o instanceof MessageTarget) {
				var other = (MessageTarget)o;
				return Name.equals(other.Name) && Id == other.Id;
			}
			return false;
		}

		public MessageTarget(String name, long id) {
			Name = name;
			Id = id;
		}
	}

	public class MainWindow extends Window {
		public MainWindow() {
			Name = "main";
		}

		public long NodeId;
		public BFriendNode Node;

		public BFriend find(String value) {
			for (var friend : Node.getFriends()) {
				if (friend.getAccount().equals(value))
					return friend;
			}
			return null;
		}

		@Override
		public boolean process(String line) {
			var cmd = line.split(" ");
			switch (cmd[0])
			{
			case "open":
				if (cmd.length > 1) {
					var target = cmd[1];
					if (null != find(target)) {
						addWindow(target.endsWith("@group") ? new GroupWindow(target) : new ChatWindow(target));
						return true;
					}
				}
				break;

			default:
				return super.process(line);
			}
			return false;
		}

		@Override
		public void refresh() {
			Node = App.Instance.Zege_Friend.getFriendNode(NodeId);
			for (var friend : Node.getFriends()) {
				System.out.print(friend.getAccount());
				// 子部门的消息不统计。
				var list = ReceivedMessages.get(new MessageTarget(friend.getAccount(), 0));
				System.out.println(null == list ? "" : "(" + list.size() + ")");
			}
		}

		private HashMap<MessageTarget, ArrayList<BMessage>> ReceivedMessages = new HashMap<>();

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			var target = notify.getGroup().isEmpty() ? notify.getFrom() : notify.getGroup();
			var list = ReceivedMessages.computeIfAbsent(
					new MessageTarget(target, notify.getDeparmentId()),
					k -> new ArrayList<>());
			list.add(notify);
			return true;
		}
	}

	private void addWindow(Window layer) {
		Windows.add(layer);
		refresh();
	}

	private void refresh() {
		System.out.print("You Are '" + Self + "' Window=");
		for (var layer : Windows) {
			System.out.print("/");
			System.out.print(layer.Name);
		}
		System.out.println();
		Windows.get(Windows.size() - 1).refresh();
		System.out.print(">");
	}
}
