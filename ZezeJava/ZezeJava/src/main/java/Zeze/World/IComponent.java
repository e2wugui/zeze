package Zeze.World;

/**
 * World组件，一般用来完成和客户端之间的交互，自定义并处理Command或Query。
 */
public interface IComponent {
	void install(World world) throws Exception;
	void start(World world) throws Exception;
}
