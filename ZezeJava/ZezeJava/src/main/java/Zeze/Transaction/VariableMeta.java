package Zeze.Transaction;

/** Bean / BeanKey 字段结构元信息（供 History 等运行时反射遍历类型依赖）。 */
public record VariableMeta(int id, String name, String type, String key, String value) {
}
