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

    function isValidCatalogEditAction(action) {
        return typeof action === "string" && /^\/admin\/(ambulances|hospitals)\/[^/]+$/.test(action);
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
        if (form.hasAttribute("data-edit-form")) {
            var editAction = form.getAttribute("action") || "";
            if (!isValidCatalogEditAction(editAction)) {
                event.preventDefault();
                return;
            }
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

    function resetFormSubmitState(form) {
        if (!form) {
            return;
        }
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
    }

    window.addEventListener("pageshow", function () {
        document.querySelectorAll("form[data-are-submitting='true']").forEach(resetFormSubmitState);
    });

    function setupCatalogEditModal(modalId, formId, focusTargetId, populateFields) {
        var editModalEl = document.getElementById(modalId);
        var editFormEl = document.getElementById(formId);
        if (!editModalEl || !editFormEl) {
            return;
        }
        var submitBtn = editFormEl.querySelector("button[type='submit']");

        editModalEl.addEventListener("show.bs.modal", function (event) {
            var trigger = event.relatedTarget;
            if (!trigger) {
                event.preventDefault();
                return;
            }
            var action = trigger.getAttribute("data-edit-action") || "";
            if (!isValidCatalogEditAction(action)) {
                event.preventDefault();
                return;
            }
            editFormEl.setAttribute("action", action);
            resetFormSubmitState(editFormEl);
            populateFields(trigger);

            var fieldset = editFormEl.querySelector("fieldset");
            var isLocked = fieldset && fieldset.disabled;
            if (submitBtn) {
                submitBtn.disabled = Boolean(isLocked);
            }
        });

        editModalEl.addEventListener("shown.bs.modal", function () {
            var target = document.getElementById(focusTargetId);
            if (target && !target.disabled && typeof target.focus === "function") {
                target.focus();
            }
        });

        editModalEl.addEventListener("hidden.bs.modal", function () {
            resetFormSubmitState(editFormEl);
            if (submitBtn) {
                submitBtn.disabled = true;
            }
        });
    }

    setupCatalogEditModal("editAmbulanceModal", "editAmbulanceForm", "editAmbEquipment", function (trigger) {
        var ambId = trigger.getAttribute("data-amb-id") || "";
        var ambIdInput = document.getElementById("editAmbId");
        var ambIdBadge = document.getElementById("editAmbIdBadge");
        var ambEquip = document.getElementById("editAmbEquipment");
        var ambNode = document.getElementById("editAmbNode");
        var ambState = document.getElementById("editAmbStateLabel");

        if (ambIdInput) {
            ambIdInput.value = ambId;
        }
        if (ambIdBadge) {
            ambIdBadge.textContent = ambId || "—";
        }
        if (ambEquip) {
            ambEquip.value = trigger.getAttribute("data-amb-equipment") || "BASIC";
        }
        if (ambNode) {
            ambNode.value = trigger.getAttribute("data-amb-node") || "";
        }
        if (ambState) {
            ambState.textContent = trigger.getAttribute("data-amb-state") || "—";
        }
    });

    setupCatalogEditModal("editHospitalModal", "editHospitalForm", "editHospName", function (trigger) {
        var hospId = trigger.getAttribute("data-hosp-id") || "";
        var hospIdInput = document.getElementById("editHospId");
        var hospIdBadge = document.getElementById("editHospIdBadge");
        var hospName = document.getElementById("editHospName");
        var hospNode = document.getElementById("editHospNode");
        var hospCapacity = document.getElementById("editHospCapacity");
        var hospEnabled = document.getElementById("editHospEnabled");
        var hospReserved = document.getElementById("editHospReserved");
        var hospOccupied = document.getElementById("editHospOccupied");
        var hospAvailable = document.getElementById("editHospAvailable");

        if (hospIdInput) {
            hospIdInput.value = hospId;
        }
        if (hospIdBadge) {
            hospIdBadge.textContent = hospId || "—";
        }
        if (hospName) {
            hospName.value = trigger.getAttribute("data-hosp-name") || "";
        }
        if (hospNode) {
            hospNode.value = trigger.getAttribute("data-hosp-node") || "";
        }
        if (hospCapacity) {
            hospCapacity.value = trigger.getAttribute("data-hosp-capacity") || "0";
        }
        if (hospEnabled) {
            hospEnabled.checked = trigger.getAttribute("data-hosp-enabled") === "true";
        }
        if (hospReserved) {
            hospReserved.textContent = trigger.getAttribute("data-hosp-reserved") || "0";
        }
        if (hospOccupied) {
            hospOccupied.textContent = trigger.getAttribute("data-hosp-occupied") || "0";
        }
        if (hospAvailable) {
            hospAvailable.textContent = trigger.getAttribute("data-hosp-available") || "0";
        }
    });
});
