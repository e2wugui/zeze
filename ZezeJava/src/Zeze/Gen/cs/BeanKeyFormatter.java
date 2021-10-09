package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;

public class BeanKeyFormatter {
	private Types.BeanKey beanKey;

	public BeanKeyFormatter(Types.BeanKey beanKey) {
		this.beanKey = beanKey;
	}

	public final void Make(String baseDir) {
		try (OutputStreamWriter sw = beanKey.getSpace().OpenWriter(baseDir, beanKey.getName() + ".cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("using Zeze.Serialize;" + System.lineSeparator());
			sw.write("using System;" + System.lineSeparator());
			//sw.WriteLine("using Zeze.Transaction.Collections;");
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + beanKey.getSpace().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write("    public sealed class " + beanKey.getName() + " : Serializable, System.IComparable" + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			// declare enums
			for (Types.Enum e : beanKey.getEnums()) {
				sw.write("        public const int " + e.getName() + " = " + e.getValue() + ";" + e.getComment() + System.lineSeparator());
			}
			if (!beanKey.getEnums().isEmpty()) {
				sw.write("" + System.lineSeparator());
			}
    
			// declare variables
			for (Types.Variable v : beanKey.getVariables()) {
				sw.write("        private " + TypeName.GetName(v.getVariableType()) + " " + v.getNamePrivate() + ";" + v.getComment() + System.lineSeparator());
			}
			sw.write("" + System.lineSeparator());
    
			sw.write("        // for decode only" + System.lineSeparator());
			sw.write("        public " + beanKey.getName() + "()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
			sw.write("        }" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
    
			{
			// params construct
				sw.write("        public " + beanKey.getName() + "(" + ParamName.GetParamList(beanKey.getVariables()) + ")" + System.lineSeparator());
				sw.write("        {" + System.lineSeparator());
				for (Types.Variable v : beanKey.getVariables()) {
					sw.write("            this." + v.getNamePrivate() + " = " + v.getNamePrivate() + "_;" + System.lineSeparator());
				}
				sw.write("        }" + System.lineSeparator());
				sw.write("" + System.lineSeparator());
			}
			PropertyBeanKey.Make(beanKey, sw, "        ");
			sw.write("" + System.lineSeparator());
			Tostring.Make(beanKey, sw, "        ");
			Encode.Make(beanKey, sw, "        ");
			Decode.Make(beanKey, sw, "        ");
			Equal.Make(beanKey, sw, "        ");
			HashCode.Make(beanKey, sw, "        ");
			Compare.Make(beanKey, sw, "        ");
			NegativeCheck.Make(beanKey, sw, "        ");
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
    
		}
	}
}