package com.smartparking.core.response;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> data;
    private int page;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean lastPage;

    public PaginatedResponse() {}

    public PaginatedResponse(List<T> data, int page, int pageSize, long totalElements) {
        this.data = data;
        this.page = page;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        this.lastPage = page >= totalPages;
    }

    // Gettery
    public List<T> getData() { return data; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isLastPage() { return lastPage; }

    // Settery
    public void setData(List<T> data) { this.data = data; }
    public void setPage(int page) { this.page = page; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public void setLastPage(boolean lastPage) { this.lastPage = lastPage; }
}