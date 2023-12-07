package Zeze.Onz;

import Zeze.Transaction.Bean;

@FunctionalInterface
public interface OnzFuncSagaEnd<A extends Bean> {
	long call(OnzSaga saga, A cancelArgument) throws Exception;
}
