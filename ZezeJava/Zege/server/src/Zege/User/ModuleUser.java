package Zege.User;

import java.io.IOException;
import java.security.GeneralSecurityException;
import Zege.Friend.BFriend;
import Zege.Friend.BGroupMember;
import Zeze.Net.Binary;
import Zeze.Netty.HttpExchange;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Cert;
import Zeze.Util.Random;
import io.netty.handler.codec.http.HttpContent;

public class ModuleUser extends AbstractModule {
    public void Start(Zege.App app) {
    }

    public void Stop(Zege.App app) {
    }

    public BUser create(String account) {
        if (_tUser.contains(account))
            return null;
        return _tUser.getOrAdd(account);
    }

    public BUser get(String account) {
        return _tUser.get(account);
    }

    public BUser selectDirty(String account) {
        return _tUser.selectDirty(account);
    }

    public boolean containsKey(String account) {
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
    protected long ProcessCreateRequest(Zege.User.Create r) throws GeneralSecurityException, IOException {
        var account = r.Argument.getAccount();
        var user = _tUser.getOrAdd(account);

        var now = System.currentTimeMillis();
        if (now - user.getPrepareTime() > 15 * 60 * 1000)
            return ErrorCode(ePrepareExpired);

        user.setCreateTime(System.currentTimeMillis());
        user.setAccount(account);
        var publicKey = Cert.loadPublicKeyByPkcs1(r.Argument.getRsaPublicKey().bytesUnsafe());
        var passwd = "123";
        if (!Cert.verifySign(publicKey, user.getPrepareRandomData().bytesUnsafe(), r.Argument.getSigned().bytesUnsafe()))
            return ErrorCode(ePrepareNotOwner);

        user.setState(BUser.StateCreated);
        user.setPrepareRandomData(Binary.Empty);

        var keyStore = App.FakeCa;
        var privateKey = Cert.getPrivateKey(keyStore, passwd, "ZegeFakeCa");
        var cert = Cert.generate(account, publicKey, "ZegeFakeCa", privateKey, 10000);
        var certEncoded = new Binary(cert.getEncoded());
        user.setCert(certEncoded);
        r.Result.setLastCertIndex(user.getLastCertIndex());
        user.setLastCertIndex(user.getLastCertIndex() + 1);
        r.Result.setCert(certEncoded);

        Transaction.whileCommit(r::SendResult);

        return Procedure.Success;
    }

    @Override
    protected long ProcessCreateWithCertRequest(Zege.User.CreateWithCert r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessVerifyChallengeResultRequest(Zege.User.VerifyChallengeResult r) throws GeneralSecurityException {
        // 【注意】此时还没有验证通过
        // 【注意】这条协议是linkd直接转发过来的，没有Session。
        var account = r.Argument.getAccount();
        var user = _tUser.getOrAdd(account);
        var cert = Cert.loadCertificate(user.getCert().bytesUnsafe());
        if (!Cert.verifySign(cert.getPublicKey(), r.Argument.getRandomData().bytesUnsafe(), r.Argument.getSigned().bytesUnsafe()))
            r.setResultCode(1);
        Transaction.whileCommit(r::SendResult);

        if (r.getResultCode() == 0) {
            // 【准备测试数据】
            // 把用户加入默认群，并且把群加入用户好友列表。
            var defaultGroup = "wanmei@group";
            var groupUser = _tUser.get(defaultGroup);
            if (null == groupUser) {
                groupUser = _tUser.getOrAdd(defaultGroup);
                groupUser.setNick("完美");
                for (int i = 0; i < 48; ++i) {
                    _tUser.getOrAdd("user" + i).setNick("user nick " + i);
                }
            }
            // add friends
            for (int i = 0; i < 48; ++i) {
                App.Zege_Friend.getFriends(account).getOrAdd("user" + i);
                App.Zege_Friend.getFriends("user" + i).getOrAdd(account);
            }
            Zeze.Util.Task.run(() -> App.Zege_Friend.getFriends(account).walk((id, value) -> true), "");
            // join group
            var group = App.Zege_Friend.getGroup(defaultGroup);
            group.create().setRoot(account);
            group.getGroupMembers().put(account, new BGroupMember());
            App.Zege_Friend.getFriends(account).put(defaultGroup, new BFriend());
        }
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
        var account = r.Argument.getAccount();
        var user = _tUser.getOrAdd(account);
        if (user.getState() == BUser.StateCreated)
            return ErrorCode(eAccountHasUsed);

        var now = System.currentTimeMillis();
        if (now - user.getPrepareTime() < 15 * 60 * 1000)
            return ErrorCode(eAccountHasPrepared);

        user.setState(BUser.StatePrepare);
        user.setPrepareTime(now);
        var rands = new byte[64];
        Random.getInstance().nextBytes(rands);
        user.setPrepareRandomData(new Binary(rands));

        r.Result.setRandomData(new Binary(rands));
        r.SendResult();
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleUser(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
