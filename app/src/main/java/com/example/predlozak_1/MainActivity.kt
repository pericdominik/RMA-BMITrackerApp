package com.example.predlozak_1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import kotlin.math.sqrt


// Ovdje upiši točan ID dokumenta iz Firestore kolekcije "bmidata"
private const val BMI_DOCUMENT_ID = "Auto-ID"


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermission()

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            Scaffold(
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->

                NavHost(
                    navController = navController,
                    startDestination = "main_screen",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("main_screen") {
                        MainScreen(navController = navController)
                    }

                    composable("step_counter") {
                        StepCounter(navController = navController)
                    }
                }
            }
        }
    }

    private fun requestPermission() {
        val permissionsToRequest = mutableListOf<String>()

        // Dozvola za praćenje aktivnosti / senzore
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // Dozvola za notifikacije na Androidu 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                1
            )
        }
    }
}


@Composable
fun MainScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        UserPreview(
            modifier = Modifier.fillMaxSize()
        )

        Button(
            onClick = {
                navController.navigate("step_counter")
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Idi na brojač koraka")
        }
    }
}


@Composable
fun UserPreview(
    modifier: Modifier = Modifier
) {
    // Stanja za podatke iz Firestore baze
    var heightCm by remember { mutableStateOf(0) }
    var weightKg by remember { mutableStateOf(0) }

    // Izračun trenutnog BMI-a
    val heightMeters = heightCm / 100f
    val bmi = if (heightMeters > 0) {
        weightKg / (heightMeters * heightMeters)
    } else {
        0f
    }

    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var rezultat by remember { mutableStateOf("") }

    // Unosi za novu visinu i težinu
    var newWeightInput by remember { mutableStateOf("") }
    var newHeightInput by remember { mutableStateOf("") }

    // Stanja za prikaz napretka
    var progressResult by remember { mutableStateOf(0f) }
    var newBmiText by remember { mutableStateOf("") }
    var showProgress by remember { mutableStateOf(false) }
    var isProgressLoading by remember { mutableStateOf(false) }

    // Greške i poruke
    var inputError by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf("") }

    // Čitanje visine i težine iz Firestore-a pri prvom prikazu ekrana
    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()

            val document = db
                .collection("bmidata")
                .document(BMI_DOCUMENT_ID)
                .get()
                .await()

            if (document.exists()) {
                heightCm = document.getLong("heightCm")?.toInt() ?: 0
                weightKg = document.getLong("weightKg")?.toInt() ?: 0
            }
        } catch (e: Exception) {
            saveMessage = "Greška pri dohvaćanju podataka iz baze."
        }
    }

    // Određivanje BMI statusa
    val bmiStatus = when {
        bmi == 0f -> "Učitavanje BMI podataka..."
        bmi < 18.5 -> "Prenizak BMI"
        bmi in 18.5..24.9 -> "Idealan BMI"
        else -> "Previsok BMI"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = "Pozadinska slika",
            contentScale = ContentScale.Crop,
            alpha = 0.1f,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_pic),
                    contentDescription = "Profilna slika",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Pozdrav, Dominik",
                        fontSize = 18.sp
                    )

                    Text(
                        text = bmiStatus,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true

                        delay(1000)

                        val idealBmi = 21.7
                        val razlika = kotlin.math.abs(bmi - idealBmi)

                        rezultat =
                            "Udaljeni ste %.1f od idealnog BMI-a.".format(razlika)

                        isLoading = false
                    }
                }
            ) {
                Text("Izračunaj razliku od idealnog BMI")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(text = rezultat)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = newHeightInput,
                onValueChange = {
                    newHeightInput = it
                },
                label = {
                    Text("Nova visina (cm)")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = inputError.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = newWeightInput,
                onValueChange = {
                    newWeightInput = it
                },
                label = {
                    Text("Nova težina (kg)")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = inputError.isNotEmpty()
            )

            if (inputError.isNotEmpty()) {
                Text(
                    text = inputError,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            if (saveMessage.isNotEmpty()) {
                Text(
                    text = saveMessage,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val newHeight = newHeightInput.toIntOrNull()
                    val newWeight = newWeightInput.toIntOrNull()

                    if (
                        newHeight == null ||
                        newHeight <= 0 ||
                        newWeight == null ||
                        newWeight <= 0
                    ) {
                        inputError = "Unesite ispravnu visinu i težinu."
                        saveMessage = ""
                    } else {
                        inputError = ""
                        saveMessage = ""

                        scope.launch {
                            try {
                                val newHeightMeters = newHeight / 100f
                                val newBmi =
                                    newWeight / (newHeightMeters * newHeightMeters)

                                val data = hashMapOf(
                                    "heightCm" to newHeight,
                                    "weightKg" to newWeight,
                                    "bmi" to newBmi
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("bmidata")
                                    .document(BMI_DOCUMENT_ID)
                                    .set(data)
                                    .await()

                                heightCm = newHeight
                                weightKg = newWeight

                                saveMessage = "Podaci su spremljeni u Firebase bazu."
                            } catch (e: Exception) {
                                saveMessage = "Greška pri spremanju u bazu."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Spremi u bazu")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val weight = newWeightInput.toFloatOrNull()

                    if (weight == null || weight <= 0f) {
                        inputError =
                            "Molimo unesite ispravnu numeričku vrijednost težine."
                        showProgress = false
                    } else {
                        inputError = ""

                        scope.launch {
                            isProgressLoading = true

                            delay(1000)

                            val newBmi = weight / (heightMeters * heightMeters)
                            val idealBmi = 21.7f

                            val progress = if (newBmi > bmi) {
                                0f
                            } else if (newBmi <= idealBmi) {
                                1f
                            } else {
                                val distanceFromStart = bmi - newBmi
                                val totalDistance = bmi - idealBmi

                                if (totalDistance > 0) {
                                    distanceFromStart / totalDistance
                                } else {
                                    0f
                                }
                            }

                            progressResult = progress.coerceIn(0f, 1f)

                            newBmiText =
                                "Novi BMI: %.1f – Napredak: %.0f%%".format(
                                    newBmi,
                                    progressResult * 100
                                )

                            showProgress = true
                            isProgressLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = newWeightInput.isNotEmpty() && !isProgressLoading
            ) {
                if (isProgressLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Izračunaj napredak")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showProgress) {
                Text(
                    text = newBmiText,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressResult },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
        }
    }
}


@Composable
fun StepCounter(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val db = FirebaseFirestore.getInstance()

    var stepCount by remember { mutableStateOf(0) }
    var lastSavedStepGoal by remember { mutableStateOf(0) }
    var lastStepTime by remember { mutableStateOf(0L) }
    var statusText by remember {
        mutableStateOf("Pomaknite uređaj kako bi se detektirali koraci.")
    }

    val channelId = "steps_channel"

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Steps Notifications"

            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showStepNotification(currentSteps: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Bravo!")
            .setContentText("Napravili ste više od $currentSteps koraka!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(currentSteps, notification)
    }

    fun saveStepsToFirestore(currentSteps: Int) {
        val podatak = hashMapOf(
            "koraci" to currentSteps,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("Koraci")
            .add(podatak)
            .addOnSuccessListener {
                statusText = "Spremljeno u Firebase: $currentSteps koraka."
            }
            .addOnFailureListener {
                statusText = "Greška pri spremanju koraka u Firebase."
            }
    }

    val accelerometerListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val magnitude = sqrt(x * x + y * y + z * z)
                    val currentTime = System.currentTimeMillis()

                    if (magnitude > 12f && currentTime - lastStepTime > 400) {
                        lastStepTime = currentTime
                        stepCount++

                        val nextGoal = (stepCount / 50) * 50

                        if (nextGoal >= 50 && nextGoal > lastSavedStepGoal) {
                            lastSavedStepGoal = nextGoal

                            showStepNotification(nextGoal)
                            saveStepsToFirestore(nextGoal)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Ovdje nije potrebna dodatna logika.
            }
        }
    }

    DisposableEffect(Unit) {
        createNotificationChannel()

        val accelerometer =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            sensorManager.registerListener(
                accelerometerListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )

            Log.d("step_counter", "Akcelerometar je registriran.")
        } else {
            statusText = "Akcelerometar nije dostupan na ovom uređaju."
            Log.d("step_counter", "Akcelerometar nije dostupan.")
        }

        onDispose {
            sensorManager.unregisterListener(accelerometerListener)
            Log.d("step_counter", "Akcelerometar je odjavljen.")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.fitness1),
            contentDescription = "Pozadinska slika",
            contentScale = ContentScale.Crop,
            alpha = 0.1f,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Broj koraka: $stepCount",
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = statusText,
                fontSize = 16.sp,
                color = Color.Gray
            )
        }

        Button(
            onClick = {
                navController.navigate("main_screen")
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Idi na glavni ekran")
        }
    }
}