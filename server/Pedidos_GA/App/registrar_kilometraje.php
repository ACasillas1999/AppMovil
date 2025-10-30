<?php
header('Content-Type: application/json; charset=UTF-8');

require_once __DIR__ . '/Conexiones/Conexion.php';
if (isset($conn) && $conn instanceof mysqli) {
    $conn->set_charset('utf8mb4');
}

$username = isset($_POST['username']) ? trim($_POST['username']) : (isset($_GET['username']) ? trim($_GET['username']) : '');
$kmActual = isset($_POST['km']) ? trim($_POST['km']) : (isset($_GET['km']) ? trim($_GET['km']) : '');

if ($username === '' || $kmActual === '' || !is_numeric($kmActual)) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'PARAMS']);
    exit;
}

$kmActual = (int)$kmActual;
if ($kmActual < 0) { $kmActual = 0; }

try {
    // Resolver vehículo e id_chofer por username
    $sqlVeh = 'SELECT v.id_vehiculo, c.ID AS id_chofer
               FROM vehiculos AS v
               INNER JOIN choferes AS c ON c.ID = v.id_chofer_asignado
               WHERE c.username = ?
               LIMIT 1';
    $st = $conn->prepare($sqlVeh);
    if (!$st) { throw new Exception('Prepare veh: ' . $conn->error); }
    $st->bind_param('s', $username);
    $st->execute();
    $st->store_result();
    $idVehiculo = null; $idChofer = null;
    $st->bind_result($idVehiculo, $idChofer);
    $found = $st->fetch();
    $st->free_result();
    $st->close();

    if (!$found) {
        http_response_code(404);
        echo json_encode(['ok' => false, 'error' => 'NO_ASIGNADO']);
        exit;
    }

    // Último kilometraje_final para usarlo como inicial
    $sqlLast = 'SELECT kilometraje_final
                FROM registro_kilometraje
                WHERE id_vehiculo = ?
                ORDER BY fecha_registro DESC, id_registro DESC
                LIMIT 1';
    $stLast = $conn->prepare($sqlLast);
    if (!$stLast) { throw new Exception('Prepare last: ' . $conn->error); }
    $stLast->bind_param('i', $idVehiculo);
    $stLast->execute();
    $stLast->store_result();
    $kmInicial = 0; $tmp = null;
    $stLast->bind_result($tmp);
    if ($stLast->fetch() && $tmp !== null) { $kmInicial = (int)$tmp; }
    $stLast->free_result();
    $stLast->close();

    // Insertar registro
    $sqlIns = 'INSERT INTO registro_kilometraje (id_vehiculo, id_chofer, Tipo_Registro, fecha_registro, kilometraje_inicial, kilometraje_final)
               VALUES (?, ?, "Registro", CURDATE(), ?, ?)';
    $stIns = $conn->prepare($sqlIns);
    if (!$stIns) { throw new Exception('Prepare ins: ' . $conn->error); }
    $stIns->bind_param('iiii', $idVehiculo, $idChofer, $kmInicial, $kmActual);
    $stIns->execute();
    $idReg = $conn->insert_id;
    $stIns->close();

    echo json_encode([
        'ok' => true,
        'id_registro' => (int)$idReg,
        'id_vehiculo' => (int)$idVehiculo,
        'id_chofer' => is_null($idChofer) ? null : (int)$idChofer,
        'km_inicial' => (int)$kmInicial,
        'km_final' => (int)$kmActual,
    ]);
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => 'ERROR']);
}

