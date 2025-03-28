package cn.mengxi.trigger.http;

import cn.mengxi.api.IRAGService;
import cn.mengxi.api.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

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
    private TokenTextSplitter tokenTextSplitter;
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
     * 修改向量库维度：
     *    ALTER TABLE vector_store
     *    ALTER COLUMN embedding TYPE VECTOR(768);
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
    /**
     * @author zhumang
     * @date 2025/3/15 15:35
     * @description clone git 仓库然后分割上传至向量库
     * @params
     * @return
     */
    @RequestMapping(value = "analyze_git_repository", method = RequestMethod.POST)
    @Override
    public Response<String> analyzeGitRepository(@RequestParam("repoUrl") String repoUrl, @RequestParam("userName") String userName, @RequestParam("token") String token){
        String localPath = "./git-cloned-repo";
        String repoProjectName = extractProjectName(repoUrl);
        log.info("克隆路径：{}", new File(localPath).getAbsolutePath());
        try {
            FileUtils.deleteDirectory(new File(localPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Git git = null;
        try {
            git = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                    .call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
        try {
            Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    log.info("{} 遍历解析路径，上传知识库:{}", repoProjectName, file.getFileName());
                    try {
                        TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
                        List<Document> documents = reader.get();
                        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
                        documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
                        documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
                        pgVectorStore.accept(documentSplitterList);
                    } catch (Exception e) {
                        log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
            FileUtils.deleteDirectory(new File(localPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RList<String> elements = redissonClient.getList("ragTag");
        if (!elements.contains(repoProjectName)) {
            elements.add(repoProjectName);
        }
        git.close();
        log.info("遍历解析路径，上传完成:{}", repoUrl);
        return Response.<String>builder().code(0).info("调用成功").build();
    }

    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String projectNameWithGit = parts[parts.length - 1];
        return projectNameWithGit.replace(".git", "");
    }
}
