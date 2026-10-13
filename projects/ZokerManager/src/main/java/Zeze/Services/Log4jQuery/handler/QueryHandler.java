package Zeze.Services.Log4jQuery.handler;

/**
 * 查询处理器接口：按参数执行查询并返回结果。
 */
public interface QueryHandler<T, K> {
	K invoke(T param);
}
