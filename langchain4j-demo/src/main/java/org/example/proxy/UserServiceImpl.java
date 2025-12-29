package org.example.proxy;

public class UserServiceImpl implements UserService {
    @Override
    public void addUser(String name) {
        System.out.println("添加用户: " + name);
    }

    @Override
    public String getUser(String name) {
        return "用户: " + name;
    }
}