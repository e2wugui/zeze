package Zeze.World;

public interface IComponent {
	void install(World world) throws Exception;
	void start(World world) throws Exception;
}
