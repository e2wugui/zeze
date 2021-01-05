using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace ConfigEditor.Property
{
    public class UndecidedVerify : IProperty
    {
        public override string Name => "undecided";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
        }
    }

    public class IntVerify : IProperty
    {
        public override string Name => "int";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
            if (int.TryParse(param.NewValue, out var _))
            {
                param.FormMain.FormError.RemoveError(param.Cell, this);
            }
            else
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Error, "It is not a int value.");
            }
        }
    }

    public class LongVerify : IProperty
    {
        public override string Name => "long";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
            if (long.TryParse(param.NewValue, out var _))
            {
                param.FormMain.FormError.RemoveError(param.Cell, this);
            }
            else
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Error, "It is not a int value.");
            }
        }
    }

    public class DoubleVerify : IProperty
    {
        public override string Name => "double";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
            if (double.TryParse(param.NewValue, out var _))
            {
                param.FormMain.FormError.RemoveError(param.Cell, this);
            }
            else
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Error, "It is not a int value.");
            }
        }
    }

    public class StringVerify : IProperty
    {
        public override string Name => "string";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
        }
    }

    public class ListVerify : IProperty
    {
        public override string Name => "list";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
        }
    }

    public class FloatVerify : IProperty
    {
        public override string Name => "float";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
            if (float.TryParse(param.NewValue, out var _))
            {
                param.FormMain.FormError.RemoveError(param.Cell, this);
            }
            else
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Error, "It is not a int value.");
            }
        }
    }

    public class DateVerify : IProperty
    {
        public override string Name => "date";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
            if (DateTime.TryParse(param.NewValue, out var _))
            {
                param.FormMain.FormError.RemoveError(param.Cell, this);
            }
            else
            {
                param.FormMain.FormError.AddError(param.Cell, this, ErrorLevel.Error, "It is not a Date.");
            }
        }
    }

    public class EnumVerify : IProperty
    {
        public override string Name => "enum";

        public override string Comment => "";

        public override bool BuildIn => true;

        public override void VerifyCell(VerifyParam param)
        {
            // TODO
        }
    }
}
