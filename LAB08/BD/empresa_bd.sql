-- Creacion de la BD 
DROP DATABASE IF EXISTS EmpresaDB;
CREATE DATABASE EmpresaDB;
USE EmpresaDB;

-- Creacion de tablas
CREATE TABLE Departamento (
    IDDpto INT AUTO_INCREMENT PRIMARY KEY,
    Nombre VARCHAR(100) NOT NULL UNIQUE,
    Telefono VARCHAR(15),
    Fax VARCHAR(25)
);
CREATE TABLE Proyecto (
    IDProy INT AUTO_INCREMENT PRIMARY KEY,
    Nombre VARCHAR(100) NOT NULL,
    Fec_Inicio DATE NOT NULL,
    Fec_Termino DATE,
    IDDpto INT NOT NULL,
    CONSTRAINT fk_departamento FOREIGN KEY (IDDpto)
        REFERENCES Departamento(IDDpto)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CHECK (Fec_Termino IS NULL OR Fec_Termino >= Fec_Inicio)
);
CREATE TABLE Ingeniero (
    IDIng INT AUTO_INCREMENT PRIMARY KEY,
    Especialidad VARCHAR(100) NOT NULL,
    Cargo VARCHAR(50) NOT NULL
);
CREATE TABLE Ingeniero_Proyecto (
    IDIng INT NOT NULL,
    IDProy INT NOT NULL,
    Fecha_Asignacion DATE NOT NULL,
    PRIMARY KEY (IDIng, IDProy),
    CONSTRAINT fk_ing FOREIGN KEY (IDIng) REFERENCES Ingeniero(IDIng)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_proy FOREIGN KEY (IDProy) REFERENCES Proyecto(IDProy)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Ejemplos de indices secundarios
CREATE INDEX idx_nombre_departamento ON Departamento(Nombre);
CREATE INDEX idx_especialidad ON Ingeniero(Especialidad);
CREATE INDEX idx_fecha_inicio ON Proyecto(Fec_Inicio);
CREATE INDEX idx_proy_dpto ON Proyecto(IDDpto);
CREATE INDEX idx_ing_proy_idproy ON Ingeniero_Proyecto(IDProy);
CREATE INDEX idx_ing_proy_iding ON Ingeniero_Proyecto(IDIng);

-- Ejemplo de sentencia preparada
PREPARE consultar_proyectos FROM
  'SELECT IDProy, Nombre FROM Proyecto WHERE IDDpto = ?';
SET @id_dpto = 1;
EXECUTE consultar_proyectos USING @id_dpto;
DEALLOCATE PREPARE consultar_proyectos;

-- Ejemplo de procedimientos almacenados
DELIMITER $$
CREATE PROCEDURE AsignarIngenieroAProyecto (
    IN p_IDIng INT,
    IN p_IDProy INT,
    IN p_Fecha DATE
)
BEGIN
    INSERT INTO Ingeniero_Proyecto (IDIng, IDProy, Fecha_Asignacion)
    VALUES (p_IDIng, p_IDProy, p_Fecha);
END$$

CREATE PROCEDURE ActualizarFechaTerminoProyecto (
    IN p_IDProy INT,
    IN p_NuevaFecha DATE
)
BEGIN
    UPDATE Proyecto
    SET Fec_Termino = p_NuevaFecha
    WHERE IDProy = p_IDProy;
END$$

DELIMITER ;

-- Ejemplo de transaccion segura
START TRANSACTION;
INSERT INTO Departamento (Nombre, Telefono, Fax)
VALUES ('Tecnología', '931932438', '123-456-7890');
INSERT INTO Proyecto (Nombre, Fec_Inicio, Fec_Termino, IDDpto)
VALUES ('Nuevo Sistema', '2025-06-07', '2025-12-07', LAST_INSERT_ID());
COMMIT;
