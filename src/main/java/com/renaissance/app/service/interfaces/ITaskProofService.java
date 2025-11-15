package com.renaissance.app.service.interfaces;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.model.TaskProof;
import com.renaissance.app.model.User;

public interface ITaskProofService {

	TaskProof uploadProof(MultipartFile file, Long taskId, Long requestId, User uploadedBy)
			throws IOException, BadRequestException;

}
