package Zeze.Onz;

import Zeze.Transaction.Bean;

/** Onz procedure参与方的业务函数接口：在OnzProcedure上下文中执行业务，返回结果码。 */
@FunctionalInterface
public interface OnzFuncProcedure<A extends Bean, R extends Bean> {
	long call(OnzProcedure onzProcedure, A argument, R result) throws Exception;
}
