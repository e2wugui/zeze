package tangible;

@FunctionalInterface
public interface Func2Param<T1, T2, TResult>
{
	TResult invoke(T1 t1, T2 t2);
}