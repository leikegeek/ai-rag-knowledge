package cn.mengxi.app.test;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

/**
 * @author zhumang
 * @description
 * @date 2025/3/14 22:33
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class JGitTest {

    @Test
    public void test() throws GitAPIException {
        String repoURL = "";
        String username = "";
        String password = "";
        String localPath = "./clone-repo";
        log.info("克隆路径:{}",new File(localPath).getAbsolutePath());
        Git git = Git.cloneRepository()
                .setURI(repoURL)
                .setDirectory(new File(localPath))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .call();
    }




}
