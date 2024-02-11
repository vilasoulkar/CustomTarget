package com.manh.custom.target.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInputData {

	private String mainTargetName;
	private String failedSubTargetName;
	private String customTargetName;
}
