package Zege.Friend;

import Zeze.Transaction.EmptyBean;
import Zeze.Util.TaskCompletionSource;

public class ModuleFriend extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    public TaskCompletionSource<EmptyBean> addFriend(String account) {
        var req = new AddFriend();
        req.Argument.setAccount(account);
        return req.SendForWait(App.Connector.TryGetReadySocket());
    }

    public BFriendNode getFriendNode(long nodeId) {
        var req = new GetFriendNode();
        req.Argument.setNodeId(nodeId);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        return req.getResultCode() == 0 ? req.Result : new BFriendNode();
    }

    public BMemberNode getGroupMemberNode(String group, long nodeId) {
        var req = new GetGroupMemberNode();
        req.Argument.setGroup(group);
        req.Argument.setNodeId(nodeId);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        return req.getResultCode() == 0 ? req.Result : new BMemberNode();
    }

    public BDepartmentMemberNode getDepartmentMemberNode(String group, long departmentId, long nodeId) {
        var req = new GetDepartmentMemberNode();
        req.Argument.setGroup(group);
        req.Argument.setDepartmentId(departmentId);
        req.Argument.setNodeId(nodeId);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        return req.getResultCode() == 0 ? req.Result : new BDepartmentMemberNode();
    }

    public BGroup getGroupRoot(String group) {
        var req = new GetGroupRoot();
        req.Argument.setGroup(group);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        return req.Result;
    }

    public BDepartmentId createDepartment(String group, long parent, String name) {
        var req = new CreateDepartment();
        req.Argument.setGroup(group);
        req.Argument.setParentDepartment(parent);
        req.Argument.setName(name);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        if (req.getResultCode() != 0)
            throw new RuntimeException("createDepartment faild.");
        return req.Result;
    }

    public void deleteDepartment(String group, long id) {
        var req = new DeleteDepartment();
        req.Argument.setGroup(group);
        req.Argument.setId(id);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        if (req.getResultCode() != 0)
            throw new RuntimeException("DeleteDepartment code=" + req.getResultCode());
    }

    public BDepartmentNode getDepartmentNode(String group, long id) {
        var req = new GetDepartmentNode();
        req.Argument.setGroup(group);
        req.Argument.setId(id);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        return req.getResultCode() == 0 ? req.Result : new BDepartmentNode();
    }

    public void moveDepartment(String group, long id, long newParent) {
        var req = new MoveDepartment();
        req.Argument.setGroup(group);
        req.Argument.setId(id);
        req.Argument.setNewParent(newParent);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        if (req.getResultCode() != 0)
            throw new RuntimeException("MoveDepartment code=" + req.getResultCode());
    }

    public void addDepartmentMember(String group, long departmentId, String account) {
        var req = new AddDepartmentMember();
        req.Argument.setGroup(group);
        req.Argument.setDepartmentId(departmentId);
        req.Argument.setAccount(account);
        req.SendForWait(App.Connector.TryGetReadySocket()).await();
        if (req.getResultCode() != 0)
            throw new RuntimeException("AddDepartmentMember code=" + req.getResultCode());
    }
    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFriend(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
