package com.xl.rpc.dispatch.message;

import com.xl.rpc.codec.PracticalBuffer;

/**
 * Created by Caedmon on 2015/7/11.
 */
public interface MessageProxy{
    Object decode(PracticalBuffer data) throws Exception;
    PracticalBuffer encode(Object bean) throws Exception;
}
