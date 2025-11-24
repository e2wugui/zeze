
#include "Gen/demo/Module1/BItem.hpp"

namespace demo {
namespace Module1 {

Zeze::DynamicBean BItem::constructDynamicBean_Subclass() {
    return Zeze::DynamicBean(BItem::GetSpecialTypeIdFromBean_14, BItem::CreateBeanFromSpecialTypeId_14);
}

int64_t BItem::GetSpecialTypeIdFromBean_14(const Zeze::Bean* bean) {
    auto _typeId_ = bean->TypeId();
    if (_typeId_ == Zeze::EmptyBean::TYPEID)
        return Zeze::EmptyBean::TYPEID;
    if (_typeId_ == -410057899348847631L)
        return 1LL; // demo::Bean1
    if (_typeId_ == 4513771153805810055L)
        return 2LL; // demo::Module1::BSimple
    if (_typeId_ == -3820308016141122965L)
        return 3LL; // demo::Module1::BFood
    throw std::exception("Unknown Bean! dynamic@demo::Module1::BItem:Subclass");
}

Zeze::Bean* BItem::CreateBeanFromSpecialTypeId_14(int64_t typeId) {
    if (typeId == 1LL)
        return new demo::Bean1();
    if (typeId == 2LL)
        return new demo::Module1::BSimple();
    if (typeId == 3LL)
        return new demo::Module1::BFood();
    return nullptr;
}

BItem::BItem()
    : Subclass(GetSpecialTypeIdFromBean_14, CreateBeanFromSpecialTypeId_14)
{
}

void BItem::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BItem&>(other));
}

void BItem::Assign(const BItem& other) {
    Subclass.Assign(other.Subclass);
}

BItem& BItem::operator=(const BItem& other) {
    Assign(other);
    return *this;
}

void BItem::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        const auto& _x_ = Subclass;
        if (!_x_.Empty()) {
            _i_ = _o_.WriteTag(_i_, 14, Zeze::ByteBuffer::DYNAMIC);
            _x_.Encode(_o_);
        }
    }
    _o_.WriteByte(0);
}

void BItem::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    while ((_t_ & 0xff) > 1 && _i_ < 14) {
        _o_.SkipUnknownField(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 14) {
        _o_.ReadDynamic(Subclass, _t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
