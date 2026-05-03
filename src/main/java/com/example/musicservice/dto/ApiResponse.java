package com.example.musicservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
	private boolean success;
	private String message;
	private T data;
	private List<ErrorDetail> errors;

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, "Success", data, null);
	}

	public static <T> ApiResponse<T> success(String message, T data) {
		return new ApiResponse<>(true, message, data, null);
	}

	public static <T> ApiResponse<T> error(String message) {
		return new ApiResponse<>(false, message, null, null);
	}

	public static <T> ApiResponse<T> error(String message, List<ErrorDetail> errors) {
		return new ApiResponse<>(false, message, null, errors);
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ErrorDetail {
		private String field;
		private String message;
	}
}
