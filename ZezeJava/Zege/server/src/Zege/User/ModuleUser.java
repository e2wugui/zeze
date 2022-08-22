package Zege.User;

import Zege.Friend.BFriend;
import Zege.Friend.BMember;
import Zeze.Netty.HttpExchange;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import io.netty.handler.codec.http.HttpContent;

public class ModuleUser extends AbstractModule {
    public void Start(Zege.App app) {
    }

    public void Stop(Zege.App app) {
    }

    public boolean contains(String account) {
        return _tUser.get(account) != null;
    }

    @Override
    protected void OnServletBeginStreamUpload(HttpExchange x, long from, long to, long size) {

    }

    @Override
    protected void OnServletStreamContentUpload(HttpExchange x, HttpContent c) {

    }

    @Override
    protected void OnServletEndStreamUpload(HttpExchange x) {

    }

    @Override
    protected long ProcessCreateRequest(Zege.User.Create r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessCreateWithCertRequest(Zege.User.CreateWithCert r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessVerifyChallengeResultRequest(Zege.User.VerifyChallengeResult r) {
        // 【注意】此时还没有验证通过
        // 【注意】这条协议是linkd直接转发过来的，没有Session。
        var account = r.Argument.getAccount();
        var user = _tUser.getOrAdd(account);
        if (user.getCreateTime() == 0) {
            user.setCreateTime(System.currentTimeMillis());
        }
        Transaction.whileCommit(r::SendResult);

        // 【准备测试数据】
        // 把用户加入默认群，并且把群加入用户好友列表。
        var defaultGroup = "wanmei@group";
        var group = App.Zege_Friend.getGroup(defaultGroup);
        group.create();
        var member = new BMember();
        member.setAccount(account);
        group.getGroupMembers().put(account, member);
        var friend = new BFriend();
        friend.setAccount(defaultGroup);
        App.Zege_Friend.getFriends(account).put(friend.getAccount(), friend);
        return Procedure.Success;
    }

    @Override
    protected void OnServletCreate(HttpExchange x) throws Exception {
    }

    @Override
    protected void OnServletCreateWithCert(HttpExchange x) throws Exception {

    }

    @Override
    protected void OnServletPrepare(HttpExchange x) throws Exception {

    }

    @Override
    protected long ProcessPrepareRequest(Zege.User.Prepare r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleUser(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
