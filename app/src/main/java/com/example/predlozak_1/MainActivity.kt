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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.example.predlozak_1.ui.theme.Predlozak_1Theme
import kotlinx.coroutines.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Predlozak_1Theme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    UserPreview(191,100,

                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Composable
fun UserPreview(heightCm: Int, weightKg: Int, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    var isLoading1 by remember { mutableStateOf(false) }
    var isLoading2 by remember { mutableStateOf(false) }
    var rezultat by remember { mutableStateOf("") }

    var novaTezina by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf<Float?>(null) }
    var progressText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    // Izračun BMI-a
    val heightMeters = heightCm / 100f
    val bmi = weightKg / (heightMeters * heightMeters)

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
            painter = painterResource(id = R.drawable.fitness1),
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
                Button(
                    onClick = {
                        scope.launch {
                            isLoading1 = true

                            delay(1000)

                            val idealBmi = 21.7
                            val razlika = kotlin.math.abs(bmi - idealBmi)
                            rezultat = "Udaljeni ste %.1f od idealnog BMI-a.".format(razlika)

                            isLoading1 = false
                        }
                    }
                ) {
                    Text("Izračunaj razliku od idealnog BMI")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading1) {
                    CircularProgressIndicator()
                } else {
                    Text(text = rezultat)
                }

                //if (rezultat.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = novaTezina,
                        onValueChange = { novaTezina = it },
                        label = { Text("Unesite novu težinu (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading2 = true
                                errorText = ""
                                progress = null
                                progressText = ""

                                val unesenaTezina = novaTezina.toFloatOrNull()

                                if (unesenaTezina == null) {
                                    errorText = "Unesite ispravnu brojčanu vrijednost."
                                    isLoading2 = false
                                    return@launch
                                }

                                delay(1000)

                                val noviBmi = unesenaTezina / (heightMeters * heightMeters)
                                val idealBmi = 21.7f

                                val calculatedProgress = when {
                                    noviBmi > bmi -> 0f
                                    noviBmi <= idealBmi -> 1f
                                    else -> {
                                        val ukupnaUdaljenost = bmi - idealBmi
                                        val novaUdaljenost = noviBmi - idealBmi
                                        ((ukupnaUdaljenost - novaUdaljenost) / ukupnaUdaljenost)
                                            .coerceIn(0f, 1f)
                                    }
                                }

                                progress = calculatedProgress
                                progressText = "Novi BMI: %.1f - Napredak: %d%%".format(
                                    noviBmi,
                                    (calculatedProgress * 100).toInt()
                                )

                                isLoading2 = false
                            }
                        }
                    ) {
                        Text("Izračunaj napredak")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading2) {
                        CircularProgressIndicator()
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = Color.Red
                        )
                    }

                    if (progress != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = progressText)

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress ?: 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                //}
            }
        }
    }
}

