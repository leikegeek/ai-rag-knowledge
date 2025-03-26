package cn.mengxi.trigger.http;

import cn.mengxi.api.IAiService;
import jakarta.annotation.Resource;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhumang
 * @description AI服务 Ollama 实现
 * @date 2025/3/13 15:32
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/ollama/")
public class OllamaController implements IAiService {
    @Resource
    private OllamaChatModel chatModel;
    @Resource
    private PgVectorStore pgVectorStore;
    /**
     * @author zhumang
     * @date 2025/3/13 15:39
     * @description 
     * @params 
     * @return 
     */
    @RequestMapping(value = "generate", method = RequestMethod.GET)
    @Override
    public ChatResponse generate(@RequestParam  String model, @RequestParam String message) {
        return chatModel.call(new Prompt(message,OllamaOptions.builder().model(model).build()));
    }
    /**
     * @author zhumang
     * @date 2025/3/13 18:52
     * @description 
     * @params 
     * @return 
     */
    @RequestMapping(value = "generate_stream", method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> generateStream(String model, String message) {
        return chatModel.stream(new Prompt(message, OllamaOptions.builder().model(model).build()));
    }

    @RequestMapping(value = "generate_stream_rag", method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> generateStreamRag(@RequestParam("model") String model, @RequestParam("ragTag") String ragTag, @RequestParam("message") String message) {
        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;
        // 指定文档搜索
        SearchRequest request = SearchRequest.builder().query(message).topK(5)
                .filterExpression("knowledge == '" + ragTag + "'").build();

        List<Document> documents = pgVectorStore.similaritySearch(request);
        String documentCollectors = documents.stream().map(Document::getFormattedContent).collect(Collectors.joining());
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentCollectors));

        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        return chatModel.stream(new Prompt(
                messages,
                OllamaOptions.builder()
                        .model(model).build()
        ));
    }
}
