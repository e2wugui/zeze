package Zeze.Transaction;

import java.io.Serial;

/** bean 已被事务占有（受管）时再次加入受管容器抛出的异常。 */
public final class HasManagedException extends RuntimeException {
	@Serial private static final long serialVersionUID = -396862403074130523L;
}
