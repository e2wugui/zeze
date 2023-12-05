package Zeze.Onz;

import Zeze.Transaction.Bean;

@FunctionalInterface
public interface OnzFuncSaga<A extends Bean, R extends Bean> {
	long call(OnzSaga sage, A argument, R result) throws Exception;
}
