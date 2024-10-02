package com.example.demo;

import jakarta.annotation.PostConstruct;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_java;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/v2/media")
public class Controller {

    @PostConstruct
    void init() {
        Loader.load(opencv_java.class);
    }

    @GetMapping
    public ResponseEntity<StreamingResponseBody> processMedia(@RequestParam Map<String, String> params) throws IOException {
        String inputMediaPath = "/path/to/media.png";
        Path outputMediaPath = Files.createTempFile("test", ".png");


        try (PointerScope pointerScope = new PointerScope()) {
            Mat mat = opencv_imgcodecs.imread(String.valueOf(inputMediaPath));
            Mat resizedMat = new Mat();
            BytePointer bytePointer = new BytePointer(String.valueOf(outputMediaPath));
            if (mat.empty()) {
                throw new RuntimeException("Could not read the input image.");
            }

            String newWidth = params.get("w");
            String newHeight = params.get("h");
            Size size = new Size(Integer.parseInt(newWidth), Integer.parseInt(newHeight));

            // Resize the image
            opencv_imgproc.resize(mat, resizedMat, size, 0D, 0D, opencv_imgproc.INTER_AREA);

            // Write the resized image to the output path
            opencv_imgcodecs.imwrite(bytePointer, resizedMat);

            // Stream the file as response and clean up after
            StreamingResponseBody responseBody = outputStream -> {
                try (InputStream fileStream = new FileInputStream(String.valueOf(outputMediaPath))) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } finally {
                    // Clean up the temporary file
                    Files.deleteIfExists(outputMediaPath);
                }
            };

            mat.deallocate();
            resizedMat.deallocate();
            size.deallocate();
            bytePointer.deallocate();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputMediaPath.toFile().getName() + "\"");

            System.out.println(Pointer.physicalBytes()/ (1024*1024));
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.IMAGE_PNG)
                    .body(responseBody);

        }
    }
}