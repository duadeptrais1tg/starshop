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

    window.StarShop = window.StarShop || {};
    window.StarShop.csrfHeaders = csrfHeaders;
})();
