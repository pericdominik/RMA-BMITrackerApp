package com.example.predlozak_1.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.predlozak_1.R
import com.example.predlozak_1.viewmodel.MainViewModel

@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Podaci iz ViewModela
    val bmiStatus by viewModel.bmiStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val rezultat by viewModel.rezultat.collectAsState()

    val inputError by viewModel.inputError.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()

    val progressResult by viewModel.progressResult.collectAsState()
    val newBmiText by viewModel.newBmiText.collectAsState()
    val showProgress by viewModel.showProgress.collectAsState()
    val isProgressLoading by viewModel.isProgressLoading.collectAsState()

    // Lokalna UI stanja za unos korisnika
    var newWeightInput by remember { mutableStateOf("") }
    var newHeightInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        UserPreview(
            bmiStatus = bmiStatus,
            isLoading = isLoading,
            rezultat = rezultat,
            newHeightInput = newHeightInput,
            onNewHeightChange = { newHeightInput = it },
            newWeightInput = newWeightInput,
            onNewWeightChange = { newWeightInput = it },
            inputError = inputError,
            saveMessage = saveMessage,
            progressResult = progressResult,
            newBmiText = newBmiText,
            showProgress = showProgress,
            isProgressLoading = isProgressLoading,
            onCalculateBmiDifference = {
                viewModel.izracunajRazlikuOdIdealnogBmi()
            },
            onSaveToDatabase = {
                viewModel.spremiUBazu(
                    newHeightInput = newHeightInput,
                    newWeightInput = newWeightInput
                )
            },
            onCalculateProgress = {
                viewModel.izracunajNapredak(newWeightInput)
            },
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
    bmiStatus: String,
    isLoading: Boolean,
    rezultat: String,
    newHeightInput: String,
    onNewHeightChange: (String) -> Unit,
    newWeightInput: String,
    onNewWeightChange: (String) -> Unit,
    inputError: String,
    saveMessage: String,
    progressResult: Float,
    newBmiText: String,
    showProgress: Boolean,
    isProgressLoading: Boolean,
    onCalculateBmiDifference: () -> Unit,
    onSaveToDatabase: () -> Unit,
    onCalculateProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                onClick = onCalculateBmiDifference
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
                onValueChange = onNewHeightChange,
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
                onValueChange = onNewWeightChange,
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
                onClick = onSaveToDatabase,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Spremi u bazu")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCalculateProgress,
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