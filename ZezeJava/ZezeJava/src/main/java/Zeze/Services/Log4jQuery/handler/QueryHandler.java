package Zeze.Services.Log4jQuery.handler;

public interface QueryHandler<T, K> {
	K invoke(T param);
}
