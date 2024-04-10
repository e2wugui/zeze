package Zeze;

import java.util.concurrent.locks.Lock;
import Zeze.Net.Protocol;

public interface IModule {
	String getFullName();

	String getName();

	int getId();

	default void lock() {
		throw new UnsupportedOperationException();
	}

	default void unlock() {
		throw new UnsupportedOperationException();
	}

	default Lock getLock() {
		throw new UnsupportedOperationException();
	}

	default String getWebPathBase() {
		return "";
	}

	default boolean isBuiltin() {
		return false;
	}

	/**
	 * 整个程序完全准备好，然后可以非常安全的在这里执行任意的启动代码，比如Timer。
	 * @throws Exception exception
	 */
	default void StartLast() throws Exception {
	}

	/**
	 * 整个程序关闭前，在这里执行任意关闭代码，这个主要用于多App测试的安全关闭，对于独立进程效果同Stop。
	 * @throws Exception exception
	 */
	default void StopBefore() throws Exception {

	}

	default void Initialize(AppBase app) throws Exception {
	}

	default void Register() {

	}

	default void UnRegister() {
	}

	default long errorCode(int code) {
		return errorCode(getId(), code);
	}

	static long errorCode(int moduleId, int code) {
		if (code < 0)
			throw new IllegalArgumentException("code must greater than 0.");
		return Protocol.makeTypeId(moduleId, code);
	}

	static int getModuleId(long result) {
		return Protocol.getModuleId(result);
	}

	static int getErrorCode(long result) {
		return Protocol.getProtocolId(result);
	}
}
