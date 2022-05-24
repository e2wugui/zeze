package Zege.User;

import Zege.Friend.BDepartmentMember;
import Zege.Friend.BFriend;
import Zege.Friend.BMember;
import Zege.Friend.ModuleFriend;
import Zeze.Arch.ProviderUserSession;
import Zeze.Transaction.Procedure;

public class ModuleUser extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    public boolean contains(String account) {
        return _tUser.get(account) != null;
    }

    @Override
    protected long ProcessAuthRequest(Zege.Linkd.Auth r) {
        // 【注意】此时还没有验证通过

        var session = ProviderUserSession.get(r);
        var user = _tUser.getOrAdd(session.getAccount());
        if (user.getCreateTime() == 0) {
            user.setCreateTime(System.currentTimeMillis());
        }
        session.SendResponseWhileCommit(r);

        // 【准备测试数据】
        // 把用户加入默认群，并且把群加入用户好友列表。
        var defaultGroup = "wanmei@group";
        var group = App.Zege_Friend.getDepartmentTree(defaultGroup);
        var member = new BMember();
        member.setAccount(session.getAccount());
        group.getMembers().put(session.getAccount(), member);
        var friend = new BFriend();
        friend.setAccount(defaultGroup);
        App.Zege_Friend.getFriends(session.getAccount()).put(friend.getAccount(), friend);
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleUser(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
