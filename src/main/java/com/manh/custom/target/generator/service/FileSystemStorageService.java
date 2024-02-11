package com.manh.custom.target.generator.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.manh.custom.target.generator.model.ProcessedResult;
import com.manh.custom.target.generator.model.UserInputData;

@Service
public class FileSystemStorageService implements StorageService {

	private final Path rootLocation;
	private StorageProperties properties;

	@Autowired
	public FileSystemStorageService(StorageProperties properties) {

		if (properties.getLocation().trim().length() == 0) {
			throw new StorageException("File upload location can not be Empty.");
		}

		this.rootLocation = Paths.get(properties.getLocation());
		this.properties = properties;
	}

	@Override
	public ProcessedResult store(MultipartFile file, UserInputData data) {
		ProcessedResult result = new ProcessedResult();
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file.");
			}

			Path destinationFile = this.rootLocation.resolve(Paths.get(file.getOriginalFilename())).normalize()
					.toAbsolutePath();
			if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
				// This is a security check
				throw new StorageException("Cannot store file outside current directory.");
			}
			

			String customTargetContent = findOutFailedTargetAndCreateCustomTarget(data, file, result).getCustomTargetString();
			String content = "";

			try (InputStream inputStream = file.getInputStream()) {
				content = new BufferedReader(new InputStreamReader(inputStream)).lines()
						.collect(Collectors.joining("\n"));

			} catch (Exception e) {
				e.printStackTrace();
			}

//			String content = new String(
//					java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(file.getOriginalFilename()).normalize()));

			int projectEndIndex = content.indexOf("</project>");
			if (projectEndIndex != -1 && customTargetContent != null) {
				int customTargetStartIndex = content.indexOf("<target name=\"" + data.getCustomTargetName() + "\">");
				if (customTargetStartIndex != -1) {
					int customTargetEndIndex = content.indexOf("</target>", customTargetStartIndex);
					if (customTargetEndIndex != -1) {
						content = content.substring(0, customTargetStartIndex) + customTargetContent
								+ content.substring(customTargetEndIndex + "</target>".length());
					}
				} else {
					// System.err.println(customTarget);
					content = content.substring(0, projectEndIndex) + customTargetContent + "\n</project>";
				}
				Files.writeString(destinationFile, content, StandardCharsets.UTF_8,
						StandardOpenOption.TRUNCATE_EXISTING);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootLocation, 1).filter(path -> !path.equals(this.rootLocation))
					.map(this.rootLocation::relativize);
		} catch (IOException e) {
			throw new StorageException("Failed to read stored files", e);
		}

	}

	@Override
	public Path load(String filename) {
		return rootLocation.resolve(filename);
	}

	@Override
	public Resource loadAsResource(String filename) {
		try {
			Path file = load(filename);
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new StorageFileNotFoundException("Could not read file: " + filename);

			}
		} catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("Could not read file: " + filename, e);
		}
	}

	@Override
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(rootLocation.toFile());
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(rootLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}

	public ProcessedResult findOutFailedTargetAndCreateCustomTarget(UserInputData data, MultipartFile file, ProcessedResult result) {
		StringBuilder customTarget = new StringBuilder();

		boolean foundMainTarget = false;
		boolean startCopying = false;
		boolean runOrderCalsTargetFound = false;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String line;
			String lineDataAfterTrim;
			Stack<String> tagStack = new Stack<String>();
			while ((line = reader.readLine()) != null) {
				lineDataAfterTrim = line.trim();
				if (isMainTargetFound(lineDataAfterTrim, data.getMainTargetName())) {
					foundMainTarget = true;
					System.out.println("Main target found: " + data.getMainTargetName());
				}
				if (foundMainTarget) {

					if (lineDataAfterTrim.contains(
							"targetName=\"" + properties.getIgnoreCustomTargetGenerationIfNotCompleted() + "\">")) { // targetName="run-order-calcs"
						runOrderCalsTargetFound = true;
					}
					if (startCopying == false && isSubTargetFound(lineDataAfterTrim, data.getFailedSubTargetName())) {
						// || line.contains("targetName=\""+failedSubtargetName+"\"")) {

						if (runOrderCalsTargetFound == false) {
							System.out.println(properties.getIgnoreCustomTargetGenerationIfNotCompleted()
									+ " not found before the failed target rerun the whole night job");
							break;
						}
						System.out.println("Subtarget found: " + data.getFailedSubTargetName());
						startCopying = true;
						customTarget.append("<target name=\"" + data.getCustomTargetName() + "\">\n");
						tagStack.push("target");
						System.out.println("Stack elements before start copying::" + tagStack);
						continue;
					}
					if (startCopying) {

						if (lineDataAfterTrim != null && lineDataAfterTrim.startsWith("<")
								&& lineDataAfterTrim.startsWith("</") == false && lineDataAfterTrim.endsWith(">")
								&& lineDataAfterTrim.endsWith("/>") == false
								&& lineDataAfterTrim.startsWith("<!--") == false) {
							tagStack.push(lineDataAfterTrim.substring(1,
									lineDataAfterTrim.indexOf(" ") > 0 ? lineDataAfterTrim.indexOf(" ")
											: lineDataAfterTrim.length() - 1));
							System.out.println("Stack elements after copying::" + tagStack);
						}
						if (lineDataAfterTrim != null && lineDataAfterTrim.startsWith("</")) {

							if (tagStack.isEmpty() || tagStack.lastElement()
									.equals(lineDataAfterTrim.substring(2, lineDataAfterTrim.indexOf(">"))) == false) {
								continue;
							}
							if (!tagStack.isEmpty() && tagStack.lastElement()
									.equals(lineDataAfterTrim.substring(2, lineDataAfterTrim.indexOf(">"))))
								tagStack.pop();
							System.out.println("Stack elements after pop::" + tagStack);
						}
						customTarget.append(line).append("\n");
						if (lineDataAfterTrim.contains("</target>")) {
							startCopying = false;
							break; // Stop copying after the end of the main target
						}
					}
				}
			}
			
			if (!foundMainTarget) {
				result.setResultMessage("Given Maintarget not found");
			}
			
			if (foundMainTarget && !startCopying) {
				result.setResultMessage("Given Subtarget not found");
			}

			if (foundMainTarget && customTarget.length() > 0) {
				result.setResultMessage("You successfully uploaded " + file.getOriginalFilename() + "!");
				result.setCustomTargetString(customTarget.toString());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private boolean isMainTargetFound(String line, String maintargetName) {
		// System.err.println(line);
//		properties.getMainTargetTags().stream().map(e->(e+"\""+maintargetName+"\"")).forEach(System.out::println);
//		Predicate<CharSequence> p1 = e -> line.contains(e);  
//		System.out.println(properties.getMainTargetTags().stream().map(e->(e+"\""+maintargetName+"\"")).peek(System.out::println).anyMatch(p1));
//		
//		System.out.println(properties.getMainTargetTags().stream().map(e->(e+"\""+maintargetName+"\"")).filter(e->line.contains(e)).findFirst().orElse(null) !=null);
//		
//		return properties.getMainTargetTags().stream().map(e->(e+"\""+maintargetName+"\""))
//				.filter(e->line.contains(e)).peek(System.out::println).findFirst().orElse(null) != null;

		for (String s : properties.getMainTargetTags()) {
			if (line.contains(s + "\"" + maintargetName + "\""))
				return true;
		}
		return false;
	}

	private boolean isSubTargetFound(String line, String subTargetName) {
//		Predicate<CharSequence> p1 = e -> line.contains(e);  
//		return properties.getSubTargetTags().stream().map(e->(e+"\""+subTargetName+"\"")).anyMatch(p1);

		for (String s : properties.getSubTargetTags()) {
			
			if (line.contains(s + "\"" + subTargetName + "\""))
				return true;
		}
		return false;
	}
}
