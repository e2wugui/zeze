package Zeze.Gen.cs;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;
import java.io.*;

public class Schemas {
	private Project Project;
	public final Project getProject() {
		return Project;
	}
	private HashSet<Types.Type> Depends = new HashSet<Types.Type> ();
	public final HashSet<Types.Type> getDepends() {
		return Depends;
	}
	private String GenDir;
	public final String getGenDir() {
		return GenDir;
	}

	public Schemas(Project prj, String gendir) {
		Project = prj;
		GenDir = gendir;

		for (Table table : getProject().getAllTables().values()) {
			if (getProject().getGenTables().contains(table.getGen())) {
				table.Depends(getDepends());
			}
		}
	}

	private String GetFullName(Types.Type type) {
		if (type.isBean()) {
			if (type.isKeyable()) {
				return (type instanceof Types.BeanKey ? (Types.BeanKey)type : null).getFullName();
			}
			return (type instanceof Types.Bean ? (Types.Bean)type : null).getFullName();
		}
		return type.getName();
	}
	public final void Make() {
		try (OutputStreamWriter sw = getProject().getSolution().OpenWriter(getGenDir(), "Schemas.cs", true)) {
    
			sw.write("// auto-generated" + System.lineSeparator());
			sw.write("" + System.lineSeparator());
			sw.write("namespace " + getProject().getSolution().Path(".", null) + System.lineSeparator());
			sw.write("{" + System.lineSeparator());
			sw.write("    public class Schemas : Zeze.Schemas" + System.lineSeparator());
			sw.write("    {" + System.lineSeparator());
			sw.write("        public Schemas()" + System.lineSeparator());
			sw.write("        {" + System.lineSeparator());
    
			for (var table : getProject().getAllTables().values()) {
				sw.write("            base.AddTable(new Zeze.Schemas.Table()" + System.lineSeparator());
				sw.write("            {" + System.lineSeparator());
				sw.write(String.format("                Name = \"%1$s\",", table.Space.Path("_", table.Name)) + System.lineSeparator());
				sw.write(String.valueOf(String.format("                KeyName = \"%1$s\",", GetFullName(table.KeyType))) + System.lineSeparator());
				sw.write(String.valueOf(String.format("                ValueName = \"%1$s\",", GetFullName(table.ValueType))) + System.lineSeparator());
				sw.write("            });" + System.lineSeparator());
			}
    
			for (var type : getDepends()) {
				if (false == type.IsBean) {
					continue;
				}
    
				if (type.IsKeyable) {
					var beanKey = type instanceof Types.BeanKey ? (Types.BeanKey)type : null;
					GenAddBean(sw, beanKey.getFullName(), true, beanKey.getVariables());
				}
				else {
					var bean = type instanceof Types.Bean ? (Types.Bean)type : null;
					GenAddBean(sw, bean.getFullName(), false, bean.getVariables());
				}
			}
			sw.write("        }" + System.lineSeparator());
			sw.write("    }" + System.lineSeparator());
			sw.write("}" + System.lineSeparator());
		}
	}

	private void GenAddBean(OutputStreamWriter sw, String name, boolean isBeanKey, ArrayList<Types.Variable> vars) {
		sw.write(String.format("            {{") + System.lineSeparator());
		sw.write(String.valueOf(String.format("                var bean = new Zeze.Schemas.Bean() { Name = \"%1$s\", IsBeanKey = %2$s };", name, String.valueOf(isBeanKey).toLowerCase())) + System.lineSeparator());
		for (var v : vars) {
			sw.write(String.format("                bean.AddVariable(new Zeze.Schemas.Variable()") + System.lineSeparator());
			sw.write(String.format("                {{") + System.lineSeparator());
			sw.write(String.format("                    Id = %1$s,", v.getId()) + System.lineSeparator());
			sw.write(String.valueOf(String.format("                    Name = \"%1$s\",", v.getName())) + System.lineSeparator());
			sw.write(String.valueOf(String.format("                    TypeName = \"%1$s\",", GetFullName(v.getVariableType()))) + System.lineSeparator());
			boolean tempVar = v.VariableType instanceof Types.TypeCollection;
			Types.TypeCollection collection = tempVar ? (Types.TypeCollection)v.VariableType : null;
			if (tempVar) {
				sw.write(String.valueOf(String.format("                    ValueName = \"%1$s\",", GetFullName(collection.getValueType()))) + System.lineSeparator());
			}
			else {
				boolean tempVar2 = v.VariableType instanceof Types.TypeMap;
				Types.TypeMap map = tempVar2 ? (Types.TypeMap)v.VariableType : null;
				if (tempVar2) {
					sw.write(String.valueOf(String.format("                    KeyName = \"%1$s\",", GetFullName(map.getKeyType()))) + System.lineSeparator());
					sw.write(String.valueOf(String.format("                    ValueName = \"%1$s\",", GetFullName(map.getValueType()))) + System.lineSeparator());
				}
			}
			sw.write(String.format("                }});") + System.lineSeparator());
		}
		sw.write(String.format("                base.AddBean(bean);") + System.lineSeparator());
		sw.write(String.format("            }}") + System.lineSeparator());
	}
}