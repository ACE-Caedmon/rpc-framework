package common;

import common.UserInfo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/7/13.
 */
@Component
public class UserService {
    private Map<String,UserInfo> userInfoMap=new ConcurrentHashMap<>();
    public UserInfo getUserInfo(String userName){
        return userInfoMap.get(userName);
    }
    public void addUserInfo(UserInfo userInfo){
        userInfoMap.put(userInfo.getUsername(),userInfo);
    }
}
