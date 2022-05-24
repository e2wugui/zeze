package Zege.Friend;

import Zeze.Arch.ProviderUserSession;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Transaction.Procedure;

public class ModuleFriend extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    public DepartmentTree<BManager, BMember, BDepartmentMember> getDepartmentTree(String group) {
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
        var peer = getFriends(r.Argument.getAccount());
        self.put(r.Argument.getAccount(), new BFriend());
        peer.put(session.getAccount(), new BFriend());
        session.SendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessCreateDepartmentRequest(Zege.Friend.CreateDepartment r) {
        var session = ProviderUserSession.get(r);
        var group = getDepartmentTree(r.Argument.getGroup());
        r.Result.setId(group.createDepartment(r.Argument.getParentDepartment(), r.Argument.getName()));
        session.SendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessDeleteDepartmentRequest(Zege.Friend.DeleteDepartment r) {
        var session = ProviderUserSession.get(r);
        var group = getDepartmentTree(r.Argument.getGroup());
        var result = group.deleteDepartment(r.Argument.getId(), true);
        r.setResultCode(result ? 0 : -1);
        session.SendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetDepartmentNodeRequest(Zege.Friend.GetDepartmentNode r) {
        var session = ProviderUserSession.get(r);
        var group = getDepartmentTree(r.Argument.getGroup());
        var department = group.getDepartment(r.Argument.getId());
        r.Result.setParentDepartment(department.getParentDepartment());
        r.Result.setName(department.getName());
        r.Result.getChilds().putAll(department.getChilds());
        for (var manager : department.getManagers()) {
            r.Result.getManagers().put(manager.getKey(), (BManager)manager.getValue().getBean());
        }
        session.SendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetFriendNodeRequest(Zege.Friend.GetFriendNode r) {
        var session = ProviderUserSession.get(r);
        var friends = getFriends(session.getAccount());
        var friendNode = r.Argument.getNodeId() == 0 ? friends.getFristNode() : friends.getNode(r.Argument.getNodeId());
        r.Result.setNextNodeId(friendNode.getNextNodeId());
        r.Result.setPrevNodeId(friendNode.getPrevNodeId());
        for (var friend : friendNode.getValues()) {
            r.Result.getFriends().add((BFriend)friend.getValue().getBean());
        }
        session.SendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessMoveDepartmentRequest(Zege.Friend.MoveDepartment r) {
        var session = ProviderUserSession.get(r);
        var group = getDepartmentTree(r.Argument.getGroup());
        var result = group.moveDepartment(r.Argument.getId(), r.Argument.getNewParent());
        r.setResultCode(result ? 0 : -1);
        session.SendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetGroupRootRequest(Zege.Friend.GetGroupRoot r) {
        var session = ProviderUserSession.get(r);
        var group = getDepartmentTree(r.Argument.getGroup());
        var root = group.getRoot();
        r.Result.setRoot(root.getRoot());
        r.Result.getChilds().putAll(root.getChilds());
        for (var manager : root.getManagers()) {
            r.Result.getManagers().put(manager.getKey(), (BManager)manager.getValue().getBean());
        }
        session.SendResponseWhileCommit(r);
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFriend(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
