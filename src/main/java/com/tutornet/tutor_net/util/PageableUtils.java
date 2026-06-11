package com.tutornet.tutor_net.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableUtils {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private PageableUtils() {} // Prevent instantiation

    public static Pageable build(Integer page, Integer size, Integer limit,
                                 String sortBy, String sortDir) {
        int effectiveSize = resolveSize(size, limit);
        int jpaPage = Math.max(0, (page != null ? page : 1) - 1);
        Sort sort = buildSort(sortBy, sortDir);
        return PageRequest.of(jpaPage, Math.min(effectiveSize, MAX_PAGE_SIZE), sort);
    }

    private static int resolveSize(Integer size, Integer limit) {
        if (limit != null && limit > 0) return limit;
        if (size  != null && size  > 0) return size;
        return DEFAULT_PAGE_SIZE;
    }

    private static Sort buildSort(String sortBy, String sortDir) {
        String field = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        return "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(field).ascending()
                : Sort.by(field).descending();
    }
}