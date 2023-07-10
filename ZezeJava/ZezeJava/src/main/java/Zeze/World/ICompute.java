package Zeze.World;

/**
 * 移动同步之外其他逻辑计算接口。
 */
public interface ICompute {
	ISelector selector();
	void compute(); // todo 参数，结果？
}
