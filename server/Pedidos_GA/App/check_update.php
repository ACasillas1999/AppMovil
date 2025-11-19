<?php
header('Content-Type: application/json; charset=UTF-8');

// Opcional: si en el futuro quieres leer de BD,
// incluye la conexión:
// require_once __DIR__ . '/Conexiones/Conexion.php';

// Ajusta estos valores cada vez que publiques una nueva APK.
$latestVersionCode = 2;        // Debe ser mayor que BuildConfig.VERSION_CODE actual
$latestVersionName = '2.0';    // Texto que verá el usuario

// URL pública donde subes el APK de la nueva versión.
// EJEMPLO: https://pedidos.grupoascencio.com.mx/apk/app_pedidos_v1.apk
$apkUrl = 'http://192.168.60.194/Pedidos_GA/App/Apks/app_pedidos_v2.apk';

$response = [
    'ok' => true,
    'versionCode' => $latestVersionCode,
    'versionName' => $latestVersionName,
    'apkUrl' => $apkUrl,
    'changelog' => "Mejoras y correcciones generales."
];

echo json_encode($response);
