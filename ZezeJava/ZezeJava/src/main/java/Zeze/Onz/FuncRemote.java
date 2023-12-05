package Zeze.Onz;

import Zeze.Transaction.Bean;

@FunctionalInterface
public interface FuncRemote<T extends Bean, R extends Bean> {
	long call(T argument, R result) throws Exception;
}
