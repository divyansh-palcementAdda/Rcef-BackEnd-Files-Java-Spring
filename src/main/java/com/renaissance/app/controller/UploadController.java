package com.renaissance.app.controller;

//@RestController
//@RequestMapping("/api/uploads")
//public class UploadController {
//
//    private final UploadService uploadService;
//
//    public UploadController(UploadService uploadService) {
//        this.uploadService = uploadService;
//    }
//
//    @PostMapping(consumes = "multipart/form-data")
//    public ResponseEntity<UploadResult> uploadFile(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam(value = "requestId", required = false) String requestId,
//            Principal principal) throws Exception {
//        UploadResult result = uploadService.storeTemporaryAndPush(file, requestId, principal);
//        return ResponseEntity.ok(result);
//    }
//}
