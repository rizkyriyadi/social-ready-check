const functions = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();

/**
 * Cloud Function triggered when a new friend_request document is created.
 * Sends an FCM notification to the receiver.
 */
exports.onFriendRequest = functions.firestore.onDocumentCreated(
  "friend_requests/{requestId}",
  async (event) => {
    const request = event.data.data();

    if (!request || !request.receiverId) {
      console.log("Invalid request data");
      return null;
    }

    try {
      // Get receiver's user document
      const receiverDoc = await db.collection("users").doc(request.receiverId).get();

      if (!receiverDoc.exists) {
        console.log("Receiver not found:", request.receiverId);
        return null;
      }

      const receiverData = receiverDoc.data();
      const fcmToken = receiverData?.metadata?.fcmToken;

      if (!fcmToken) {
        console.log("No FCM token for receiver:", request.receiverId);
        return null;
      }

      // Send FCM notification
      const message = {
        token: fcmToken,
        notification: {
          title: "New Friend Request",
          body: `${request.senderName || "Someone"} wants to be your friend!`
        },
        data: {
          type: "friend_request",
          senderId: request.senderId || "",
          senderName: request.senderName || "",
          click_action: "FRIEND_REQUESTS"
        },
        android: {
          priority: "high",
          notification: {
            channelId: "tripglide_notifications"
          }
        }
      };

      const response = await admin.messaging().send(message);
      console.log("Notification sent successfully:", response);
      return response;

    } catch (error) {
      console.error("Error sending notification:", error);
      return null;
    }
  }
);

/**
 * Cloud Function triggered when a friend_request is updated.
 * When status changes to ACCEPTED:
 * 1. Adds both users to each other's friends list
 * 2. Sends notification to the sender
 */
exports.onFriendRequestUpdate = functions.firestore.onDocumentUpdated(
  "friend_requests/{requestId}",
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();

    // Only process if status changed to ACCEPTED
    if (before.status === after.status || after.status !== "ACCEPTED") {
      console.log("Status not changed to ACCEPTED, skipping");
      return null;
    }

    const senderId = after.senderId;
    const receiverId = after.receiverId;

    console.log(`Friend request accepted: ${senderId} <-> ${receiverId}`);

    try {
      // Get both users' data
      const [senderDoc, receiverDoc] = await Promise.all([
        db.collection("users").doc(senderId).get(),
        db.collection("users").doc(receiverId).get()
      ]);

      if (!senderDoc.exists || !receiverDoc.exists) {
        console.log("One or both users not found");
        return null;
      }

      const senderData = senderDoc.data();
      const receiverData = receiverDoc.data();
      const now = admin.firestore.Timestamp.now();

      // Add receiver to sender's friends list
      const senderFriendData = {
        uid: receiverId,
        displayName: receiverData.displayName || "",
        photoUrl: receiverData.photoUrl || "",
        username: receiverData.username || "",
        since: now
      };

      // Add sender to receiver's friends list
      const receiverFriendData = {
        uid: senderId,
        displayName: senderData.displayName || "",
        photoUrl: senderData.photoUrl || "",
        username: senderData.username || "",
        since: now
      };

      // Write to both friends subcollections
      await Promise.all([
        db.collection("users").doc(senderId).collection("friends").doc(receiverId).set(senderFriendData),
        db.collection("users").doc(receiverId).collection("friends").doc(senderId).set(receiverFriendData)
      ]);

      console.log("Friends added to both users successfully");

      // Send notification to sender
      const fcmToken = senderData?.metadata?.fcmToken;
      if (fcmToken) {
        const message = {
          token: fcmToken,
          notification: {
            title: "Friend Request Accepted!",
            body: `${receiverData.displayName || "Someone"} is now your friend!`
          },
          data: {
            type: "friend_accepted",
            friendId: receiverId,
            friendName: receiverData.displayName || "",
            click_action: "FRIENDS"
          },
          android: {
            priority: "high",
            notification: {
              channelId: "tripglide_notifications"
            }
          }
        };

        await admin.messaging().send(message);
        console.log("Acceptance notification sent to sender");
      }

      return { success: true };

    } catch (error) {
      console.error("Error processing friend acceptance:", error);
      return null;
    }
  }
);

/**
 * Callable function to remove a friend.
 * Removes from both users' friends lists.
 */
exports.removeFriend = functions.https.onCall(async (request) => {
  const { auth, data } = request;

  if (!auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const currentUid = auth.uid;
  const friendUid = data.friendUid;

  if (!friendUid) {
    throw new functions.https.HttpsError("invalid-argument", "friendUid is required");
  }

  try {
    // Remove from both friends lists
    await Promise.all([
      db.collection("users").doc(currentUid).collection("friends").doc(friendUid).delete(),
      db.collection("users").doc(friendUid).collection("friends").doc(currentUid).delete()
    ]);

    console.log(`Friend removed: ${currentUid} <-> ${friendUid}`);
    return { success: true };

  } catch (error) {
    console.error("Error removing friend:", error);
    throw new functions.https.HttpsError("internal", "Failed to remove friend");
  }
});

/**
 * Cloud Function triggered when a user is added to a circle.
 * Sends an FCM notification to the added user.
 */
exports.sendSquadInviteNotification = functions.firestore.onDocumentCreated(
  "circles/{circleId}/members/{memberDocId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
        console.log("No data associated with the event");
        return;
    }
    const memberData = snapshot.data();
    const circleId = event.params.circleId;
    // Assuming the document contains userId, or the document ID IS the userId. 
    // The prompt says "Get the userId from the created document data."
    const userId = memberData.userId || event.params.memberDocId; 

    if (!userId) {
        console.log("No userId found in member document");
        return;
    }

    try {
        // 1. Fetch Circle Name
        const circleDoc = await db.collection("circles").doc(circleId).get();
        const circleName = circleDoc.exists ? circleDoc.data().name : "Unknown Circle";

        // 2. Fetch User's FCM Token
        const userDoc = await db.collection("users").doc(userId).get();
        if (!userDoc.exists) {
            console.log(`User ${userId} not found`);
            return;
        }
        
        const userData = userDoc.data();
        // Check requested path first, then fallback to metadata
        const fcmToken = userData.fcmToken || userData?.metadata?.fcmToken;

        if (!fcmToken) {
            console.log(`No FCM token found for user ${userId}`);
            return;
        }

        // 3. Construct Payload
        const payload = {
            token: fcmToken,
            notification: {
                title: "SQUAD SUMMON!",
                body: `You have been added to the squad: ${circleName}.`,
            },
            data: {
                click_action: "FLUTTER_NOTIFICATION_CLICK", 
                screen: "squad_detail",
                circleId: circleId
            }
        };

        // 4. Send Notification
        await admin.messaging().send(payload);
        console.log(`Notification sent to user ${userId} for circle ${circleId}`);

    } catch (error) {
        console.error("Error sending squad invite notification:", error);
    }
  }
);

