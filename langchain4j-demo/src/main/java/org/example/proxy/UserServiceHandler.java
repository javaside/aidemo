package org.example.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class UserServiceHandler implements InvocationHandler {
    //private Map<String,String> cache = new HashMap<>();
    private UserService userService = new UserServiceImpl();

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 前置增强
        System.out.println("方法调用前: " + method.getName());
        String name = (String)args[0];

        Object result = method.invoke(userService, args);

//        if(method.getName().equals("addUser")){
//
//            cache.put(name, name);
//        }
//
//        if(method.getName().equals("getUser")){
//            result = cache.get(name);
//        }

        // 后置增强
        System.out.println("方法调用后: " + method.getName());
        return result+":invoked";
    }
}