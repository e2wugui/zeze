package Zeze.Onz;

import Zeze.Transaction.Bean;

@FunctionalInterface
public interface OnzFuncProcedure<A extends Bean, R extends Bean> {
	long call(OnzProcedure onzProcedure, A argument, R result) throws Exception;
}
