package Zeze.Gen.lua;

import Zeze.*;
import Zeze.Gen.*;
import java.util.*;
import java.io.*;

public class BeanFormatter {
	public static void Make(String moduleName, String beanName, long beanTypeId, ArrayList<Types.Variable> vars, ArrayList<Types.Enum> enums, OutputStreamWriter sw) {
		sw.write(String.valueOf(String.format("%1$s.%2$s = {", moduleName, beanName)) + System.lineSeparator());
		sw.write(String.format("    _TypeId_ = %1$s,", beanTypeId) + System.lineSeparator());
		for (var v : vars) {
			sw.write(String.valueOf(String.format("    %1$s = %2$s,", v.getName(), v.getId())) + System.lineSeparator());
		}
		for (var e : enums) {
			double _;
			tangible.OutObject<Double> tempOut__ = new tangible.OutObject<Double>();
			if (tangible.TryParseHelper.tryParseDouble(e.getValue(), tempOut__)) { // is number
			_ = tempOut__.outArgValue;
				sw.write(String.valueOf(String.format("    %1$s = %2$s,", e.getNamePinyin(), e.getValue())) + System.lineSeparator());
			}
			else {
			_ = tempOut__.outArgValue;
				sw.write(String.valueOf(String.format("    %1$s = \"%2$s\",", e.getNamePinyin(), e.getValue())) + System.lineSeparator());
			}
		}
		sw.write("}" + System.lineSeparator());
	}

	public static void MakeMeta(String beanFullName, long typeId, ArrayList<Types.Variable> vars, OutputStreamWriter sw) {
		sw.write("meta.beans[" + typeId + "] = {" + System.lineSeparator());
		sw.write(String.valueOf(String.format("    [0] = \"%1$s\", ", beanFullName)) + System.lineSeparator());
		for (var v : vars) {
			sw.write(String.format("    [%1$s] = %2$s,", v.getId(), TypeMeta.Get(v, v.getVariableType())) + System.lineSeparator());
		}
		sw.write("}" + System.lineSeparator());
		for (var v : vars) {
			boolean tempVar = v.VariableType instanceof Types.TypeDynamic;
			Types.TypeDynamic d = tempVar ? (Types.TypeDynamic)v.VariableType : null;
			if (tempVar) {
				sw.write(String.valueOf(String.format("function Zeze_GetRealBeanTypeIdFromSpecial_%1$s_%2$s(specialTypeId)", beanFullName, v.getNamePinyin())) + System.lineSeparator());
				for (var r : d.getRealBeans().entrySet()) {
					sw.write(String.format("    if (specialTypeId == %1$s) then", r.getKey()) + System.lineSeparator());
					sw.write(String.format("        return %1$s", r.getValue().TypeId) + System.lineSeparator());
					sw.write(String.format("    end") + System.lineSeparator());
				}
				sw.write(String.format("    return specialTypeId") + System.lineSeparator());
				sw.write(String.format("end") + System.lineSeparator());
			}
		}
	}
}