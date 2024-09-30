package boluo.repositories;

import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.*;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 阿里云存储
 * */
@Setter
@Component
@ConditionalOnProperty(value = "oss.enable", matchIfMissing = false)
public class OssURLRepository implements URLRepository, InitializingBean, DisposableBean {

    @Resource
    private OssConfiguration ossConfig;
    private OSS ossClient;

    @Override
    public boolean support(URL url) {
        return "oss".equalsIgnoreCase(url.getProtocol());
    }

    @Override
    public boolean support(String url) {
        if(StrUtil.isBlank(url)) return false;
        return url.startsWith("oss:/");
    }

    @Override
    public boolean exist(URL url) {
        return ossClient.doesObjectExist(ossConfig.getBucketName(), trimObjectName(url.getPath()));
    }

    @Override
    public InputStream inputStream(URL url) {
        OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), trimObjectName(url.getPath()));
        return new OssInputStream(ossObject);
    }

    @Override
    public void upload(URL url, InputStream input) {
        if(url.getPath().endsWith("m3u8") || url.getPath().endsWith("M3U8")) {//m3u8单独处理
            uploadM3u8(url, input);
        }else {
            boolean multipart = false;
            if(input instanceof LocalFileInputStream lf) {
                long perFileMaxSize = DataSizeUtil.parse(ossConfig.getPerFileMaxSize());
                if(lf.getFile().length() > perFileMaxSize) {
                    multipart = true;
                }
            }
            if(multipart) {
                //分片上传
                try {
                    multipartUpload(url, input);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else {
                ossSimpleUpload(url.getPath(), input);
            }
        }
    }

    protected void multipartUpload(URL url, InputStream input) throws IOException {
        Assert.isTrue(input instanceof LocalFileInputStream, "m3u8 inputStream must be LocalFileInputStream");
        LocalFileInputStream lf = (LocalFileInputStream) input;
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(ossConfig.getBucketName(), trimObjectName(url.getPath()));
        ObjectMetadata md = new ObjectMetadata();
        Optional<MediaType> mtOpt = MediaTypeFactory.getMediaType(trimObjectName(url.getPath()));
        if(mtOpt.isPresent()) {
            md.setContentType(mtOpt.get().toString());
        }
        request.setObjectMetadata(md);
        InitiateMultipartUploadResult upResult = ossClient.initiateMultipartUpload(request);
        String uploadId = upResult.getUploadId();
        List<PartETag> partETags =  new ArrayList<>();
        final long partSize = DataSizeUtil.parse(ossConfig.getPerFileMaxSize());   //1 MB。
        long fileLength = lf.getFile().length();
        int partCount = (int) (fileLength / partSize);
        if (fileLength % partSize != 0) {
            partCount++;
        }
        // 遍历分片上传。
        for (int i = 0; i < partCount; i++) {
            long startPos = i * partSize;
            long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(ossConfig.getBucketName());
            uploadPartRequest.setKey(trimObjectName(url.getPath()));
            uploadPartRequest.setUploadId(uploadId);
            InputStream inStream = new FileInputStream(lf.getFile());
            inStream.skip(startPos);
            uploadPartRequest.setInputStream(inStream);
            uploadPartRequest.setPartSize(curPartSize);
            uploadPartRequest.setPartNumber(i + 1);
            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());
        }
        CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(ossConfig.getBucketName(), trimObjectName(url.getPath()), uploadId, partETags);
        ossClient.completeMultipartUpload(completeMultipartUploadRequest);
    }

    protected void uploadM3u8(URL url, InputStream input) {
        Assert.isTrue(input instanceof LocalFileInputStream, "m3u8 inputStream must be LocalFileInputStream");
        LocalFileInputStream lf = (LocalFileInputStream) input;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            String s;
            StringBuilder content = new StringBuilder();
            List<String> m3u8List = new ArrayList<>();
            while((s = br.readLine()) != null) {
                content.append(s).append("\r\n");
                s = s.trim();
                if(s.isEmpty() || s.startsWith("#")) {
                    continue;
                }
                m3u8List.add(s);
            }
            if(!m3u8List.isEmpty()) {//上传文件
                String m3u8File = url.getPath();
                String dir = StrUtil.EMPTY;
                int index = m3u8File.lastIndexOf("/");
                if(index > 0) {
                    dir = m3u8File.substring(0, index);
                }
                String dirFinal = dir;
                String localDir = lf.getFile().getParent();
                List<OssUploadFileCommand> commands = m3u8List.stream().map(it ->
                        new OssUploadFileCommand(dirFinal + "/" + it, new File(localDir, it))).toList();
                for(OssUploadFileCommand command : commands) {
                    ossSimpleUpload(command.getObjectName(), command.getLocalFile());
                }
                ossSimpleUpload(url.getPath(), new ByteArrayInputStream(content.toString().getBytes()));
                //删除本地文件
                commands.forEach(it -> it.getLocalFile().delete());
            }
        }catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected void ossSimpleUpload(String objectName, File file) throws IOException {
        try(FileInputStream input = new FileInputStream(file)) {
            ossSimpleUpload(objectName, input);
        }
    }

    protected void ossSimpleUpload(String objectName, InputStream input) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(ossConfig.getBucketName(), trimObjectName(objectName), input);
        ObjectMetadata md = new ObjectMetadata();
        Optional<MediaType> mtOpt = MediaTypeFactory.getMediaType(objectName);
        if(mtOpt.isPresent()) {
            md.setContentType(mtOpt.get().toString());
        }
        putObjectRequest.setMetadata(md);
        ossClient.putObject(putObjectRequest);
    }

    private String trimObjectName(String objectName) {
        if(objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }
        return objectName;
    }

    @Override
    public URL toURL(String url) {
        try {
            return new URL("oss", null, -1, url.substring(4), new OSSURLStreamHandler());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() {
        Objects.requireNonNull(ossConfig.getAccessKey(), "oss.accessKey is null");
        Objects.requireNonNull(ossConfig.getAccessSecret(), "oss.accessKey is null");
        Objects.requireNonNull(ossConfig.getEndpoint(), "oss.endpoint is null");
        Objects.requireNonNull(ossConfig.getRegion(), "oss.getRegion is null");
        DataSizeUtil.parse(ossConfig.getPerFileMaxSize());
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(ossConfig.getAccessKey(), ossConfig.getAccessSecret());
        ossClient = OSSClientBuilder.create()
                .endpoint(ossConfig.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(ossConfig.getRegion())
                .build();
    }

    @Override
    public void destroy() {
        ossClient.shutdown();
    }

    static class OSSURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL u) {
            return null;
        }

    }

    static class OssInputStream extends InputStream {

        private final InputStream input;
        private final OSSObject ossObject;

        public OssInputStream(OSSObject ossObject) {
            this.ossObject = ossObject;
            this.input = ossObject.getObjectContent();
        }

        @Override
        public int read() throws IOException {
            return input.read();
        }

        @Override
        public void close() throws IOException {
            input.close();
            ossObject.close();
        }

    }

    @Getter
    static class OssUploadFileCommand {

        private String objectName;
        private File localFile;

        public OssUploadFileCommand(String objectName, File localFile) {
            this.objectName = objectName;
            this.localFile = localFile;
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationProperties(prefix = "oss")
    @Setter
    @Getter
    public static class OssConfiguration {
        private String accessKey;
        private String accessSecret;
        private String endpoint;
        private String region;
        private String bucketName;
        private String perFileMaxSize = "100M";
    }

}
