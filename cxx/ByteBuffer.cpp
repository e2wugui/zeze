#include "ByteBuffer.h"
#include "Bean.h"

namespace Zeze {
DynamicBean& ByteBuffer::ReadDynamic(DynamicBean& dynBean, int type)
{
    type &= TAG_MASK;
    if (type == DYNAMIC)
    {
        dynBean.Decode(*this);
        return dynBean;
    }
    if (type == BEAN)
    {
        dynBean.NewBean(0)->Decode(*this);
        return dynBean;
    }
    SkipUnknownField(type);
    return dynBean;
}
}
