package Zeze.Util;

import java.lang.reflect.Method;
import java.util.HashMap;
import Zeze.Transaction.TransactionLevel;

public class Reflect {
    private HashMap<String, Method> Methods = new HashMap<>();
    public Reflect(Class<?> cls) {
        for (var method : cls.getDeclaredMethods()) {
            Methods.put(method.getName(), method);
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
