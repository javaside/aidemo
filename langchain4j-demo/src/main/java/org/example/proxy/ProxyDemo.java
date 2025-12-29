package org.example.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class ProxyDemo {
    public static void main(String[] args) {

        // 2. 创建调用处理器
        InvocationHandler handler = new UserServiceHandler();

        // 3. 创建代理实例
        UserService proxy = (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(), // 类加载器
                new Class<?>[] {UserService.class},   // 接口数组
                handler                              // 调用处理器
        );

        // 4. 通过代理调用方法
        proxy.addUser("张三");
        System.out.println(proxy.getUser("李四"));
        System.out.println(proxy.getUser("张三"));
    }
}