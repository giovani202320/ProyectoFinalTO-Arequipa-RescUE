workspace "ARE — Arequipa RescUE (MVP Académico)" "Modelo arquitectónico C4 (Nivel 1 y Nivel 2) del simulador académico ARE — Arequipa RescUE." {

    !identifiers hierarchical

    model {
        !impliedRelationships false

        # =====================================================================
        # Actores humanos aprobados (DEC-009)
        # =====================================================================
        act01 = person "Operador de recepción (ACT-01)" "Registra solicitudes de emergencia y consulta su clasificación y estado." "Actor"
        act02 = person "Despachador (ACT-02)" "Gestiona el despacho de ambulancias, las colas FIFO y los estados operativos." "Actor"
        act03 = person "Administrador (ACT-03)" "Administra catálogos, reglas, altas simuladas, escenarios y métricas." "Actor"

        # =====================================================================
        # Cliente externo al límite de contenedores de ARE (Nivel 2)
        # =====================================================================
        webBrowser = softwareSystem "Navegador Web" "Cliente web del usuario para interactuar con las vistas HTML del simulador." "WebBrowser"

        # =====================================================================
        # Sistema principal: ARE — Arequipa RescUE (DEC-001, DEC-002, ADR-0001)
        # =====================================================================
        areSystem = softwareSystem "ARE — Arequipa RescUE" "Simulador académico de atención de emergencias médicas, despacho de ambulancias y gestión hospitalaria." "MainSystem" {

            areWebApp = container "Aplicación Web ARE" "Monolito modular hexagonal: interfaz web MVC, casos de uso, dominio, despacho, rutas, hospitales, simulación y reportes." "Java 21, Spring Boot 3.4.5, Spring MVC y Thymeleaf" "WebApp"

            areDatabase = container "Base de Datos ARE" "Persistencia transaccional de entidades, escenarios, eventos discretos y registros experimentales." "MySQL 8.4 LTS" "Database"
        }

        # =====================================================================
        # Relaciones de Nivel 1 (System Context: Actores -> Sistema ARE)
        # =====================================================================
        act01 -> areSystem "Registra y consulta emergencias" "HTTP"
        act02 -> areSystem "Supervisa el despacho y los estados operativos" "HTTP"
        act03 -> areSystem "Administra catálogos, reglas, escenarios y métricas" "HTTP"

        # =====================================================================
        # Relaciones de Nivel 2 (Container: Actores -> Navegador -> WebApp -> BD)
        # =====================================================================
        act01 -> webBrowser "Registra y consulta emergencias" "Interacción web"
        act02 -> webBrowser "Supervisa el despacho y los estados operativos" "Interacción web"
        act03 -> webBrowser "Administra catálogos, reglas, escenarios y métricas" "Interacción web"

        webBrowser -> areSystem.areWebApp "Envía peticiones y recibe vistas HTML" "HTTP"
        areSystem.areWebApp -> areSystem.areDatabase "Lee y persiste datos transaccionalmente" "JDBC/JPA"
    }

    views {
        systemContext areSystem "C1_SystemContext" "Diagrama C4 Nivel 1 (System Context): Actores humanos e interacción con ARE — Arequipa RescUE." {
            include act01 act02 act03 areSystem
            autoLayout lr 350 180
        }

        container areSystem "C2_Containers" "Diagrama C4 Nivel 2 (Container): Navegador cliente y contenedores del monolito modular ARE." {
            include act01 act02 act03 webBrowser areSystem.areWebApp areSystem.areDatabase
            autoLayout lr 350 180
        }

        styles {
            element "Element" {
                fontSize 22
            }
            element "Person" {
                shape Person
                background "#1D4ED8"
                color "#FFFFFF"
                stroke "#93C5FD"
            }
            element "MainSystem" {
                shape RoundedBox
                background "#0284C7"
                color "#FFFFFF"
                stroke "#7DD3FC"
            }
            element "WebBrowser" {
                shape WebBrowser
                background "#475569"
                color "#FFFFFF"
                stroke "#CBD5E1"
            }
            element "WebApp" {
                shape RoundedBox
                background "#2563EB"
                color "#FFFFFF"
                stroke "#93C5FD"
            }
            element "Database" {
                shape Cylinder
                background "#15803D"
                color "#FFFFFF"
                stroke "#86EFAC"
            }
            relationship "Relationship" {
                thickness 3
                color "#94A3B8"
                style Solid
                fontSize 20
                width 220
            }
        }
    }
}
