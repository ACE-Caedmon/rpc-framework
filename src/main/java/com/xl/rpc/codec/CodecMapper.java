package com.xl.rpc.codec;

import com.xl.rpc.codec.PracticalBuffer;

/**
 * Created by Administrator on 2015/8/22.
 */
public interface CodecMapper {
    PracticalBuffer encode();
    void decode(PracticalBuffer data);
}
