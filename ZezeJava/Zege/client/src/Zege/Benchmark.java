package Zege;

import Zege.Friend.BGetFriendNode;

public class Benchmark extends Thread {
	private BGetFriendNode friendNode;
	private volatile boolean running = true;

	public void stopAndJoin() {
		running = false;
		while (true) {
			try {
				this.join();
				return;
			} catch (InterruptedException e) {
				// skip
			}
		}
	}

	@Override
	public void run() {
		addFriendsInWanmeiGroup();
		var lastAddFriendsInWanmeiGroupTime = System.currentTimeMillis();
		while (running) {
			var now = System.currentTimeMillis();
			if (now - lastAddFriendsInWanmeiGroupTime > 30_000) {
				addFriendsInWanmeiGroup();
				lastAddFriendsInWanmeiGroupTime = now;
			}
			var rand = Zeze.Util.Random.getInstance().nextInt(1000);
			if (rand > 800)
				sendGroup(); // 20%
			else
				sendFriend(); // 80%
		}
	}

	private void addFriendsInWanmeiGroup() {
		var groupMemberNode = App.Instance.Zege_Friend.getGroupMemberNode("wanmei@group", 0);
		for (var member : groupMemberNode.getMembers()) {
			App.Instance.Zege_Friend.addFriend(member.getAccount()).await();
		}
		friendNode = App.Instance.Zege_Friend.getFriendNode(0);
	}

	private void sendFriend() {
		if (null == friendNode || friendNode.getNode().getValues().isEmpty())
			return;
		var rand = Zeze.Util.Random.getInstance().nextInt(friendNode.getNode().getValues().size());
		App.Instance.Zege_Message.send(friendNode.getNode().getValues().get(rand).getId(), null, 0).await();
	}

	private void sendGroup() {
		App.Instance.Zege_Message.send("wanmei@group", null, 0).await();
	}
}
