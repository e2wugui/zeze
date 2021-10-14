package tangible;

@FunctionalInterface
public interface Action3Param<T1, T2, T3>
{
	void invoke(T1 t1, T2 t2, T3 t3);
}