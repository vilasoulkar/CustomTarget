package com.manh.custom.target.generator.service;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage")
public class StorageProperties {

	/**
	 * Folder location for storing files
	 */
	private String location = "upload-dir";
	private List<String> mainTargetTags;
	private List<String> subTargetTags;
	private String ignoreCustomTargetGenerationIfNotCompleted;

	public String getIgnoreCustomTargetGenerationIfNotCompleted() {
		return ignoreCustomTargetGenerationIfNotCompleted;
	}

	public void setIgnoreCustomTargetGenerationIfNotCompleted(String ignoreCustomTargetGenerationIfNotCompleted) {
		this.ignoreCustomTargetGenerationIfNotCompleted = ignoreCustomTargetGenerationIfNotCompleted;
	}

	public List<String> getMainTargetTags() {
		return mainTargetTags;
	}

	public void setMainTargetTags(List<String> mainTargetTag) {
		this.mainTargetTags = mainTargetTag;
	}

	public String getLocation() {
		return location;
	}

	public List<String> getSubTargetTags() {
		return subTargetTags;
	}

	public void setSubTargetTags(List<String> subTargetTags) {
		this.subTargetTags = subTargetTags;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
