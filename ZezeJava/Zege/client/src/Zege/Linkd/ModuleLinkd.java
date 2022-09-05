package Zege.Linkd;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import Zege.User.Create;
import Zege.User.ModuleUser;
import Zege.User.Prepare;
import Zeze.Net.Binary;
import Zeze.Transaction.Procedure;
import Zeze.Util.Cert;
import Zeze.Util.TaskCompletionSource;

public class ModuleLinkd extends AbstractModule {
    public void Start(Zege.App app) throws Throwable {
    }

    public void Stop(Zege.App app) throws Throwable {
    }

    @Override
    protected long ProcessKeepAlive(Zege.Linkd.KeepAlive p) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    private String account;
    private TaskCompletionSource<Boolean> authFuture;

    public void waitAuthed() {
        authFuture.await();
    }

    public void create(String account) throws GeneralSecurityException, IOException {
        this.account = account;
        authFuture = new TaskCompletionSource<>();

        var fileName = account + ".pkcs12";
        if (Files.exists(Path.of(fileName))) {
            new ChallengeMe().Send(App.Connector.TryGetReadySocket());
            return; // 已经注册。先简单用文件是否存在判断一下。
        }

        var p = new Prepare();
        p.Argument.setAccount(account);
        p.SendForWait(App.Connector.TryGetReadySocket()).await();
        if (p.getResultCode() == ModuleUser.eAccountHasUsed)
            return; // done

        if (p.getResultCode() != 0)
            throw new RuntimeException("Create Error! rc=" + p.getResultCode());

        var c = new Create();

        var rsa = Cert.generateRsaKeyPair();
        var sign = Cert.sign(rsa.getPrivate(), p.Result.getRandomData().bytesUnsafe());

        c.Argument.setAccount(account);
        c.Argument.setRsaPublicKey(new Binary(Cert.exportPublicKeyToPkcs1(rsa.getPublic())));
        c.Argument.setSigned(new Binary(sign));

        c.SendForWait(App.Connector.TryGetReadySocket()).await();
        if (c.getResultCode() != 0)
            throw new RuntimeException("Create Error! rc=" + c.getResultCode());

        var keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(null, null);
        var cert = Cert.loadCertificate(c.Result.getCert().bytesUnsafe());
        var passwd = "123".toCharArray();
        keyStore.setKeyEntry(account, rsa.getPrivate(), passwd, new Certificate[] { cert });
        keyStore.store(new FileOutputStream(fileName), passwd);

        // skip result
        new ChallengeMe().Send(App.Connector.TryGetReadySocket());
    }

    @Override
    protected long ProcessChallengeRequest(Zege.Linkd.Challenge r) throws GeneralSecurityException, IOException {
        r.Result.setAccount(account);
        var file = account + ".pkcs12";
        if (!Files.exists(Path.of(file))) {
            r.SendResultCode(1);
            return 0; // done
        }

        var passwd = "123";
        var keyStore = Cert.loadKeyStore(new FileInputStream(file), passwd);
        var privateKey = Cert.getPrivateKey(keyStore, passwd, account);

        var signed = Cert.sign(privateKey, r.Argument.getRandomData().bytesUnsafe());
        r.Result.setSigned(new Binary(signed));
        r.SendResult();

        return 0;
    }

    @Override
    protected long ProcessChallengeOkRequest(Zege.Linkd.ChallengeOk r) {
        authFuture.SetResult(true);
        r.SendResult();
        return Procedure.Success;
    }

    // ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleLinkd(Zege.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
