package d10.backend.Controller;

import d10.backend.Service.S3Service;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;

@RestController
@RequestMapping("/img")
@AllArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @GetMapping
    public String getPresignedUrl(@RequestParam String fileName) {
        String bucketName = "fa-sa-bucket";
        URL url = s3Service.generatePresignedUrl(bucketName, fileName);
        return url.toString();
    }

}
