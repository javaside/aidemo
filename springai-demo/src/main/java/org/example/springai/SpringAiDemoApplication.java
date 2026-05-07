package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringAiDemoApplication implements ApplicationRunner {

    private final ChatClient chatClient;

    public SpringAiDemoApplication(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SpringAiDemoApplication.class);
        //配置非web项目
        //application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("ddds");
        System.out.println(chatClient.prompt().user("你好?").call().content());
    }
}
