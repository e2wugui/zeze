package Temp;

public class DemoMain {
	public interface Ia {
		void helloA();
	}

	public interface Ib {
		void helloB();
	}

	static String Impl =

	"""
	public class Ab implements Temp.DemoMain.Ia, Temp.DemoMain.Ib {
		@Override
		public void helloA() {
			System.out.println("helloA");
		}

		@Override
		public void helloB() {
			System.out.println("helloB");
		}

	}
	""";

	public static void main(String[] args) throws Exception {
		System.out.println(ClassLoader.getSystemClassLoader());
		var compiler = new Zeze.Util.InMemoryJavaCompiler();
		var abClass = compiler.compile("Ab", Impl);
		System.out.println(abClass.getClassLoader());
		System.out.println(Ia.class.getClassLoader());
		System.out.println(Ib.class.getClassLoader());

		Ia ia = (Ia)abClass.getConstructor().newInstance();
		ia.helloA();
		Ib ib = (Ib)ia;
		ib.helloB();
	}
}
