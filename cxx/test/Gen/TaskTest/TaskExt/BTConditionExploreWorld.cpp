
#include "Gen/TaskTest/TaskExt/BTConditionExploreWorld.hpp"

namespace TaskTest {
namespace TaskExt {

BTConditionExploreWorld::BTConditionExploreWorld()
{
    ExploreRate = 0.0;
    Finished = false;
}

BTConditionExploreWorld::BTConditionExploreWorld(double ExploreRate_, bool Finished_)
{
    ExploreRate = ExploreRate_;
    Finished = Finished_;
}

void BTConditionExploreWorld::Assign(const Zeze::Bean& other) {
    Assign(dynamic_cast<const BTConditionExploreWorld&>(other));
}

void BTConditionExploreWorld::Assign(const BTConditionExploreWorld& other) {
    ExploreRate = other.ExploreRate;
    Finished = other.Finished;
}

BTConditionExploreWorld& BTConditionExploreWorld::operator=(const BTConditionExploreWorld& other) {
    Assign(other);
    return *this;
}

void BTConditionExploreWorld::Encode(Zeze::ByteBuffer& _o_) const {
    int _i_ = 0;
    {
        auto _x_ = ExploreRate;
        if (_x_ != 0) {
            _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::DOUBLE);
            _o_.WriteDouble(_x_);
        }
    }
    {
        auto _x_ = Finished;
        if (_x_) {
            _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::INTEGER);
            _o_.WriteByte(1);
        }
    }
    _o_.WriteByte(0);
}

void BTConditionExploreWorld::Decode(Zeze::ByteBuffer& _o_) {
    int _t_ = _o_.ReadByte();
    int _i_ = _o_.ReadTagSize(_t_);
    if (_i_ == 1) {
        ExploreRate = _o_.ReadDouble(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    if (_i_ == 2) {
        Finished = _o_.ReadBool(_t_);
        _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
    while (_t_ != 0) {
        _o_.SkipUnknownField(_t_);
        _o_.ReadTagSize(_t_ = _o_.ReadByte());
    }
}
}
}
