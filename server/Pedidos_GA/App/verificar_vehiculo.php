<?php
header('Content-Type: text/plain; charset=UTF-8');

require_once __DIR__ . '/Conexiones/Conexion.php';
if (isset($conn) && $conn instanceof mysqli) {
    $conn->set_charset('utf8mb4');
}

// Acepta username o usuario como parámetro
$username = '';
if (isset($_POST['username'])) $username = trim($_POST['username']);
elseif (isset($_GET['username'])) $username = trim($_GET['username']);
elseif (isset($_POST['usuario'])) $username = trim($_POST['usuario']);
elseif (isset($_GET['usuario'])) $username = trim($_GET['usuario']);

if ($username === '') {
    http_response_code(400);
    echo 'FALTA_USERNAME';
    exit;
}

$sql = 'SELECT v.id_vehiculo
        FROM vehiculos AS v
        INNER JOIN choferes AS c ON c.ID = v.id_chofer_asignado
        WHERE c.username = ?
        LIMIT 1';

try {
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        throw new Exception('Prepare failed: ' . $conn->error);
    }
    $stmt->bind_param('s', $username);
    $stmt->execute();

    // Evitar dependencia de mysqlnd/get_result
    $stmt->store_result();
    $idVehiculo = null;
    $stmt->bind_result($idVehiculo);
    $rowFound = $stmt->fetch();
    $stmt->free_result();
    $stmt->close();

    echo ($rowFound && !is_null($idVehiculo)) ? 'ASIGNADO' : 'NO_ASIGNADO';
} catch (Throwable $e) {
    http_response_code(500);
    echo 'ERROR';
}
