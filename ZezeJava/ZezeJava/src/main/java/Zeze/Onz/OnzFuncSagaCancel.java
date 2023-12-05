package Zeze.Onz;

@FunctionalInterface
public interface OnzFuncSagaCancel {
	long call(OnzSaga saga) throws Exception;
}
