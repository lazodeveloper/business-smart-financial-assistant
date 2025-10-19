package com.bcp.ia.asistent.caller;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;

import static com.bcp.ia.asistent.util.constants.Constants.*;


@Service
@RequiredArgsConstructor
public class OpenAiAssistantService {
    private final OpenAIClient openAIClient;

    public OpenAiAssistantService() {

        this.openAIClient = new OpenAIClientBuilder()
                .endpoint(API_ENDPOINT_GPT)
                .credential(new AzureKeyCredential(API_KEY_GPT))
                .buildClient();
    }

    public Mono<String> generateFinancialReport(String clientFinancialData) {

        List<ChatRequestMessage> chatMessages = Arrays.asList(
                new ChatRequestSystemMessage(
                "Actúa como un Asesor Financiero Senior. Genera un resumen ejecutivo DIRECTO para el cliente " +
                        ", basándote en el JSON provisto. Tu respuesta debe ser corta (máximo 1000 caracteres), " +
                        "profesional y precisa. NO uses tablas ni recálculos. Solo explica los beneficios (ahorro en dinero y tiempo) " +
                        "de la Consolidación y el Plan Optimizado comparados con el Pago Mínimo, " +
                        "y establece la recomendación final. retornar respuesta en markdown. " +
                        "el tipo de moneda es en soles y resalta los valores importantes en negrita."),
                new ChatRequestUserMessage(
                        "Genera el informe basado en estos datos del cliente y los escenarios:\n" +
                                clientFinancialData)
        );

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);
        chatCompletionsOptions.setMaxTokens(1024);

        return Mono.fromCallable(() -> {
                    ChatCompletions chatCompletions = openAIClient.getChatCompletions(
                            DEPLOYMENT_NAME_GPT,
                            chatCompletionsOptions
                    );
                    if (chatCompletions != null && !chatCompletions.getChoices().isEmpty()) {
                        return chatCompletions.getChoices().get(0).getMessage().getContent();
                    } else {
                        throw new RuntimeException("Respuesta de OpenAI vacía o inválida.");
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

}
