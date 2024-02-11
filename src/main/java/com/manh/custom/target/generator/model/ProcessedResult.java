package com.manh.custom.target.generator.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProcessedResult {

	int result;
	String resultMessage;
	String customTargetString;
}