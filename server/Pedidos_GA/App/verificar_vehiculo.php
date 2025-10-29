<?php
// Respuesta en texto plano
header('Content-Type: text/plain; charset=UTF-8');

// ===== Configuración de BD (ajusta a tu entorno) =====
// Host de MySQL
$DB_HOST = 'localhost';
// Nombre de la base (según la captura parece ser gopascen_pedidos_app)
$DB_NAME = 'gopascen_pedidos_app';
// Usuario y contraseña de MySQL
$DB_USER = 'root';
$DB_PASS = '';

// ===== Entrada =====
$username = isset($_POST['username']) ? trim($_POST['username']) : (isset($_GET['username']) ? trim($_GET['username']) : '');
if ($username === '') {
    http_response_code(400);
    echo 'FALTA_USERNAME';
    exit;
}

try {
    $dsn = "mysql:host={$DB_HOST};dbname={$DB_NAME};charset=utf8";
    $pdo = new PDO($dsn, $DB_USER, $DB_PASS, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);

    // Intento 1: choferes.username
    $sql = "SELECT v.id_vehiculo
            FROM vehiculos v
            INNER JOIN choferes c ON c.id_chofer = v.id_chofer_asignado
            WHERE c.username = :username
            LIMIT 1";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':username' => $username]);
    $row = $stmt->fetch();

    // Intento 2: si no existe, probar choferes.usuario (algunas BD usan este nombre)
    if (!$row) {
        $sqlAlt = "SELECT v.id_vehiculo
                   FROM vehiculos v
                   INNER JOIN choferes c ON c.id_chofer = v.id_chofer_asignado
                   WHERE c.usuario = :username
                   LIMIT 1";
        try {
            $stmtAlt = $pdo->prepare($sqlAlt);
            $stmtAlt->execute([':username' => $username]);
            $row = $stmtAlt->fetch();
        } catch (Throwable $e) {
            // Si la columna no existe, ignoramos este intento
        }
    }

    if ($row && isset($row['id_vehiculo'])) {
        echo 'ASIGNADO';
    } else {
        echo 'NO_ASIGNADO';
    }
} catch (Throwable $e) {
    // No exponer detalles en producción
    http_response_code(500);
    echo 'ERROR';
}

