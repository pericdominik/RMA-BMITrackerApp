package com.example.predlozak_1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

private const val BMI_DOCUMENT_ID = "Auto-ID"

class MainViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Podaci dohvaćeni iz Firestore baze
    private val _heightCm = MutableStateFlow(0)
    val heightCm: StateFlow<Int> = _heightCm

    private val _weightKg = MutableStateFlow(0)
    val weightKg: StateFlow<Int> = _weightKg

    private val _bmi = MutableStateFlow(0f)
    val bmi: StateFlow<Float> = _bmi

    private val _bmiStatus = MutableStateFlow("Učitavanje BMI podataka...")
    val bmiStatus: StateFlow<String> = _bmiStatus

    // Izračun razlike od idealnog BMI-a
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _rezultat = MutableStateFlow("")
    val rezultat: StateFlow<String> = _rezultat

    // Poruke i greške za unos/spremanje
    private val _inputError = MutableStateFlow("")
    val inputError: StateFlow<String> = _inputError

    private val _saveMessage = MutableStateFlow("")
    val saveMessage: StateFlow<String> = _saveMessage

    // Izračun napretka
    private val _progressResult = MutableStateFlow(0f)
    val progressResult: StateFlow<Float> = _progressResult

    private val _newBmiText = MutableStateFlow("")
    val newBmiText: StateFlow<String> = _newBmiText

    private val _showProgress = MutableStateFlow(false)
    val showProgress: StateFlow<Boolean> = _showProgress

    private val _isProgressLoading = MutableStateFlow(false)
    val isProgressLoading: StateFlow<Boolean> = _isProgressLoading

    init {
        ucitajBmiPodatke()
    }

    // Dohvat početne visine i težine iz Firebase baze
    private fun ucitajBmiPodatke() {
        viewModelScope.launch {
            try {
                val document = db
                    .collection("bmidata")
                    .document(BMI_DOCUMENT_ID)
                    .get()
                    .await()

                if (document.exists()) {
                    val visina = document.getLong("heightCm")?.toInt() ?: 0
                    val tezina = document.getLong("weightKg")?.toInt() ?: 0

                    _heightCm.value = visina
                    _weightKg.value = tezina

                    azurirajBmi(visina, tezina)
                } else {
                    _saveMessage.value = "Dokument s BMI podacima nije pronađen."
                }
            } catch (e: Exception) {
                _saveMessage.value = "Greška pri dohvaćanju podataka iz baze."
            }
        }
    }

    // Izračun trenutnog BMI-a i statusa
    private fun azurirajBmi(visinaCm: Int, tezinaKg: Int) {
        val visinaM = visinaCm / 100f

        val izracunatiBmi = if (visinaM > 0) {
            tezinaKg / (visinaM * visinaM)
        } else {
            0f
        }

        _bmi.value = izracunatiBmi

        _bmiStatus.value = when {
            izracunatiBmi == 0f -> "Učitavanje BMI podataka..."
            izracunatiBmi < 18.5f -> "Prenizak BMI"
            izracunatiBmi in 18.5f..24.9f -> "Idealan BMI"
            else -> "Previsok BMI"
        }
    }

    // Izračun razlike od idealnog BMI-a
    fun izracunajRazlikuOdIdealnogBmi() {
        viewModelScope.launch {
            _isLoading.value = true

            delay(1000)

            val idealBmi = 21.7f
            val razlika = abs(_bmi.value - idealBmi)

            _rezultat.value =
                "Udaljeni ste %.1f od idealnog BMI-a.".format(razlika)

            _isLoading.value = false
        }
    }

    // Spremanje nove visine i težine u Firebase
    fun spremiUBazu(newHeightInput: String, newWeightInput: String) {
        val newHeight = newHeightInput.toIntOrNull()
        val newWeight = newWeightInput.toIntOrNull()

        if (
            newHeight == null ||
            newHeight <= 0 ||
            newWeight == null ||
            newWeight <= 0
        ) {
            _inputError.value = "Unesite ispravnu visinu i težinu."
            _saveMessage.value = ""
            return
        }

        _inputError.value = ""
        _saveMessage.value = ""

        viewModelScope.launch {
            try {
                val newHeightMeters = newHeight / 100f
                val newBmi = newWeight / (newHeightMeters * newHeightMeters)

                val data = hashMapOf(
                    "heightCm" to newHeight,
                    "weightKg" to newWeight,
                    "bmi" to newBmi
                )

                db.collection("bmidata")
                    .document(BMI_DOCUMENT_ID)
                    .set(data)
                    .await()

                _heightCm.value = newHeight
                _weightKg.value = newWeight

                azurirajBmi(newHeight, newWeight)

                _saveMessage.value = "Podaci su spremljeni u Firebase bazu."
            } catch (e: Exception) {
                _saveMessage.value = "Greška pri spremanju u bazu."
            }
        }
    }

    // Izračun napretka prema idealnom BMI-u
    fun izracunajNapredak(newWeightInput: String) {
        val weight = newWeightInput.toFloatOrNull()

        if (weight == null || weight <= 0f) {
            _inputError.value =
                "Molimo unesite ispravnu numeričku vrijednost težine."
            _showProgress.value = false
            return
        }

        val visinaM = _heightCm.value / 100f

        if (visinaM <= 0f) {
            _inputError.value = "Visina nije pravilno učitana."
            _showProgress.value = false
            return
        }

        _inputError.value = ""

        viewModelScope.launch {
            _isProgressLoading.value = true

            delay(1000)

            val noviBmi = weight / (visinaM * visinaM)
            val idealBmi = 21.7f
            val trenutniBmi = _bmi.value

            val progress = if (noviBmi > trenutniBmi) {
                0f
            } else if (noviBmi <= idealBmi) {
                1f
            } else {
                val distanceFromStart = trenutniBmi - noviBmi
                val totalDistance = trenutniBmi - idealBmi

                if (totalDistance > 0) {
                    distanceFromStart / totalDistance
                } else {
                    0f
                }
            }

            _progressResult.value = progress.coerceIn(0f, 1f)

            _newBmiText.value =
                "Novi BMI: %.1f – Napredak: %.0f%%".format(
                    noviBmi,
                    _progressResult.value * 100
                )

            _showProgress.value = true
            _isProgressLoading.value = false
        }
    }
}