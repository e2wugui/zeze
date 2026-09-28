package Zeze.Onz;

import Zeze.Transaction.Bean;

/** Onz saga参与方的业务函数接口：在OnzSaga上下文中执行业务，返回结果码。 */
@FunctionalInterface
public interface OnzFuncSaga<A extends Bean, R extends Bean> {
	long call(OnzSaga sage, A argument, R result) throws Exception;
}
