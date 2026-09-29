package Zeze.Onz;

import Zeze.Transaction.Bean;

/**
 * Onz saga参与方的补偿函数接口：事务回滚时以补偿参数撤销已提交的saga步骤。
 * <p>契约（onz-04，FND26）：协调者的FuncSagaEnd从不携带补偿参数，{@code cancelArgument}
 * 恒为cancelClass的默认构造空bean——补偿数据从{@link OnzSaga}上下文自取，详见
 * {@link Onz#registerSaga}的契约说明。</p>
 */
@FunctionalInterface
public interface OnzFuncSagaEnd<A extends Bean> {
	long call(OnzSaga saga, A cancelArgument) throws Exception;
}
