package Zeze.Onz;

public interface OnzTransaction {
	String getName();
	int getFlushMode();

	long perform() throws Exception;
}
