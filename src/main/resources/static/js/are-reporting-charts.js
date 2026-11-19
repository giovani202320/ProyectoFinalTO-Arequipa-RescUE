document.addEventListener("DOMContentLoaded", function () {
    if (typeof window.Chart !== "function") {
        return;
    }

    var root = document.getElementById("are-reporting-charts-root");
    if (!root) {
        return;
    }

    function parseJsonAttr(el, attr, fallback) {
        if (!el) {
            return fallback;
        }
        var raw = el.getAttribute(attr);
        if (!raw) {
            return fallback;
        }
        try {
            return JSON.parse(raw);
        } catch (e) {
            return fallback;
        }
    }

    // 1. Temporal closed averages (MET-01, MET-02, MET-03, MET-07)
    var temporalCanvas = document.getElementById("chart-temporal-metrics");
    if (temporalCanvas) {
        var tLabels = ["MET-01 Espera amb.", "MET-02 Respuesta", "MET-03 Traslado", "MET-07 Espera hosp."];
        var tRaw = [
            temporalCanvas.getAttribute("data-met01"),
            temporalCanvas.getAttribute("data-met02"),
            temporalCanvas.getAttribute("data-met03"),
            temporalCanvas.getAttribute("data-met07")
        ].map(function (v) {
            return v === null || v === "" || v === "null" ? null : Number(v);
        });
        var hasTemporal = tRaw.some(function (v) { return v !== null && !Number.isNaN(v); });
        if (hasTemporal) {
            new window.Chart(temporalCanvas, {
                type: "bar",
                data: {
                    labels: tLabels,
                    datasets: [{
                        label: "Promedio cerrado (u. tiempo lógico)",
                        data: tRaw,
                        backgroundColor: ["#1d4ed8", "#0f2547", "#15803d", "#b45309"],
                        borderRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        y: {
                            beginAtZero: true,
                            title: { display: true, text: "Unidades de tiempo lógico" }
                        }
                    }
                }
            });
        }
    }

    // 2. Emergency distribution by state (MET-04)
    var stateCanvas = document.getElementById("chart-emergency-states");
    if (stateCanvas) {
        var stateLabels = [];
        var stateValues = [];
        root.querySelectorAll("[data-met04-item]").forEach(function (el) {
            var val = Number(el.getAttribute("data-count") || "0");
            if (val > 0) {
                stateLabels.push(el.getAttribute("data-label") || "");
                stateValues.push(val);
            }
        });
        if (stateValues.length > 0) {
            new window.Chart(stateCanvas, {
                type: "doughnut",
                data: {
                    labels: stateLabels,
                    datasets: [{
                        data: stateValues,
                        backgroundColor: ["#15803d", "#64748b", "#d97706", "#1d4ed8", "#0284c7", "#b45309", "#0f2547"]
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { position: "bottom" } }
                }
            });
        }
    }

    // 3. Ambulance fleet utilization (MET-05)
    var fleetCanvas = document.getElementById("chart-ambulance-utilization");
    if (fleetCanvas) {
        var ambLabels = [];
        var ambValues = [];
        root.querySelectorAll("[data-met05-item]").forEach(function (el) {
            ambLabels.push(el.getAttribute("data-id") || "");
            ambValues.push(Number(el.getAttribute("data-percent") || "0"));
        });
        if (ambLabels.length > 0) {
            new window.Chart(fleetCanvas, {
                type: "bar",
                data: {
                    labels: ambLabels,
                    datasets: [{
                        label: "Utilización (%)",
                        data: ambValues,
                        backgroundColor: "#1d4ed8",
                        borderRadius: 6
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        y: {
                            beginAtZero: true,
                            suggestedMax: 100,
                            title: { display: true, text: "Porcentaje (%)" }
                        }
                    }
                }
            });
        }
    }

    // 4. Concurrency conflicts and retries (MET-06)
    var concCanvas = document.getElementById("chart-concurrency-metrics");
    if (concCanvas) {
        var ambDisp = parseJsonAttr(concCanvas, "data-ambulance", [0, 0, 0]);
        var hospRes = parseJsonAttr(concCanvas, "data-hospital", [0, 0, 0]);
        new window.Chart(concCanvas, {
            type: "bar",
            data: {
                labels: ["Conflictos", "Reintentos", "Agotamientos"],
                datasets: [
                    {
                        label: "Asignación ambulancia (DEC-017)",
                        data: ambDisp,
                        backgroundColor: "#1d4ed8",
                        borderRadius: 6
                    },
                    {
                        label: "Reserva hospitalaria (DEC-018)",
                        data: hospRes,
                        backgroundColor: "#b45309",
                        borderRadius: 6
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: "bottom" } },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { precision: 0 },
                        title: { display: true, text: "Cantidad de eventos" }
                    }
                }
            }
        });
    }
});
