package com.starshop.controller;

import com.starshop.dto.category.CategoryForm;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import com.starshop.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Xử lý chung cho màn hình quản lý danh mục. Lớp con chỉ khai báo @RequestMapping (đường dẫn gốc):
 * AdminCategoryController (/admin/categories), sau này ManagerCategoryController (/manager/categories).
 * JSP dùng biến baseUrl nên cùng một bộ view phục vụ được cả hai.
 */
public abstract class CategoryManagementController {

    private static final String LIST_VIEW = "admin/categories/list";
    private static final String FORM_VIEW = "admin/categories/form";
    private static final Map<String, String> MESSAGES = Map.of(
            "created", "Đã thêm danh mục.",
            "updated", "Đã cập nhật danh mục.",
            "toggled", "Đã đổi trạng thái hiển thị.",
            "deleted", "Đã xóa danh mục.");

    protected final CategoryService categoryService;

    protected CategoryManagementController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** Đường dẫn gốc, ví dụ "/admin/categories". */
    protected abstract String baseUrl();

    @ModelAttribute("baseUrl")
    public String baseUrlAttribute() {
        return baseUrl();
    }

    /**
     * @param active "true" = đang hiển thị, "false" = đang ẩn, rỗng = tất cả
     */
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String active,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String msg,
                       Model model) {
        Boolean activeFilter = "true".equals(active) ? Boolean.TRUE : "false".equals(active) ? Boolean.FALSE : null;
        model.addAttribute("page", categoryService.search(keyword, activeFilter, page - 1));
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);
        addMessage(msg, model);
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        return showForm(new CategoryForm(), null, model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CategoryForm form, BindingResult result,
                         @RequestParam(value = "image", required = false) MultipartFile image, Model model) {
        if (result.hasErrors()) {
            return showForm(form, null, model);
        }
        try {
            categoryService.create(form, image);
            return "redirect:" + baseUrl() + "?msg=created";
        } catch (BusinessException | InvalidFileException | FileStorageException e) {
            model.addAttribute("error", e.getMessage());
            return showForm(form, null, model);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(categoryService.getForm(id), id, model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CategoryForm form, BindingResult result,
                         @RequestParam(value = "image", required = false) MultipartFile image, Model model) {
        if (result.hasErrors()) {
            return showForm(withCurrentImage(form, id), id, model);
        }
        try {
            categoryService.update(id, form, image);
            return "redirect:" + baseUrl() + "?msg=updated";
        } catch (BusinessException | InvalidFileException | FileStorageException e) {
            model.addAttribute("error", e.getMessage());
            return showForm(withCurrentImage(form, id), id, model);
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        categoryService.toggleActive(id);
        return "redirect:" + baseUrl() + "?msg=toggled";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Model model) {
        try {
            categoryService.delete(id);
            return "redirect:" + baseUrl() + "?msg=deleted";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return list(null, null, 1, null, model);
        }
    }

    private String showForm(CategoryForm form, Long id, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("categoryId", id);
        model.addAttribute("parents", categoryService.parentOptions(id));
        return FORM_VIEW;
    }

    /** Khi form báo lỗi, giữ lại ảnh hiện tại để hiển thị (form gửi lên không chứa URL ảnh). */
    private CategoryForm withCurrentImage(CategoryForm form, Long id) {
        form.setCurrentImageUrl(categoryService.getForm(id).getCurrentImageUrl());
        return form;
    }

    private static void addMessage(String msg, Model model) {
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
    }
}
