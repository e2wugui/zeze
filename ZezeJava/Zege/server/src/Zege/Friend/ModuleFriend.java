package Zege.Friend;

import Zege.User.BUser;
import Zeze.Arch.ProviderUserSession;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Transaction.Procedure;
import Zeze.Util.OutLong;
import Zeze.Util.Random;
import org.apache.commons.codec.binary.Base64;

public class ModuleFriend extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    public DepartmentTree<BManager, BGroupMember, BDepartmentMember, BGroupData, BDepartmentData> getGroup(String group) {
        return App.DepartmentTrees.open(
                group + "@Zege.DepartmentTree",
                BManager.class,
                BGroupMember.class,
                BDepartmentMember.class,
                BGroupData.class,
                BDepartmentData.class);
    }

    public LinkedMap<BFriend> getFriends(String owner) {
        return App.LinkedMaps.open(owner + "@Zege.Friend", BFriend.class);
    }

    @Override
    protected long ProcessAddFriendRequest(Zege.Friend.AddFriend r) {
        var session = ProviderUserSession.get(r);
        var self = getFriends(session.getAccount());

        // 参数检查
        if (!App.Zege_User.containsKey(r.Argument.getAccount()))
            return ErrorCode(eUserNotFound);

        if (r.Argument.getAccount().endsWith("@group")) {
            // 添加群，todo，如果目标需要群主批准，需要走审批流程
            var peer = getGroup(r.Argument.getAccount()).getGroupMembers();
            self.getOrAdd(r.Argument.getAccount()).setMemo(r.Argument.getMemo());
            // 虽然互相添加的代码看起来一样，但group members bean类型和下面的好友bean类型不一样，所以需要分开写。
            peer.getOrAdd(session.getAccount());
        } else {
            // 添加好友，双向添加。todo 审批流程
            var peer = getFriends(r.Argument.getAccount());
            self.getOrAdd(r.Argument.getAccount()).setMemo(r.Argument.getMemo());
            peer.getOrAdd(session.getAccount());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
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
        r.Result.setDepartmentId(out.Value);

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
            return ErrorCode(eDepartmentNotFound);

        // 读取组织架构，只验证是否群成员。
        if (group.getGroupMembers().get(session.getAccount()) == null)
            return ErrorCode(eNotGroupMember);

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
            return ErrorCode(eFriendNodeNotFound);

        r.Result.setNextNodeId(friendNode.getNextNodeId());
        r.Result.setPrevNodeId(friendNode.getPrevNodeId());
        for (var friend : friendNode.getValues()) {
            var get = new BGetFriend();

            // fill from friend list
            var data = (BFriend)friend.getValue().getBean();
            get.setAccount(friend.getId());
            get.setMemo(data.getMemo());

            // fill from account
            var account = App.Zege_User.get(get.getAccount());
            get.setLastCertIndex(account.getLastCertIndex());
            get.setCert(account.getCert());
            get.setNick(account.getNick());

            r.Result.getFriends().add(get);
        }
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
            return ErrorCode(eParameterError);

        // 读取组织架构，只验证是否群成员。
        if (group.getGroupMembers().get(session.getAccount()) == null)
            return ErrorCode(eNotGroupMember);

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
            return ErrorCode(eNotGroupMember);

        var node = r.Argument.getNodeId() == 0
                ? group.getDepartmentMembers(r.Argument.getDepartmentId()).getFristNode()
                : group.getDepartmentMembers(r.Argument.getDepartmentId()).getNode(r.Argument.getNodeId());

        if (null == node)
            return ErrorCode(eMemberNodeNotFound);

        r.Result.setNextNodeId(node.getNextNodeId());
        r.Result.setPrevNodeId(node.getPrevNodeId());
        for (var member : node.getValues()) {
            var get = new BGetDepartmentMember();
            get.setAccount(member.getId());

            var data = (BDepartmentMember)member.getValue().getBean();
            var account = App.Zege_User.get(get.getAccount());

            get.setNick(data.getNick());
            get.setLastCertIndex(account.getLastCertIndex());
            get.setCert(account.getCert());

            r.Result.getDepartmentMembers().add(get);
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessGetGroupMemberNodeRequest(Zege.Friend.GetGroupMemberNode r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());
        // 读取组织架构，只验证是否群成员。
        if (group.getGroupMembers().get(session.getAccount()) == null)
            return ErrorCode(eNotGroupMember);

        var node = r.Argument.getNodeId() == 0
                ? group.getGroupMembers().getFristNode()
                : group.getGroupMembers().getNode(r.Argument.getNodeId());
        if (null == node)
            return ErrorCode(eMemberNodeNotFound);
        r.Result.setNextNodeId(node.getNextNodeId());
        r.Result.setPrevNodeId(node.getPrevNodeId());
        for (var member : node.getValues()) {
            var get = new BGetMember();
            get.setAccount(member.getId());

            var data = (BGroupMember)member.getValue().getBean();
            var account = App.Zege_User.get(get.getAccount());
            get.setNick(data.getNick());
            get.setLastCertIndex(account.getLastCertIndex());
            get.setCert(account.getCert());

            r.Result.getMembers().add(get);
        }
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
                return ErrorCode(eDeparmentMemberNotInGroup);
            if (groupMember.getBelongDepartments().size() > App.ZegeConfig.BelongDepartmentLimit)
                return ErrorCode(eToomanyBelongDepartments);
            groupMember.getBelongDepartments().add(r.Argument.getDepartmentId());
            group.getDepartmentMembers(r.Argument.getDepartmentId()).getOrAdd(r.Argument.getAccount());
        }
        session.sendResponseWhileCommit(r);
        return Procedure.Success;
    }

    @Override
    protected long ProcessDelDepartmentMemberRequest(Zege.Friend.DelDepartmentMember r) {
        var session = ProviderUserSession.get(r);
        var group = getGroup(r.Argument.getGroup());

        r.setResultCode(group.checkManagePermission(session.getAccount(), r.Argument.getDepartmentId()));
        if (r.getResultCode() != 0)
            return r.getResultCode();

        var groupMembers = group.getGroupMembers();
        var groupMember = groupMembers.get(r.Argument.getAccount());
        if (r.Argument.getDepartmentId() == 0) {
            for (var belong : groupMember.getBelongDepartments()) {
                group.getDepartmentMembers(belong).remove(r.Argument.getAccount());
            }
            groupMembers.remove(r.Argument.getAccount());
        } else {
            groupMember.getBelongDepartments().remove(r.Argument.getDepartmentId());
            group.getDepartmentMembers(r.Argument.getDepartmentId()).remove(r.Argument.getAccount());
        }
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

        group.getOrAddManager(r.Argument.getDepartmentId(), r.Argument.getAccount()).Assign(r.Argument.getManager());
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
        for (int i = 0; i < 255; ++i) {
            var randId = new byte[20]; // len same cert.serial
            Random.getInstance().nextBytes(randId);
            var groupId = Base64.encodeBase64String(randId) + "@group";
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
        }
        return ErrorCode(eUserExists);
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFriend(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
