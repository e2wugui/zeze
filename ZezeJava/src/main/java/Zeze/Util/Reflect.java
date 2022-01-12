package Zeze.Util;

import java.lang.reflect.Method;
import java.util.HashMap;
import Zeze.Transaction.TransactionLevel;

public class Reflect {
    private final HashMap<String, Method> Methods = new HashMap<>();
    public Reflect(Class<?> cls) {
        for (var method : cls.getDeclaredMethods()) {
            if (method.getName().startsWith("Process")) { // 只有协议处理函数能配置TransactionLevel
                if (null != Methods.putIfAbsent(method.getName(), method))
                    throw new RuntimeException("Duplicate Method Name Of Protocol Handle: " + method.getName());
            }
        }
    }

    public TransactionLevel getTransactionLevel(String methodName, TransactionLevel def) {
        var method = Methods.get(methodName);
        if (null == method)
            return def;

        var annotation = method.getAnnotation(Zeze.Util.TransactionLevel.class);
        if (null == annotation)
            return def;

        return TransactionLevel.valueOf(annotation.Level());
    }
}
