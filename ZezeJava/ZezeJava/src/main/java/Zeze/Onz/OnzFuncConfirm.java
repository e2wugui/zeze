package Zeze.Onz;

import Zeze.Transaction.Bean;

@FunctionalInterface
public interface OnzFuncConfirm<A extends Bean, R extends Bean> {
	long call(OnzSaga sage, A argument, R result) throws Exception;
}
