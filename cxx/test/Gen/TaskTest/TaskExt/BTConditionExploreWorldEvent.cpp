
#include "Gen/TaskTest/TaskExt/BTConditionExploreWorldEvent.hpp"

namespace TaskTest {
namespace TaskExt {

BTConditionExploreWorldEvent::BTConditionExploreWorldEvent()
{
    ExploreRate = 0.0;
}

BTConditionExploreWorldEvent::BTConditionExploreWorldEvent(double ExploreRate_)
{
    ExploreRate = ExploreRate_;
}

void BTConditionExploreWorldEvent::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BTConditionExploreWorldEvent&>(other));
}

void BTConditionExploreWorldEvent::Assign(const BTConditionExploreWorldEvent& other) {
    ExploreRate = other.ExploreRate;
}

BTConditionExploreWorldEvent& BTConditionExploreWorldEvent::operator=(const BTConditionExploreWorldEvent& other) {
    Assign(other);
    return *this;
}

void BTConditionExploreWorldEvent::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = ExploreRate;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::DOUBLE);
            _o_.WriteDouble(_x_);
        }
    }
    _o_.WriteByte(0);
}

void BTConditionExploreWorldEvent::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        ExploreRate = _o_.ReadDouble(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
