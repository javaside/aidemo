package org.example;

import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WanxImageModelTest {
    public static void main(String[] args) {
        WanxImageModel model = WanxImageModel.builder()
                //.baseUrl("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation")
                .apiKey("sk-f126474509894a68acf2ff0140890279")
                //.modelName(DALL_E_3)
                //.modelName("qwen-image")
                .build();

        Response<Image> response = model.generate("a man in a suit and tie");

        try {
            // 获取图片URI并转换为URL
            java.net.URI imageUri = response.content().url();
            java.net.URL imageUrl = imageUri.toURL();

            // 创建目标路径
            Path destination = Paths.get("/tmp/donald_duck.jpg");

            // 从URL下载并保存图片
            try (InputStream in = imageUrl.openStream()) {
                Files.copy(in, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            System.out.println("图片已保存至: " + destination.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
