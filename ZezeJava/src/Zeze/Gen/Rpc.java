package Zeze.Gen;

import Zeze.*;
import java.util.*;

public class Rpc extends Protocol {
	private String Result;
	public final String getResult() {
		return Result;
	}

	// setup in compile
	private Zeze.Gen.Types.Type ResultType;
	public final Zeze.Gen.Types.Type getResultType() {
		return ResultType;
	}
	private void setResultType(Zeze.Gen.Types.Type value) {
		ResultType = value;
	}

	public Rpc(ModuleSpace space, XmlElement self) {
		super(space, self);
		Result = self.GetAttribute("result");
	}

	@Override
	public void Compile() {
		super.Compile();
		setResultType(getResult().length() > 0 ? Types.Type.Compile(getSpace(), getResult()) : null);
	}

	@Override
	public void Depends(HashSet<Zeze.Gen.Types.Type> depends) {
		if (getArgumentType() != null) {
			getArgumentType().Depends(depends);
		}
		if (getResultType() != null) {
			getResultType().Depends(depends);
		}
	}
}