package Zeze.Hot;

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

/**
 * 热更发布控制台。
 * 【其中，多版本bean方案的服务模块。未用，计划用直接bean支持热更方案替换，先保留】
 * 现在使用中的功能：拷贝jar的文件操作；全部拷贝完成提交命令；
 */
public class HotDistribute extends AbstractHotDistribute {
    private final DistributeManager distributeManager;

    public HotDistribute(DistributeManager distributeManager) {
        this.distributeManager = distributeManager;
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
    protected long ProcessPrepareDistributeRequest(PrepareDistribute r) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessTryDistributeRequest(TryDistribute r) throws Exception {
        return 0;
    }

    @Override
    protected long ProcessTryRollbackRequest(TryRollback r) throws Exception {
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
    protected long ProcessCommitRequest(Commit r) throws Exception {
        distributeManager.commitDistribute();
        var rc = distributeManager.getHotManager().tryDistribute();
        r.SendResultCode(rc);
        return 0;
    }

    @Override
    protected long ProcessCommit2Request(Commit2 r) throws Exception {
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
