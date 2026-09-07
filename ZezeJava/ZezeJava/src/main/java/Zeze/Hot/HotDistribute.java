package Zeze.Hot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import Zeze.Builtin.HotDistribute.AppendFile;
import Zeze.Builtin.HotDistribute.CloseFile;
import Zeze.Builtin.HotDistribute.Commit;
import Zeze.Builtin.HotDistribute.Commit2;
import Zeze.Builtin.HotDistribute.OpenFile;
import Zeze.Builtin.HotDistribute.PrepareDistribute;
import Zeze.Builtin.HotDistribute.TryDistribute;
import Zeze.Builtin.HotDistribute.TryRollback;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.FastLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 热更发布控制台。
 * 【其中，多版本bean方案的服务模块。未用，计划用直接bean支持热更方案替换，先保留】
 * 现在使用中的功能：拷贝jar的文件操作；全部拷贝完成提交命令；
 */
public class HotDistribute extends AbstractHotDistribute {
    private static final Logger logger = LogManager.getLogger(HotDistribute.class);
    private final DistributeManager distributeManager;
    private final FastLock lock = new FastLock();
    private final Condition cond = lock.newCondition();
    private int state = eIdle;
    private long distributeId = 0; // 由于发布不能并发，这个参数用于错误检查，没有实质作用。
    private TryDistribute tryDistribute;
    private Commit commit;

    // ePrepare上传会话（OpenFile/AppendFile/CloseFile）最近活动时间。
    // PrepareDistribute之后控制台崩溃/断链时state永停ePrepare且没有任何收敛路径
    // （setIdle只由安装流程调用），需要靠活动时间戳判死会话回收（见setPrepare）。
    private long prepareActiveTime = 0;
    // 上传会话无活动超时：超过即视为死会话（控制台崩溃/断链），允许下一轮发布回收。
    private static final long prepareTimeoutMillis = 10 * 60 * 1000;

    public HotDistribute(DistributeManager distributeManager) {
        this.distributeManager = distributeManager;
    }

    public long setPrepare(long distributeId) {
        lock.lock();
        try {
            if (state != eIdle) {
                // 仅ePrepare可判死回收：eTryDistribute及之后的state由安装流程
                // （tryDistribute的finally setIdle）收敛，正在安装时拒绝新会话是正确行为；
                // ePrepare是唯一没有收敛路径的state——控制台PrepareDistribute后崩溃/断链，
                // 后续所有发布恒收errorCode(ePrepare)，通道死锁到进程重启。
                if (state != ePrepare || System.currentTimeMillis() - prepareActiveTime < prepareTimeoutMillis)
                    return errorCode(ePrepare);
                // 死会话：复位为eIdle后按新会话继续（closeAll回收遗留的未CloseFile的FileBin）。
                logger.warn("HotDistribute prepare session {} dead (no activity {} ms), reclaim",
                        this.distributeId, prepareTimeoutMillis);
                state = eIdle;
                this.distributeId = 0;
            }

            // 新发布会话开始：上一会话崩溃/断链遗留的未CloseFile的FileBin在此回收。
            distributeManager.closeAll();
            state = ePrepare;
            this.distributeId = distributeId;
            prepareActiveTime = System.currentTimeMillis();
            return 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected long ProcessPrepareDistributeRequest(PrepareDistribute r) {
        r.SendResultCode(setPrepare(r.Argument.getDistributeId()));
        return 0;
    }

    public void sendTryDistributeResultAndWaitCommit(long rc) throws InterruptedException, TimeoutException {
        lock.lock();
        try {
            if (null != tryDistribute) {
                tryDistribute.SendResultCode(rc);
                tryDistribute = null;

                if (rc == 0) {
                    // 成功的结果才等待后续步骤。
                    while (state != eCommit && state != eTryRollback) {
                        if (!cond.await(10_000, TimeUnit.MILLISECONDS))
                            throw new TimeoutException("timeout"); // 超时不是中断，不能用InterruptedException伪装
                    }

                    if (state == eTryRollback)
                        throw new IllegalStateException();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected long ProcessTryDistributeRequest(TryDistribute r) {
        lock.lock();
        try {
            if (state != ePrepare)
                return errorCode(eTryDistribute);
            if (distributeId != r.Argument.getDistributeId())
                return errorCode(eDistributeIdMismatch);

            this.tryDistribute = r;
            state = eTryDistribute;
        } finally {
            lock.unlock();
        }
        try {
            distributeManager.commitDistribute(); // Files.createFile(ready)：ready已存在/IO错误时抛
        } catch (Exception ex) {
            // 此时state已置eTryDistribute且rpc r已存入this.tryDistribute，异常逃出handler会
            // 同时卡死状态机（tryDistribute安装流程未进入，setIdle永不执行，死到重启）与
            // rpc应答（控制台await超时误报）。setIdle一步完成应答+复位；
            // 返回错误码时派发层trySendResultCode已被setIdle的应答挡住（sendResultDone），不会重发。
			logger.error("commitDistribute fail. distributeId={}", distributeId, ex);
            setIdle(errorCode(eTryDistribute));
            return errorCode(eTryDistribute);
        }
        distributeManager.getHotManager().tryDistribute(r.Argument.isAtomicAll());
        // atomicAll 模式，在sendTryDistributeResultAndWaitCommit发送结果。
        // !atomicAll 模式，在setIdle发送结果。
        return 0;
    }

    @Override
    protected long ProcessTryRollbackRequest(TryRollback r) {
        lock.lock();
        try {
            if (state != eTryDistribute)
                return errorCode(eTryRollback);
            if (distributeId != r.Argument.getDistributeId())
                return errorCode(eDistributeIdMismatch);
            state = eTryRollback;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
        return 0;
    }

    @Override
    protected long ProcessCommitRequest(Commit r) {
        lock.lock();
        try {
            if (state != eTryDistribute)
                return errorCode(eCommit);
            if (distributeId != r.Argument.getDistributeId())
                return errorCode(eDistributeIdMismatch);

            state = eCommit;
            commit = r;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
        return 0;
    }

    public void sendCommitResultAndWaitCommit2(long rc) throws InterruptedException, TimeoutException {
        lock.lock();
        try {
            if (null != commit) {
                commit.SendResultCode(rc);
                commit = null;

                if (rc == 0) {
                    // 成功的结果才等待后续步骤。
                    while (state != eCommit2) {
                        if (!cond.await(10_000, TimeUnit.MILLISECONDS))
                            throw new TimeoutException("timeout"); // 超时不是中断，不能用InterruptedException伪装
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected long ProcessCommit2Request(Commit2 r) {
        lock.lock();
        try {
            if (state != eCommit)
                return errorCode(eCommit2);
            if (distributeId != r.Argument.getDistributeId())
                return errorCode(eDistributeIdMismatch);

            state = eCommit2;
            cond.signalAll();
        } finally {
            lock.unlock();
        }
        r.SendResult();
        return 0;
    }

    public void setIdle(long rc) {
        lock.lock();
        try {
            if (state == ePrepare) {
                // 本setIdle属于旧（本地ready定时器）安装流程的收尾，但安装期间远程控制台已开启
                // 新上传会话（ePrepare）：不能closeAll掉新会话上传中的FileBin、也不能复位新会话状态，
                // 仅应答滞留的tryDistribute/commit rpc（此场景正常为null）后直接返回。
                if (null != tryDistribute)
                    tryDistribute.SendResultCode(rc);
                tryDistribute = null;

                if (null != commit)
                    commit.SendResultCode(rc);
                commit = null;
                return;
            }
            // 会话结束：本会话中未走到CloseFile的残留FileBin（上传中断、md5失败）在此回收。
            distributeManager.closeAll();
            state = eIdle;
            distributeId = 0;

            if (null != tryDistribute)
                tryDistribute.SendResultCode(rc);
            tryDistribute = null;

            if (null != commit)
                commit.SendResultCode(rc);
            commit = null;
        } finally {
            lock.unlock();
        }
    }

    public static String removeVersion(String beanName) {
        if (!beanName.endsWith("_"))
            return beanName; // un-versioned

        var i = beanName.length() - 1;
        var count_ = 0;
        for (; i >= 0; --i) {
            if (beanName.charAt(i) == '_' && ++count_ == 2)
                break;
        }
        if (i < 0)
            throw new RuntimeException("invalid versioned bean name");
        return beanName.substring(0, i);
    }

    // ePrepare上传会话活动续期：OpenFile/AppendFile/CloseFile处理时在锁内刷新时间戳，
    // 供setPrepare的死会话判定（prepareTimeoutMillis内无活动）使用。
    private void touchPrepareActive() {
        lock.lock();
        try {
            prepareActiveTime = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected long ProcessOpenFileRequest(OpenFile r) throws Exception {
        touchPrepareActive();
        var fileBin = distributeManager.open(r.Argument.getFileName());
        r.Result.setOffset(fileBin.getLength());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessAppendFileRequest(AppendFile r) throws Exception {
        touchPrepareActive();
        distributeManager.append(
                r.Argument.getFileName(),
                r.Argument.getOffset(),
                r.Argument.getChunk());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessCloseFileRequest(CloseFile r) throws Exception {
        touchPrepareActive();
        if (!distributeManager.closeAndVerify(r.Argument.getFileName(), r.Argument.getMd5()))
            return errorCode(eMd5Mismatch);
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessGetLastVersionBeanInfoRequest(Zeze.Builtin.HotDistribute.GetLastVersionBeanInfo r) {
        var baseName = removeVersion(r.Argument.getName());
        var beans = DistributeServer.getBeans().tailMap(baseName);

        Bean lastVersion = null;
        for (var bean : beans.values()) {
            if (!bean.getClass().getName().startsWith(baseName))
                break;
            lastVersion = bean;
        }
        if (null == lastVersion)
            return Procedure.LogicError; // 这个简单服务，就不定义自己的错误码了。

        r.Result.setName(lastVersion.getClass().getName());
        r.Result.setVariables(lastVersion.variables());

        r.SendResult();
        return Procedure.Success;
    }

    public static void main(String [] args) {
        System.out.println(removeVersion("X_1"));
        System.out.println(removeVersion("X_1_"));
    }
}
