package common.server;

import com.xl.rpc.annotation.RpcRequest;

/**
 * Created by Administrator on 2015/8/7.
 */
public class SameControl implements ISameControl{
    @Override
    public void sameRequest(@RpcRequest String param) {
        System.out.println(param);
    }
}
