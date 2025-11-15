//package com.renaissance.app.controller;
//
//import java.security.Principal;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.renaissance.app.exception.BadRequestException;
//import com.renaissance.app.payload.ApiResult;
//import com.renaissance.app.payload.UploadResult;
//import com.renaissance.app.service.impl.UploadService;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//@RestController
//@RequestMapping("/api/uploads")
//@RequiredArgsConstructor
//@Slf4j
//public class UploadController {
//
//    private final UploadService uploadService;
//
//    // ==============================================================
//    // UPLOAD FILE
//    // ==============================================================
//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(summary = "Upload a file")
//    @ApiResponse(responseCode = "200", description = "File uploaded")
//    @ApiResponse(responseCode = "400", description = "Bad request")
//    public ResponseEntity<ApiResult<UploadResult>> uploadFile(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam(value = "requestId", required = false) String requestId,
//            Principal principal) {
//        try {
//            UploadResult result = uploadService.storeTemporaryAndPush(file, requestId, principal);
//            return ResponseEntity.ok(ApiResult.ok(result, "File uploaded successfully"));
//        } catch (BadRequestException e) {
//            return ResponseEntity.badRequest().body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
//        } catch (Exception e) {
//            log.error("Error uploading file", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResult.error("Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR));
//        }
//    }
//}