document.addEventListener("DOMContentLoaded", function () {
    var svg = document.getElementById("areTopologySvg");
    if (!svg || !window.ARE_TOPOLOGY_DATA) {
        return;
    }

    var detailEmptyEl = document.getElementById("areNodeDetailEmpty");
    var detailBodyEl = document.getElementById("areNodeDetailBody");
    var detailIdEl = document.getElementById("areNodeDetailId");
    var detailNameEl = document.getElementById("areNodeDetailName");
    var detailZoneEl = document.getElementById("areNodeDetailZone");
    var detailDegreeEl = document.getElementById("areNodeDetailDegree");
    var detailStatusEl = document.getElementById("areNodeDetailStatus");
    var detailEdgesEl = document.getElementById("areNodeDetailEdges");
    var detailAmbsSummaryEl = document.getElementById("areNodeDetailAmbsSummary");
    var detailAmbsListEl = document.getElementById("areNodeDetailAmbsList");
    var detailHospsSummaryEl = document.getElementById("areNodeDetailHospsSummary");
    var detailHospsListEl = document.getElementById("areNodeDetailHospsList");
    var useOriginBtn = document.getElementById("areNodeUseOriginBtn");
    var useDestBtn = document.getElementById("areNodeUseDestBtn");
    var originSelect = document.getElementById("routeOrigin");
    var destSelect = document.getElementById("routeDestination");

    var SVG_NS = "http://www.w3.org/2000/svg";
    var selectedNodeId = null;

    function createSvgEl(tag, attrs) {
        var el = document.createElementNS(SVG_NS, tag);
        if (attrs) {
            Object.keys(attrs).forEach(function (k) {
                el.setAttribute(k, String(attrs[k]));
            });
        }
        return el;
    }

    function buildEdgePairMap(nodeList) {
        var map = {};
        for (var i = 0; i < nodeList.length - 1; i++) {
            var u = nodeList[i];
            var v = nodeList[i + 1];
            map[u + "|" + v] = true;
            map[v + "|" + u] = true;
        }
        return map;
    }

    function buildNodeSet(nodeList) {
        var set = {};
        nodeList.forEach(function (nid) {
            set[nid] = true;
        });
        return set;
    }

    function renderNodeResources(node) {
        if (detailAmbsSummaryEl && detailAmbsListEl) {
            var ambCount = node.ambulanceCount || 0;
            var ambAvail = node.availableAmbulanceCount || 0;
            var ambUnavail = node.unavailableAmbulanceCount || 0;
            detailAmbsSummaryEl.textContent = ambCount + " total (" + ambAvail + " disp. / " + ambUnavail + " ocup.)";
            detailAmbsListEl.innerHTML = "";
            var ambs = Array.isArray(node.ambulances) ? node.ambulances : [];
            if (ambs.length === 0) {
                var liNoAmb = document.createElement("li");
                liNoAmb.className = "text-secondary";
                liNoAmb.textContent = "Sin ambulancias registradas en este nodo.";
                detailAmbsListEl.appendChild(liNoAmb);
            } else {
                ambs.forEach(function (a) {
                    var li = document.createElement("li");
                    li.textContent = a.id + " — " + a.equipmentLabel + " (" + a.stateLabel + ")";
                    detailAmbsListEl.appendChild(li);
                });
            }
        }

        if (detailHospsSummaryEl && detailHospsListEl) {
            var hospCount = node.hospitalCount || 0;
            detailHospsSummaryEl.textContent = hospCount + " vinculado(s)";
            detailHospsListEl.innerHTML = "";
            var hosps = Array.isArray(node.hospitals) ? node.hospitals : [];
            if (hosps.length === 0) {
                var liNoHosp = document.createElement("li");
                liNoHosp.className = "text-secondary";
                liNoHosp.textContent = "Sin hospitales vinculados a este nodo.";
                detailHospsListEl.appendChild(liNoHosp);
            } else {
                hosps.forEach(function (h) {
                    var li = document.createElement("li");
                    var estado = h.enabled ? "Habilitado" : "Deshabilitado";
                    li.textContent = h.id + " — " + h.name + " [" + estado + ", D=" + h.availableSlots + "/" + h.totalCapacity + "]";
                    detailHospsListEl.appendChild(li);
                });
            }
        }
    }

    function selectNode(node, edges, nodesLayer) {
        selectedNodeId = node.id;
        nodesLayer.querySelectorAll("[data-node-id]").forEach(function (el) {
            var isSel = el.getAttribute("data-node-id") === node.id;
            el.setAttribute("aria-pressed", isSel ? "true" : "false");
            var ring = el.querySelector(".are-svg-select-ring");
            if (ring) {
                ring.setAttribute("opacity", isSel ? "1" : "0");
            }
        });

        if (detailEmptyEl) {
            detailEmptyEl.classList.add("d-none");
        }
        if (detailBodyEl) {
            detailBodyEl.classList.remove("d-none");
        }
        if (detailIdEl) {
            detailIdEl.textContent = node.id;
        }
        if (detailNameEl) {
            detailNameEl.textContent = node.displayName;
        }
        if (detailZoneEl) {
            detailZoneEl.textContent = node.zoneCode;
        }
        if (detailDegreeEl) {
            detailDegreeEl.textContent = String(node.degree);
        }
        if (detailStatusEl) {
            detailStatusEl.textContent = node.isolated ? "Nodo aislado sin aristas activas" : "Conectado";
        }
        if (detailEdgesEl) {
            detailEdgesEl.innerHTML = "";
            var incident = edges.filter(function (e) {
                return e.sourceNodeId === node.id || e.targetNodeId === node.id;
            });
            if (incident.length === 0) {
                var liEmpty = document.createElement("li");
                liEmpty.className = "text-secondary";
                liEmpty.textContent = "Sin aristas incidentes.";
                detailEdgesEl.appendChild(liEmpty);
            } else {
                incident.forEach(function (e) {
                    var other = e.sourceNodeId === node.id ? e.targetNodeId : e.sourceNodeId;
                    var li = document.createElement("li");
                    li.textContent = e.id + ": hacia " + other + " (costo " + e.abstractCost + ")";
                    detailEdgesEl.appendChild(li);
                });
            }
        }
        renderNodeResources(node);
    }

    if (useOriginBtn && originSelect) {
        useOriginBtn.addEventListener("click", function () {
            if (selectedNodeId) {
                originSelect.value = selectedNodeId;
                originSelect.focus();
            }
        });
    }
    if (useDestBtn && destSelect) {
        useDestBtn.addEventListener("click", function () {
            if (selectedNodeId) {
                destSelect.value = selectedNodeId;
                destSelect.focus();
            }
        });
    }

    function renderGraph(data) {
        var nodes = Array.isArray(data.nodes) ? data.nodes.slice() : [];
        var edges = Array.isArray(data.edges) ? data.edges.slice() : [];

        var legacyRouteNodes = Array.isArray(data.routeNodes) ? data.routeNodes : [];
        var routeNodesA = Array.isArray(data.routePathNodesA)
            ? data.routePathNodesA
            : (data.routeType !== "TRAMO_B" ? legacyRouteNodes : []);
        var routeNodesB = Array.isArray(data.routePathNodesB)
            ? data.routePathNodesB
            : (data.routeType === "TRAMO_B" ? legacyRouteNodes : []);

        var routeOrigin = data.routeOrigin || "";
        var routeDestination = data.routeDestination || "";
        var incidentNodeId = data.incidentNodeId || "";

        svg.innerHTML = "";

        if (nodes.length === 0) {
            var emptyText = createSvgEl("text", {
                x: 400,
                y: 230,
                "text-anchor": "middle",
                fill: "#64748b",
                "font-size": "15",
                "font-weight": "600"
            });
            emptyText.textContent = "No hay nodos configurados en la topología.";
            svg.appendChild(emptyText);
            return;
        }

        nodes.sort(function (a, b) {
            return String(a.id).localeCompare(String(b.id));
        });

        var connectedNodes = nodes.filter(function (n) { return !n.isolated; });
        var isolatedNodes = nodes.filter(function (n) { return Boolean(n.isolated); });

        var positions = {};
        var hasIsolated = isolatedNodes.length > 0;
        var centerX = hasIsolated ? 320 : 400;
        var centerY = 235;
        var radiusX = hasIsolated ? 225 : 270;
        var radiusY = 160;

        connectedNodes.forEach(function (node, idx) {
            var angle = (-Math.PI / 2) + (2 * Math.PI * idx) / Math.max(connectedNodes.length, 1);
            positions[node.id] = {
                x: Math.round(centerX + radiusX * Math.cos(angle)),
                y: Math.round(centerY + radiusY * Math.sin(angle))
            };
        });

        isolatedNodes.forEach(function (node, idx) {
            var spacing = 90;
            var startY = centerY - ((isolatedNodes.length - 1) * spacing) / 2;
            positions[node.id] = {
                x: 695,
                y: Math.round(startY + idx * spacing)
            };
        });

        var routePairsA = buildEdgePairMap(routeNodesA);
        var routePairsB = buildEdgePairMap(routeNodesB);
        var routeSetA = buildNodeSet(routeNodesA);
        var routeSetB = buildNodeSet(routeNodesB);
        var hasAnyRoute = routeNodesA.length > 0 || routeNodesB.length > 0;

        if (hasIsolated) {
            svg.appendChild(createSvgEl("line", {
                x1: 605,
                y1: 28,
                x2: 605,
                y2: 442,
                stroke: "#cbd5e1",
                "stroke-width": "1.5",
                "stroke-dasharray": "6 5"
            }));

            var sepLabel = createSvgEl("text", {
                x: 695,
                y: 42,
                "text-anchor": "middle",
                fill: "#b45309",
                "font-size": "11",
                "font-weight": "700"
            });
            sepLabel.textContent = "SIN CONEXIONES";
            svg.appendChild(sepLabel);
        }

        var edgesLayer = createSvgEl("g", { id: "areTopologyEdgesLayer" });
        var routeEdgesLayer = createSvgEl("g", { id: "areTopologyRouteEdgesLayer" });
        var nodesLayer = createSvgEl("g", { id: "areTopologyNodesLayer" });

        svg.appendChild(edgesLayer);
        svg.appendChild(routeEdgesLayer);
        svg.appendChild(nodesLayer);

        edges.forEach(function (edge) {
            var p1 = positions[edge.sourceNodeId];
            var p2 = positions[edge.targetNodeId];
            if (!p1 || !p2) {
                return;
            }
            var pairKey = edge.sourceNodeId + "|" + edge.targetNodeId;
            var onA = Boolean(routePairsA[pairKey]);
            var onB = Boolean(routePairsB[pairKey]);
            var onRoute = onA || onB;
            var targetLayer = onRoute ? routeEdgesLayer : edgesLayer;

            var gEdge = createSvgEl("g", {
                "data-edge-id": edge.id,
                "data-on-route": onRoute ? "true" : "false",
                "data-route-a": onA ? "true" : "false",
                "data-route-b": onB ? "true" : "false",
                "data-route-overlap": (onA && onB) ? "true" : "false"
            });

            var title = createSvgEl("title");
            title.textContent = edge.id + ": " + edge.sourceNodeId + " \u2194 " + edge.targetNodeId + " (costo " + edge.abstractCost + ")";
            gEdge.appendChild(title);

            if (onA && onB) {
                gEdge.appendChild(createSvgEl("line", {
                    x1: p1.x,
                    y1: p1.y,
                    x2: p2.x,
                    y2: p2.y,
                    stroke: "#0284c7",
                    "stroke-width": "6",
                    "stroke-linecap": "round"
                }));
                gEdge.appendChild(createSvgEl("line", {
                    x1: p1.x,
                    y1: p1.y,
                    x2: p2.x,
                    y2: p2.y,
                    stroke: "#7c3aed",
                    "stroke-width": "3",
                    "stroke-dasharray": "8 6",
                    "stroke-linecap": "round"
                }));
            } else {
                var strokeColor = onB ? "#7c3aed" : (onA ? "#0284c7" : "#94a3b8");
                gEdge.appendChild(createSvgEl("line", {
                    x1: p1.x,
                    y1: p1.y,
                    x2: p2.x,
                    y2: p2.y,
                    stroke: strokeColor,
                    "stroke-width": onRoute ? "4.5" : "2",
                    "stroke-linecap": "round",
                    opacity: (hasAnyRoute && !onRoute) ? "0.4" : "1"
                }));
            }

            var midX = Math.round((p1.x + p2.x) / 2);
            var midY = Math.round((p1.y + p2.y) / 2);
            var pillFill = (onA && onB) ? "#4338ca" : (onB ? "#7c3aed" : (onA ? "#0284c7" : "#ffffff"));
            var pillStroke = (onA && onB) ? "#312e81" : (onB ? "#5b21b6" : (onA ? "#0369a1" : "#cbd5e1"));
            gEdge.appendChild(createSvgEl("rect", {
                x: midX - 13,
                y: midY - 10,
                width: 26,
                height: 20,
                rx: 10,
                fill: pillFill,
                stroke: pillStroke,
                "stroke-width": "1.2"
            }));

            var costText = createSvgEl("text", {
                x: midX,
                y: midY + 4,
                "text-anchor": "middle",
                fill: onRoute ? "#ffffff" : "#334155",
                "font-size": "11",
                "font-weight": "700",
                "font-family": "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace"
            });
            costText.textContent = String(edge.abstractCost);
            gEdge.appendChild(costText);

            targetLayer.appendChild(gEdge);
        });

        nodes.forEach(function (node) {
            var pos = positions[node.id];
            var isOrigin = routeOrigin && node.id === routeOrigin && (routeSetA[node.id] || routeSetB[node.id]);
            var isDest = routeDestination && node.id === routeDestination && (routeSetA[node.id] || routeSetB[node.id]);
            var isIncident = incidentNodeId && node.id === incidentNodeId;
            var isOnA = Boolean(routeSetA[node.id]);
            var isOnB = Boolean(routeSetB[node.id]);

            var fill = "#ffffff";
            var stroke = "#1e293b";
            var textColor = "#0f172a";
            var dash = "";

            if (node.isolated) {
                fill = "#fffbeb";
                stroke = "#d97706";
                textColor = "#92400e";
                dash = "4 3";
            }
            if (isOnA) {
                fill = "#e0f2fe";
                stroke = "#0284c7";
                textColor = "#0c4a6e";
                dash = "";
            }
            if (isOnB) {
                fill = "#ede9fe";
                stroke = "#7c3aed";
                textColor = "#4c1d95";
                dash = "";
            }
            if (isOrigin || isIncident) {
                fill = "#0f766e";
                stroke = "#115e59";
                textColor = "#ffffff";
            }
            if (isDest) {
                fill = "#dc2626";
                stroke = "#991b1b";
                textColor = "#ffffff";
            }

            var gNode = createSvgEl("g", {
                transform: "translate(" + pos.x + "," + pos.y + ")",
                role: "button",
                tabindex: "0",
                "aria-pressed": selectedNodeId === node.id ? "true" : "false",
                "aria-label": "Nodo " + node.id + " — " + node.displayName + ", grado " + node.degree + ", " + (node.isolated ? "aislado" : "conectado"),
                "data-node-id": node.id,
                style: "cursor: pointer; outline: none;"
            });

            var selectRing = createSvgEl("circle", {
                cx: 0,
                cy: 0,
                r: 29,
                fill: "none",
                stroke: "#0f172a",
                "stroke-width": "2.5",
                "stroke-dasharray": "3 2",
                opacity: selectedNodeId === node.id ? "1" : "0",
                "class": "are-svg-select-ring"
            });
            gNode.appendChild(selectRing);

            var circleAttrs = {
                cx: 0,
                cy: 0,
                r: 23,
                fill: fill,
                stroke: stroke,
                "stroke-width": (isOrigin || isDest || isIncident || isOnA || isOnB) ? "3" : "2.2"
            };
            if (dash) {
                circleAttrs["stroke-dasharray"] = dash;
            }
            gNode.appendChild(createSvgEl("circle", circleAttrs));

            var idText = createSvgEl("text", {
                x: 0,
                y: 5,
                "text-anchor": "middle",
                fill: textColor,
                "font-size": "13",
                "font-weight": "700",
                "font-family": "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace"
            });
            idText.textContent = node.id;
            gNode.appendChild(idText);

            var nameText = createSvgEl("text", {
                x: 0,
                y: 38,
                "text-anchor": "middle",
                fill: "#334155",
                "font-size": "11",
                "font-weight": "600"
            });
            nameText.textContent = node.displayName.length > 22
                ? node.displayName.slice(0, 20) + "…"
                : node.displayName;
            gNode.appendChild(nameText);

            var ambCount = node.ambulanceCount || 0;
            var hospCount = node.hospitalCount || 0;
            if (ambCount > 0 || hospCount > 0) {
                var badgeParts = [];
                if (ambCount > 0) {
                    badgeParts.push("A:" + ambCount);
                }
                if (hospCount > 0) {
                    badgeParts.push("H:" + hospCount);
                }
                var resText = createSvgEl("text", {
                    x: 0,
                    y: 51,
                    "text-anchor": "middle",
                    fill: "#0f766e",
                    "font-size": "10",
                    "font-weight": "700",
                    "font-family": "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace"
                });
                resText.textContent = badgeParts.join(" · ");
                gNode.appendChild(resText);
            }

            gNode.addEventListener("click", function () {
                selectNode(node, edges, nodesLayer);
            });
            gNode.addEventListener("keydown", function (ev) {
                if (ev.key === "Enter" || ev.key === " ") {
                    ev.preventDefault();
                    selectNode(node, edges, nodesLayer);
                }
            });
            gNode.addEventListener("focus", function () {
                selectRing.setAttribute("opacity", "1");
            });
            gNode.addEventListener("blur", function () {
                if (selectedNodeId !== node.id) {
                    selectRing.setAttribute("opacity", "0");
                }
            });

            nodesLayer.appendChild(gNode);
        });
    }

    window.areRenderTopologyGraph = function (nextData) {
        window.ARE_TOPOLOGY_DATA = nextData;
        renderGraph(nextData);
    };

    renderGraph(window.ARE_TOPOLOGY_DATA);
});
