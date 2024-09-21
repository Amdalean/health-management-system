package com.hms.test;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.exceptions.OllamaBaseException;
import io.github.amithkoujalgi.ollama4j.core.models.chat.*;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception{

        OllamaAPI ollamaAPI = new OllamaAPI("http://localhost:11434/");
        ollamaAPI.setRequestTimeoutSeconds(60);

        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance("qwen2");
        Scanner scanner = new Scanner(System.in);
        String input1 = scanner.nextLine();
        OllamaChatRequestModel requestModel = builder.withMessage(OllamaChatMessageRole.USER, input1).build();
        OllamaChatResult initialChatResult = ollamaAPI.chat(requestModel);
        System.out.println(initialChatResult.getResponse());
        List<OllamaChatMessage> history = initialChatResult.getChatHistory();

        while (true) {
            String input2 = scanner.nextLine();
            OllamaChatResult chatResult = ollamaAPI.chat(builder.withMessages(history).withMessage(OllamaChatMessageRole.USER, input2).build());
//            System.out.println(chatResult.getResponse());
            history = chatResult.getChatHistory();
        }
    }
}