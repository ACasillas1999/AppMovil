<?php
header('Content-Type: application/json; charset=UTF-8');

require_once __DIR__ . '/Conexiones/Conexion.php';
if (isset($conn) && $conn instanceof mysqli) { $conn->set_charset('utf8mb4'); }

$username = isset($_GET['username']) ? trim($_GET['username']) : (isset($_POST['username']) ? trim($_POST['username']) : '');
if ($username === '') { echo json_encode(['ok'=>false,'error'=>'FALTA_USERNAME']); exit; }

try {
    $sqlVeh = 'SELECT v.id_vehiculo FROM vehiculos v INNER JOIN choferes c ON c.ID = v.id_chofer_asignado WHERE c.username=? LIMIT 1';
    $st = $conn->prepare($sqlVeh);
    if (!$st) { throw new Exception('Prepare veh: '.$conn->error); }
    $st->bind_param('s', $username);
    $st->execute();
    $st->store_result();
    $idVehiculo = null; $st->bind_result($idVehiculo);
    $found = $st->fetch();
    $st->free_result(); $st->close();
    if (!$found) { echo json_encode(['ok'=>false,'error'=>'NO_ASIGNADO']); exit; }

    $sql = 'SELECT seccion, item, calificacion, observaciones_rotulado
            FROM checklist_vehicular
            WHERE id_vehiculo = ? AND DATE(fecha_inspeccion) = CURDATE()
            ORDER BY seccion, id ASC';
    $q = $conn->prepare($sql);
    if (!$q) { throw new Exception('Prepare q: '.$conn->error); }
    $q->bind_param('i', $idVehiculo);
    $q->execute();
    $q->store_result();
    $sec = $it = $cal = $obsRow = null;
    $q->bind_result($sec, $it, $cal, $obsRow);
    $items = [];
    while ($q->fetch()) {
        $obsOut = null;
        if (!is_null($obsRow) && $obsRow !== '') {
            $tmp = (string)$obsRow;
            if (stripos($tmp, '[AUTO]') === 0) {
                $tmp = trim(substr($tmp, 6)); // limpiar etiqueta antigua si existiera
            }
            $obsOut = ($tmp === '') ? null : $tmp;
        }
        $items[] = [
            'seccion' => $sec,
            'item' => $it,
            'calificacion' => $cal,
            // Enviar cadena vacia en lugar de null para evitar "null" literal en clientes
            'observacion' => is_null($obsOut) ? '' : $obsOut,
        ];
    }
    $q->free_result();
    $q->close();

    if (count($items) === 0) { echo json_encode(['ok'=>false,'error'=>'SIN_DATOS']); exit; }

    echo json_encode(['ok'=>true,'items'=>$items]);
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(['ok'=>false,'error'=>'ERROR']);
}
