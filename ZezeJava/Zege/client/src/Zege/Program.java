package Zege;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import Zege.App;
import Zege.Friend.BFriend;
import Zege.Friend.BFriendNode;
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

	private ArrayList<Layer> Layers = new ArrayList<>();
	private MainLayer Main;
	public String Self;

	public void run(String[] args) throws Throwable {
		var app = App.Instance;
		app.Start(null, 0);
		try {
			app.Connector.WaitReady();
			var account = getComputerName().toLowerCase(Locale.ROOT);
			app.Zege_Linkd.auth(account).await();
			app.Zeze_Builtin_Online.login("PC").await();
			Main = new MainLayer();
			Layers.add(Main);
			refresh();
			Self = account;
			var cmd = "";
			Main.process(cmd);
		} finally {
			app.Stop();
		}
	}

	public class Layer {
		public String Name;

		public boolean process(String line) {
			if (line.isEmpty()) {
				refresh();
				return true;
			}

			var cmd = line.split(" ");
			switch (cmd[0])
			{
			case "b": case "back":
				if (cmd.length > 1) {
					var find = Layers.indexOf(cmd[1]);
					if (find >= 0) {
						for (int i = find + 1; i < Layers.size(); ++i)
							Layers.remove(i);
						refresh();
						return true;
					}
				} else if (Layers.size() > 1) {
					Layers.remove(Layers.size() - 1);
					refresh();
					return true;
				}
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
		var current = Layers.get(Layers.size() - 1);
		if (current.processNotifyMessage(r.Argument))
			return;
		Main.processNotifyMessage(r.Argument);
	}

	public class ChatLayer extends Layer {
		public String Target;
		public ChatLayer(String target) {
			this.Target = target;
			Name = "chat:" + target;
		}

		@Override
		public boolean process(String line) {
			// 聊天消息先处理基类命令。
			if (super.process(line))
				return true;

			App.Instance.Zege_Message.send(Target, line).await();
			return true;
		}

		@Override
		public boolean processNotifyMessage(BMessage notify) {
			if (notify.getGroup().isEmpty()) {
				// user chat
				if (!notify.getFrom().equals(Target)) {
					return false;
				}
				var bb = ByteBuffer.Wrap(notify.getSecureMessage());
				var bMsg = new BTextMessage();
				bMsg.Decode(bb);
				System.out.println(bMsg.getMessage());
				return true;
			} else {
				if (!notify.getGroup().equals(Target))
					return false;
				var bb = ByteBuffer.Wrap(notify.getSecureMessage());
				var bMsg = new BTextMessage();
				bMsg.Decode(bb);
				System.out.println(bMsg.getMessage());
				return true;
			}
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

	public class MainLayer extends Layer {
		public MainLayer() {
			Name = "main";
		}

		public long FriendNodeId;
		public BFriendNode FriendNode;

		public BFriend find(String value) {
			for (var friend : FriendNode.getFriends()) {
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
			case "chat":
				if (cmd.length > 1) {
					var target = cmd[1];
					if (null != find(target)) {
						addLayer(new ChatLayer(target));
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
			FriendNode = App.Instance.Zege_Friend.getFriendNode(FriendNodeId);
			for (var friend : FriendNode.getFriends()) {
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

	private void addLayer(Layer layer) {
		Layers.add(layer);
		refresh();
	}

	private void refresh() {
		System.out.print("path: ");
		for (var layer : Layers) {
			System.out.print("/");
			System.out.print(layer.Name);
		}
		System.out.println();
		Layers.get(Layers.size() - 1).refresh();
	}
}
