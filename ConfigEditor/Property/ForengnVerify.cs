using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Property
{
    public class ForengnVerify : IProperty
    {
        public static readonly string PName = "foreign";

        public override string Name => PName;

        public override string Comment => "";
        public override bool BuildIn => true;

        private bool ExistData(VerifyParam param, VarDefine foreignVar)
        {
            foreach (var beanData in foreignVar.Parent.Document.Beans)
            {
                Bean.VarData varData = beanData.GetLocalVarData(foreignVar.Name);
                if (null != varData && param.NewValue.Equals(varData.Value))
                    return true;
            }
            return false;
        }

        public override void VerifyCell(VerifyParam param)
        {
            string result = param.ColumnTag.PathLast.Define.OpenForeign(out var foreignVar);
            if (null == result)
            {
                if (null == foreignVar || ExistData(param, foreignVar)) // no foreign is ok.
                    param.FormMain.FormError.RemoveError(param.Cell, this);
                else
                    param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Warn, "value not exist in foreign.");
            }
            else
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Warn, result);
            }
        }
    }
}
