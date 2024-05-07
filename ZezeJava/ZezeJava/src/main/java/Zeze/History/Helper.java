package Zeze.History;

import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.LogList1;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSet1;
import Zeze.Transaction.Collections.LogOne;
import Zeze.Transaction.Log;
import Zeze.Transaction.LogDynamic;
import Zeze.Transaction.Logs.LogBeanKey;
import Zeze.Transaction.Logs.LogBinary;
import Zeze.Transaction.Logs.LogBool;
import Zeze.Transaction.Logs.LogByte;
import Zeze.Transaction.Logs.LogDecimal;
import Zeze.Transaction.Logs.LogDouble;
import Zeze.Transaction.Logs.LogFloat;
import Zeze.Transaction.Logs.LogInt;
import Zeze.Transaction.Logs.LogLong;
import Zeze.Transaction.Logs.LogQuaternion;
import Zeze.Transaction.Logs.LogShort;
import Zeze.Transaction.Logs.LogString;
import Zeze.Transaction.Logs.LogVector2;
import Zeze.Transaction.Logs.LogVector2Int;
import Zeze.Transaction.Logs.LogVector3;
import Zeze.Transaction.Logs.LogVector3Int;
import Zeze.Transaction.Logs.LogVector4;
import org.jetbrains.annotations.NotNull;

public class Helper {
	public static <T extends Serializable> void registerLogBeanKey(@NotNull Class<T> beanClass) {
		Log.register(() -> new LogBeanKey<>(beanClass));
	}

	public static <T> void registerLogList1(@NotNull Class<T> valueClass) {
		Log.register(() -> new LogList1<>(valueClass));
	}

	public static <T extends Bean> void registerLogList2(@NotNull Class<T> beanClass) {
		Log.register(() -> new LogList2<>(beanClass));
	}

	public static <K, V> void registerLogMap1(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		Log.register(() -> new LogMap1<>(keyClass, valueClass));
	}

	public static <K, V extends Bean> void registerLogMap2(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		Log.register(() -> new LogMap2<>(keyClass, valueClass));
	}

	public static <V> void registerLogSet1(@NotNull Class<V> valueClass) {
		Log.register(() -> new LogSet1<>(valueClass));
	}

	public static <V extends Bean> void registerLogOne(@NotNull Class<V> beanClass) {
		Log.register(() -> new LogOne<>(beanClass));
	}

	public static void registerLogs() {
		Log.register(LogBinary::new);
		Log.register(LogBool::new);
		Log.register(LogByte::new);
		Log.register(LogDecimal::new);
		Log.register(LogDouble::new);
		Log.register(LogFloat::new);
		Log.register(LogInt::new);
		Log.register(LogLong::new);
		Log.register(LogQuaternion::new);
		Log.register(LogShort::new);
		Log.register(LogString::new);
		Log.register(LogVector2::new);
		Log.register(LogVector2Int::new);
		Log.register(LogVector3::new);
		Log.register(LogVector3Int::new);
		Log.register(LogVector4::new);
		Log.register(LogDynamic::new);
	}

}
