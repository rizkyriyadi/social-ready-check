package com.example.tripglide.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class AuditLog(
    @DocumentId
    val id: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actionType: String = "", // e.g., "UPDATED_NAME", "UPDATED_PHOTO"
    val details: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
