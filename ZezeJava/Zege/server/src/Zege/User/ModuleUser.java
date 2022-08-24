package Zege.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import Zege.Friend.BFriend;
import Zege.Friend.BMember;
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

    public BUser getAccount(String account) {
        return _tUser.get(account);
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
    protected long ProcessCreateRequest(Zege.User.Create r) throws GeneralSecurityException, IOException {
        var account = r.Argument.getAccount();
        var user = _tUser.getOrAdd(account);
        // todo verify prepare 状态，防止过期。
        user.setCreateTime(System.currentTimeMillis());
        user.setAccount(account);
        var publicKey = Cert.loadPublicKey(r.Argument.getRsaPublicKey().bytesUnsafe());
        var passwd = "123";
        // todo verify client has private key。
        //if (Cert.verifySign(publicKey, user.getPrepareRandomData(); r.Argument.getSigned().bytesUnsafe()));
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
            var group = App.Zege_Friend.getGroup(defaultGroup);
            group.create();
            var member = new BMember();
            group.getGroupMembers().put(account, member);
            var friend = new BFriend();
            App.Zege_Friend.getFriends(account).put(defaultGroup, friend);
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
        /*
        user.state 1 pending 2 createok
        user.timexxx ;
        //user.sessionid?;
        */
        // todo 记住random，用来在create时verify客户端拥有privateKey。
        var rands = new byte[64];
        Random.getInstance().nextBytes(rands);
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
