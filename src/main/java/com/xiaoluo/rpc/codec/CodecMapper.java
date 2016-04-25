package com.xiaoluo.rpc.codec;

/**
 * Created by Administrator on 2015/8/22.
 */
public interface CodecMapper {
    PracticalBuffer encode();
    void decode(PracticalBuffer data);
}
