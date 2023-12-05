package Zeze.Onz;

@FunctionalInterface
public interface OnzFuncCancel {
	long call(OnzSaga saga) throws Exception;
}
