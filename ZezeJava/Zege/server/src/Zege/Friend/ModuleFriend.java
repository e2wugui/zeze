package Zege.Friend;

public class ModuleFriend extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessAddFriendRequest(Zege.Friend.AddFriend r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessCreateDepartmentRequest(Zege.Friend.CreateDepartment r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessDeleteDepartmentRequest(Zege.Friend.DeleteDepartment r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessGetDepartmentNodeRequest(Zege.Friend.GetDepartmentNode r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessGetFriendNodeRequest(Zege.Friend.GetFriendNode r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessMoveDepartmentRequest(Zege.Friend.MoveDepartment r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleFriend(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
