package Zeze.Onz;

@FunctionalInterface
public interface OnzFuncSagaEnd {
	long call(OnzSaga saga) throws Exception;
}
