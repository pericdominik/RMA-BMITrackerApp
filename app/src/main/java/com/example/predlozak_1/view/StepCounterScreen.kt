package com.example.predlozak_1.view

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
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.predlozak_1.R
import com.example.predlozak_1.viewmodel.StepCounterViewModel
import kotlin.math.sqrt

@Composable
fun StepCounterScreen(
    navController: NavController,
    viewModel: StepCounterViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val stepCount by viewModel.stepCount.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val notificationStepGoal by viewModel.notificationStepGoal.collectAsState()

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(currentSteps, notification)
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

                    if (magnitude > 12f) {
                        viewModel.obradiMoguciKorak(currentTime)
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
            viewModel.postaviPorukuDaSenzorNijeDostupan()
            Log.d("step_counter", "Akcelerometar nije dostupan.")
        }

        onDispose {
            sensorManager.unregisterListener(accelerometerListener)
            Log.d("step_counter", "Akcelerometar je odjavljen.")
        }
    }

    LaunchedEffect(notificationStepGoal) {
        notificationStepGoal?.let { goal ->
            showStepNotification(goal)
            viewModel.notifikacijaPrikazana()
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