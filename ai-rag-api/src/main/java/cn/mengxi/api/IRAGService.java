package cn.mengxi.api;

import cn.mengxi.api.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author zhumang
 * @description
 * @date 2025/3/14 17:33
 */
public interface IRAGService {
    Response<List<String>>  queryRagTagList();

    Response<String> uploadFile(String ragTag, List<MultipartFile> files);
}
