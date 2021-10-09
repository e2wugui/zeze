package tangible;

@FunctionalInterface
public interface Action2Param<T1, T2>
{
	void invoke(T1 t1, T2 t2);
}