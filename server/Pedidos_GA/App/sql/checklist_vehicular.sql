-- Tabla para checklist vehicular (1 fila por ítem)
CREATE TABLE IF NOT EXISTS checklist_vehicular (
  id INT AUTO_INCREMENT PRIMARY KEY,
  id_vehiculo INT NOT NULL,
  id_chofer INT NULL,
  fecha_inspeccion DATETIME NOT NULL,
  kilometraje INT NULL,
  seccion VARCHAR(100) NOT NULL,
  item VARCHAR(150) NOT NULL,
  calificacion ENUM('Bien','Mal','N/A') NOT NULL,
  observaciones_rotulado TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_checklist_vehiculo (id_vehiculo, fecha_inspeccion),
  INDEX idx_checklist_chofer (id_chofer, fecha_inspeccion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

