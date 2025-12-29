package org.example.langchain4j.proxy;

public interface UserService {
    void addUser(String name);
    String getUser(String name);
}