package Zeze.Gen;

import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Program {
	private static HashMap<String, Solution> solutions = new HashMap<String, Solution>();
	public static Zeze.Util.AtomicLong IdGen = new Zeze.Util.AtomicLong();

	public static void CheckReserveName(String name) {
		if (name.startsWith("_")) {
			throw new RuntimeException(String.format("Name Can Not Starts With '_' name=%1$s", name));
		}
	}

	public static String GenUniqVarName() {
		return "_v_" + IdGen.IncrementAndGet() + "_";
	}

	private static Zeze.Util.Ranges GlobalModuleIdChecker = new Zeze.Util.Ranges();
	public static Zeze.Util.Ranges getGlobalModuleIdChecker() {
		return GlobalModuleIdChecker;
	}
	private static void setGlobalModuleIdChecker(Zeze.Util.Ranges value) {
		GlobalModuleIdChecker = value;
	}

	private static boolean Debug = false;
	public static boolean getDebug() {
		return Debug;
	}
	private static void setDebug(boolean value) {
		Debug = value;
	}

	// 用来保存可命名对象（bean,protocol,rpc,table,Project,Service,Module...)，用来 1 检查命名是否重复，2 查找对象。
	// key 为全名：包含完整的名字空间。
	private static TreeMap<String, Object> NamedObjects = new TreeMap<String, Object> ();
	public static TreeMap<String, Object> getNamedObjects() {
		return NamedObjects;
	}
	private static void setNamedObjects(TreeMap<String, Object> value) {
		NamedObjects = value;
	}
	private static HashSet<Long> BeanTypeIdDuplicateChecker = new HashSet<Long> ();
	public static HashSet<Long> getBeanTypeIdDuplicateChecker() {
		return BeanTypeIdDuplicateChecker;
	}

	public static void AddNamedObject(String fullName, Object obj) {
		// 由于创建文件在 windows 下大小写不敏感，所以名字需要大小写不敏感。
		String lower = fullName.toLowerCase();
		if (getNamedObjects().containsKey(lower)) {
			throw new RuntimeException("duplicate name(Not Case Sensitive): " + fullName);
		}

		getNamedObjects().put(lower, obj);
	}

	public static <T> T GetNamedObject(String fullName) {
		String lower = fullName.toLowerCase();

		Object value;
		if (getNamedObjects().containsKey(lower) && (value = getNamedObjects().get(lower)) == value) {
			if (value instanceof T) {
				return (T)value;
			}
			throw new RuntimeException("NamedObject is not " + fullName); // 怎么得到模板参数类型？
		}
		throw new RuntimeException("NamedObject not found(Not Case Sensitive): " + fullName);
	}

	public static void ImportSolution(String xmlfile) {
		if (solutions.containsKey(xmlfile)) {
			return;
		}

		XmlDocument doc = new XmlDocument();
		doc.Load(xmlfile);
		Solution solution = new Solution(doc.DocumentElement);
		/*
		foreach (KeyValuePair<string, Solution> exist in solutions)
		{
		    if (exist.Value.Name.Equals(solution.Name))
		        Console.WriteLine("WARN duplicate solution name: " + solution.Name + " in file: " + exist.Key + "," + xmlfile);
		}
		*/
		solutions.put(xmlfile, solution);
	}
	public static void main(String[] args) {
		getBeanTypeIdDuplicateChecker().add(Zeze.Transaction.EmptyBean.TYPEID);

		ArrayList<String> xmlFileList = new ArrayList<String>();
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
				case "-debug":
					setDebug(true);
					break;
				default:
					xmlFileList.add(args[i]);
					break;
			}
		}
		if (xmlFileList.isEmpty()) {
			String xmlDefault = "solution.xml";
			if ((new File(xmlDefault)).isFile()) {
				xmlFileList.add(xmlDefault);
			}
			else {
				System.out.println(xmlDefault + " not found");
				return;
			}
		}
		for (String file : xmlFileList) {
			ImportSolution(file);
		}

		for (Solution sol : solutions.values()) { // compile all .包含从文件中 import 进来的。
			sol.Compile();
		}

		for (String file : xmlFileList) { // make 参数指定的 Solution
			Solution sol = null;
			sol = solutions.get(file);
			sol.Make();
		}
	}

	public static void Print(Object obj) {
		System.out.println(obj);
	}

	public static ArrayList<Module> CompileModuleRef(Collection<String> fullNames) {
		ArrayList<Module> result = new ArrayList<Module>();
		for (String fullName : fullNames) {
			result.add(Zeze.Gen.Program.<Module>GetNamedObject(fullName));
		}
		return result;
	}

	public static ArrayList<Protocol> CompileProtocolRef(Collection<String> fullNames) {
		ArrayList<Protocol> result = new ArrayList<Protocol>();
		for (String fullName : fullNames) {
			result.add(Zeze.Gen.Program.<Protocol>GetNamedObject(fullName));
		}
		return result;
	}

	public static boolean IsFullName(String name) {
		return name.indexOf('.') != -1;
	}

	public static String ToFullNameIfNot(String path, String name) {
		return IsFullName(name) ? name : path + "." + name;
	}

	public static ArrayList<String> ToFullNameIfNot(String path, Collection<String> names) {
		ArrayList<String> fullNames = new ArrayList<String>();
		for (String name : names) {
			fullNames.add(ToFullNameIfNot(path, name));
		}
		return fullNames;
	}

	public static ArrayList<String> Refs(XmlElement self, String nodename, String refName) {
		var refs = new ArrayList<String>();
		XmlNodeList childnodes = self.ChildNodes;
		for (XmlNode node : childnodes) {
			if (XmlNodeType.Element != node.NodeType) {
				continue;
			}

			XmlElement e = (XmlElement)node;
			if (e.Name.equals(nodename)) {
				var attr = e.GetAttribute(refName);
				if (refs.contains(attr)) {
					throw new RuntimeException("duplicate ref name " + attr);
				}
				refs.add(attr);
			}
		}
		return refs;
	}

	public static Collection<String> Refs(XmlElement self, String nodename) {
		return Refs(self, nodename, "ref");
	}

	public static int HandleServerFlag = 1;
	public static int HandleClientFlag = 2;
	public static int HandleScriptServerFlag = 8;
	public static int HandleScriptClientFlag = 16;
	public static int HandleCSharpFlags = HandleServerFlag | HandleClientFlag; // 底层语言。如果c++需要生成协议之类的，也是用这个。
	public static int HandleScriptFlags = HandleScriptServerFlag | HandleScriptClientFlag;

	public static int ToHandleFlags(String handle) {
		int f = 0;
		String hs = handle.strip().toLowerCase();
		for (String h : hs.split("[,]", -1)) {
			switch (h.strip()) {
				case "server":
					f |= HandleServerFlag;
					break;
				case "client":
					f |= HandleClientFlag;
					break;
				case "serverscript":
					f |= HandleScriptServerFlag;
					break;
				case "clientscript":
					f |= HandleScriptClientFlag;
					break;
				default:
					throw new RuntimeException("unknown handle: " + handle);
			}
		}
		return f;
	}


	public static System.IO.StreamWriter OpenWriterNoPath(String baseDir, String fileName) {
		return OpenWriterNoPath(baseDir, fileName, true);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public static System.IO.StreamWriter OpenWriterNoPath(string baseDir, string fileName, bool overwrite = true)
	public static System.IO.StreamWriter OpenWriterNoPath(String baseDir, String fileName, boolean overwrite) {
		(new File(baseDir)).mkdirs();
		String fullFileName = Paths.get(baseDir).resolve(fileName).toString();
		boolean exists = (new File(fullFileName)).isFile();
		if (!exists || overwrite) {
			Program.Print("file " + (exists ? "overwrite" : "new") + " '" + fullFileName + "'");
//C# TO JAVA CONVERTER WARNING: The java.io.OutputStreamWriter constructor does not accept all the arguments passed to the System.IO.StreamWriter constructor:
//ORIGINAL LINE: System.IO.StreamWriter sw = new System.IO.StreamWriter(fullFileName, false, Encoding.UTF8);
			OutputStreamWriter sw = new OutputStreamWriter(fullFileName, java.nio.charset.StandardCharsets.UTF_8);
			return sw;
		}
		Program.Print("file skip '" + fullFileName + "'");
		return null;
	}


	public static String ToPinyin(String text) {
		StringBuilder sb = new StringBuilder();
		for (var c : text) {
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_')) {
				sb.append(c);
				continue;
			}
			String py = NPinyin.Pinyin.GetPinyin(c);
			if (py.length() > 0) {
				sb.append(py.substring(0, 1).toUpperCase() + py.substring(1));
			}
		}
		return sb.toString();
	}

	public static String FullModuleNameToFullClassName(String name) {
		var index = name.lastIndexOf('.');
		if (index == -1) {
			return name + ".Module" + name;
		}
		String lastname = name.substring(index + 1);
		return name + ".Module" + lastname;
	}

}