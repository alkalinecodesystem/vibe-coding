package com.example.musicservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
	private List<T> content;
	private int totalPages;
	private long totalElements;
	private int currentPage;
	private int size;
	private boolean first;
	private boolean last;
	private boolean hasNext;
	private boolean hasPrevious;
}