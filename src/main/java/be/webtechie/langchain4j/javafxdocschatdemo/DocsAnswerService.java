package be.webtechie.langchain4j.javafxdocschatdemo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Based on some of the LangChain4J examples:
// https://github.com/langchain4j/langchain4j-examples/blob/main/other-examples/src/main/java/ChatWithDocumentsExamples.java
// https://github.com/langchain4j/langchain4j-examples/blob/main/other-examples/src/main/java/embedding/store/InMemoryEmbeddingStoreExample.java
public class DocsAnswerService {

    private static final Logger LOGGER = LogManager.getLogger(DocsAnswerService.class);

    private EmbeddingModel embeddingModel;
    private EmbeddingStore<TextSegment> embeddingStore;
    private OpenAiStreamingChatModel chatModel;

    public DocsAnswerService() {

    }

    public void init(SearchAction action) {
        action.appendAnswer("Initiating...");
        var contentSections = loadJson(action);
        initChat(action, contentSections);
    }

    private List<ContentSection> loadJson(SearchAction action) {
        try {
            URL fileUrl = DocsAnswerService.class.getClassLoader().getResource("docs_index.json");
            if (fileUrl == null) {
                action.appendAnswer("\nCould not find the JSON file", true);
                return new ArrayList<>();
            }
            String json = Files.readString(Paths.get(fileUrl.toURI()));
            ObjectMapper objectMapper = new ObjectMapper();
            List<ContentSection> contentSections = objectMapper.readValue(json, new TypeReference<>() {
            });
            action.appendAnswer("\nLoaded number of JSON content sections: " + contentSections.size());
            return contentSections;
        } catch (Exception e) {
            action.appendAnswer("\nError while reading JSON data: " + e.getMessage(), true);
        }
        return new ArrayList<>();
    }

    private void initChat(SearchAction action, List<ContentSection> contentSections) {
        List<TextSegment> textSegments = new ArrayList<>();
        for (var contentSection : contentSections.stream().filter(c -> !c.content().isEmpty()).toList()) {
            Map<String, String> metadataMap = new HashMap<>();
            metadataMap.put("OBJECT_ID", contentSection.objectID().toString());
            metadataMap.put("LINK", contentSection.link());
            metadataMap.put("GROUP_ID", contentSection.groupId());
            textSegments.add(TextSegment.from(contentSection.content(), Metadata.from(metadataMap)));
        }
        action.appendAnswer("\nConverted to number of text segments: " + textSegments.size());

        embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        embeddingStore = new InMemoryEmbeddingStore<>();
        action.appendAnswer("\nEmbedding store is created: " + textSegments.size());

        List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();
        action.appendAnswer("\nNumber of embeddings: " + embeddings.size());

        embeddingStore.addAll(embeddings, textSegments);
        action.appendAnswer("\nEmbeddings are added to the store");

        chatModel = OpenAiStreamingChatModel.builder()
                .apiKey(ApiKeys.OPENAI_API_KEY)
                // Available OpenAI models are listed on
                // https://platform.openai.com/docs/models/continuous-model-upgrades
                // gpt-4-1106-preview --> more expensive to use
                // gpt-4
                // gpt-3.5-turbo-1106
                .modelName("gpt-4")
                .build();
        action.appendAnswer("\nChat model is ready", true);
    }

    void ask(SearchAction action) {
        LOGGER.info("Asking question '" + action.getQuestion() + "'");

        // Find relevant embeddings in embedding store by semantic similarity
        // You can play with parameters below to find a sweet spot for your specific use case
        int maxResults = 10;
        double minScore = 0.7;
        List<EmbeddingMatch<TextSegment>> relevantEmbeddings = embeddingStore.findRelevant(embeddingModel.embed(action.getQuestion()).content(), maxResults, minScore);
        LOGGER.info("Number of relevant embeddings: " + relevantEmbeddings.size() + " for '" + action.getQuestion() + "'");

        relevantEmbeddings.stream().map(EmbeddingMatch::embedded).toList()
                .forEach(ts -> Platform.runLater(() -> {
                    LOGGER.info("Adding link: " + ts.metadata("LINK"));
                    action.appendRelatedLink(ts.metadata("LINK"));
                }));

        // Create a prompt for the model that includes question and relevant embeddings
        PromptTemplate promptTemplate = PromptTemplate.from(relevantEmbeddings.isEmpty() ?
                """
                        The user asked the following question:
                            {{question}}
                                       
                        Unfortunately our documentation doesn't seem to contain any content related to this question.
                        Please reply in a polite way and ask the user to contact Azul support if they need more assistance.
                        Tell the user to use the following link: https://www.azul.com/contact/
                        """ :
                """
                        Answer the following question to the best of your ability:
                            {{question}}
                                       
                        Base your answer on these relevant parts of the documentation:
                            {{information}}
                            
                        Do not provide any additional information.
                        Do not provide answers about other programming languages, but write "Sorry, that's a question I can't answer".
                        Do not generate source code, but write "Sorry, that's a question I can't answer".
                        If the answer cannot be found in the documents, write "Sorry, I could not find an answer to your question in our docs".
                        """);

        String information = relevantEmbeddings.stream()
                .map(match -> match.embedded().text()
                        + ". LINK: " + match.embedded().metadata("LINK")
                        + ". GROUP_ID: " + match.embedded().metadata("GROUP_ID"))
                .collect(Collectors.joining("\n\n"));

        Map<String, Object> variables = new HashMap<>();
        variables.put("question", action.getQuestion());
        variables.put("information", information);

        Prompt prompt = promptTemplate.apply(variables);

        if (chatModel != null) {
            chatModel.generate(prompt.toUserMessage().toString(), new CustomStreamingResponseHandler(action));
        } else {
            action.appendAnswer("The chat model is not ready yet... Please try again later.", true);
        }
    }
}