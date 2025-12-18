#pragma once

#include "zeze/cxx/Bean.h"

namespace demo {
namespace Module1 {
// beankey的注释
class EmptyKey : public Zeze::Serializable {
public:

    // for decode only
    EmptyKey() {
    }

    void Assign(const EmptyKey& other) {
    }

    EmptyKey& operator=(const EmptyKey& other) {
        Assign(other);
        return *this;
    }

    virtual void Encode(Zeze::ByteBuffer& _o_) const override {
        _o_.WriteByte(0);
    }

    virtual void Decode(Zeze::ByteBuffer& _o_) override {
        int _t_ = _o_.ReadByte();
        _o_.ReadTagSize(_t_);
        while (_t_ != 0) {
            _o_.SkipUnknownField(_t_);
            _o_.ReadTagSize(_t_ = _o_.ReadByte());
        }
    }

    int CompareTo(const EmptyKey& _o_) const {
        if (&_o_ == this)
            return 0;
        int _c_ = 0;
        return _c_;
    }

    bool operator<(const EmptyKey& _o_) const {
        return CompareTo(_o_) < 0;
    }
};
}
}
