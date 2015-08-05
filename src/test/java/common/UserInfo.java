package common;

import java.io.Serializable;

/**
 * Created by Administrator on 2015/7/10.
 */
public class UserInfo implements Serializable{
    private int userId;
    private String username;
    private String password;
    private String qq;
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;

    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getQq() {
        return qq;
    }

    public void setQq(String qq) {
        this.qq = qq;
    }
}