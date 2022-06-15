package Zege.Friend;

import Zeze.Arch.ProviderUserSession;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutLong;

public class ModuleFriend extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    public DepartmentTree<BManager, BMember, BDepartmentMember> getGroup(String group) {
        return App.DepartmentTrees.open(group + "@Zege.DepartmentTree", BManager.class, BMember.class, BDepartmentMember.class);
    }

    public LinkedMap<BFriend> getFriends(String owner) {
        return App.LinkedMaps.open(owner + "@Zege.Friend", BFriend.class);
    }

    @Override
    protected long ProcessAddFriendRequest(Zege.Friend.AddFriend r) {
        var session = ProviderUserSession.get(r);
        var self = getFriends(session.getAccount());
        if (!App.Zege_User.contains(r.Argument.getAccount()))
            return Procedure.LogicError;
        if (r.Argument.getAccount().endsWith("@group")) {
            var peer = getGroup(r.Argument.getAccount()).getGroupMembers();
            self.getOrAdd(r.Argument.getAccount()).setAccount(r.Argument.getAccount());
            // 虽然互相添加的代码看起来一样，但group members bean类型和下面的好友bean类型不一样，所以需要分开写。
            peer.getOrAdd(session.getAccount()).setAccount(session.getAccount());
        } else {
            var peer = getFriends(r.Argument.getAccount());
            self.getOrAdd(r.Argument.getAccount()).setAccount(r.Argument.getAccount());
            peer.getOrAdd(session.getAccount()).setAccount(session.getAccount());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessCreateDepartmentRequest(Zege.Friend.CreateDepartment r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());
        var out = new OutLong();
        r.setResultCode(group.createDepartment(r.Argument.getParentDepartment(), r.Argument.getName(), out));
        r.Result.setId(out.Value);
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessDeleteDepartmentRequest(Zege.Friend.DeleteDepartment r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());
        r.setResultCode(group.deleteDepartment(r.Argument.getId(), true));
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetDepartmentNodeRequest(Zege.Friend.GetDepartmentNode r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());
        var department = group.getDepartmentTreeNode(r.Argument.getId());
        if (null == department)
            return ErrorCode(ErrorDepartmentNotFound);
        r.Result.setParentDepartment(department.getParentDepartment());
        r.Result.setName(department.getName());
        r.Result.getChilds().putAll(department.getChilds());
        for (var manager : department.getManagers()) {
            r.Result.getManagers().put(manager.getKey(), (BManager)manager.getValue().getBean());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetFriendNodeRequest(Zege.Friend.GetFriendNode r) {
        var session = ProviderUserSession.get(r);
        var friends = getFriends(session.getAccount());
        var friendNode = r.Argument.getNodeId() == 0 ? friends.getFristNode() : friends.getNode(r.Argument.getNodeId());
        if (null == friendNode)
            return ErrorCode(ErrorFriendNodeNotFound);

        r.Result.setNextNodeId(friendNode.getNextNodeId());
        r.Result.setPrevNodeId(friendNode.getPrevNodeId());
        for (var friend : friendNode.getValues()) {
            r.Result.getFriends().add((BFriend)friend.getValue().getBean());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessMoveDepartmentRequest(Zege.Friend.MoveDepartment r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());
        r.setResultCode(group.moveDepartment(r.Argument.getId(), r.Argument.getNewParent()));
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetGroupRootRequest(Zege.Friend.GetGroupRoot r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());
        var root = group.getRoot();
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
        var node = r.Argument.getNodeId() == 0
                ? group.getDepartmentMembers(r.Argument.getDepartmentId()).getFristNode()
                : group.getDepartmentMembers(r.Argument.getDepartmentId()).getNode(r.Argument.getNodeId());

        if (null == node)
            return ErrorCode(ErrorMemberNodeNotFound);

        r.Result.setNextNodeId(node.getNextNodeId());
        r.Result.setPrevNodeId(node.getPrevNodeId());
        for (var friend : node.getValues()) {
            r.Result.getDepartmentMembers().add((BDepartmentMember)friend.getValue().getBean());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetGroupMemberNodeRequest(Zege.Friend.GetGroupMemberNode r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());
        var node = r.Argument.getNodeId() == 0
                ? group.getGroupMembers().getFristNode()
                : group.getGroupMembers().getNode(r.Argument.getNodeId());
        if (null == node)
            return ErrorCode(ErrorMemberNodeNotFound);
        r.Result.setNextNodeId(node.getNextNodeId());
        r.Result.setPrevNodeId(node.getPrevNodeId());
        for (var friend : node.getValues()) {
            r.Result.getMembers().add((BMember)friend.getValue().getBean());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessAddDepartmentMemberRequest(Zege.Friend.AddDepartmentMember r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());
        var groupMembers = group.getGroupMembers();
        if (r.Argument.getDepartmentId() == 0) {
            groupMembers.getOrAdd(r.Argument.getAccount()).setAccount(r.Argument.getAccount());
        } else {
            if (null == groupMembers.get(r.Argument.getAccount()))
                return ErrorCode(ErrorDeparmentMemberNotInGroup);
            group.getDepartmentMembers(r.Argument.getDepartmentId()).getOrAdd(r.Argument.getAccount()).setAccount(r.Argument.getAccount());
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
