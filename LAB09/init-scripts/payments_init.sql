-- Inicialización simplificada de la base de datos Payments
-- Compatible con el código Python FastAPI
-- =======================================================

-- Crear extensiones básicas
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabla principal de pagos
CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) UNIQUE,
    customer_id INTEGER NOT NULL,
    amount DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'PEN',
    payment_method VARCHAR(50) NOT NULL DEFAULT 'credit_card',
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    fraud_score DECIMAL(3,2) DEFAULT 0.0,
    gateway_transaction_id VARCHAR(255),
    gateway_response JSONB,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints de validación
    CONSTRAINT valid_payment_status CHECK (status IN ('pending', 'processed', 'failed', 'refunded', 'cancelled')),
    CONSTRAINT valid_payment_method CHECK (payment_method IN ('credit_card', 'debit_card', 'paypal', 'bank_transfer', 'crypto')),
    CONSTRAINT valid_currency CHECK (currency IN ('PEN', 'USD', 'EUR', 'BTC')),
    CONSTRAINT valid_fraud_score CHECK (fraud_score >= 0.0 AND fraud_score <= 1.0)
);

-- Tabla de métodos de pago de clientes (opcional pero útil)
CREATE TABLE IF NOT EXISTS customer_payment_methods (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    card_last_four VARCHAR(4),
    card_brand VARCHAR(20),
    expires_at DATE,
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de reembolsos
CREATE TABLE IF NOT EXISTS refunds (
    id SERIAL PRIMARY KEY,
    payment_id INTEGER REFERENCES payments(id) ON DELETE CASCADE,
    amount DECIMAL(12,2) NOT NULL,
    reason TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    gateway_refund_id VARCHAR(255),
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_refund_status CHECK (status IN ('pending', 'processed', 'failed', 'cancelled'))
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
CREATE TRIGGER update_payments_updated_at 
    BEFORE UPDATE ON payments 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_customer_payment_methods_updated_at 
    BEFORE UPDATE ON customer_payment_methods 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Función para calcular fraud score básico
CREATE OR REPLACE FUNCTION calculate_fraud_score(
    p_customer_id INTEGER,
    p_amount DECIMAL,
    p_payment_method VARCHAR
) RETURNS DECIMAL AS $$
DECLARE
    fraud_score DECIMAL := 0.0;
    recent_payments_count INTEGER;
    large_amount_threshold DECIMAL := 5000.0;
    frequent_payments_threshold INTEGER := 5;
BEGIN
    -- Verificar cantidad de pagos recientes (últimas 24 horas)
    SELECT COUNT(*) INTO recent_payments_count
    FROM payments
    WHERE customer_id = p_customer_id
    AND created_at > CURRENT_TIMESTAMP - INTERVAL '24 hours'
    AND status = 'processed';
    
    -- Incrementar score si hay muchos pagos recientes
    IF recent_payments_count >= frequent_payments_threshold THEN
        fraud_score := fraud_score + 0.3;
    END IF;
    
    -- Incrementar score para montos muy altos
    IF p_amount > large_amount_threshold THEN
        fraud_score := fraud_score + 0.2;
    END IF;
    
    -- Incrementar score para métodos de pago riesgosos
    IF p_payment_method = 'crypto' THEN
        fraud_score := fraud_score + 0.1;
    END IF;
    
    -- Asegurar que el score esté en el rango válido
    fraud_score := LEAST(fraud_score, 1.0);
    
    RETURN fraud_score;
END;
$$ LANGUAGE plpgsql;

-- Trigger para calcular fraud score automáticamente (opcional)
CREATE OR REPLACE FUNCTION set_fraud_score()
RETURNS TRIGGER AS $$
BEGIN
    -- Solo calcular si no se proporcionó un fraud_score
    IF NEW.fraud_score = 0.0 OR NEW.fraud_score IS NULL THEN
        NEW.fraud_score := calculate_fraud_score(NEW.customer_id, NEW.amount, NEW.payment_method);
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER calculate_fraud_score_trigger
    BEFORE INSERT ON payments
    FOR EACH ROW EXECUTE FUNCTION set_fraud_score();

-- Índices para optimización
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payments_customer_id ON payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
CREATE INDEX IF NOT EXISTS idx_payments_amount ON payments(amount);
CREATE INDEX IF NOT EXISTS idx_payments_fraud_score ON payments(fraud_score);
CREATE INDEX IF NOT EXISTS idx_customer_payment_methods_customer_id ON customer_payment_methods(customer_id);
CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds(payment_id);

-- Datos de ejemplo para testing (solo si las tablas están vacías)
DO $$
BEGIN
    -- Insertar métodos de pago de ejemplo solo si no existen
    IF NOT EXISTS (SELECT 1 FROM customer_payment_methods LIMIT 1) THEN
        INSERT INTO customer_payment_methods (customer_id, payment_type, card_last_four, card_brand, expires_at, is_default) VALUES 
            (1, 'credit_card', '1234', 'Visa', '2026-12-31', true),
            (1, 'debit_card', '5678', 'Mastercard', '2025-08-31', false),
            (2, 'credit_card', '9012', 'American Express', '2027-03-31', true),
            (3, 'credit_card', '3456', 'Visa', '2026-06-30', true),
            (3, 'paypal', NULL, NULL, NULL, false),
            (4, 'credit_card', '7890', 'Mastercard', '2025-11-30', true),
            (5, 'debit_card', '2468', 'Visa', '2026-09-30', true);
        
        RAISE NOTICE 'Métodos de pago de ejemplo insertados';
    END IF;
END $$;

-- Vistas para reportes
CREATE OR REPLACE VIEW payment_summary AS
SELECT 
    p.id,
    p.transaction_id,
    p.customer_id,
    p.amount,
    p.currency,
    p.payment_method,
    p.status,
    p.fraud_score,
    p.created_at,
    p.processed_at,
    CASE 
        WHEN p.fraud_score > 0.7 THEN 'HIGH_RISK'
        WHEN p.fraud_score > 0.3 THEN 'MEDIUM_RISK'
        ELSE 'LOW_RISK'
    END as risk_level
FROM payments p;

CREATE OR REPLACE VIEW daily_payment_stats AS
SELECT 
    DATE(created_at) as payment_date,
    COUNT(*) as total_payments,
    COUNT(CASE WHEN status = 'processed' THEN 1 END) as successful_payments,
    COUNT(CASE WHEN status = 'failed' THEN 1 END) as failed_payments,
    SUM(CASE WHEN status = 'processed' THEN amount ELSE 0 END) as total_revenue,
    AVG(CASE WHEN status = 'processed' THEN amount END) as avg_payment_amount,
    AVG(fraud_score) as avg_fraud_score
FROM payments
GROUP BY DATE(created_at)
ORDER BY payment_date DESC;

-- Función para obtener estadísticas de pagos
CREATE OR REPLACE FUNCTION get_payment_statistics()
RETURNS TABLE(
    total_payments INTEGER,
    successful_payments INTEGER,
    failed_payments INTEGER,
    pending_payments INTEGER,
    total_revenue DECIMAL(15,2),
    avg_payment_amount DECIMAL(12,2),
    high_risk_payments INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(*)::INTEGER as total_payments,
        COUNT(CASE WHEN status = 'processed' THEN 1 END)::INTEGER as successful_payments,
        COUNT(CASE WHEN status = 'failed' THEN 1 END)::INTEGER as failed_payments,
        COUNT(CASE WHEN status = 'pending' THEN 1 END)::INTEGER as pending_payments,
        COALESCE(SUM(CASE WHEN status = 'processed' THEN amount END), 0) as total_revenue,
        COALESCE(AVG(CASE WHEN status = 'processed' THEN amount END), 0) as avg_payment_amount,
        COUNT(CASE WHEN fraud_score > 0.7 THEN 1 END)::INTEGER as high_risk_payments
    FROM payments;
END;
$$ LANGUAGE plpgsql;

-- Función para procesar reembolsos
CREATE OR REPLACE FUNCTION process_refund(
    p_payment_id INTEGER,
    p_amount DECIMAL,
    p_reason TEXT DEFAULT 'Customer request'
) RETURNS INTEGER AS $$
DECLARE
    refund_id INTEGER;
    payment_amount DECIMAL;
    payment_status VARCHAR;
BEGIN
    -- Verificar que el pago existe y está procesado
    SELECT amount, status INTO payment_amount, payment_status
    FROM payments
    WHERE id = p_payment_id;
    
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Payment not found with ID: %', p_payment_id;
    END IF;
    
    IF payment_status != 'processed' THEN
        RAISE EXCEPTION 'Payment must be processed to refund. Current status: %', payment_status;
    END IF;
    
    IF p_amount > payment_amount THEN
        RAISE EXCEPTION 'Refund amount (%) cannot exceed payment amount (%)', p_amount, payment_amount;
    END IF;
    
    -- Crear registro de reembolso
    INSERT INTO refunds(payment_id, amount, reason, status, processed_at)
    VALUES(p_payment_id, p_amount, p_reason, 'processed', CURRENT_TIMESTAMP)
    RETURNING id INTO refund_id;
    
    -- Actualizar estado del pago si es reembolso total
    IF p_amount = payment_amount THEN
        UPDATE payments SET status = 'refunded', updated_at = CURRENT_TIMESTAMP WHERE id = p_payment_id;
    END IF;
    
    RETURN refund_id;
END;
$$ LANGUAGE plpgsql;

-- Configurar permisos para el usuario payments_user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO payments_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO payments_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO payments_user;

-- Comentarios para documentación
COMMENT ON TABLE payments IS 'Tabla principal que almacena todas las transacciones de pago';
COMMENT ON TABLE customer_payment_methods IS 'Métodos de pago registrados por los clientes';
COMMENT ON TABLE refunds IS 'Gestión de reembolsos de pagos procesados';
COMMENT ON VIEW payment_summary IS 'Vista con información completa de pagos incluyendo nivel de riesgo';
COMMENT ON VIEW daily_payment_stats IS 'Estadísticas diarias de pagos para reportes';

-- Mensaje de confirmación
DO $$
BEGIN
    RAISE NOTICE '✅ Base de datos Payments inicializada correctamente';
    RAISE NOTICE '💳 Tablas creadas: payments, customer_payment_methods, refunds';
    RAISE NOTICE '🔍 Vistas creadas: payment_summary, daily_payment_stats';
    RAISE NOTICE '⚡ Funciones disponibles: get_payment_statistics(), process_refund()';
    RAISE NOTICE '🛡️ Sistema de detección de fraudes habilitado';
END $$;