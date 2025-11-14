<?php
// Conexion MySQLi centralizada para los endpoints
// Intenta obtener credenciales de variables de entorno y luego usa defaults

if (!isset($conn) || !($conn instanceof mysqli)) {
    $dbHost = getenv('DB_HOST') !== false ? getenv('DB_HOST') : 'localhost';
    $dbUser = getenv('DB_USER') !== false ? getenv('DB_USER') : 'root';
    $dbPass = getenv('DB_PASS') !== false ? getenv('DB_PASS') : '';
    $dbName = getenv('DB_NAME') !== false ? getenv('DB_NAME') : 'gpoascen_pedidos_app';

    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
    try {
        $conn = new mysqli($dbHost, $dbUser, $dbPass, $dbName);
        $conn->set_charset('utf8mb4');
    } catch (Throwable $e) {
        http_response_code(500);
        header('Content-Type: application/json; charset=UTF-8');
        echo json_encode(['ok' => false, 'error' => 'DB_CONN', 'detail' => '']);
        exit;
    }
}
?>

