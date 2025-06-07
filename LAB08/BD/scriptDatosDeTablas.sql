USE EmpresaDB;

-- Insertar múltiples Departamentos
INSERT INTO Departamento (Nombre, Telefono, Fax) VALUES
('Tecnología', '931932438', '123-456-7890'),
('Recursos Humanos', '934567890', '456-123-7890'),
('Finanzas', '987654321', '321-654-0987'),
('Logística', '900123456', '555-111-2222'),
('Marketing', '912345678', '444-222-3333'),
('Calidad', '916543210', '222-333-4444'),
('Ventas', '917654321', '333-444-5555'),
('Soporte Técnico', '918765432', '444-555-6666'),
('Investigación y Desarrollo', '929191919', '101-202-3030'),
('Atención al Cliente', '928282828', '404-505-6060'),
('Legal', '927373737', '707-808-9090'),
('Administración', '926262626', '111-222-3333');


-- Insertar múltiples Proyectos
INSERT INTO Proyecto (Nombre, Fec_Inicio, Fec_Termino, IDDpto) VALUES
('Sistema de Nómina', '2025-01-10', '2025-06-10',
 (SELECT IDDpto FROM Departamento WHERE Nombre = 'Recursos Humanos')),
('Aplicación Móvil', '2025-03-15', NULL,
 (SELECT IDDpto FROM Departamento WHERE Nombre = 'Tecnología')),
('Sistema Contable', '2025-02-01', '2025-09-01',
 (SELECT IDDpto FROM Departamento WHERE Nombre = 'Finanzas')),
('Gestión de Inventario', '2025-04-01', NULL,
 (SELECT IDDpto FROM Departamento WHERE Nombre = 'Logística')),
('Campaña Digital 2025', '2025-05-01', '2025-08-30',
 (SELECT IDDpto FROM Departamento WHERE Nombre = 'Marketing')),
('Control de Calidad', '2025-08-01', '2025-12-15',
 (SELECT IDDpto FROM Departamento WHERE Nombre = 'Calidad')),
('Expansión de Ventas', '2025-07-10', NULL,
 (SELECT IDDpto FROM Departamento WHERE Nombre = 'Ventas')),
('Sistema de Tickets', '2025-06-01', '2025-09-30',
 (SELECT IDDpto FROM Departamento WHERE Nombre = 'Soporte Técnico'));

-- Insertar múltiples Ingenieros
INSERT INTO Ingeniero (Especialidad, Cargo) VALUES
('Software', 'Senior Developer'),
('Redes', 'Network Engineer'),
('Seguridad', 'Security Analyst'),
('Datos', 'Data Engineer'),
('DevOps', 'Cloud Engineer'),
('Backend', 'Junior Developer'),
('Frontend', 'UI Developer'),
('QA', 'Tester'),
('Calidad', 'Quality Analyst'),
('Ventas', 'Sales Manager'),
('Soporte', 'Support Specialist'),
('Analítica', 'Business Analyst'),
('IA', 'Machine Learning Engineer'),
('CRM', 'CRM Specialist'),
('LegalTech', 'Legal Advisor'),
('Administrativo', 'Office Manager'),
('Soporte', 'Technical Support'),
('Finanzas', 'Financial Analyst'),
('Marketing Digital', 'Digital Marketer');

-- Insertar múltiples asignaciones en Ingeniero_Proyecto
INSERT INTO Ingeniero_Proyecto (IDIng, IDProy, Fecha_Asignacion) VALUES
((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Software'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Sistema de Nómina'), '2025-01-15'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Redes'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Aplicación Móvil'), '2025-03-20'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Seguridad'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Aplicación Móvil'), '2025-04-01'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Datos'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Sistema Contable'), '2025-02-10'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Software'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Aplicación Móvil'), '2025-03-22'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Backend'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Gestión de Inventario'), '2025-04-05'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Frontend'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Campaña Digital 2025'), '2025-05-10'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'QA'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Campaña Digital 2025'), '2025-05-12'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'DevOps'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Gestión de Inventario'), '2025-04-08'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Calidad'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Control de Calidad'), '2025-08-05'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Ventas'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Expansión de Ventas'), '2025-07-12'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Soporte'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Sistema de Tickets'), '2025-06-02'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Analítica'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Expansión de Ventas'), '2025-07-15'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'IA'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Desarrollo IA'), '2025-09-05'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'CRM'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Mejora CRM'), '2025-07-20'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'LegalTech'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Actualización Legal'), '2025-05-25'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Administrativo'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Planificación Estratégica'), '2025-03-10'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Soporte'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Mejora CRM'), '2025-07-22'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Finanzas'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Planificación Estratégica'), '2025-03-15'),

((SELECT IDIng FROM Ingeniero WHERE Especialidad = 'Marketing Digital'),
 (SELECT IDProy FROM Proyecto WHERE Nombre = 'Campaña Digital 2025'), '2025-05-15');