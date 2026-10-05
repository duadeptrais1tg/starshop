package com.starshop.controller.vendor;

import com.starshop.dto.product.VendorProductForm;
import com.starshop.dto.product.VendorProductStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.CategoryService;
import com.starshop.service.VendorProductService;
import com.starshop.util.EnumParams;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Vendor quản lý sản phẩm: danh sách (tìm, lọc, phân trang), thêm, sửa, ảnh, bật/tắt bán, xóa.
 * Quyền sở hữu sản phẩm được kiểm tra trong VendorProductService.
 */
@Controller
@RequestMapping("/vendor/products")
@RequiredArgsConstructor
public class VendorProductController {

    private static final String LIST_VIEW = "vendor/products/list";
    private static final String FORM_VIEW = "vendor/products/form";
    private static final Map<String, String> MESSAGES = Map.of(
            "created", "Đã thêm sản phẩm.",
            "updated", "Đã lưu sản phẩm.",
            "deleted", "Đã xóa sản phẩm.",
            "hidden", "Sản phẩm đã có đơn hàng nên không xóa được: đã chuyển sang ngừng bán.",
            "toggled", "Đã cập nhật trạng thái bán.",
            "thumbnail", "Đã đổi ảnh đại diện.",
            "imageDeleted", "Đã xóa ảnh.");

    private final VendorProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String msg,
                       Model model) {
        VendorProductStatus selected = EnumParams.parse(VendorProductStatus.class, status);
        String trimmed = keyword == null ? null : keyword.trim();
        if (trimmed != null && trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        model.addAttribute("page", productService.search(user.getId(), trimmed, categoryId, selected, page - 1));
        model.addAttribute("totalProducts", productService.countAll(user.getId()));
        model.addAttribute("keyword", trimmed);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("selectedStatus", selected);
        model.addAttribute("statuses", VendorProductStatus.values());
        model.addAttribute("categories", categoryService.activeOptions());
        addMessage(msg, model);
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        return showForm(null, new VendorProductForm(), null, model);
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserPrincipal user,
                         @Valid @ModelAttribute("form") VendorProductForm form, BindingResult result,
                         @RequestParam(value = "images", required = false) List<MultipartFile> images,
                         Model model) {
        if (!result.hasErrors()) {
            try {
                Long id = productService.create(user.getId(), form, images);
                return "redirect:/vendor/products/" + id + "/edit?msg=created";
            } catch (BusinessException | InvalidFileException | FileStorageException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return showForm(null, form, null, model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                           @RequestParam(required = false) String msg, Model model) {
        addMessage(msg, model);
        return showForm(id, productService.getForm(user.getId(), id), user.getId(), model);
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                         @Valid @ModelAttribute("form") VendorProductForm form, BindingResult result,
                         @RequestParam(value = "images", required = false) List<MultipartFile> images,
                         Model model) {
        if (!result.hasErrors()) {
            try {
                productService.update(user.getId(), id, form, images);
                return "redirect:/vendor/products/" + id + "/edit?msg=updated";
            } catch (BusinessException | InvalidFileException | FileStorageException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return showForm(id, form, user.getId(), model);
    }

    @PostMapping("/{id}/images/{imageId}/thumbnail")
    public String setThumbnail(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                               @PathVariable Long imageId) {
        productService.setThumbnail(user.getId(), id, imageId);
        return "redirect:/vendor/products/" + id + "/edit?msg=thumbnail";
    }

    @PostMapping("/{id}/images/{imageId}/delete")
    public String deleteImage(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                              @PathVariable Long imageId) {
        try {
            productService.deleteImage(user.getId(), id, imageId);
            return "redirect:/vendor/products/" + id + "/edit?msg=imageDeleted";
        } catch (BusinessException e) {
            return "redirect:/vendor/products/" + id + "/edit?msg=lastImage";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        productService.toggleActive(user.getId(), id);
        return "redirect:/vendor/products?msg=toggled";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        boolean deleted = productService.delete(user.getId(), id);
        return "redirect:/vendor/products?msg=" + (deleted ? "deleted" : "hidden");
    }

    private String showForm(Long productId, VendorProductForm form, Long ownerId, Model model) {
        model.addAttribute("productId", productId);
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.activeOptions());
        model.addAttribute("maxImages", VendorProductService.MAX_IMAGES);
        if (productId != null) {
            model.addAttribute("images", productService.images(ownerId, productId));
            model.addAttribute("productSlug", productService.slugOf(ownerId, productId));
        }
        return FORM_VIEW;
    }

    private static void addMessage(String msg, Model model) {
        if ("lastImage".equals(msg)) {
            model.addAttribute("error", "Sản phẩm cần ít nhất 1 ảnh. Hãy thêm ảnh khác trước khi xóa ảnh này.");
        } else if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
    }
}
