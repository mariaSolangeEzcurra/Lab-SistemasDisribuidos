#!/bin/bash
# Script de Setup para Sistema de Transacciones Distribuidas
# ===========================================================

set -e  # Salir si cualquier comando falla

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para imprimir mensajes
print_message() {
    echo -e "${BLUE}[SETUP]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Función para verificar si un comando existe
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Verificar dependencias
check_dependencies() {
    print_message "Verificando dependencias..."
    
    if ! command_exists docker; then
        print_error "Docker no está instalado. Por favor instalar Docker primero."
        exit 1
    fi
    
    if ! command_exists docker-compose; then
        print_error "Docker Compose no está instalado. Por favor instalar Docker Compose primero."
        exit 1
    fi
    
    print_success "Dependencias verificadas"
}

# Crear estructura de directorios
create_directories() {
    print_message "Creando estructura de directorios..."
    
    mkdir -p data/orders_db
    mkdir -p data/payments_db
    mkdir -p logs
    mkdir -p init-scripts
    mkdir -p nginx
    mkdir -p grafana/dashboards
    mkdir -p grafana/datasources
    mkdir -p pgadmin
    
    print_success "Directorios creados"
}

# Configurar permisos
set_permissions() {
    print_message "Configurando permisos..."
    
    # Permisos para volúmenes de PostgreSQL
    sudo chown -R 999:999 data/orders_db 2>/dev/null || true
    sudo chown -R 999:999 data/payments_db 2>/dev/null || true
    
    # Permisos para logs
    chmod 755 logs
    
    print_success "Permisos configurados"
}

# Crear archivo de configuración de pgAdmin
create_pgadmin_config() {
    print_message "Creando configuración de pgAdmin..."
    
    cat > pgadmin/servers.json << 'EOF'
{
    "Servers": {
        "1": {
            "Name": "Orders Database",
            "Group": "Distributed System",
            "Host": "orders_db",
            "Port": 5432,
            "MaintenanceDB": "orders_db",
            "Username": "orders_user",
            "SSLMode": "prefer",
            "PassFile": "/tmp/pgpassfile"
        },
        "2": {
            "Name": "Payments Database",
            "Group": "Distributed System",
            "Host": "payments_db",
            "Port": 5432,
            "MaintenanceDB": "payments_db",
            "Username": "payments_user",
            "SSLMode": "prefer",
            "PassFile": "/tmp/pgpassfile"
        }
    }
}
EOF
    
    print_success "Configuración de pgAdmin creada"
}

# Crear configuración básica de Nginx
create_nginx_config() {
    print_message "Creando configuración de Nginx..."
    
    cat > nginx/nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream app {
        server app:8000;
    }
    
    server {
        listen 80;
        server_name localhost;
        
        location / {
            proxy_pass http://app;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        location /health {
            proxy_pass http://app/health;
            access_log off;
        }
    }
}
EOF
    
    print_success "Configuración de Nginx creada"
}

# Crear configuración de Grafana
create_grafana_config() {
    print_message "Creando configuración de Grafana..."
    
    # Datasource para PostgreSQL
    cat > grafana/datasources/postgres.yml << 'EOF'
apiVersion: 1

datasources:
  - name: Orders DB
    type: postgres
    url: orders_db:5432
    database: orders_db
    user: orders_user
    secureJsonData:
      password: orders_pass
    jsonData:
      sslmode: disable
      maxOpenConns: 10
      maxIdleConns: 10
      connMaxLifetime: 14400
    
  - name: Payments DB
    type: postgres
    url: payments_db:5432
    database: payments_db
    user: payments_user
    secureJsonData:
      password: payments_pass
    jsonData:
      sslmode: disable
      maxOpenConns: 10
      maxIdleConns: 10
      connMaxLifetime: 14400
EOF
    
    # Dashboard básico
    mkdir -p grafana/dashboards
    cat > grafana/dashboards/dashboard.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'distributed-transactions'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
EOF
    
    print_success "Configuración de Grafana creada"
}

# Verificar y corregir archivos necesarios
check_files() {
    print_message "Verificando archivos necesarios..."
    
    files_needed=(
        "main.py"
        "requirements.txt"
        "Dockerfile"
        "docker-compose.yml"
        "init-scripts/orders_init.sql"
        "init-scripts/payments_init.sql"
    )
    
    missing_files=()
    
    for file in "${files_needed[@]}"; do
        if [[ ! -f "$file" ]]; then
            missing_files+=("$file")
        fi
    done
    
    if [[ ${#missing_files[@]} -gt 0 ]]; then
        print_warning "Archivos faltantes:"
        for file in "${missing_files[@]}"; do
            echo "  - $file"
        done
        print_warning "Asegúrate de tener todos los archivos necesarios antes de continuar"
    else
        print_success "Todos los archivos necesarios están presentes"
    fi
}

# Construir e iniciar servicios
start_services() {
    print_message "Construyendo e iniciando servicios..."
    
    # Parar servicios existentes
    docker-compose down -v 2>/dev/null || true
    
    # Construir imágenes
    docker-compose build --no-cache
    
    # Iniciar servicios
    docker-compose up -d
    
    print_success "Servicios iniciados"
}

# Verificar estado de servicios
check_services() {
    print_message "Verificando estado de servicios..."
    
    sleep 30  # Esperar a que los servicios se inicien
    
    services=("orders_database" "payments_database" "distributed_transactions_app")
    
    for service in "${services[@]}"; do
        if docker ps --format "table {{.Names}}" | grep -q "$service"; then
            print_success "$service está corriendo"
        else
            print_error "$service no está corriendo"
        fi
    done
}

# Mostrar información de conexión
show_connection_info() {
    print_message "Información de conexión:"
    echo ""
    echo -e "${GREEN}🌐 Dashboard Principal:${NC} http://localhost:8000"
    echo -e "${GREEN}📚 API Documentation:${NC} http://localhost:8000/docs"
    echo -e "${GREEN}💊 Health Check:${NC} http://localhost:8000/health"
    echo -e "${GREEN}🔍 pgAdmin:${NC} http://localhost:8080"
    echo -e "${GREEN}📊 Grafana:${NC} http://localhost:3000"
    echo -e "${GREEN}🔄 Nginx Proxy:${NC} http://localhost"
    echo ""
    echo -e "${YELLOW}Credenciales:${NC}"
    echo "  pgAdmin: admin@distributed-transactions.com / admin123"
    echo "  Grafana: admin / admin123"
    echo ""
}

# Función principal
main() {
    echo -e "${BLUE}"
    echo "=============================================================="
    echo "  Sistema de Transacciones Distribuidas - Setup Script"
    echo "=============================================================="
    echo -e "${NC}"
    
    check_dependencies
    create_directories
    set_permissions
    create_pgadmin_config
    create_nginx_config
    create_grafana_config
    check_files
    
    read -p "¿Deseas iniciar los servicios ahora? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        start_services
        check_services
        show_connection_info
    else
        print_message "Setup completado. Ejecuta 'docker-compose up -d' para iniciar los servicios."
    fi
    
    print_success "Setup completado exitosamente!"
}

# Función de cleanup
cleanup() {
    print_message "Limpiando recursos..."
    docker-compose down -v
    docker system prune -f
    print_success "Cleanup completado"
}

# Función de logs
show_logs() {
    echo "Selecciona el servicio para ver logs:"
    echo "1) app"
    echo "2) orders_db"
    echo "3) payments_db"
    echo "4) todos"
    read -p "Opción (1-4): " choice
    
    case $choice in
        1) docker-compose logs -f app ;;
        2) docker-compose logs -f orders_db ;;
        3) docker-compose logs -f payments_db ;;
        4) docker-compose logs -f ;;
        *) print_error "Opción inválida" ;;
    esac
}

# Manejar argumentos de línea de comandos
case "${1:-setup}" in
    setup)
        main
        ;;
    start)
        start_services
        check_services
        show_connection_info
        ;;
    stop)
        docker-compose down
        ;;
    restart)
        docker-compose restart
        ;;
    logs)
        show_logs
        ;;
    cleanup)
        cleanup
        ;;
    status)
        docker-compose ps
        ;;
    *)
        echo "Uso: $0 {setup|start|stop|restart|logs|cleanup|status}"
        echo ""
        echo "Comandos:"
        echo "  setup    - Configuración inicial completa"
        echo "  start    - Iniciar servicios"
        echo "  stop     - Detener servicios"
        echo "  restart  - Reiniciar servicios"
        echo "  logs     - Ver logs de servicios"
        echo "  cleanup  - Limpiar recursos"
        echo "  status   - Ver estado de servicios"
        exit 1
        ;;
esac