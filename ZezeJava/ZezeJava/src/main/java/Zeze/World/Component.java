package Zeze.World;

public interface Component {
	void install(World world) throws Exception;
	void start(World world) throws Exception;
}
