/* StarShop – script dùng chung cho mọi decorator */
(function () {
    'use strict';

    /**
     * Đánh dấu mục menu đang mở: link có data-ss-nav khớp dài nhất với URL hiện tại.
     * Ví dụ đang ở /admin/users/5 thì mục /admin/users được active (không phải /admin).
     */
    function highlightActiveNav() {
        var path = window.location.pathname.replace(/\/+$/, '') || '/';
        var best = null;
        var bestLength = -1;

        document.querySelectorAll('a[data-ss-nav]').forEach(function (link) {
            var href = new URL(link.href, window.location.origin).pathname.replace(/\/+$/, '') || '/';
            var matches = path === href || path.indexOf(href + '/') === 0;
            if (matches && href.length > bestLength) {
                best = link;
                bestLength = href.length;
            }
        });

        if (best) {
            best.classList.add('active');
            var item = best.closest('.nav-item');
            if (item) {
                item.classList.add('active');
            }
        }
    }

    document.addEventListener('DOMContentLoaded', highlightActiveNav);

    /**
     * Header CSRF cho request AJAX thay đổi dữ liệu (POST/PUT/DELETE).
     * Ví dụ: fetch('/api/cart', {method: 'POST', headers: StarShop.csrfHeaders({'Content-Type': 'application/json'}), body: ...})
     */
    function csrfHeaders(extra) {
        var headers = Object.assign({}, extra);
        var token = document.querySelector('meta[name="_csrf"]');
        var name = document.querySelector('meta[name="_csrf_header"]');
        if (token && name) {
            headers[name.content] = token.content;
        }
        return headers;
    }

    /** Chống XSS khi chèn dữ liệu (tên sản phẩm, tên shop...) vào HTML. */
    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    var priceFormat = new Intl.NumberFormat('vi-VN');

    function stars(rounded) {
        var html = '';
        for (var i = 1; i <= 5; i++) {
            var cls = i <= rounded ? ' on' : (i - 0.5 === rounded ? ' half' : '');
            html += '<span class="ss-star' + cls + '">★</span>';
        }
        return html;
    }

    /**
     * Dựng card sản phẩm từ JSON của /api/products. Markup giống common/product-card.jsp.
     */
    function renderProductCard(p) {
        var url = '/products/' + encodeURIComponent(p.slug);
        var img = p.imageUrl
            ? '<img src="' + escapeHtml(p.imageUrl) + '" alt="' + escapeHtml(p.name) + '" loading="lazy">'
            : '<span class="ss-product-noimg"><i class="ti ti-flower"></i></span>';
        return '<div class="card card-sm h-100 ss-product-card">'
            + '<a href="' + url + '" class="ss-product-thumb">' + img
            + (p.discountPercent > 0 ? '<span class="badge bg-red text-white ss-product-badge">-' + p.discountPercent + '%</span>' : '')
            + (!p.inStock ? '<span class="ss-product-soldout">Hết hàng</span>' : '')
            + '</a><div class="card-body d-flex flex-column">'
            + '<div class="small text-secondary text-truncate"><i class="ti ti-building-store me-1"></i>' + escapeHtml(p.shopName) + '</div>'
            + '<a href="' + url + '" class="text-reset fw-semibold ss-line-2 mb-2" title="' + escapeHtml(p.name) + '">' + escapeHtml(p.name) + '</a>'
            + '<div class="mt-auto"><div class="d-flex flex-wrap align-items-baseline gap-1">'
            + '<span class="text-primary fw-bold fs-3">' + priceFormat.format(p.finalPrice) + '₫</span>'
            + (p.compareAtPrice ? '<del class="text-secondary small">' + priceFormat.format(p.compareAtPrice) + '₫</del>' : '')
            + '</div><div class="d-flex justify-content-between align-items-center small text-secondary mt-1">'
            + '<span class="ss-stars">' + stars(p.ratingRounded) + (p.reviewCount > 0 ? '<span class="ms-1">(' + p.reviewCount + ')</span>' : '') + '</span>'
            + '<span>Đã bán ' + priceFormat.format(p.soldCount) + '</span>'
            + '</div></div></div></div>';
    }

    window.StarShop = window.StarShop || {};
    window.StarShop.csrfHeaders = csrfHeaders;
    window.StarShop.escapeHtml = escapeHtml;
    window.StarShop.renderProductCard = renderProductCard;
})();
