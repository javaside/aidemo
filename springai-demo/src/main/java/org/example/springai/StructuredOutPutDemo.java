package org.example.springai;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.List;
import java.util.Map;

public class StructuredOutPutDemo {
    public static void main(String[] args) {
        BeanOutputConverter<ActorsFilms> beanOutputConverter =
                new BeanOutputConverter<>(ActorsFilms.class);

        String format = beanOutputConverter.getFormat();

        System.out.println(format);


        String userInputTemplate = """
        ... user text input ....
        {format}
        """; // user input with a "format" placeholder.
        Prompt prompt = new Prompt(
                PromptTemplate.builder()
                        .template(userInputTemplate)
                        .variables(Map.of("format", beanOutputConverter.getFormat())) // replace the "format" placeholder with the converter's format.
						.build().createMessage());

        System.out.println(prompt);
    }
}

@JsonPropertyOrder({"actor", "movies"})
record ActorsFilms(String actor, List<String> movies) {}