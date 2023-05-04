package Zege.Friend;

import Zege.Notify.BNotify;
import Zege.User.BUser;
import Zeze.Arch.ProviderUserSession;
import Zeze.Builtin.Collections.LinkedMap.BLinkedMapNodeKey;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Component.AutoKey;
import Zeze.Net.Binary;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutLong;

public class ModuleFriend extends AbstractModule {
	private AutoKey GroupIdAutoKey;

	public static final String eFriendsLinkedMapNameEndsWith = "@Zege.Friend";
	public static final String eTopmostLinkedMapNameEndsWith = "@Zege.Topmost";

	private void onChangeListener(Object key, Changes.Record r) {
		var nodeKey = (BLinkedMapNodeKey)key; // 这里带了LinkedMap#Name
		var indexOf = nodeKey.getName().lastIndexOf('@');
		var account = nodeKey.getName().substring(0, indexOf);
		var notify = new FriendNodeLogBeanNotify();
		var encoded = App.LinkedMaps.encodeChangeListenerWithSpecialName(nodeKey.getName(), key, r);
		notify.Argument.setChangeLog(new Binary(encoded));
		App.Provider.getOnline().sendAccount(account, notify); // TODO online sender
	}

	public void Start(Zege.App app) throws Exception {
		GroupIdAutoKey = app.getZeze().getAutoKey("Zege.GroupId");
		App.LinkedMaps.NodeListeners.put(eFriendsLinkedMapNameEndsWith, this::onChangeListener);
		App.LinkedMaps.NodeListeners.put(eTopmostLinkedMapNameEndsWith, this::onChangeListener);
	}

	public void Stop(Zege.App app) throws Exception {
		App.LinkedMaps.NodeListeners.remove(eFriendsLinkedMapNameEndsWith);
		App.LinkedMaps.NodeListeners.remove(eTopmostLinkedMapNameEndsWith);
	}

	public DepartmentTree<BManager, BGroupMember, BDepartmentMember, BGroupData, BDepartmentData> getGroup(String group) {
		return App.DepartmentTrees.open(
				group + "@Zege.Group",
				BManager.class,
				BGroupMember.class,
				BDepartmentMember.class,
				BGroupData.class,
				BDepartmentData.class);
	}

	public LinkedMap<BFriend> getFriends(String owner) {
		return App.LinkedMaps.open(owner + eFriendsLinkedMapNameEndsWith, BFriend.class, App.ZegeConfig.FriendCountPerNode);
	}

	public LinkedMap<BFriend> getTopmosts(String owner) {
		return App.LinkedMaps.open(owner + eTopmostLinkedMapNameEndsWith, BFriend.class, App.ZegeConfig.FriendCountPerNode);
	}

	/*
	 * 现在好友是双向的。被对方删除，自己会知道。
	 * 为了能查阅旧的聊天历史，自己这一方不会删除，而是设置标志。
	 */
	public boolean isFriend(String owner, String friend) {
		var bf = getFriends(owner).get(friend);
		if (bf != null && bf.getState() == BFriend.eNormal)
			return true;

		var tf = getTopmosts(owner).get(friend);
		return tf != null && tf.getState() == BFriend.eNormal;
	}

	@Override
	protected long ProcessAcceptFriendRequest(Zege.Friend.AcceptFriend r) {
		var session = ProviderUserSession.get(r);

		// 首先尝试设置对方的好友状态，如果失败，表示对方取消了申请，那么接受好友也失败。
		if (!trySetFriendState(r.Argument.getAccount(), session.getAccount(), BFriend.eNormal))
			return errorCode(eAcceptFromNotExist);
		// todo 此时是否应该需要删除notify，【注意】这里返回错误码了，事务会回滚，需要删除的话，需要特别处理错误。

		// 增加申请者到自己的好友列表
		// 首先检查申请者是否已经存在，存在的话仅修改状态（目前想到，双方同时添加对方会出现这个），
		// 都不存在最后才添加。
		if (!trySetFriend(session.getAccount(), r.Argument.getAccount(), BFriend.eNormal, r.Argument.getMemo())) {
			// 第一次申请
			var firstAccept = getFriends(session.getAccount()).getOrAdd(r.Argument.getAccount());
			firstAccept.setState(BFriend.eNormal);
			firstAccept.setMemo(r.Argument.getMemo());
		}
		// 请求已经接受，删除通知。
		App.Zege_Notify.getNotify(session.getAccount()).remove(makeNotifyId(r.Argument.getAccount()));
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessDenyFriendRequest(Zege.Friend.DenyFriend r) {
		var session = ProviderUserSession.get(r);

		// 拒绝时，尝试设置双方的好友状态，忽略不存在的情况。
		trySetFriendState(session.getAccount(), r.Argument.getAccount(), BFriend.eDeny);
		trySetFriendState(r.Argument.getAccount(), session.getAccount(), BFriend.eDeny);

		App.Zege_Notify.getNotify(session.getAccount()).remove(makeNotifyId(r.Argument.getAccount()));

		// 总是成功。
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	public static String makeNotifyId(String account) {
		return "AddFriend@" + account;
	}

	@Override
	protected long ProcessAddFriendRequest(Zege.Friend.AddFriend r) {
		var session = ProviderUserSession.get(r);
		var self = getFriends(session.getAccount());

		// 参数检查
		if (!App.Zege_User.containsKey(r.Argument.getAccount()))
			return errorCode(eUserNotFound);

		if (isFriend(session.getAccount(), r.Argument.getAccount()))
			return errorCode(eAlreadyIsFriend);

		if (r.Argument.getAccount().endsWith("@group")) {
			// 添加群，todo，如果目标需要群主批准，需要走审批流程
			var peer = getGroup(r.Argument.getAccount()).getGroupMembers();
			self.getOrAdd(r.Argument.getAccount()).setMemo(r.Argument.getMemo());
			// 虽然互相添加的代码看起来一样，但group members bean类型和下面的好友bean类型不一样，所以需要分开写。
			peer.getOrAdd(session.getAccount());
		} else {
			// 添加好友，双向添加。
			var bf = self.getOrAdd(r.Argument.getAccount());
			bf.setMemo(r.Argument.getMemo());
			bf.setState(BFriend.eInvite);

			var notify = new BNotify();
			var nick = App.Zege_User.get(session.getAccount()).getNick();
			notify.setTitle(nick.isEmpty() ? session.getAccount() : nick);
			notify.setType(BNotify.eTypeAddFriend);
			notify.getProperties().put("from", session.getAccount());
			App.Zege_Notify.getNotify(r.Argument.getAccount()).put(makeNotifyId(session.getAccount()), notify);
		}
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	private boolean trySetFriendState(String owner, String friend, int state) {
		var peer = getFriends(owner);
		var pf = peer.get(friend);
		if (null != pf) {
			pf.setState(state);
			return true;
		}
		var tops = getTopmosts(owner);
		pf = tops.get(owner);
		if (null != pf) {
			pf.setState(state);
			return true;
		}
		return false;
	}

	private boolean trySetFriend(String owner, String friend, int state, String memo) {
		var peer = getFriends(owner);
		var pf = peer.get(friend);
		if (null != pf) {
			pf.setState(state);
			pf.setMemo(memo);
			return true;
		}
		var tops = getTopmosts(owner);
		pf = tops.get(owner);
		if (null != pf) {
			pf.setState(state);
			pf.setMemo(memo);
			return true;
		}
		return false;
	}

	@Override
	protected long ProcessCreateDepartmentRequest(Zege.Friend.CreateDepartment r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());

		// permission
		r.setResultCode(group.checkManagePermission(session.getAccount(), r.Argument.getDepartmentId()));
		if (r.getResultCode() != 0)
			return r.getResultCode();

		// create
		var out = new OutLong();
		r.setResultCode(group.createDepartment(r.Argument.getDepartmentId(), r.Argument.getName(),
				App.ZegeConfig.DepartmentChildrenLimit, out));
		r.Result.setDepartmentId(out.value);

		session.sendResponseWhileCommit(r);
		// todo 创建部门客户端自己根据rpc结果更新数据？这样的话需要在Result里带上新创建的部门的数据。
		// todo 或者重新刷新一次parent-department？
		return Procedure.Success;
	}

	@Override
	protected long ProcessDeleteDepartmentRequest(Zege.Friend.DeleteDepartment r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());
		// permission
		r.setResultCode(group.checkManagePermission(session.getAccount(), r.Argument.getDepartmentId()));
		if (r.getResultCode() != 0)
			return r.getResultCode();

		// delete
		r.setResultCode(group.deleteDepartment(r.Argument.getDepartmentId(), true));
		session.sendResponseWhileCommit(r);

		// 客户端根据rpc结果自己修改（同步）部门树。
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetDepartmentNodeRequest(Zege.Friend.GetDepartmentNode r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());
		var department = group.getDepartmentTreeNode(r.Argument.getDepartmentId());
		if (null == department)
			return errorCode(eDepartmentNotFound);

		// 读取组织架构，只验证是否群成员。
		if (group.getGroupMembers().get(session.getAccount()) == null)
			return errorCode(eNotGroupMember);

		r.Result.setParentDepartment(department.getParentDepartment());
		r.Result.setName(department.getName());
		r.Result.getChilds().putAll(department.getChilds());
		for (var manager : department.getManagers()) {
			r.Result.getManagers().put(manager.getKey(), (BManager)manager.getValue().getBean());
		}
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	private LinkedMap<BFriend> getFriendsOrTopmost(String account, String endsWith) {
		if (endsWith.equals(eFriendsLinkedMapNameEndsWith))
			return getFriends(account);
		if (endsWith.equals(eTopmostLinkedMapNameEndsWith))
			return getTopmosts(account);
		throw new UnsupportedOperationException();
	}

	@Override
	protected long ProcessGetFriendNodeRequest(Zege.Friend.GetFriendNode r) {
		var session = ProviderUserSession.get(r);
		var friends = getFriendsOrTopmost(session.getAccount(), r.Argument.getLinkedMapNameEndsWith());
		var nodeId = new OutLong(r.Argument.getNodeId());
		var friendNode = nodeId.value == 0 ? friends.getFirstNode(nodeId) : friends.getNode(nodeId.value);
		if (null == friendNode)
			return errorCode(eFriendNodeNotFound);

		r.Result.setNodeKey(new BLinkedMapNodeKey(friends.getName(), nodeId.value));
		r.Result.setNode(friendNode);

		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessMoveDepartmentRequest(Zege.Friend.MoveDepartment r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());

		// permission 验证被移动部门管理权限
		r.setResultCode(group.checkManagePermission(session.getAccount(), r.Argument.getDepartmentId()));
		if (r.getResultCode() != 0)
			return r.getResultCode();

		// 验证拥有目标部门管理权限，
		r.setResultCode(group.checkManagePermission(session.getAccount(), r.Argument.getNewParent()));
		if (r.getResultCode() != 0)
			return r.getResultCode();

		// 执行移动。
		r.setResultCode(group.moveDepartment(r.Argument.getDepartmentId(), r.Argument.getNewParent()));
		session.sendResponseWhileCommit(r);
		// 客户端根据rpc结果自己修改（同步）部门树。
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetGroupRootRequest(Zege.Friend.GetGroupRoot r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());
		var root = group.getRoot();

		if (r.Argument.getDepartmentId() != 0)
			return errorCode(eParameterError);

		// 读取组织架构，只验证是否群成员。
		if (group.getGroupMembers().get(session.getAccount()) == null)
			return errorCode(eNotGroupMember);

		r.Result.setRoot(root.getRoot());
		r.Result.getChilds().putAll(root.getChilds());
		for (var manager : root.getManagers()) {
			r.Result.getManagers().put(manager.getKey(), (BManager)manager.getValue().getBean());
		}
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetDepartmentMemberNodeRequest(Zege.Friend.GetDepartmentMemberNode r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());
		// 读取组织架构，只验证是否群成员。
		// todo 需求：秘密部门增加验证部门成员
		if (group.getGroupMembers().get(session.getAccount()) == null)
			return errorCode(eNotGroupMember);

		var nodeId = new OutLong(r.Argument.getNodeId());
		var node = r.Argument.getNodeId() == 0
				? group.getDepartmentMembers(r.Argument.getDepartmentId()).getFirstNode(nodeId)
				: group.getDepartmentMembers(r.Argument.getDepartmentId()).getNode(r.Argument.getNodeId());

		if (null == node)
			return errorCode(eMemberNodeNotFound);

		r.Result.setNodeId(nodeId.value);
		r.Result.setNode(node);

		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetGroupMemberNodeRequest(Zege.Friend.GetGroupMemberNode r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());
		// 读取组织架构，只验证是否群成员。
		if (group.getGroupMembers().get(session.getAccount()) == null)
			return errorCode(eNotGroupMember);

		var nodeId = new OutLong(r.Argument.getNodeId());
		var node = r.Argument.getNodeId() == 0
				? group.getGroupMembers().getFirstNode(nodeId)
				: group.getGroupMembers().getNode(r.Argument.getNodeId());
		if (null == node)
			return errorCode(eMemberNodeNotFound);

		r.Result.setNodeId(nodeId.value);
		r.Result.setNode(node);

		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessAddDepartmentMemberRequest(Zege.Friend.AddDepartmentMember r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());

		r.setResultCode(group.checkManagePermission(session.getAccount(), r.Argument.getDepartmentId()));
		if (r.getResultCode() != 0)
			return r.getResultCode();

		var groupMembers = group.getGroupMembers();
		var groupMember = groupMembers.get(r.Argument.getAccount());
		if (r.Argument.getDepartmentId() == 0) {
			if (groupMembers.size() > App.ZegeConfig.GroupInviteLimit) {
				// todo 群成员比较多，进入邀请确认模式。
			} else {
				// 直接加入成功
				groupMembers.getOrAdd(r.Argument.getAccount());
			}
		} else {
			if (null == groupMember)
				return errorCode(eDeparmentMemberNotInGroup);
			if (groupMember.getBelongDepartments().size() > App.ZegeConfig.BelongDepartmentLimit)
				return errorCode(eToomanyBelongDepartments);
			groupMember.getBelongDepartments().add(r.Argument.getDepartmentId());
			group.getDepartmentMembers(r.Argument.getDepartmentId()).getOrAdd(r.Argument.getAccount());
		}
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	/**
	 * @param group        x
	 * @param account      x
	 * @param departmentId 为0时，完全从群中退出，否则仅退出部门。
	 */
	private static void quitMember(DepartmentTree<BManager, BGroupMember, BDepartmentMember, BGroupData, BDepartmentData> group,
								   String account, long departmentId) {
		var groupMembers = group.getGroupMembers();
		var groupMember = groupMembers.get(account);
		if (departmentId == 0) {
			for (var belong : groupMember.getBelongDepartments()) {
				group.getDepartmentMembers(belong).remove(account);
			}
			groupMembers.remove(account);
		} else {
			groupMember.getBelongDepartments().remove(departmentId);
			group.getDepartmentMembers(departmentId).remove(account);
		}
	}

	@Override
	protected long ProcessDelDepartmentMemberRequest(Zege.Friend.DelDepartmentMember r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());

		r.setResultCode(group.checkManagePermission(session.getAccount(), r.Argument.getDepartmentId()));
		if (r.getResultCode() != 0)
			return r.getResultCode();
		quitMember(group, r.Argument.getAccount(), r.Argument.getDepartmentId());
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessAddManagerRequest(Zege.Friend.AddManager r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());

		// 管理员管理权限检查
		r.setResultCode(group.checkParentManagePermission(session.getAccount(), r.Argument.getDepartmentId()));
		if (r.getResultCode() != 0)
			return r.getResultCode();

		group.getOrAddManager(r.Argument.getDepartmentId(), r.Argument.getAccount()).assign(r.Argument.getManager());
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessDeleteManagerRequest(Zege.Friend.DeleteManager r) {
		var session = ProviderUserSession.get(r);
		var group = getGroup(r.Argument.getGroup());

		// 管理员管理权限检查
		r.setResultCode(group.checkParentManagePermission(session.getAccount(), r.Argument.getDepartmentId()));
		if (r.getResultCode() != 0)
			return r.getResultCode();

		group.deleteManager(r.Argument.getDepartmentId(), r.Argument.getAccount());
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessCreateGroupRequest(Zege.Friend.CreateGroup r) {
		var session = ProviderUserSession.get(r);
		var groupId = GroupIdAutoKey.nextString() + "@group";
		var user = App.Zege_User.create(groupId);
		if (null != user) {
			user.setCreateTime(System.currentTimeMillis());
			user.setState(BUser.StateCreated);
			var group = getGroup(groupId);
			group.create().setRoot(session.getAccount());
			var members = group.getGroupMembers();
			for (var member : r.Argument.getMembers()) {
				members.put(member, new BGroupMember());
			}
			if (!r.Argument.getMembers().contains(session.getAccount()))// add self
				members.put(session.getAccount(), new BGroupMember());
			session.sendResponseWhileCommit(r);
			return Procedure.Success;
		}

		return errorCode(eUserExists);
	}

	@Override
	protected long ProcessGetPublicUserInfoRequest(Zege.Friend.GetPublicUserInfo r) {
		var session = ProviderUserSession.get(r);
		var account = r.Argument.getAccount();
		var user = App.Zege_User.get(account);
		if (null == user)
			return errorCode(eUserNotFound);

		r.Result.setAccount(r.Argument.getAccount());
		r.Result.setNick(user.getNick());
		r.Result.setLastCertIndex(user.getLastCertIndex());
		r.Result.setCert(user.getCert());
		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetPublicUserPhotoRequest(Zege.Friend.GetPublicUserPhoto r) {
		var session = ProviderUserSession.get(r);
		var account = r.Argument.getAccount();
		var userPhoto = App.Zege_User.getUserPhoto(account);
		if (null == userPhoto)
			return errorCode(eUserNotFound);

		r.Result.setAccount(r.Argument.getAccount());
		r.Result.setPhoto(userPhoto.getPhoto());

		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessDeleteFriendRequest(Zege.Friend.DeleteFriend r) {
		var session = ProviderUserSession.get(r);

		getFriends(session.getAccount()).remove(r.Argument.getAccount());
		getTopmosts(session.getAccount()).remove(r.Argument.getAccount());

		if (r.Argument.getAccount().endsWith("@group")) {
			// todo 确认群的退出流程。
			var group = getGroup(r.Argument.getAccount());
			quitMember(group, session.getAccount(), 0);
		} else {
			trySetFriendState(r.Argument.getAccount(), session.getAccount(), BFriend.eRemove);
		}

		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSetTopmostRequest(Zege.Friend.SetTopmost r) {
		var session = ProviderUserSession.get(r);

		if (r.Argument.isTopmost()) {
			var tops = getTopmosts(session.getAccount());
			if (tops.size() >= App.ZegeConfig.FriendCountPerNode)
				return errorCode(eTooManyTopmost);
			var friends = getFriends(session.getAccount());
			var willTopmost = friends.remove(r.Argument.getAccount());
			if (null == willTopmost)
				return errorCode(eNotFriend);
			tops.getOrAdd(r.Argument.getAccount()).assign(willTopmost);
		} else {
			var tops = getTopmosts(session.getAccount());
			var removeTopmost = tops.remove(r.Argument.getAccount());
			if (null == removeTopmost)
				return errorCode(eNotTopmost);
			var friends = getFriends(session.getAccount());
			friends.getOrAdd(r.Argument.getAccount()).assign(removeTopmost);
		}

		session.sendResponseWhileCommit(r);
		return Procedure.Success;
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFriend(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
