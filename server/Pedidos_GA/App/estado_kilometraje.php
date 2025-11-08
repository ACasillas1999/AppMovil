<?php
header('Content-Type: application/json; charset=UTF-8');

require_once __DIR__ . '/Conexiones/Conexion.php';
if (isset($conn) && $conn instanceof mysqli) {
    $conn->set_charset('utf8mb4');
}

$username = isset($_POST['username']) ? trim($_POST['username']) : (isset($_GET['username']) ? trim($_GET['username']) : '');
if ($username === '') {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'FALTA_USERNAME']);
    exit;
}

try {
    // Vehículo asignado al chofer (por username)
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
        echo json_encode(['ok' => true, 'assigned' => false]);
        exit;
    }

    // Último registro de kilometraje del vehículo
    $sqlLast = 'SELECT id_registro, fecha_registro, kilometraje_final
                FROM registro_kilometraje
                WHERE id_vehiculo = ?
                ORDER BY fecha_registro DESC, id_registro DESC
                LIMIT 1';
    $stLast = $conn->prepare($sqlLast);
    if (!$stLast) { throw new Exception('Prepare last: ' . $conn->error); }
    $stLast->bind_param('i', $idVehiculo);
    $stLast->execute();
    $stLast->store_result();
    $idReg = null; $fecha = null; $kmFin = null;
    $stLast->bind_result($idReg, $fecha, $kmFin);
    $hasLast = $stLast->fetch();
    $stLast->free_result();
    $stLast->close();

    $needs = true;
    $lastFecha = null;
    $lastKm = null;

    if ($hasLast) {
        $lastFecha = $fecha;
        $lastKm = is_null($kmFin) ? null : (int)$kmFin;

        $hoy = new DateTime('today');
        $f = DateTime::createFromFormat('Y-m-d', substr((string)$lastFecha, 0, 10));

        // Verificar si hoy es lunes
        $esLunes = ((int)$hoy->format('N') === 1); // 1 = Lunes en ISO-8601

        if ($esLunes) {
            // Si es lunes, verificar si ya registró esta semana (desde el lunes)
            $lunesActual = (clone $hoy)->modify('monday this week');
            if ($f && $f >= $lunesActual) {
                $needs = false; // Ya registró esta semana
            }
        } else {
            // Si no es lunes, verificar si registró desde el lunes de esta semana
            $lunesActual = (clone $hoy)->modify('monday this week');
            if ($f && $f >= $lunesActual) {
                $needs = false; // Ya registró desde el lunes
            }
        }
    }

    echo json_encode([
        'ok' => true,
        'assigned' => true,
        'id_vehiculo' => (int)$idVehiculo,
        'id_chofer' => is_null($idChofer) ? null : (int)$idChofer,
        'last_fecha' => $lastFecha,
        'Km_Total' => $lastKm,
        'needs_km' => $needs,
    ]);
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => 'ERROR']);
}
