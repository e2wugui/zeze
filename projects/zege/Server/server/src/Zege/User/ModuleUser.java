package Zege.User;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;
import Zeze.Net.Binary;
import Zeze.Netty.HttpExchange;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import Zeze.Util.Cert;
import Zeze.Util.Random;

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

    public BUserPhoto getUserPhoto(String account) {
        return _tUserPhoto.get(account);
    }

    public BUser selectDirty(String account) {
        return _tUser.selectDirty(account);
    }

    public boolean containsKey(String account) {
        return _tUser.get(account) != null;
    }

    // 用owner的公钥为owner生成证书,并用issuer的私钥为证书签名,返回该证书
    public static X509Certificate generateRsaCert(String ownerName, PublicKey publicKey, String issuerName,
                                                  PrivateKey privateKeyForSign, int validDays)
            throws GeneralSecurityException, IOException {
        var certInfo = new X509CertInfo();
        certInfo.setVersion(new CertificateVersion(CertificateVersion.V3));
        certInfo.setSerialNumber(new CertificateSerialNumber(new BigInteger(160, new SecureRandom())));
        certInfo.setSubject(new X500Name("CN=" + ownerName));
        certInfo.setIssuer(new X500Name("CN=" + issuerName));
        var nowTime = System.currentTimeMillis();
        certInfo.setValidity(new CertificateValidity(new Date(nowTime), new Date(nowTime + validDays * 86400_000L)));
        certInfo.setKey(new CertificateX509Key(publicKey));
        certInfo.setAlgorithmId(new CertificateAlgorithmId(new AlgorithmId(AlgorithmId.SHA256withRSA_oid)));
        return X509CertImpl.newSigned(certInfo, privateKeyForSign, "SHA256withRSA");
    }

    public Binary generateCert(String account, PublicKey publicKey) throws Exception {
        var keyStore = App.CaKeyStore;
        var passwd = "123";
        var privateKey = Cert.getPrivateKey(keyStore, passwd, "ZegeCa");
        var cert = generateRsaCert(account, publicKey, "Zege", privateKey, 365 * 100);
        return new Binary(cert.getEncoded());
    }

    public Binary tryEncryptWithCert(BUser user, Binary data) throws Exception {
        if (user.getLastCertIndex() == -1 || user.getCert().size() == 0)
            return data;
        var cert = Cert.loadCertificate(user.getCert().copyIf());
        var encrypted = Cert.encryptRsa(cert.getPublicKey(), data.bytesUnsafe(), data.getOffset(), data.size());
        return new Binary(encrypted);
    }

    public Binary tryEncryptWithCert(String account, Binary data) throws Exception {
        var user = get(account);
        if (null == user)
            return data;
        return tryEncryptWithCert(user, data);
    }

    @Override
    protected long ProcessCreateRequest(Zege.User.Create r) throws Exception {
        var account = r.Argument.getAccount();
        var user = _tUser.getOrAdd(account);

        if (account.indexOf('@') >= 0)
            return errorCode(eAccountInvalid);

        var now = System.currentTimeMillis();
        if (now - user.getPrepareTime() > 15 * 60 * 1000)
            return errorCode(ePrepareExpired);

        user.setCreateTime(System.currentTimeMillis());
        user.setAccount(account);
        var publicKey = Cert.loadRsaPublicKeyByPkcs1(r.Argument.getRsaPublicKey().bytesUnsafe());
        if (!Cert.verifySignRsa(publicKey, user.getPrepareRandomData().bytesUnsafe(), r.Argument.getSigned().bytesUnsafe()))
            return errorCode(ePrepareNotOwner);

        user.setState(BUser.StateCreated);
        user.setPrepareRandomData(Binary.Empty);

        var certEncoded = generateCert(account, publicKey);
        user.setCert(certEncoded);
        user.setLastCertIndex(user.getLastCertIndex() + 1);

        r.Result.setLastCertIndex(user.getLastCertIndex());
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
        if (!Cert.verifySignRsa(cert.getPublicKey(), r.Argument.getRandomData().bytesUnsafe(), r.Argument.getSigned().bytesUnsafe()))
            r.setResultCode(1);
        Transaction.whileCommit(r::SendResult);
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
            return errorCode(eAccountHasUsed);

        var now = System.currentTimeMillis();
        if (now - user.getPrepareTime() < 15 * 60 * 1000)
            return errorCode(eAccountHasPrepared);

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
