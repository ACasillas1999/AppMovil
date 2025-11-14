<?php

// Definir los datos de conexión como constantes
define('DB_SERVER', 'localhost');
define('DB_USERNAME', 'root');
define('DB_PASSWORD', '');
define('DB_NAME', 'gpoascen_pedidos_app');

// Conectar a la base de datos
$conn = new mysqli(DB_SERVER, DB_USERNAME, DB_PASSWORD, DB_NAME);
$conn->set_charset("utf8mb4");


// Verificar la conexión
if ($conn->connect_error) {
    die("Error de conexión: " . $conn->connect_error);
}

//require_once __DIR__ . "/Conexiones/Conexion.php";

// ---------------------
// Consulta por GRUPO (nuevo)
// Si vienen grupo_id o grupo_nombre, resolvemos aquí y salimos
// ---------------------
// Helpers de parámetros
$__get = function ($k, $def = null) {
    if (isset($_POST[$k])) return is_string($_POST[$k]) ? trim($_POST[$k]) : $_POST[$k];
    if (isset($_GET[$k])) return is_string($_GET[$k]) ? trim($_GET[$k]) : $_GET[$k];
    return $def;
};
function __parse_int($v, $def = null) { if ($v === null || $v === '') return $def; if (!is_numeric($v)) return $def; return (int)$v; }
function __safe_date($v) { if ($v === null || $v === '') return null; $ts = strtotime($v); if ($ts === false) return null; return date('Y-m-d', $ts); }

$__grupoId = __parse_int($__get('grupo_id'));
$__grupoNombre = $__get('grupo_nombre');
if ($__grupoId !== null || ($__grupoNombre !== null && $__grupoNombre !== '')) {
    $__sucursal = $__get('sucursal');
    $__chofer = $__get('chofer');
    $__estado = $__get('estado');
    $__fDesde = __safe_date($__get('fecha_entrega_desde'));
    $__fHasta = __safe_date($__get('fecha_entrega_hasta'));
    $__page = max(1, __parse_int($__get('page'), 1));
    $__limit = __parse_int($__get('limit'), 100); if ($__limit === null) $__limit = 100; if ($__limit > 1000) $__limit = 1000; $__offset = ($__page - 1) * $__limit;

    try {
        // Resolver grupo
        if ($__grupoId === null) {
            $sqlG = 'SELECT id, nombre_grupo, sucursal, chofer_asignado, fecha_creacion, usuario_creo, estado, notas FROM grupos_rutas WHERE LOWER(nombre_grupo) = LOWER(?) LIMIT 1';
            $stG = $conn->prepare($sqlG);
            $stG->bind_param('s', $__grupoNombre);
        } else {
            $sqlG = 'SELECT id, nombre_grupo, sucursal, chofer_asignado, fecha_creacion, usuario_creo, estado, notas FROM grupos_rutas WHERE id = ? LIMIT 1';
            $stG = $conn->prepare($sqlG);
            $stG->bind_param('i', $__grupoId);
        }
        $stG->execute();
        $resG = $stG->get_result();
        if (!$resG || $resG->num_rows === 0) {
            http_response_code(404);
            echo json_encode(['ok' => false, 'error' => 'GRUPO_NOT_FOUND']);
            exit;
        }
        $grupo = $resG->fetch_assoc();
        $stG->close();
        $__grupoId = (int)$grupo['id'];

        // Filtros
        $where = ['pg.grupo_id = ?'];
        $types = 'i';
        $params = [$__grupoId];
        if ($__sucursal !== null && $__sucursal !== '') { $where[] = 'p.SUCURSAL = ?'; $types .= 's'; $params[] = $__sucursal; }
        if ($__chofer !== null && $__chofer !== '') { $where[] = 'gr.chofer_asignado = ?'; $types .= 's'; $params[] = $__chofer; }
        if ($__estado !== null && $__estado !== '') { $where[] = 'p.ESTADO = ?'; $types .= 's'; $params[] = $__estado; }
        if ($__fDesde !== null) { $where[] = 'p.FECHA_ENTREGA_CLIENTE >= ?'; $types .= 's'; $params[] = $__fDesde; }
        if ($__fHasta !== null) { $where[] = 'p.FECHA_ENTREGA_CLIENTE <= ?'; $types .= 's'; $params[] = $__fHasta; }
        $whereSql = implode(' AND ', $where);

        // Total
        $sqlCount = "SELECT COUNT(*) AS total FROM pedidos_grupos pg INNER JOIN pedidos p ON p.ID = pg.pedido_id INNER JOIN grupos_rutas gr ON gr.id = pg.grupo_id WHERE $whereSql";
        $stC = $conn->prepare($sqlCount);
        $stC->bind_param($types, ...$params);
        $stC->execute();
        $resC = $stC->get_result();
        $total = 0; if ($resC && $resC->num_rows > 0) { $tmp = $resC->fetch_assoc(); $total = (int)$tmp['total']; }
        $stC->close();

        // Datos
        $sql = "SELECT p.*, pg.orden_entrega, pg.fecha_asignacion FROM pedidos_grupos pg INNER JOIN pedidos p ON p.ID = pg.pedido_id INNER JOIN grupos_rutas gr ON gr.id = pg.grupo_id WHERE $whereSql ORDER BY pg.orden_entrega ASC, p.ID ASC LIMIT ? OFFSET ?";
        $st = $conn->prepare($sql);
        $types2 = $types . 'ii';
        $params2 = array_merge($params, [ $__limit, $__offset ]);
        $st->bind_param($types2, ...$params2);
        $st->execute();
        $res = $st->get_result();
        $items = [];
        while ($row = $res->fetch_assoc()) {
            $items[] = [
                'ID' => $row['ID'] ?? null,
                'FACTURA' => $row['FACTURA'] ?? null,
                'SUCURSAL' => $row['SUCURSAL'] ?? null,
                'ESTADO' => $row['ESTADO'] ?? null,
                'CHOFER_ASIGNADO' => $row['CHOFER_ASIGNADO'] ?? null,
                'VENDEDOR' => $row['VENDEDOR'] ?? null,
                'NOMBRE_CLIENTE' => $row['NOMBRE_CLIENTE'] ?? null,
                'DIRECCION' => $row['DIRECCION'] ?? null,
                'TELEFONO' => $row['TELEFONO'] ?? null,
                'CONTACTO' => $row['CONTACTO'] ?? null,
                'FECHA_RECEPCION_FACTURA' => $row['FECHA_RECEPCION_FACTURA'] ?? null,
                'FECHA_ENTREGA_CLIENTE' => $row['FECHA_ENTREGA_CLIENTE'] ?? null,
                'FECHA_MIN_ENTREGA' => $row['FECHA_MIN_ENTREGA'] ?? null,
                'FECHA_MAX_ENTREGA' => $row['FECHA_MAX_ENTREGA'] ?? null,
                'MIN_VENTANA_HORARIA_1' => $row['MIN_VENTANA_HORARIA_1'] ?? null,
                'MAX_VENTANA_HORARIA_1' => $row['MAX_VENTANA_HORARIA_1'] ?? null,
                'Ruta' => $row['Ruta'] ?? null,
                'Coord_Origen' => $row['Coord_Origen'] ?? null,
                'Coord_Destino' => $row['Coord_Destino'] ?? null,
                'Ruta_Fotos' => $row['Ruta_Fotos'] ?? null,
                'COMENTARIOS' => $row['COMENTARIOS'] ?? null,
                'estado_factura_caja' => $row['estado_factura_caja'] ?? null,
                'fecha_entrega_jefe' => $row['fecha_entrega_jefe'] ?? null,
                'usuario_entrega_jefe' => $row['usuario_entrega_jefe'] ?? null,
                'fecha_devolucion_caja' => $row['fecha_devolucion_caja'] ?? null,
                'usuario_devolucion_caja' => $row['usuario_devolucion_caja'] ?? null,
                'precio_factura_vendedor' => $row['precio_factura_vendedor'] ?? null,
                'precio_factura_real' => $row['precio_factura_real'] ?? null,
                'precio_validado_jc' => $row['precio_validado_jc'] ?? null,
                'fecha_validacion_precio' => $row['fecha_validacion_precio'] ?? null,
                'usuario_validacion_precio' => $row['usuario_validacion_precio'] ?? null,
                'tiene_destinatario_capturado' => $row['tiene_destinatario_capturado'] ?? null,
                'orden_entrega' => isset($row['orden_entrega']) ? (int)$row['orden_entrega'] : null,
                'fecha_asignacion' => $row['fecha_asignacion'] ?? null,
            ];
        }
        $st->close();

        $grupoOut = [
            'id' => (int)$grupo['id'],
            'nombre' => $grupo['nombre_grupo'] ?? null,
            'sucursal' => $grupo['sucursal'] ?? null,
            'chofer_asignado' => $grupo['chofer_asignado'] ?? null,
            'fecha_creacion' => $grupo['fecha_creacion'] ?? null,
            'usuario_creo' => $grupo['usuario_creo'] ?? null,
            'estado' => $grupo['estado'] ?? null,
            'notas' => $grupo['notas'] ?? null,
        ];

        ob_clean();
        echo json_encode([
            'ok' => true,
            'grupo' => $grupoOut,
            'page' => (int)$__page,
            'limit' => (int)$__limit,
            'total' => (int)$total,
            'pedidos' => $items,
        ]);
        exit;
    } catch (Throwable $e) {
        http_response_code(500);
        echo json_encode(['ok' => false, 'error' => 'ERROR']);
        exit;
    }
}

// ---------------------
// Consulta por FACTURA (legacy/nuevo detalle)
// Si viene ?factura=... y no se usó modo grupo, responde con detalle + bloque grupo si existe
// ---------------------
$__factura = isset($__get) ? $__get('factura') : (isset($_GET['factura']) ? trim($_GET['factura']) : null);
if ($__factura !== null && $__factura !== '') {
    try {
        $sqlF = 'SELECT p.* FROM pedidos AS p WHERE p.FACTURA = ? LIMIT 1';
        $stF = $conn->prepare($sqlF);
        $stF->bind_param('s', $__factura);
        $stF->execute();
        $resF = $stF->get_result();
        if (!$resF || $resF->num_rows === 0) {
            ob_clean();
            echo json_encode(['ok' => true, 'pedido' => null, 'grupo' => null]);
            exit;
        }
        $row = $resF->fetch_assoc();
        $stF->close();

        // Último enlace de grupo para este pedido
        $grupoOut = null; $ord = null; $fAsig = null;
        $sqlL = 'SELECT pg.orden_entrega, pg.fecha_asignacion, gr.id, gr.nombre_grupo, gr.sucursal, gr.chofer_asignado, gr.estado
                 FROM pedidos_grupos AS pg
                 INNER JOIN grupos_rutas AS gr ON gr.id = pg.grupo_id
                 WHERE pg.pedido_id = ?
                 ORDER BY pg.fecha_asignacion DESC, pg.id DESC
                 LIMIT 1';
        $stL = $conn->prepare($sqlL);
        $pedidoIdTmp = (int)($row['ID'] ?? 0);
        $stL->bind_param('i', $pedidoIdTmp);
        $stL->execute();
        $resL = $stL->get_result();
        if ($resL && $resL->num_rows > 0) {
            $lr = $resL->fetch_assoc();
            $grupoOut = [
                'id' => isset($lr['id']) ? (int)$lr['id'] : null,
                'nombre' => $lr['nombre_grupo'] ?? null,
                'sucursal' => $lr['sucursal'] ?? null,
                'chofer_asignado' => $lr['chofer_asignado'] ?? null,
                'estado' => $lr['estado'] ?? null,
                'orden_entrega' => isset($lr['orden_entrega']) ? (int)$lr['orden_entrega'] : null,
            ];
            $ord = $lr['orden_entrega'] ?? null;
            $fAsig = $lr['fecha_asignacion'] ?? null;
        }
        $stL->close();

        // Mapear campos del pedido
        $pedidoOut = [
            'ID' => $row['ID'] ?? null,
            'FACTURA' => $row['FACTURA'] ?? null,
            'SUCURSAL' => $row['SUCURSAL'] ?? null,
            'ESTADO' => $row['ESTADO'] ?? null,
            'CHOFER_ASIGNADO' => $row['CHOFER_ASIGNADO'] ?? null,
            'VENDEDOR' => $row['VENDEDOR'] ?? null,
            'NOMBRE_CLIENTE' => $row['NOMBRE_CLIENTE'] ?? null,
            'DIRECCION' => $row['DIRECCION'] ?? null,
            'TELEFONO' => $row['TELEFONO'] ?? null,
            'CONTACTO' => $row['CONTACTO'] ?? null,
            'FECHA_RECEPCION_FACTURA' => $row['FECHA_RECEPCION_FACTURA'] ?? null,
            'FECHA_ENTREGA_CLIENTE' => $row['FECHA_ENTREGA_CLIENTE'] ?? null,
            'FECHA_MIN_ENTREGA' => $row['FECHA_MIN_ENTREGA'] ?? null,
            'FECHA_MAX_ENTREGA' => $row['FECHA_MAX_ENTREGA'] ?? null,
            'MIN_VENTANA_HORARIA_1' => $row['MIN_VENTANA_HORARIA_1'] ?? null,
            'MAX_VENTANA_HORARIA_1' => $row['MAX_VENTANA_HORARIA_1'] ?? null,
            'Ruta' => $row['Ruta'] ?? null,
            'Coord_Origen' => $row['Coord_Origen'] ?? null,
            'Coord_Destino' => $row['Coord_Destino'] ?? null,
            'Ruta_Fotos' => $row['Ruta_Fotos'] ?? null,
            'COMENTARIOS' => $row['COMENTARIOS'] ?? null,
            'estado_factura_caja' => $row['estado_factura_caja'] ?? null,
            'fecha_entrega_jefe' => $row['fecha_entrega_jefe'] ?? null,
            'usuario_entrega_jefe' => $row['usuario_entrega_jefe'] ?? null,
            'fecha_devolucion_caja' => $row['fecha_devolucion_caja'] ?? null,
            'usuario_devolucion_caja' => $row['usuario_devolucion_caja'] ?? null,
            'precio_factura_vendedor' => $row['precio_factura_vendedor'] ?? null,
            'precio_factura_real' => $row['precio_factura_real'] ?? null,
            'precio_validado_jc' => $row['precio_validado_jc'] ?? null,
            'fecha_validacion_precio' => $row['fecha_validacion_precio'] ?? null,
            'usuario_validacion_precio' => $row['usuario_validacion_precio'] ?? null,
            'tiene_destinatario_capturado' => $row['tiene_destinatario_capturado'] ?? null,
            'orden_entrega' => isset($ord) ? (is_null($ord) ? null : (int)$ord) : null,
            'fecha_asignacion' => $fAsig,
        ];

        ob_clean();
        echo json_encode(['ok' => true, 'pedido' => $pedidoOut, 'grupo' => $grupoOut]);
        exit;
    } catch (Throwable $e) {
        http_response_code(500);
        echo json_encode(['ok' => false, 'error' => 'ERROR']);
        exit;
    }
}

// Verificar si se recibió el parámetro 'username'
if (!isset($_GET['username'])) {
    die("Error: Falta el parámetro 'username'");
}

// Obtener el nombre de usuario de los parámetros GET
$username = $_GET['username'];

// Validar el nombre de usuario
if (empty($username)) {
    die("Error: El nombre de usuario no puede estar vacío");
}

// Preparar la consulta con un placeholder para evitar inyección SQL
$sql = "SELECT 
            p.ID,
            p.SUCURSAL,
            p.ESTADO,
            p.FECHA_RECEPCION_FACTURA,
            p.FECHA_ENTREGA_CLIENTE,
            p.CHOFER_ASIGNADO,
            p.VENDEDOR,
            p.FACTURA,
            p.DIRECCION,
            p.FECHA_MIN_ENTREGA,
            p.FECHA_MAX_ENTREGA,
            p.MIN_VENTANA_HORARIA_1,
            p.MAX_VENTANA_HORARIA_1,
            p.NOMBRE_CLIENTE,
            p.TELEFONO,
            p.CONTACTO,
            p.COMENTARIOS,
            p.Ruta,
            p.Coord_Origen,
            p.Coord_Destino,
            lg.orden_entrega AS _g_orden_entrega,
            lg.fecha_asignacion AS _g_fecha_asignacion,
            gr.id AS _g_id,
            gr.nombre_grupo AS _g_nombre,
            gr.sucursal AS _g_sucursal,
            gr.chofer_asignado AS _g_chofer,
            gr.estado AS _g_estado
        FROM pedidos AS p
        JOIN choferes ON p.CHOFER_ASIGNADO = choferes.username
        LEFT JOIN (
            SELECT pg1.*
            FROM pedidos_grupos pg1
            LEFT JOIN pedidos_grupos pg2
              ON pg1.pedido_id = pg2.pedido_id
             AND (pg1.fecha_asignacion < pg2.fecha_asignacion
                  OR (pg1.fecha_asignacion = pg2.fecha_asignacion AND pg1.id < pg2.id))
            WHERE pg2.pedido_id IS NULL
        ) AS lg ON lg.pedido_id = p.ID
        LEFT JOIN grupos_rutas AS gr ON gr.id = lg.grupo_id
        WHERE
            choferes.username = ? 
        ORDER BY 
            CASE 
                WHEN p.ESTADO = 'Activo' THEN 1
                WHEN p.ESTADO = 'En Ruta' THEN 2
                WHEN p.ESTADO = 'En Tienda' THEN 3
                WHEN p.ESTADO = 'Reprogramado' THEN 4
                WHEN p.ESTADO = 'En Ruta' THEN 5
                WHEN p.ESTADO = 'Entregado' THEN 6
                WHEN p.ESTADO = 'Cancelado' THEN 7
                
                ELSE 8
            END,
            p.ESTADO"; // Placeholder

// Preparar la declaración
$stmt = $conn->prepare($sql);

// Verificar si la preparación de la consulta tuvo éxito
if (!$stmt) {
    die("Error al preparar la consulta: " . $conn->error);
}

// Vincular el parámetro con el valor del nombre de usuario
$stmt->bind_param("s", $username);

// Ejecutar la consulta
if (!$stmt->execute()) {
    die("Error al ejecutar la consulta: " . $stmt->error);
}

// Obtener el resultado
$result = $stmt->get_result();



$pedidos = array();

// Consultar si el chofer tiene vehículo asignado
$vehiculo = null;
$sqlVeh = "SELECT v.id_vehiculo, v.placa, v.numero_serie, v.tipo, v.Km_Actual, v.Sucursal
           FROM vehiculos v
           JOIN choferes c ON c.ID = v.id_chofer_asignado
           WHERE c.username = ?
           LIMIT 1";
$stVeh = $conn->prepare($sqlVeh);
if ($stVeh) {
    $stVeh->bind_param('s', $username);
    if ($stVeh->execute()) {
        $resVeh = $stVeh->get_result();
        if ($resVeh && $resVeh->num_rows > 0) {
            $vehiculo = $resVeh->fetch_assoc();
        }
    }
    $stVeh->close();
}

if ($result->num_rows > 0) {
    // Primero cachear filas para poder consultar grupos en lote
    $rows = [];
    $ids = [];
    while ($row = $result->fetch_assoc()) {
        $rows[] = $row;
        if (isset($row['ID'])) { $ids[] = (int)$row['ID']; }
    }

    // Mapa pedido_id -> info de grupo más reciente
    $grupoMap = [];
    if (count($ids) > 0) {
        // placeholders dinámicos
        $ph = implode(',', array_fill(0, count($ids), '?'));
        $types = str_repeat('i', count($ids));
        $sqlGrp = "SELECT pg.pedido_id, pg.orden_entrega, pg.fecha_asignacion,
                           gr.id AS grupo_id, gr.nombre_grupo, gr.sucursal, gr.chofer_asignado, gr.estado
                    FROM pedidos_grupos AS pg
                    INNER JOIN grupos_rutas AS gr ON gr.id = pg.grupo_id
                    LEFT JOIN pedidos_grupos AS newer
                        ON newer.pedido_id = pg.pedido_id
                       AND (pg.fecha_asignacion < newer.fecha_asignacion
                            OR (pg.fecha_asignacion = newer.fecha_asignacion AND pg.id < newer.id))
                    WHERE newer.pedido_id IS NULL
                      AND pg.pedido_id IN ($ph)";
        $stGrp = $conn->prepare($sqlGrp);
        if ($stGrp) {
            $stGrp->bind_param($types, ...$ids);
            if ($stGrp->execute()) {
                $resGrp = $stGrp->get_result();
                if ($resGrp) {
                    while ($gr = $resGrp->fetch_assoc()) {
                        $grupoMap[(int)$gr['pedido_id']] = [
                            'id' => isset($gr['grupo_id']) ? (int)$gr['grupo_id'] : null,
                            'nombre' => $gr['nombre_grupo'] ?? null,
                            'sucursal' => $gr['sucursal'] ?? null,
                            'chofer_asignado' => $gr['chofer_asignado'] ?? null,
                            'estado' => $gr['estado'] ?? null,
                            'orden_entrega' => isset($gr['orden_entrega']) ? (int)$gr['orden_entrega'] : null,
                        ];
                    }
                }
            }
            $stGrp->close();
        }
    }

    foreach ($rows as $row) {
        $pid = isset($row['ID']) ? (int)$row['ID'] : null;
        $grupoInfo = ($pid !== null && isset($grupoMap[$pid])) ? $grupoMap[$pid] : null;
        // Fallback: si la consulta principal ya trae alias del grupo, úsalos
        if ($grupoInfo === null && isset($row['_g_id'])) {
            if ($row['_g_id'] !== null) {
                $grupoInfo = [
                    'id' => (int)$row['_g_id'],
                    'nombre' => $row['_g_nombre'] ?? null,
                    'sucursal' => $row['_g_sucursal'] ?? null,
                    'chofer_asignado' => $row['_g_chofer'] ?? null,
                    'estado' => $row['_g_estado'] ?? null,
                    'orden_entrega' => isset($row['_g_orden_entrega']) ? (int)$row['_g_orden_entrega'] : null,
                ];
            }
        }
        $pedido = array(
            'ID' => $row['ID'],
            'SUCURSAL' => $row['SUCURSAL'],
            'ESTADO' => $row['ESTADO'],
            'FECHA_RECEPCION_FACTURA' => $row['FECHA_RECEPCION_FACTURA'],
            'FECHA_ENTREGA_CLIENTE' => $row['FECHA_ENTREGA_CLIENTE'],
            'CHOFER_ASIGNADO' => $row['CHOFER_ASIGNADO'],
            'VENDEDOR' => $row['VENDEDOR'],
            'FACTURA' => $row['FACTURA'],
            'DIRECCION' => $row['DIRECCION'],
            'FECHA_MIN_ENTREGA' => $row['FECHA_MIN_ENTREGA'],
            'FECHA_MAX_ENTREGA' => $row['FECHA_MAX_ENTREGA'],
            'MIN_VENTANA_HORARIA_1' => $row['MIN_VENTANA_HORARIA_1'],
            'MAX_VENTANA_HORARIA_1' => $row['MAX_VENTANA_HORARIA_1'],
            'NOMBRE_CLIENTE' => $row['NOMBRE_CLIENTE'],
            'TELEFONO' => $row['TELEFONO'],
            'CONTACTO' => $row['CONTACTO'],
            'COMENTARIOS' => $row['COMENTARIOS'],
            'Ruta' => $row['Ruta'],
            'Coord_Origen' => $row['Coord_Origen'],
            'Coord_Destino' => $row['Coord_Destino'],
            'VEHICULO_ASIGNADO' => $vehiculo !== null,
            'VEHICULO_DETALLE' => $vehiculo,
            'grupo' => $grupoInfo
        );
        array_push($pedidos, $pedido);
    }

} else {
   
     $pedidos = [];
}
ob_clean();
// Devolver los pedidos como JSON
header('Content-Type: application/json');

$v2 = isset($_GET['v2']) ? (int)$_GET['v2'] : 0;
    if ($v2 === 1) {
        $payload = [
            'ok' => true,
            'version' => 'v2-grupos-2025-11-14',
            'username' => $username,
            'vehiculo_asignado' => $vehiculo !== null,
            'vehiculo' => $vehiculo,
            'pedidos' => $pedidos,
        ];
    $json = json_encode($payload);
} else {
    // Compatibilidad: respuesta como arreglo de pedidos (cada pedido ya incluye info de vehículo si existe)
    $json = json_encode($pedidos);
}

if ($json === false) {
    echo json_encode(["error" => "Error al codificar JSON", "detalle" => json_last_error_msg()]);
} else {
    echo $json;
}


// Cerrar la conexión
$stmt->close();
$conn->close();
?>
