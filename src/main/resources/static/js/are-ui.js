document.addEventListener("DOMContentLoaded", function () {
    if (window.lucide && typeof window.lucide.createIcons === "function") {
        window.lucide.createIcons();
    }

    var modalEl = document.getElementById("areConfirmModal");
    var modalTitleEl = document.getElementById("areConfirmModalTitle");
    var modalBodyEl = document.getElementById("areConfirmModalBody");
    var modalAcceptBtn = document.getElementById("areConfirmModalAccept");
    var bsModal = (modalEl && window.bootstrap && typeof window.bootstrap.Modal === "function")
            ? new window.bootstrap.Modal(modalEl)
            : null;

    var pendingForm = null;
    var pendingSubmitter = null;

    if (modalEl) {
        modalEl.addEventListener("hidden.bs.modal", function () {
            pendingForm = null;
            pendingSubmitter = null;
        });
    }

    if (modalAcceptBtn && bsModal) {
        modalAcceptBtn.addEventListener("click", function () {
            if (!pendingForm) {
                bsModal.hide();
                return;
            }
            var formToSubmit = pendingForm;
            var submitterToUse = pendingSubmitter;
            pendingForm = null;
            pendingSubmitter = null;
            formToSubmit.dataset.areConfirmed = "true";
            bsModal.hide();

            if (typeof formToSubmit.requestSubmit === "function") {
                formToSubmit.requestSubmit(submitterToUse || undefined);
            } else {
                formToSubmit.submit();
            }
        });
    }

    function preserveSubmitterValue(form, submitter) {
        if (!submitter || !submitter.name) {
            return;
        }
        var existing = form.querySelector("input[type='hidden'][data-are-submitter-proxy='true']");
        if (existing) {
            existing.remove();
        }
        var proxy = document.createElement("input");
        proxy.type = "hidden";
        proxy.name = submitter.name;
        proxy.value = submitter.value || "";
        proxy.setAttribute("data-are-submitter-proxy", "true");
        form.appendChild(proxy);
    }

    function lockFormSubmission(form, submitter) {
        form.dataset.areSubmitting = "true";
        form.setAttribute("aria-busy", "true");
        preserveSubmitterValue(form, submitter);

        var buttons = form.querySelectorAll("button[type='submit'], input[type='submit'], button:not([type])");
        buttons.forEach(function (btn) {
            if (!btn.disabled) {
                btn.disabled = true;
                btn.setAttribute("aria-disabled", "true");
                btn.dataset.areJsDisabled = "true";
                if (btn === submitter && btn.tagName === "BUTTON") {
                    btn.dataset.areOriginalText = btn.textContent;
                    btn.textContent = "Procesando...";
                }
            }
        });
    }

    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (!form || form.tagName !== "FORM") {
            return;
        }
        var method = (form.getAttribute("method") || "get").toLowerCase();
        if (method !== "post") {
            return;
        }
        if (event.defaultPrevented) {
            return;
        }
        if (typeof form.checkValidity === "function" && !form.checkValidity()) {
            return;
        }

        if (form.dataset.areSubmitting === "true") {
            event.preventDefault();
            return;
        }

        var submitter = event.submitter || null;
        var confirmMessage = (submitter && submitter.getAttribute("data-confirm"))
                || form.getAttribute("data-confirm");

        if (confirmMessage && form.dataset.areConfirmed !== "true") {
            event.preventDefault();
            var confirmTitle = (submitter && submitter.getAttribute("data-confirm-title"))
                    || form.getAttribute("data-confirm-title")
                    || "Confirmar operación";

            if (bsModal && modalTitleEl && modalBodyEl) {
                pendingForm = form;
                pendingSubmitter = submitter;
                modalTitleEl.textContent = confirmTitle;
                modalBodyEl.textContent = confirmMessage;
                bsModal.show();
            } else if (window.confirm(confirmMessage)) {
                form.dataset.areConfirmed = "true";
                if (typeof form.requestSubmit === "function") {
                    form.requestSubmit(submitter || undefined);
                } else {
                    lockFormSubmission(form, submitter);
                    form.submit();
                }
            }
            return;
        }

        lockFormSubmission(form, submitter);
    });

    window.addEventListener("pageshow", function () {
        document.querySelectorAll("form[data-are-submitting='true']").forEach(function (form) {
            delete form.dataset.areSubmitting;
            delete form.dataset.areConfirmed;
            form.removeAttribute("aria-busy");

            var proxy = form.querySelector("input[type='hidden'][data-are-submitter-proxy='true']");
            if (proxy) {
                proxy.remove();
            }
            form.querySelectorAll("[data-are-js-disabled='true']").forEach(function (btn) {
                btn.disabled = false;
                btn.removeAttribute("aria-disabled");
                delete btn.dataset.areJsDisabled;
                if (btn.dataset.areOriginalText !== undefined) {
                    btn.textContent = btn.dataset.areOriginalText;
                    delete btn.dataset.areOriginalText;
                }
            });
        });
    });
});
