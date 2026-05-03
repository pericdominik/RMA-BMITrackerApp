package com.example.predlozak_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    UserPreview(

                        modifier = Modifier.padding(innerPadding)
                    )

            }
        }
    }
}


@Composable
fun UserPreview(modifier: Modifier = Modifier) {
    // Izračun BMI-a
    // Stanja za visinu i težinu -- početne vrijednosti, bit će zamijenjene iz Firestore-a var
    var heightCm by remember { mutableStateOf(0) }
    var weightKg by remember { mutableStateOf(0) }

    // Izračun BMI-a
    val heightMeters = heightCm / 100f
    val bmi = if (heightMeters > 0) weightKg / (heightMeters * heightMeters) else 0f

    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var rezultat by remember { mutableStateOf("") }

    // Stanja za novu težinu i napredak
    var newWeightInput by remember { mutableStateOf("") }
    var newHeightInput by remember { mutableStateOf("") }

    var progressResult by remember { mutableStateOf(0f) }
    var newBmiText by remember { mutableStateOf("") }
    var showProgress by remember { mutableStateOf(false) }
    var isProgressLoading by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf("") }

    // čitanje podataka iz Firestore-a pri prvom pokretanju
    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val document = db.collection("bmidata").document("Auto-ID").get().await()
            if (document.exists()) {
                heightCm = document.getLong("heightCm")?.toInt() ?: 0
                weightKg = document.getLong("weightKg")?.toInt() ?: 0
            }
        } catch (e: Exception) {
        // Greška pri čitanju -- ostaju početne vrijednosti
        }
    }

    // Određivanje poruke na temelju BMI-a
    val bmiStatus = when {
        bmi < 18.5 -> "Prenizak BMI"
        bmi in 18.5..24.9 -> "Idealan BMI"
        else -> "Previsok BMI"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = "Pozadinska slika",
            contentScale = ContentScale.Crop,
            alpha = 0.1f,
            modifier = Modifier.fillMaxSize()
        )

        Column {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                // Profilna slika
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

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Button(onClick = {
                    scope.launch {
                        isLoading = true
                        delay(10000)
                        //Thread.sleep(10000)
                        val idealBmi = 21.7
                        val razlika = (bmi - idealBmi).let { kotlin.math.abs(it) }
                        rezultat = "Udaljeni ste %.1f od idealnog BMI-a.".format(razlika)
                        isLoading = false
                    }
                }) {
                    Text("Izračunaj razliku od idealnog BMI")
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(text = rezultat)
                }

                Spacer(modifier = Modifier.height(16.dp))

                //TextField za visinu
                TextField(
                    value = newHeightInput,
                    onValueChange = { newHeightInput = it },
                    label = { Text("Nova visina (cm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = inputError.isNotEmpty()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // TextField za unos nove težine
                TextField(
                    value = newWeightInput,
                    onValueChange = { newWeightInput = it },
                    label = { Text("Nova težina (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = inputError.isNotEmpty()
                )

                if (inputError.isNotEmpty()) {
                    Text(
                        text = inputError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                //Gumb Spremi
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val newHeight = newHeightInput.toIntOrNull()
                        val newWeight = newWeightInput.toIntOrNull()

                        if (newHeight == null || newHeight <= 0 || newWeight == null || newWeight <= 0) {
                            inputError = "Unesite ispravnu visinu i težinu."
                        } else {
                            inputError = ""

                            scope.launch {
                                val newHeightMeters = newHeight / 100f
                                val newBmi = newWeight / (newHeightMeters * newHeightMeters)

                                val data = hashMapOf(
                                    "heightCm" to newHeight,
                                    "weightKg" to newWeight,
                                    "bmi" to newBmi
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("bmidata")
                                    .document("Auto-ID")
                                    .set(data)
                                    .await()

                                heightCm = newHeight
                                weightKg = newWeight
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Spremi u bazu")
                }

                // Gumb za izračun napretka
                Button(
                    onClick = {
                        val weight = newWeightInput.toFloatOrNull()
                        if (weight == null || weight <= 0) {
                            inputError = "Molimo unesite ispravnu numeričku vrijednost"
                            showProgress = false
                        } else {
                            inputError = ""
                            scope.launch {
                                isProgressLoading = true
                                delay(1000)


                                val newBmi = weight / (heightMeters * heightMeters)
                                val idealBmi = 21.7f

                                // Izračun napretka prema idealnom BMI-ju
                                val progress = if (newBmi > bmi) {
                                    0f
                                } else if (newBmi <= idealBmi) {
                                    1f
                                } else {
                                    // Napredak se računa u odnosu na udaljenost od početnog BMI-ja do idealnog
                                    val distanceFromStart = bmi - newBmi
                                    val totalDistance = bmi - idealBmi
                                    if (totalDistance > 0) {
                                        distanceFromStart / totalDistance
                                    } else {
                                        0f
                                    }
                                }

                                progressResult = progress.coerceIn(0f, 1f)
                                newBmiText = "Novi BMI: %.1f – Napredak: %.0f%%".format(newBmi, progress * 100)
                                showProgress = true
                                isProgressLoading = false
                            }
                        }
                    },
                    enabled = newWeightInput.isNotEmpty() && !isProgressLoading
                ) {
                    if (isProgressLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Izračunaj napredak")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LinearProgressIndicator i tekstualni prikaz
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
                            .height(8.dp),
                    )
                }
            }

        }

    }
}