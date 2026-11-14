-- =============================================================================
-- V2__seed_initial_graph.sql
-- Semilla normativa del grafo simplificado de Arequipa: 10 nodos y 12 aristas
-- Referencias: DEC-013, DEC-019, business-rules.md (RN-RUT-006, CA-SIM-05)
-- =============================================================================

INSERT INTO graph_nodes (id, zone_code, display_name, isolated_node) VALUES
    ('N01', 'CERCADO', 'Cercado (Nodo central de referencia)', FALSE),
    ('N02', 'YANAHUARA', 'Yanahuara (Zona norte-central)', FALSE),
    ('N03', 'CAYMA', 'Cayma (Zona norte)', FALSE),
    ('N04', 'CERRO_COLORADO', 'Cerro Colorado (Zona noroeste)', FALSE),
    ('N05', 'JLB_Y_RIVERO', 'José Luis Bustamante y Rivero (Zona sur-central)', FALSE),
    ('N06', 'PAUCARPATA', 'Paucarpata (Zona este)', FALSE),
    ('N07', 'MIRAFLORES', 'Miraflores (Zona noreste)', FALSE),
    ('N08', 'SOCABAYA', 'Socabaya (Zona sur)', FALSE),
    ('N09', 'SACHACA', 'Sachaca (Zona oeste)', FALSE),
    ('N10', 'YURA_AISLADO', 'Yura Aislado (Nodo sin aristas activas para pruebas de inalcanzabilidad)', TRUE);

INSERT INTO graph_edges (id, source_node_id, target_node_id, abstract_cost) VALUES
    ('EDG-01', 'N01', 'N02', 2),
    ('EDG-02', 'N01', 'N05', 2),
    ('EDG-03', 'N01', 'N07', 3),
    ('EDG-04', 'N01', 'N09', 4),
    ('EDG-05', 'N02', 'N03', 3),
    ('EDG-06', 'N02', 'N09', 2),
    ('EDG-07', 'N03', 'N04', 2),
    ('EDG-08', 'N03', 'N07', 2),
    ('EDG-09', 'N04', 'N09', 4),
    ('EDG-10', 'N05', 'N06', 3),
    ('EDG-11', 'N05', 'N08', 3),
    ('EDG-12', 'N06', 'N07', 2);
