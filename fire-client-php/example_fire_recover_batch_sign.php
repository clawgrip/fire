<html>
 <head>
  <title>Recuperar firma del lote de firmas</title>
 </head>
 <body>
 <?php 
	// Cargamos el componente distribuido de FIRe
	include 'fire_client.php';
	
	
	//$appId = "7BA5453995EC";	// Identificador de la aplicacion (dada de alta previamente en el sistema) - PREPRODUCCION
	$appId = "B244E473466F";	// Identificador de la aplicacion (dada de alta previamente en el sistema) - LOCAL
	$subjectId = "00001";		// DNI de la persona
	$transactionId = "9727febd-ec92-4503-8b5d-d6f4cbc086af";	// Identificador de la transaccion
	$dataB64 = base64_encode("Hola Mundo!!");
	
	
	$fireClient = new FireClient($appId); // Identificador de la aplicacion (dada de alta previamente en el sistema)
	$transactionResult;
	
	// Resultado de la primera firma
	$docId = "0001";
	try {
		$transactionResult = $fireClient->recoverBatchSign(
			$subjectId,			// DNI de la persona
			$transactionId,		// Identificador de transaccion recuperado en la operacion createBatch()
			$docId				// Identificador del documento
		);
	}
	catch(Exception $e) {
		echo 'Error: ',  $e->getMessage(), "\n";
		return;
	}

	// Mostramos los datos obtenidos
	echo "<br><b>Firma del documento ".$docId.":</b><br>".(base64_encode($transactionResult->result));
	
	// Resultado de la segunda firma
	$docId = "0002";
	try {
		$transactionResult = $fireClient->recoverBatchSign(
			$subjectId,			// DNI de la persona
			$transactionId,		// Identificador de transaccion recuperado en la operacion createBatch()
			$docId				// Identificador del documento
		);
	}
	catch(Exception $e) {
		echo 'Error: ',  $e->getMessage(), "\n";
		return;
	}

	// Mostramos los datos obtenidos
	echo "<br><b>Firma del documento ".$docId.":</b><br>".(base64_encode($transactionResult->result));

 ?>
 </body>
</html>