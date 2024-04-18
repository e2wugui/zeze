package Zeze.Hot;

import java.util.concurrent.TimeUnit;
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

/**
 * 热更发布控制台。
 * 【其中，多版本bean方案的服务模块。未用，计划用直接bean支持热更方案替换，先保留】
 * 现在使用中的功能：拷贝jar的文件操作；全部拷贝完成提交命令；
 */
public class HotDistribute extends AbstractHotDistribute {
    private final DistributeManager distributeManager;
    private final FastLock lock = new FastLock();
    private final Condition cond = lock.newCondition();
    private int state = eIdle;
    private long distributeId = 0; // 由于发布不能并发，这个参数用于错误检查，没有实质作用。
    private TryDistribute tryDistribute;
    private Commit commit;

    public HotDistribute(DistributeManager distributeManager) {
        this.distributeManager = distributeManager;
    }

    public long setPrepare(long distributeId) {
        lock.lock();
        try {
            if (state != eIdle)
                return errorCode(ePrepare);

            state = ePrepare;
            this.distributeId = distributeId;
            return 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected long ProcessPrepareDistributeRequest(PrepareDistribute r) throws Exception {
        r.SendResultCode(setPrepare(r.Argument.getDistributeId()));
        return 0;
    }

    public void sendTryDistributeResultAndWaitCommit(long rc) throws InterruptedException {
        lock.lock();
        try {
            if (null != tryDistribute) {
                tryDistribute.SendResultCode(rc);
                tryDistribute = null;

                if (rc == 0) {
                    // 成功的结果才等待后续步骤。
                    while (state != eCommit && state != eTryRollback) {
                        if (!cond.await(10_000, TimeUnit.MILLISECONDS))
                            throw new InterruptedException("timeout");
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
    protected long ProcessTryDistributeRequest(TryDistribute r) throws Exception {
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
        distributeManager.commitDistribute();
        distributeManager.getHotManager().tryDistribute(r.Argument.isAtomicAll());
        // atomicAll 模式，在sendTryDistributeResultAndWaitCommit发送结果。
        // !atomicAll 模式，在setIdle发送结果。
        return 0;
    }

    @Override
    protected long ProcessTryRollbackRequest(TryRollback r) throws Exception {
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
    protected long ProcessCommitRequest(Commit r) throws Exception {
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

    public void sendCommitResultAndWaitCommit2(long rc) throws InterruptedException {
        lock.lock();
        try {
            if (null != commit) {
                commit.SendResultCode(rc);
                commit = null;

                if (rc == 0) {
                    // 成功的结果才等待后续步骤。
                    while (state != eCommit2) {
                        if (!cond.await(10_000, TimeUnit.MILLISECONDS))
                            throw new InterruptedException("timeout");
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected long ProcessCommit2Request(Commit2 r) throws Exception {
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

    @Override
    protected long ProcessOpenFileRequest(OpenFile r) throws Exception {
        var fileBin = distributeManager.open(r.Argument.getFileName());
        r.Result.setOffset(fileBin.getLength());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessAppendFileRequest(AppendFile r) throws Exception {
        distributeManager.append(
                r.Argument.getFileName(),
                r.Argument.getOffset(),
                r.Argument.getChunk());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessCloseFileRequest(CloseFile r) throws Exception {
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
