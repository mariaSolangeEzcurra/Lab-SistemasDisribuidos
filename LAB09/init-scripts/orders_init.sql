-- Inicialización simplificada de la base de datos Orders
-- Compatible con el código Python FastAPI
-- ====================================================

-- Crear extensiones básicas
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabla de clientes
CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de productos
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de órdenes
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) UNIQUE,
    customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    amount DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_order_status CHECK (status IN ('pending', 'confirmed', 'cancelled', 'processing'))
);

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers para updated_at
CREATE TRIGGER update_customers_updated_at 
    BEFORE UPDATE ON customers 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at 
    BEFORE UPDATE ON products 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at 
    BEFORE UPDATE ON orders 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Índices para optimización
CREATE INDEX IF NOT EXISTS idx_orders_transaction_id ON orders(transaction_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_product_id ON orders(product_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);

-- Datos de ejemplo para testing (solo si las tablas están vacías)
DO $$
BEGIN
    -- Insertar clientes solo si no existen
    IF NOT EXISTS (SELECT 1 FROM customers LIMIT 1) THEN
        INSERT INTO customers (name, email, phone, address) VALUES 
            ('Juan Pérez', 'juan.perez@email.com', '+51987654321', 'Av. Arequipa 123, Lima'),
            ('María García', 'maria.garcia@email.com', '+51987654322', 'Calle Libertad 456, Lima'),
            ('Carlos Rodríguez', 'carlos.rodriguez@email.com', '+51987654323', 'Jr. Unión 789, Lima'),
            ('Ana López', 'ana.lopez@email.com', '+51987654324', 'Av. Brasil 321, Lima'),
            ('Pedro Martínez', 'pedro.martinez@email.com', '+51987654325', 'Calle Real 654, Lima');
        
        RAISE NOTICE 'Clientes de ejemplo insertados';
    END IF;
    
    -- Insertar productos solo si no existen
    IF NOT EXISTS (SELECT 1 FROM products LIMIT 1) THEN
        INSERT INTO products (name, description, price, stock_quantity, category) VALUES 
            ('Laptop Dell XPS 13', 'Laptop ultrabook con procesador Intel i7', 3500.00, 10, 'Electrónicos'),
            ('Smartphone Samsung Galaxy', 'Teléfono inteligente con cámara de 108MP', 1200.00, 25, 'Electrónicos'),
            ('Auriculares Sony WH-1000XM4', 'Auriculares con cancelación de ruido', 350.00, 15, 'Audio'),
            ('Tablet iPad Air', 'Tablet Apple con pantalla de 10.9 pulgadas', 800.00, 20, 'Electrónicos'),
            ('Cámara Canon EOS R6', 'Cámara mirrorless profesional', 2500.00, 8, 'Fotografía'),
            ('Reloj Apple Watch', 'Smartwatch con monitoreo de salud', 450.00, 30, 'Wearables'),
            ('Teclado Mecánico Logitech', 'Teclado gaming con switches mecánicos', 150.00, 40, 'Accesorios'),
            ('Monitor 4K LG', 'Monitor 27 pulgadas 4K UHD', 600.00, 12, 'Monitores'),
            ('Mouse Gaming Razer', 'Mouse para gaming con DPI ajustable', 80.00, 50, 'Accesorios'),
            ('Disco SSD Samsung 1TB', 'Unidad de estado sólido NVMe', 120.00, 35, 'Almacenamiento');
        
        RAISE NOTICE 'Productos de ejemplo insertados';
    END IF;
END $$;

-- Vista para reportes (compatible con el código Python)
CREATE OR REPLACE VIEW order_summary AS
SELECT 
    o.id,
    o.transaction_id,
    c.name as customer_name,
    c.email as customer_email,
    p.name as product_name,
    p.price as product_price,
    o.quantity,
    o.amount,
    o.status,
    o.created_at
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
LEFT JOIN products p ON o.product_id = p.id;

-- Función para obtener estadísticas (utilizable desde Python)
CREATE OR REPLACE FUNCTION get_order_statistics()
RETURNS TABLE(
    total_orders INTEGER,
    pending_orders INTEGER,
    confirmed_orders INTEGER,
    cancelled_orders INTEGER,
    total_revenue DECIMAL(12,2),
    avg_order_value DECIMAL(12,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*)::INTEGER as total_orders,
        COUNT(CASE WHEN status = 'pending' THEN 1 END)::INTEGER as pending_orders,
        COUNT(CASE WHEN status = 'confirmed' THEN 1 END)::INTEGER as confirmed_orders,
        COUNT(CASE WHEN status = 'cancelled' THEN 1 END)::INTEGER as cancelled_orders,
        COALESCE(SUM(CASE WHEN status = 'confirmed' THEN amount END), 0) as total_revenue,
        COALESCE(AVG(CASE WHEN status = 'confirmed' THEN amount END), 0) as avg_order_value
    FROM orders;
END;
$$ LANGUAGE plpgsql;

-- Configurar permisos para el usuario orders_user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO orders_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO orders_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO orders_user;

-- Comentarios para documentación
COMMENT ON TABLE customers IS 'Información de clientes registrados en el sistema';
COMMENT ON TABLE products IS 'Catálogo de productos disponibles para venta';
COMMENT ON TABLE orders IS 'Órdenes de compra realizadas por los clientes';
COMMENT ON VIEW order_summary IS 'Vista con información completa de órdenes';

-- Mensaje de confirmación
DO $$
BEGIN
    RAISE NOTICE '✅ Base de datos Orders inicializada correctamente con datos de ejemplo';
    RAISE NOTICE '📊 Tablas creadas: customers, products, orders';
    RAISE NOTICE '🔍 Vista creada: order_summary';
    RAISE NOTICE '⚡ Funciones disponibles: get_order_statistics()';
END $$;