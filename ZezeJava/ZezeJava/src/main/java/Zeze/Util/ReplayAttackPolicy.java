package Zeze.Util;

/** 防重放策略：仅允许递增（IncreasingOnly）或允许窗口内乱序（AllowDisorder）。 */
public enum ReplayAttackPolicy {
	IncreasingOnly,
	AllowDisorder,
}
