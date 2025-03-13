package cn.mengxi.trigger.http;

import cn.mengxi.api.IAiService;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author zhumang
 * @description AI服务 Ollama 实现
 * @date 2025/3/13 15:32
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/ollama/")
public class OllamaController implements IAiService {

    private OllamaChatClient chatClient;
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
        return chatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
    /**
     * @author zhumang
     * @date 2025/3/13 18:52
     * @description 
     * @params 
     * @return 
     */
    @Override
    public Flux<ChatResponse> generateStream(String model, String message) {
        return chatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
}
