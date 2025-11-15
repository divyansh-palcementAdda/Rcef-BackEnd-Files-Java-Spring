package com.renaissance.app.service.interfaces;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.payload.UploadResult;

public interface IUploadService {

	UploadResult storeTemporaryAndPush(MultipartFile file, Long requestId) throws IOException, BadRequestException;

}
