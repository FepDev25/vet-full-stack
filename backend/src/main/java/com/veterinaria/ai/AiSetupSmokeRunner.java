package com.veterinaria.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Smoke test de arranque para el setup de IA.
 *
 * Verifica que los beans de Spring AI (ChatModel + EmbeddingModel) estan
 * correctamente cableados a partir de las API keys del .env. NO realiza
 * llamadas a los providers (eso cuesta dinero y se hace bajo demanda).
 *
 * Si las API keys faltan en el entorno, el contexto de Spring ni siquiera
 * arranca, asi que el solo echo de este runner es prueba suficiente de
 * que el setup esta OK.
 */
@Component
public class AiSetupSmokeRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AiSetupSmokeRunner.class);

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;

    @Value("${spring.ai.anthropic.chat.model:unknown}")
    private String chatModelName;

    @Value("${spring.ai.google.genai.embedding.text.model:unknown}")
    private String embeddingModelName;

    @Value("${spring.ai.anthropic.chat.temperature:N/A}")
    private String chatTemperature;

    public AiSetupSmokeRunner(ChatModel chatModel, EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        log.info("AI Setup Smoke Test");
        log.info("  ChatModel: {} ({})",
                chatModel.getClass().getSimpleName(), chatModelName);
        log.info("    temperature: {}", chatTemperature);
        log.info("  EmbeddingModel: {} ({})",
                embeddingModel.getClass().getSimpleName(), embeddingModelName);
        log.info("    expected dimensions: 768 (text-multilingual-embedding-002)");
        log.info("  Next step: add pgvector for RAG (feature #5)");
    }
}
