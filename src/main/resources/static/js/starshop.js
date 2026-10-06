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
            + '</a><button type="button" class="ss-fav-btn" data-fav-id="' + p.id + '" aria-pressed="false"'
            + ' aria-label="Yêu thích" title="Yêu thích"><i class="ti ti-heart"></i></button>'
            + '<div class="card-body d-flex flex-column">'
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

    /** Thông báo nhỏ góc màn hình (type: success | danger). */
    function toast(message, type) {
        var box = document.getElementById('ss-toasts');
        if (!box) {
            box = document.createElement('div');
            box.id = 'ss-toasts';
            box.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            box.style.zIndex = 1080;
            document.body.appendChild(box);
        }
        var el = document.createElement('div');
        el.className = 'alert alert-' + (type || 'success') + ' shadow mb-2';
        el.setAttribute('role', 'status');
        el.textContent = message;
        box.appendChild(el);
        setTimeout(function () { el.remove(); }, 3000);
    }

    /** Cập nhật số trên biểu tượng giỏ hàng ở header. */
    function setCartCount(count) {
        var badge = document.getElementById('cart-count');
        if (!badge) {
            return;
        }
        badge.textContent = count;
        badge.classList.toggle('d-none', !(count > 0));
    }

    /** Chưa đăng nhập (API trả 401) -> sang trang đăng nhập, xong quay lại trang hiện tại. */
    function goLogin() {
        window.location.href = '/auth/login?redirect=' + encodeURIComponent(window.location.pathname + window.location.search);
    }

    /**
     * Gửi POST dạng form tới API (kèm CSRF), trả về Promise JSON. Lỗi nghiệp vụ -> reject với message.
     */
    function postForm(url, params) {
        return fetch(url, {
            method: 'POST',
            headers: csrfHeaders({'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json'}),
            body: new URLSearchParams(params)
        }).then(function (res) {
            if (res.status === 401) {
                goLogin();
                return Promise.reject(null);
            }
            return res.json().then(function (data) {
                return res.ok ? data : Promise.reject(data.message || 'Có lỗi xảy ra, vui lòng thử lại.');
            });
        });
    }

    /** Thêm vào giỏ bằng AJAX, cập nhật badge và báo kết quả. */
    function addToCart(productId, quantity) {
        return postForm('/api/cart/items', {productId: productId, quantity: quantity || 1})
            .then(function (data) {
                setCartCount(data.cartCount);
                toast(data.message, 'success');
                return data;
            })
            .catch(function (message) {
                if (message) {
                    toast(message, 'danger');
                }
                return Promise.reject(message);
            });
    }

    /** Tô trạng thái các nút tim theo id sản phẩm (mọi nút cùng sản phẩm trên trang). */
    function setFavorite(productId, favorited, count) {
        document.querySelectorAll('[data-fav-id="' + productId + '"]').forEach(function (btn) {
            btn.classList.toggle('active', favorited);
            btn.setAttribute('aria-pressed', favorited ? 'true' : 'false');
            btn.title = favorited ? 'Bỏ yêu thích' : 'Yêu thích';
            var label = btn.querySelector('[data-fav-label]');
            if (label) {
                label.textContent = favorited ? 'Đã yêu thích' : 'Yêu thích';
            }
        });
        if (count != null) {
            document.querySelectorAll('[data-fav-count="' + productId + '"]').forEach(function (el) {
                el.textContent = count;
            });
        }
    }

    /**
     * Hỏi server những sản phẩm đang hiển thị (trong root) mà user đã thích để tô đỏ tim.
     * Khách chưa đăng nhập (401) thì bỏ qua, không chuyển trang.
     */
    function syncFavorites(root) {
        var ids = [];
        (root || document).querySelectorAll('[data-fav-id]').forEach(function (btn) {
            var id = btn.getAttribute('data-fav-id');
            if (ids.indexOf(id) < 0) {
                ids.push(id);
            }
        });
        if (!ids.length) {
            return;
        }
        fetch('/api/favorites/ids?productIds=' + ids.join(','), {headers: {'Accept': 'application/json'}})
            .then(function (res) { return res.ok ? res.json() : {ids: []}; })
            .then(function (data) {
                data.ids.forEach(function (id) { setFavorite(id, true, null); });
            })
            .catch(function () { });
    }

    /** Bấm tim (ủy quyền sự kiện -> áp dụng cả card được thêm bằng AJAX). Khách: postForm chuyển sang đăng nhập. */
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('[data-fav-id]');
        if (!btn) {
            return;
        }
        e.preventDefault();
        if (btn.disabled) {
            return;
        }
        var productId = btn.getAttribute('data-fav-id');
        btn.disabled = true;
        postForm('/api/favorites/' + productId + '/toggle', {})
            .then(function (data) {
                setFavorite(productId, data.favorited, data.favoriteCount);
                toast(data.favorited ? 'Đã thêm vào yêu thích.' : 'Đã bỏ khỏi yêu thích.', 'success');
                // Trang "Yêu thích": bỏ thích thì ẩn card
                if (!data.favorited && btn.closest('[data-fav-remove-card]')) {
                    var col = btn.closest('.row > div');
                    if (col) {
                        col.remove();
                    }
                }
            })
            .catch(function (message) {
                if (message) {
                    toast(message, 'danger');
                }
            })
            .then(function () { btn.disabled = false; });
    });

    document.addEventListener('DOMContentLoaded', function () { syncFavorites(document); });

    window.StarShop = window.StarShop || {};
    window.StarShop.syncFavorites = syncFavorites;
    window.StarShop.toast = toast;
    window.StarShop.setCartCount = setCartCount;
    window.StarShop.postForm = postForm;
    window.StarShop.addToCart = addToCart;
    window.StarShop.csrfHeaders = csrfHeaders;
    window.StarShop.escapeHtml = escapeHtml;
    window.StarShop.renderProductCard = renderProductCard;
})();
