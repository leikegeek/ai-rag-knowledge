package cn.mengxi.app.config;


import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhumang
 * @description Ollama配置文件
 * @date 2025/3/13 17:31
 */
@Configuration
public class OllamaConfig {

    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl){
         return new OllamaApi(baseUrl);
    }
    @Bean
    public OllamaChatClient ollamaChatClient(OllamaApi ollamaApi){
        return new OllamaChatClient(ollamaApi);
    }
}
