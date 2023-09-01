package com.solvek.gardenmonitor.bl

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

class FirestoreDataSource(context: Context) {
    val db: FirebaseFirestore

    init {
        FirebaseApp.initializeApp(context.applicationContext)
        db = Firebase.firestore
    }
    suspend fun upload(point: Point) = coroutineScope {
        val record = hashMapOf(
            "temperature_sensor" to point.sensorTemperature,
            "temperature_aw" to point.realTemperature,
            "time" to Timestamp(Date(point.time))
        )

        var ref: DocumentReference? = null
        var ex: Exception? = null

        val job = launch{
            db.collection("temperature")
                .add(record)
                .addOnSuccessListener {
                    ref = it
                }
                .addOnFailureListener{e->
                    ex = e
                    Timber.tag(TAG).e(e, "Failed to add data to firestore")
                }
        }

        job.join()

        if (ex != null) throw ex as Exception
        ref
    }

    companion object {
        private const val TAG = "Firestore"
    }
}