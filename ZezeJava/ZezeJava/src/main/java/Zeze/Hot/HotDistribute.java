package Zeze.Hot;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;

public class HotDistribute extends AbstractHotDistribute {

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
