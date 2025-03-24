package cn.mengxi.api;

import org.springframework.ai.chat.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * @author zhumang
 * @description
 * @date 2025/3/13 15:28
 */
public interface IAiService {
     ChatResponse generate(String model, String message);
     Flux<ChatResponse> generateStream(String model, String message);
     Flux<ChatResponse> generateStreamRag(String model,String tag, String message);
}
