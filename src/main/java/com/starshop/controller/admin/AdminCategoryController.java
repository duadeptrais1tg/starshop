package com.starshop.controller.admin;

import com.starshop.controller.CategoryManagementController;
import com.starshop.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin quản lý danh mục (toàn bộ xử lý nằm ở CategoryManagementController).
 */
@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController extends CategoryManagementController {

    public AdminCategoryController(CategoryService categoryService) {
        super(categoryService);
    }

    @Override
    protected String baseUrl() {
        return "/admin/categories";
    }
}
