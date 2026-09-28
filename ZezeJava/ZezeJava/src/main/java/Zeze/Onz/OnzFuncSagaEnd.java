package Zeze.Onz;

import Zeze.Transaction.Bean;

/** Onz saga参与方的补偿函数接口：事务回滚时以补偿参数撤销已提交的saga步骤。 */
@FunctionalInterface
public interface OnzFuncSagaEnd<A extends Bean> {
	long call(OnzSaga saga, A cancelArgument) throws Exception;
}
