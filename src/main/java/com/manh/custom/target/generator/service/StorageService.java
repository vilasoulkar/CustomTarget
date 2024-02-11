package com.manh.custom.target.generator.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.manh.custom.target.generator.model.ProcessedResult;
import com.manh.custom.target.generator.model.UserInputData;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {

	void init();

	//void store(MultipartFile file);

	Stream<Path> loadAll();

	Path load(String filename);

	Resource loadAsResource(String filename);

	void deleteAll();

	ProcessedResult store(MultipartFile file,UserInputData data);

}