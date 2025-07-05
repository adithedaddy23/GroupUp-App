package com.example.studygroupfinder.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FirestoreRepository (
) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    //Collection Names
    private val EVENTS_COLLECTION = "events"
    private val USERS_COLLECTION = "users"
    private val EVENT_REQUEST_COLLECTION = "event_requests"
    private val CHATS_COLLECTION = "chats"
    private val MESSAGES_COLLECTION = "messages"

    //Event Data Class
    data class Event (
        val id: String = "",
        val title: String = "",
        val description: String = "",
        val date: Date = Date(),
        val location: GeoPoint = GeoPoint(0.0, 0.0),
        val locationName: String = "",
        val hostId: String = "",
        val hostName: String = "",
        val hostProfilePic: String? = null,
        val createdAt: Date = Date(),
        val maxParticipants: Int = 0,
        val currentParticipants: Int = 0,
        val tags: List<String> = emptyList(),
        val participants: List<String> = emptyList(),
        val pendingParticipants: List<String> = emptyList()
    )

    //User Data Class
    data class User(
        val id: String = "",
        val name: String = "",
        val email: String = "",
        val profilePic: String? = null,
        val joinedEvents: List<String> = emptyList(),
        val createdEvents: List<String> = emptyList(),
        val hostedEvents: List<String> = emptyList() // Add this field
    )


    // Event Request data class
    data class EventRequest(
        val id: String = "",
        val eventId: String = "",
        val userId: String = "",
        val status: String = "pending", // "pending", "approved", "rejected"
        val timestamp: Date = Date()
    )

    data class Participant(
        val userId: String,
        val name: String,
        val profilePic: String? = null,
        val joinedAt: Date = Date()
    )

    data class Chat(
        val id: String = "",
        val eventId: String = "",
        val participants: List<String> = emptyList(),
        val adminId: String = "", // event host
        val lastMessage: Message? = null,
        val timestamp: Date = Date(),
        val readReceipts: Map<String, Date> = emptyMap() // userId to last read timestamp
    ) {
        // Add a no-arg constructor for Firestore
        constructor() : this("", "", emptyList(), "", null, Date(), emptyMap())
    }

    data class Message(
        val id: String = "",
        val chatId: String = "",
        val senderId: String = "",
        val text: String = "",
        val timestamp: Date = Date(),
        val imageUrl: String? = null,
        val status: String = "sent" // "sent", "delivered", "read"
    ) {
        // Add a no-arg constructor for Firestore
        constructor() : this("", "", "", "", Date(), null, "sent")
    }

    data class ChatParticipant(
        val userId: String,
        val name: String,
        val profilePic: String? = null,
        val lastSeen: Date? = null
    )

    // Data class for chat with event details
    data class ChatWithEventDetails(
        val chat: Chat,
        val event: Event
    )

    //Add New Event
    suspend fun addEvent(
        title: String,
        description: String,
        date: Date,
        time: String,
        location: GeoPoint,
        locationName: String,
        maxParticipants: Int,
        tags: List<String>
    ): String {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        val event = Event(
            title = title,
            description = description,
            date = date,
            location = location,
            locationName = locationName,
            hostId = currentUser.uid,
            hostName = currentUser.displayName ?: "",
            hostProfilePic = currentUser.photoUrl?.toString(),
            maxParticipants = maxParticipants,
            tags = tags
        )

        val documentRef = db.collection(EVENTS_COLLECTION).document()
        event.copy(id = documentRef.id).let {
            documentRef.set(it).await()
        }

        // Add to User's hosted Events
        db.collection(USERS_COLLECTION).document(currentUser.uid)
            .update("hostedEvents", FieldValue.arrayUnion(documentRef.id))
            .await()

        // Create chat for the event automatically
        try {
            createChat(documentRef.id, currentUser.uid)
            Log.d("FirestoreRepository", "Chat created successfully for event: ${documentRef.id}")
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "Failed to create chat for event ${documentRef.id}: ${e.message}")
            // Don't fail the entire event creation if chat creation fails
        }

        return documentRef.id
    }

    suspend fun getNearbyEvents(center: org.osmdroid.util.GeoPoint, radiusInKm: Double): List<Event> {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        val currentUserId = currentUser.uid

        // Fetch upcoming events
        val allEvents = db.collection(EVENTS_COLLECTION)
            .whereGreaterThanOrEqualTo("date", Date())
            .orderBy("date")
            .get()
            .await()
            .toObjects(Event::class.java)

        // Filter by distance and not joined
        return allEvents.filter { event ->
            val isNearby = calculateDistanceInKm(
                lat1 = center.latitude,
                lon1 = center.longitude,
                lat2 = event.location.latitude,
                lon2 = event.location.longitude
            ) <= radiusInKm

            val hasNotJoined = !event.participants.contains(currentUserId)

            isNearby && hasNotJoined
        }
    }


    private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusinKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusinKm * c
    }

    // Get user's hosted events
    suspend fun getUserHostedEvents(userId: String) : List<Event> {
        return db.collection(EVENTS_COLLECTION)
            .whereEqualTo("hostId", userId)
            .orderBy("date")
            .get()
            .await()
            .toObjects(Event::class.java)
    }

    // Get user's joined events
    suspend fun getUserJoinedEvents(userId: String): List<Event> {
        val user = db.collection(USERS_COLLECTION).document(userId).get().await()
        val joinedEventIds = user.get("joinedEvents") as? List<String> ?: emptyList()

        if (joinedEventIds.isEmpty()) return emptyList()

        return db.collection(EVENTS_COLLECTION)
            .whereIn(FieldPath.documentId(), joinedEventIds)
            .get()
            .await()
            .toObjects(Event::class.java)
    }

    // Simplified join event function
    suspend fun joinEvent(eventId: String, userId: String) {
        // First check if event exists and has space
        val eventRef = db.collection(EVENTS_COLLECTION).document(eventId)
        val event = eventRef.get().await().toObject(Event::class.java)
            ?: throw Exception("Event not found")

        if (event.currentParticipants >= event.maxParticipants) {
            throw Exception("Event is full")
        }

        // Add to event's participants
        db.runTransaction { transaction ->
            val currentEvent = transaction.get(eventRef).toObject(Event::class.java)
            if (currentEvent?.participants?.contains(userId) == true) {
                throw Exception("User already joined")
            }

            transaction.update(eventRef,
                "participants", FieldValue.arrayUnion(userId),
                "currentParticipants", FieldValue.increment(1)
            )
        }.await()

        // Add to user's joined events
        db.collection(USERS_COLLECTION).document(userId)
            .update("joinedEvents", FieldValue.arrayUnion(eventId))
            .await()

        // Add user to the event's chat
        try {
            val chat = db.collection(CHATS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .await()
                .toObjects(Chat::class.java)
                .firstOrNull()

            chat?.let {
                db.collection(CHATS_COLLECTION).document(it.id)
                    .update("participants", FieldValue.arrayUnion(userId))
                    .await()
                Log.d("FirestoreRepository", "User $userId added to chat ${it.id}")
            } ?: run {
                Log.w("FirestoreRepository", "No chat found for event $eventId")
            }
        } catch (e: Exception) {
            Log.w("FirestoreRepository", "Failed to add user to chat: ${e.message}")
            // Don't fail the join operation if chat update fails
        }

    }

    // Add leave event function
    suspend fun leaveEvent(eventId: String, userId: String) {
        try {
            db.runTransaction { transaction ->
                val eventRef = db.collection(EVENTS_COLLECTION).document(eventId)
                val event = transaction.get(eventRef).toObject(Event::class.java)
                    ?: throw Exception("Event not found")

                if (!event.participants.contains(userId)) {
                    throw Exception("User is not a participant")
                }

                // Update event
                transaction.update(eventRef,
                    "participants", FieldValue.arrayRemove(userId),
                    "currentParticipants", FieldValue.increment(-1)
                )

                // Update user
                val userRef = db.collection(USERS_COLLECTION).document(userId)
                transaction.update(userRef, "joinedEvents", FieldValue.arrayRemove(eventId))
            }.await()
        } catch (e: Exception) {
            throw Exception("Failed to leave event: ${e.message}")
        }

        try {
            val chat = db.collection(CHATS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .await()
                .toObjects(Chat::class.java)
                .firstOrNull()

            chat?.let {
                db.collection(CHATS_COLLECTION).document(it.id)
                    .update("participants", FieldValue.arrayRemove(userId))
                    .await()
            }
        } catch (e: Exception) {
            println("Warning: Failed to remove user from chat: ${e.message}")
        }
    }


    // Create or update user profile
    suspend fun createUserProfile(userId: String, name: String, email: String, profilePic: String?) {
        val user = User(
            id = userId,
            name = name,
            email = email,
            profilePic = profilePic,
            joinedEvents = emptyList(),
            createdEvents = emptyList(),
            hostedEvents = emptyList()
        )

        db.collection(USERS_COLLECTION).document(userId).set(user, SetOptions.merge()).await()
    }
    //Get user profile
    suspend fun getUserProfile(userId: String): User? {
        return db.collection(USERS_COLLECTION).document(userId).get().await().toObject(User::class.java)
    }

    // Delete Event
    // Improved Delete Event function with better error handling
    suspend fun deleteEvent(eventId: String, hostId: String) {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        if (currentUser.uid != hostId) {
            throw Exception("Only the event host can delete the event")
        }

        try {
            // Get event details first
            val eventDoc = db.collection(EVENTS_COLLECTION).document(eventId).get().await()

            if (!eventDoc.exists()) {
                throw Exception("Event not found")
            }

            val participants = eventDoc.get("participants") as? List<String> ?: emptyList()

            // Delete the event document first
            db.collection(EVENTS_COLLECTION).document(eventId).delete().await()

            // Clean up related data with individual error handling
            try {
                // Delete related event requests
                val eventRequests = db.collection(EVENT_REQUEST_COLLECTION)
                    .whereEqualTo("eventId", eventId)
                    .get()
                    .await()

                eventRequests.documents.forEach { doc ->
                    try {
                        doc.reference.delete().await()
                    } catch (e: Exception) {
                        // Log but don't fail the entire operation for individual request deletions
                        println("Warning: Failed to delete event request ${doc.id}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("Warning: Failed to query event requests: ${e.message}")
            }

            // Remove from host's hosted events (use createdEvents field name from your User data class)
            try {
                val userRef = db.collection(USERS_COLLECTION).document(hostId)
                val userDoc = userRef.get().await()
                if (userDoc.exists()) {
                    // Use both field names to be safe
                    userRef.update("hostedEvents", FieldValue.arrayRemove(eventId)).await()
                    userRef.update("createdEvents", FieldValue.arrayRemove(eventId)).await()
                }
            } catch (e: Exception) {
                println("Warning: Failed to update host's events list: ${e.message}")
            }

            // Remove from all participants' joined events
            participants.forEach { participantId ->
                try {
                    val participantRef = db.collection(USERS_COLLECTION).document(participantId)
                    val participantDoc = participantRef.get().await()
                    if (participantDoc.exists()) {
                        participantRef.update("joinedEvents", FieldValue.arrayRemove(eventId)).await()
                    }
                } catch (e: Exception) {
                    println("Warning: Failed to update participant $participantId events list: ${e.message}")
                }
            }

        } catch (e: Exception) {
            // Only throw if it's a critical error (like permission denied for the main delete)
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                throw Exception("Permission denied: ${e.message}")
            } else if (e.message?.contains("Event not found") == true) {
                throw e
            } else {
                throw Exception("Failed to delete event: ${e.message}")
            }
        }
    }

    suspend fun getEventParticipants(eventId: String): List<Participant> {
        val event = db.collection(EVENTS_COLLECTION).document(eventId).get().await()
        val participantIds = event.get("participants") as? List<String> ?: emptyList()

        if (participantIds.isEmpty()) return emptyList()

        val users = db.collection(USERS_COLLECTION)
            .whereIn(FieldPath.documentId(), participantIds)
            .get()
            .await()
            .toObjects(User::class.java)

        return users.map { user ->
            Participant(
                userId = user.id,
                name = user.name,
                profilePic = user.profilePic
            )
        }
    }
    // Remove participant from event
    suspend fun removeParticipant(eventId: String, userId: String, hostId: String) {
        // Verify current user is host
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        if (currentUser.uid != hostId) {
            throw Exception("Only event host can remove participants")
        }

        // Remove from event's participants list
        db.collection(EVENTS_COLLECTION).document(eventId)
            .update("participants", FieldValue.arrayRemove(userId))
            .await()

        // Decrement participant count
        db.collection(EVENTS_COLLECTION).document(eventId)
            .update("currentParticipants", FieldValue.increment(-1))
            .await()

        // Remove from user's joined events
        db.collection(USERS_COLLECTION).document(userId)
            .update("joinedEvents", FieldValue.arrayRemove(eventId))
            .await()

        // Update any pending requests
        db.collection(EVENT_REQUEST_COLLECTION)
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }

        try {
            val chat = db.collection(CHATS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .await()
                .toObjects(Chat::class.java)
                .firstOrNull()

            chat?.let {
                db.collection(CHATS_COLLECTION).document(it.id)
                    .update("participants", FieldValue.arrayRemove(userId))
                    .await()
            }
        } catch (e: Exception) {
            println("Warning: Failed to remove user from chat: ${e.message}")
        }
    }



    // Add to FirestoreRepository class

    // Get current user ID
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception("User not authenticated")
    }

    // Get event by ID
    suspend fun getEvent(eventId: String): Event? {
        return try {
            db.collection(EVENTS_COLLECTION)
                .document(eventId)
                .get()
                .await()
                .toObject(Event::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Get chat by event ID
    suspend fun getChatByEventId(eventId: String): Chat? {
        return try {
            db.collection(CHATS_COLLECTION)
                .whereEqualTo("eventId", eventId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(Chat::class.java)
        } catch (e: Exception) {
            null
        }
    }


    // Get all chats for current user
    suspend fun getUserChats(): List<Chat> {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
        val userId = currentUser.uid

        return try {
            Log.d("FirestoreRepository", "Fetching chats for user: $userId")
            val result = db.collection(CHATS_COLLECTION)
                .whereArrayContains("participants", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Chat::class.java)

            Log.d("FirestoreRepository", "Found ${result.size} chats")
            result
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error fetching chats: ${e.message}")
            emptyList()
        }
    }

    // Get chat with event details
    suspend fun getChatWithEventDetails(chatId: String): ChatWithEventDetails? {
        try {
            val chat = db.collection(CHATS_COLLECTION)
                .document(chatId)
                .get()
                .await()
                .toObject(Chat::class.java) ?: return null

            val event = db.collection(EVENTS_COLLECTION)
                .document(chat.eventId)
                .get()
                .await()
                .toObject(Event::class.java) ?: return null

            return ChatWithEventDetails(
                chat = chat,
                event = event
            )
        } catch (e: Exception) {
            return null
        }
    }

    // Get unread message count for a chat
    suspend fun getUnreadMessageCount(chatId: String, userId: String): Int {
        return try {
            val chat = db.collection(CHATS_COLLECTION)
                .document(chatId)
                .get()
                .await()
                .toObject(Chat::class.java) ?: return 0

            val lastReadTimestamp = chat.readReceipts[userId] ?: Date(0)

            val unreadMessages = db.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .whereGreaterThan("timestamp", lastReadTimestamp)
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()
                .size()

            unreadMessages
        } catch (e: Exception) {
            0
        }
    }

    // Update chat timestamp when message is sent
    suspend fun updateChatTimestamp(chatId: String) {
        try {
            db.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("timestamp", Date())
                .await()
        } catch (e: Exception) {
            println("Failed to update chat timestamp: ${e.message}")
        }
    }

    // Create chat when event is created
    suspend fun createChat(eventId: String, hostId: String) {
        val chat = Chat(
            eventId = eventId,
            participants = listOf(hostId),
            adminId = hostId,
            timestamp = Date()
        )

        val chatRef = db.collection(CHATS_COLLECTION).document()
        chat.copy(id = chatRef.id).let {
            chatRef.set(it).await()
        }
    }

    // Send message
    suspend fun sendMessage(chatId: String, text: String, imageUrl: String? = null): String {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        val message = Message(
            chatId = chatId,
            senderId = currentUser.uid,
            text = text,
            imageUrl = imageUrl,
            timestamp = Date()
        )

        val messageRef = db.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .document()

        message.copy(id = messageRef.id).let {
            messageRef.set(it).await()
        }

        // Update chat with last message
        db.collection(CHATS_COLLECTION).document(chatId)
            .update(
                "lastMessage", message,
                "timestamp", Date()
            ).await()

        return messageRef.id
    }

    // Get messages for a chat
    suspend fun getMessages(chatId: String): List<Message> {
        return try {
            db.collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(Message::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Listen for new messages (real-time)
    fun listenForMessages(chatId: String, onMessagesChanged: (List<Message>) -> Unit): ListenerRegistration {
        return db.collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening for messages: ${error.message}")
                    return@addSnapshotListener
                }

                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                onMessagesChanged(messages)
            }
    }

    // Listen for chat updates (real-time)
    fun listenForChats(onChatsChanged: (List<Chat>) -> Unit): ListenerRegistration {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        return db.collection(CHATS_COLLECTION)
            .whereArrayContains("participants", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error listening for chats: ${error.message}")
                    return@addSnapshotListener
                }

                val chats = snapshot?.toObjects(Chat::class.java) ?: emptyList()
                onChatsChanged(chats)
            }
    }

    // Mark chat as read
    suspend fun markChatAsRead(chatId: String) {
        val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

        try {
            db.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("readReceipts.${currentUser.uid}", Date())
                .await()
        } catch (e: Exception) {
            println("Failed to mark chat as read: ${e.message}")
        }
    }

    // Get chat participants with details
    suspend fun getChatParticipants(chatId: String): List<ChatParticipant> {
        try {
            val chat = db.collection(CHATS_COLLECTION)
                .document(chatId)
                .get()
                .await()
                .toObject(Chat::class.java) ?: return emptyList()

            if (chat.participants.isEmpty()) return emptyList()

            val users = db.collection(USERS_COLLECTION)
                .whereIn(FieldPath.documentId(), chat.participants)
                .get()
                .await()
                .toObjects(User::class.java)

            return users.map { user ->
                ChatParticipant(
                    userId = user.id,
                    name = user.name,
                    profilePic = user.profilePic
                )
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }


}