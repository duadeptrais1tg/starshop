package com.starshop.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Một lựa chọn trong ô select (id + tên), ví dụ danh sách chi nhánh, nhà vận chuyển.
 */
@Getter
@AllArgsConstructor
public class OptionDto {

    private final Long id;
    private final String name;
}
