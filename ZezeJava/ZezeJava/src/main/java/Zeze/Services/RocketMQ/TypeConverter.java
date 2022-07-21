package Zeze.Services.RocketMQ;

public class TypeConverter {
	public static String convert2String(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof String) {
			return (String)obj;
		}
		throw new ClassCastException("To converted object is " + obj.getClass() + ", not String.class");
	}

	public static Long convert2Long(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Long) {
			return (Long)obj;
		}
		throw new ClassCastException("To converted object is " + obj.getClass() + ", not Long.class");
	}

	public static Integer convert2Integer(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Integer) {
			return (Integer)obj;
		}
		throw new ClassCastException("To converted object is " + obj.getClass() + ", not Integer.class");
	}

	public static Boolean convert2Boolean(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof Boolean) {
			return (Boolean)obj;
		}
		throw new ClassCastException("To converted object is " + obj.getClass() + ", not Boolean.class");
	}

	public static <T> T convert2Object(Object obj, Class<T> target) {
		if (obj == null) {
			return null;
		}
		if (target.isInstance(obj)) {
			return (T)obj;
		}
		throw new ClassCastException("To converted object is " + obj.getClass() + ", not " + target.getSimpleName() + ".class");
	}
}
