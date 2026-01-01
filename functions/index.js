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
        }
      };

      await admin.messaging().send(message);
      console.log("Notification sent to:", request.receiverId);
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  }
);

/**
 * Cloud Function triggered when a new summon is created in a circle.
 * Sends a high-priority FCM notification to all circle members.
 */
exports.onSummonCreated = functions.firestore.onDocumentCreated(
  "circles/{circleId}/summons/{summonId}",
  async (event) => {
    const summon = event.data.data();
    const circleId = event.params.circleId;
    const summonId = event.params.summonId;

    if (!summon) {
      console.log("Invalid summon data");
      return null;
    }

    const initiatorId = summon.initiatorId;
    const initiatorName = summon.initiatorName || "Someone";
    const initiatorPhotoUrl = summon.initiatorPhotoUrl || "";

    try {
      // 1. Fetch the Circle document to get memberIds
      const circleDoc = await db.collection("circles").doc(circleId).get();
      if (!circleDoc.exists) {
        console.log("Circle not found:", circleId);
        return null;
      }

      const circleData = circleDoc.data();
      const memberIds = circleData.memberIds || [];

      // 2. Send to ALL members (including initiator for multi-device support)
      const recipients = memberIds;

      if (recipients.length === 0) {
        console.log("No recipients for summon");
        return null;
      }

      console.log("Sending summon to", recipients.length, "members:", recipients);

      // 3. Fetch tokens for all recipients
      const userPromises = recipients.map(uid => db.collection("users").doc(uid).get());
      const userDocs = await Promise.all(userPromises);

      const tokens = [];
      userDocs.forEach((doc, idx) => {
        if (doc.exists) {
          const userData = doc.data();
          const token = userData.fcmToken || userData.metadata?.fcmToken;
          if (token) {
            tokens.push(token);
            console.log("Found token for user:", recipients[idx]);
          } else {
            console.log("No token for user:", recipients[idx]);
          }
        }
      });

      if (tokens.length === 0) {
        console.log("No tokens found for recipients");
        return null;
      }

      // 4. Send Multicast Message - DATA ONLY (no notification block)
      const message = {
        tokens: tokens,
        data: {
          type: "SUMMON",
          summonId: summonId,
          circleId: circleId,
          initiatorName: initiatorName,
          initiatorPhotoUrl: initiatorPhotoUrl,
          initiatorId: initiatorId,
          timestamp: Date.now().toString()
        },
        android: {
          priority: "high",
          ttl: 30000
        }
      };

      const response = await admin.messaging().sendEachForMulticast(message);
      console.log("Summon notifications sent:", response.successCount, "of", tokens.length);
      
      if (response.failureCount > 0) {
        response.responses.forEach((resp, idx) => {
          if (!resp.success) {
            console.log('Token failed:', tokens[idx], 'Error:', resp.error?.message);
          }
        });
      }

      return null;

    } catch (error) {
      console.error("Error sending summon notification:", error);
      return null;
    }
  }
);

/**
 * Cloud Function triggered when a new message is sent in a Circle.
 * Sends FCM notification to all circle members (except sender).
 */
exports.onCircleMessage = functions.firestore.onDocumentCreated(
  "circles/{circleId}/messages/{messageId}",
  async (event) => {
    const message = event.data.data();
    const circleId = event.params.circleId;

    if (!message || message.senderId === "SYSTEM") {
      console.log("Skipping system message notification");
      return null;
    }

    const senderId = message.senderId;
    const senderName = message.senderName || "Someone";
    const senderPhotoUrl = message.senderPhotoUrl || "";
    const messageContent = message.content || "";
    const messageType = message.type || "TEXT";

    try {
      // Get Circle info
      const circleDoc = await db.collection("circles").doc(circleId).get();
      if (!circleDoc.exists) {
        console.log("Circle not found:", circleId);
        return null;
      }

      const circleData = circleDoc.data();
      const circleName = circleData.name || "Circle";
      const memberIds = circleData.memberIds || [];

      // Exclude the sender
      const recipients = memberIds.filter(uid => uid !== senderId);

      if (recipients.length === 0) {
        console.log("No recipients for circle message");
        return null;
      }

      console.log("Sending circle message notification to", recipients.length, "members");

      // Fetch tokens for all recipients
      const userPromises = recipients.map(uid => db.collection("users").doc(uid).get());
      const userDocs = await Promise.all(userPromises);

      const tokens = [];
      userDocs.forEach((doc) => {
        if (doc.exists) {
          const userData = doc.data();
          const token = userData.fcmToken || userData.metadata?.fcmToken;
          if (token) {
            tokens.push(token);
          }
        }
      });

      if (tokens.length === 0) {
        console.log("No tokens found for circle message recipients");
        return null;
      }

      // Send Multicast Message
      const fcmMessage = {
        tokens: tokens,
        data: {
          type: "CIRCLE_MESSAGE",
          channelId: circleId,
          circleName: circleName,
          senderId: senderId,
          senderName: senderName,
          senderPhotoUrl: senderPhotoUrl,
          messageContent: messageContent.substring(0, 200), // Truncate
          messageType: messageType,
          timestamp: Date.now().toString()
        },
        android: {
          priority: "high"
        }
      };

      const response = await admin.messaging().sendEachForMulticast(fcmMessage);
      console.log("Circle message notifications sent:", response.successCount, "of", tokens.length);

      return null;

    } catch (error) {
      console.error("Error sending circle message notification:", error);
      return null;
    }
  }
);

/**
 * Cloud Function triggered when a new DM is sent.
 * Sends FCM notification to the recipient.
 */
exports.onDirectMessage = functions.firestore.onDocumentCreated(
  "chats/{channelId}/messages/{messageId}",
  async (event) => {
    const message = event.data.data();
    const channelId = event.params.channelId;

    if (!message || message.senderId === "SYSTEM") {
      console.log("Skipping system message notification");
      return null;
    }

    const senderId = message.senderId;
    const senderName = message.senderName || "Someone";
    const senderPhotoUrl = message.senderPhotoUrl || "";
    const messageContent = message.content || "";
    const messageType = message.type || "TEXT";

    try {
      // Get DM channel to find the recipient
      const channelDoc = await db.collection("chats").doc(channelId).get();
      if (!channelDoc.exists) {
        console.log("DM channel not found:", channelId);
        return null;
      }

      const channelData = channelDoc.data();
      const participants = channelData.participants || [];

      // Find the recipient (the other participant)
      const recipientId = participants.find(uid => uid !== senderId);

      if (!recipientId) {
        console.log("Recipient not found in DM channel");
        return null;
      }

      console.log("Sending DM notification to:", recipientId);

      // Get recipient's FCM token
      const recipientDoc = await db.collection("users").doc(recipientId).get();
      if (!recipientDoc.exists) {
        console.log("Recipient user not found:", recipientId);
        return null;
      }

      const recipientData = recipientDoc.data();
      const token = recipientData.fcmToken || recipientData.metadata?.fcmToken;

      if (!token) {
        console.log("No FCM token for recipient:", recipientId);
        return null;
      }

      // Send FCM notification
      const fcmMessage = {
        token: token,
        data: {
          type: "DM_MESSAGE",
          channelId: channelId,
          senderId: senderId,
          senderName: senderName,
          senderPhotoUrl: senderPhotoUrl,
          messageContent: messageContent.substring(0, 200), // Truncate
          messageType: messageType,
          timestamp: Date.now().toString()
        },
        android: {
          priority: "high"
        }
      };

      await admin.messaging().send(fcmMessage);
      console.log("DM notification sent to:", recipientId);

      return null;

    } catch (error) {
      console.error("Error sending DM notification:", error);
      return null;
    }
  }
);
