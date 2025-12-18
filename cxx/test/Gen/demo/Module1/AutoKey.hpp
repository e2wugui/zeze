#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
// beankey的注释
class AutoKey : public Zeze::Serializable {
public:
    std::string Name; // 一般就是表名。
    int64_t LocalId;

    // for decode only
    AutoKey() {
        LocalId = 0;
    }

    void Assign(const AutoKey& other) {
        Name = other.Name;
        LocalId = other.LocalId;
    }

    AutoKey& operator=(const AutoKey& other) {
        Assign(other);
        return *this;
    }

    AutoKey(const std::string& Name_, int64_t LocalId_) {
        Name = Name_;
        LocalId = LocalId_;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override {
        int _i_ = 0;
        {
            const auto& _x_ = Name;
            if (!_x_.empty()) {
                _i_ = _o_.WriteTag(_i_, 1, Zeze::ByteBuffer::BYTES);
                _o_.WriteString(_x_);
            }
        }
        {
            auto _x_ = LocalId;
            if (_x_ != 0) {
                _i_ = _o_.WriteTag(_i_, 2, Zeze::ByteBuffer::INTEGER);
                _o_.WriteLong(_x_);
            }
        }
        _o_.WriteByte(0);
    }

    virtual void Decode(Zeze::ByteBuffer& _o_) override {
        int _t_ = _o_.ReadByte();
        int _i_ = _o_.ReadTagSize(_t_);
        if (_i_ == 1) {
            Name = _o_.ReadString(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        if (_i_ == 2) {
            LocalId = _o_.ReadLong(_t_);
            _i_ += _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    int CompareTo(const AutoKey& _o_) const {
        if (&_o_ == this)
            return 0;
        int _c_;
        _c_ = Zeze::String::Compare(Name, _o_.Name);
        if (_c_ != 0)
            return _c_;
        _c_ = Zeze::Long::Compare(LocalId, _o_.LocalId);
        if (_c_ != 0)
            return _c_;
        return _c_;
    }

    bool operator<(const AutoKey& _o_) const {
        return CompareTo(_o_) < 0;
    }
};
}
}
