package cn.mengxi.trigger.http;

import cn.mengxi.api.IRAGService;
import cn.mengxi.api.response.Response;
import jakarta.annotation.Resource;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author zhumang
 * @description Rag接口服务
 * @date 2025/3/14 18:13
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/rag/")
public class RAGController implements IRAGService {
    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private SimpleVectorStore simpleVectorStore;
    @Resource
    private PgVectorStore pgVectorStore;
    @Resource
    private RedissonClient redissonClient;
    /**
     * @author zhumang
     * @date 2025/3/14 18:47
     * @description Rag 知识库查询接口
     * @params
     * @return
     */
    @RequestMapping(value = "query_rag_tag_list", method = RequestMethod.GET)
    @Override
    public Response<List<String>> queryRagTagList() {
        RList<String> elements = redissonClient.getList("ragTag");
        return Response.<List<String>>builder()
                .code(0)
                .info("调用成功")
                .data(elements)
                .build();
    }
    /**
     * @author zhumang
     * @date 2025/3/14 18:47
     * @description Rag 知识上传接口
     * @params
     * @return
     */
    @RequestMapping(value = "upload_rag_tag", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    @Override
    public Response<String> uploadFile(@RequestParam String ragTag,@RequestParam("file") List<MultipartFile> files) {
        log.info("上传知识库开始{}", ragTag);
        for(MultipartFile file:files){
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = documentReader.get();
            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
            documents.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称"));
            documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称"));
            pgVectorStore.accept(documentSplitterList);
            RList<String> elements =  redissonClient.getList("ragTag");
            if(!elements.contains(ragTag)){
                elements.add(ragTag);
            }
        }
        log.info("知识库上传完成{}", ragTag);
        return Response.<String>builder().code(0).info("调用成功").build();
    }
}
