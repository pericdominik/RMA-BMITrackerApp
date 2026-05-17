package com.example.predlozak_1.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StepCounterViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Broj trenutno prepoznatih koraka
    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount

    // Tekst koji se prikazuje korisniku na ekranu
    private val _statusText = MutableStateFlow(
        "Pomaknite uređaj kako bi se detektirali koraci."
    )
    val statusText: StateFlow<String> = _statusText

    // Zadnji spremljeni cilj: 0, 50, 100, 150...
    private var lastSavedStepGoal = 0

    // Vrijeme zadnjeg prepoznatog koraka
    // Koristi se da ne brojimo previše koraka u jako kratkom vremenu
    private var lastStepTime = 0L

    // Signal ekranu da treba prikazati notifikaciju
    private val _notificationStepGoal = MutableStateFlow<Int?>(null)
    val notificationStepGoal: StateFlow<Int?> = _notificationStepGoal

    /*
        Ova funkcija se poziva iz Viewa kada senzor procijeni
        da je pokret dovoljno jak da bi mogao biti korak.
    */
    fun obradiMoguciKorak(currentTime: Long) {
        if (currentTime - lastStepTime > 400) {
            lastStepTime = currentTime
            _stepCount.value++

            val currentSteps = _stepCount.value
            val nextGoal = (currentSteps / 50) * 50

            if (nextGoal >= 50 && nextGoal > lastSavedStepGoal) {
                lastSavedStepGoal = nextGoal

                spremiKorakeUFirebase(nextGoal)

                // View će pročitati ovu vrijednost i prikazati notifikaciju
                _notificationStepGoal.value = nextGoal
            }
        }
    }

    /*
        Ako uređaj nema akcelerometar, View poziva ovu funkciju
        kako bi se na ekranu prikazala poruka.
    */
    fun postaviPorukuDaSenzorNijeDostupan() {
        _statusText.value = "Akcelerometar nije dostupan na ovom uređaju."
    }

    /*
        Nakon što View prikaže notifikaciju, resetiramo signal.
        Time sprječavamo da se ista notifikacija prikazuje više puta.
    */
    fun notifikacijaPrikazana() {
        _notificationStepGoal.value = null
    }

    /*
        Spremanje u Firebase ostaje u ViewModelu,
        jer to nije posao korisničkog sučelja.
    */
    private fun spremiKorakeUFirebase(currentSteps: Int) {
        val podatak = hashMapOf(
            "koraci" to currentSteps,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("Koraci")
            .add(podatak)
            .addOnSuccessListener {
                _statusText.value =
                    "Spremljeno u Firebase: $currentSteps koraka."
            }
            .addOnFailureListener {
                _statusText.value =
                    "Greška pri spremanju koraka u Firebase."
            }
    }
}