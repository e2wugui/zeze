package Zeze.Onz;

import Zeze.Transaction.Bean;

/**
 * Onz saga 参与方的补偿函数接口：事务回滚时撤销已提交的 saga 步骤。
 * 协调者不传递补偿载荷，{@code cancelArgument} 恒为默认空 bean，补偿数据请从 {@link OnzSaga} 上下文或业务表获取。
 */
@FunctionalInterface
public interface OnzFuncSagaEnd<A extends Bean> {
	long call(OnzSaga saga, A cancelArgument) throws Exception;
}
