package Zege;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import javax.swing.GroupLayout;
import Zege.Friend.BFriend;
import Zege.Friend.BFriendNode;
import Zege.Friend.BMemberNode;
import Zege.Message.BMessage;
import Zege.Message.BTextMessage;
import Zege.Message.NotifyMessage;
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
					Windows.get(Windows.size() - 1).process(line);
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
						refresh();
						return true;
					}
				} else if (Windows.size() > 1) {
					Windows.remove(Windows.size() - 1);
					refresh();
					return true;
				}
				break;
			case "af":
				App.Instance.Zege_Friend.add(cmd[1]).await();
				break;
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

	public class GroupWindow extends Window {
		public String Group;
		public GroupWindow(String group) {
			this.Group = group;
			Name = "group:" + Group;
		}

		@Override
		public boolean process(String line) {
			// 聊天消息先处理基本命令。
			if (super.process(line))
				return true;

			App.Instance.Zege_Message.send(Group, line).await();
			return true;
		}

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			if (notify.getGroup().equals(Group)) {
				var bb = ByteBuffer.Wrap(notify.getSecureMessage());
				var bMsg = new BTextMessage();
				bMsg.Decode(bb);
				System.out.println(bMsg.getMessage());
				return true;
			}
			return false;
		}

		public long NodeId;
		public BMemberNode Node;

		@Override
		public void refresh() {
			Node = App.Instance.Zege_Friend.getGroupMemberNode(Group, NodeId);
			for (var member : Node.getMembers()) {
				System.out.println(member.getAccount());
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

			App.Instance.Zege_Message.send(Target, line).await();
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
			var list = Main.ReceivedMessages.remove(Target);
			if (null != list) {
				for (var notify : list)
					processNotifyMessage(notify);
			}
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
						addLayer(target.endsWith("@group") ? new GroupWindow(target) : new ChatWindow(target));
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
				var list = ReceivedMessages.get(friend);
				System.out.println(null == list ? "" : "(" + list.size() + ")");
			}
		}

		private HashMap<String, ArrayList<BMessage>> ReceivedMessages = new HashMap<>();

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			var target = notify.getGroup().isEmpty() ? notify.getFrom() : notify.getGroup();
			var list = ReceivedMessages.computeIfAbsent(target, k -> new ArrayList<>());
			list.add(notify);
			return true;
		}
	}

	private void addLayer(Window layer) {
		Windows.add(layer);
		refresh();
	}

	private void refresh() {
		System.out.print("self=" + Self + " window=");
		for (var layer : Windows) {
			System.out.print("/");
			System.out.print(layer.Name);
		}
		System.out.println();
		Windows.get(Windows.size() - 1).refresh();
		System.out.print(">");
	}
}
