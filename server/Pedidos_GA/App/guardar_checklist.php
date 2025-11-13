<?php
header('Content-Type: application/json; charset=UTF-8');

require_once __DIR__ . '/Conexiones/Conexion.php';
if (isset($conn) && $conn instanceof mysqli) { $conn->set_charset('utf8mb4'); }

function bad($msg){ http_response_code(400); echo json_encode(['ok'=>false,'error'=>$msg]); exit; }

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!is_array($data)) bad('JSON');

$username = isset($data['username']) ? trim($data['username']) : '';
$km = isset($data['kilometraje']) ? $data['kilometraje'] : null;
$fecha = isset($data['fecha_inspeccion']) ? trim($data['fecha_inspeccion']) : null;
$items = isset($data['items']) ? $data['items'] : null;

if ($username === '' || !is_array($items) || count($items) === 0) bad('PARAMS');

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
    if (!$found) bad('NO_ASIGNADO');

    $conn->begin_transaction();

    $sqlIns = 'INSERT INTO checklist_vehicular
        (id_vehiculo, id_chofer, fecha_inspeccion, kilometraje, seccion, item, calificacion, observaciones_rotulado, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())';
    $ins = $conn->prepare($sqlIns);
    if (!$ins) { throw new Exception('Prepare ins: ' . $conn->error); }

    $allowed = ['Bien','Mal','N/A'];
    $fechaIns = $fecha ?: date('Y-m-d H:i:s');

    foreach ($items as $it) {
        $seccion = isset($it['seccion']) ? trim($it['seccion']) : '';
        $item = isset($it['item']) ? trim($it['item']) : '';
        $cal = isset($it['calificacion']) ? trim($it['calificacion']) : '';
        // Se acepta 'observacion' o 'observaciones_rotulado' desde el cliente y se guarda en la columna existente
        $obs = isset($it['observacion']) ? $it['observacion'] : (isset($it['observaciones_rotulado']) ? $it['observaciones_rotulado'] : null);

        if ($seccion === '' || $item === '' || !in_array($cal, $allowed, true)) {
            $conn->rollback(); bad('FALTAN_CALIFICACIONES');
        }

        // Regla: si calificación es 'Mal', observación es obligatoria; para otras calificaciones se ignora
        if ($cal === 'Mal') {
            $obs = is_null($obs) ? '' : trim((string)$obs);
            if ($obs === '') { $conn->rollback(); bad('FALTA_OBSERVACION'); }
        } else {
            // Para Bien/N.A no se requiere observación
            $obs = null;
        }

        $kmVal = is_null($km) || $km === '' ? null : (int)$km;
        $ins->bind_param('iissssss', $idVehiculo, $idChofer, $fechaIns, $kmVal, $seccion, $item, $cal, $obs);
        $ins->execute();
    }

    $ins->close();
    $conn->commit();
    echo json_encode(['ok'=>true]);
} catch (Throwable $e) {
    if ($conn && $conn->errno) { $conn->rollback(); }
    http_response_code(500);
    echo json_encode(['ok'=>false,'error'=>'ERROR']);
}
