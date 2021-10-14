package tangible;

@FunctionalInterface
public interface Action4Param<T1, T2, T3, T4>
{
	void invoke(T1 t1, T2 t2, T3 t3, T4 t4);
}