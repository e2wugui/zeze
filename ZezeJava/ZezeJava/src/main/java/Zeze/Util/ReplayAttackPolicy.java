package Zeze.Util;

// 防重放策略：仅递增（IncreasingOnly）或允许乱序（AllowDisorder）
public enum ReplayAttackPolicy {
	IncreasingOnly,
	AllowDisorder,
}
