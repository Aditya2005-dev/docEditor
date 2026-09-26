// ==========================================
// DOCUMENT ID
// ==========================================

const urlParams = new URLSearchParams(window.location.search);

const documentId = Number(urlParams.get("id"));

if (!documentId) {
    alert("Invalid document ID");
    window.location.href = "/dashboard.html";
}


// ==========================================
// JWT
// ==========================================

const token = localStorage.getItem("token");

if (!token) {
    alert("Please login first.");
    window.location.href = "/login.html";
}


// ==========================================
// GET USER EMAIL
// ==========================================

function getUserEmail() {

    try {

        const payload = token.split(".")[1];

        const decodedPayload = JSON.parse(
            atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
        );

        console.log("Logged in user:", decodedPayload.sub);

        return decodedPayload.sub;

    } catch (error) {

        console.error("JWT decode error:", error);
        return null;
    }
}

const userEmail = getUserEmail();


// ==========================================
// SMALL HELPERS (avatars)
// ==========================================

// Deterministic color from a name/email so the same person always
// gets the same avatar color — not a random/fake indicator.
function colorForName(name) {

    let hash = 0;

    for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }

    const hue = Math.abs(hash) % 360;

    return "hsl(" + hue + ", 62%, 45%)";
}

function initialsFor(name) {

    if (!name) {
        return "?";
    }

    const parts = name.trim().split(/\s+/);

    if (parts.length === 1) {
        return parts[0].substring(0, 2).toUpperCase();
    }

    return (parts[0][0] + parts[1][0]).toUpperCase();
}


// ==========================================
// ELEMENTS / STATE
// ==========================================

let documentVersion = 0;
let typingTimer = null;
let viewingTimer = null;
const debounceTime = 500;

// Whether this user is allowed to type in the editor.
// Defaults to true so existing behavior is unaffected when the
// backend response doesn't include a permission field.
let canEdit = true;

const editor = document.getElementById("editor");
const status = document.getElementById("status");
const statusDot = document.getElementById("statusDot");
const documentTitle = document.getElementById("documentTitle");
const activeUsers = document.getElementById("activeUsers");
const activeUsersEmpty = document.getElementById("activeUsersEmpty");
const avatarStack = document.getElementById("avatarStack");
const messageBox = document.getElementById("message");
const viewerBanner = document.getElementById("viewerBanner");


// ==========================================
// PERMISSION HELPER
// ==========================================

// The document GET response may expose the caller's access level under
// a few different field names depending on how the backend serializes
// it. Read defensively rather than assuming one exact field name.
function extractPermission(data) {

    const raw =
        data.permission ||
        data.role ||
        data.accessLevel ||
        data.currentUserPermission ||
        null;

    return raw ? String(raw).toUpperCase() : null;
}


// ==========================================
// LOAD DOCUMENT
// ==========================================

async function loadDocument() {

    console.log("Loading document...");

    try {

        const response = await fetch("/documents/" + documentId, {
            method: "GET",
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        console.log("Document response status:", response.status);

        if (!response.ok) {
            alert("Could not load document. Status: " + response.status);
            return;
        }

        const data = await response.json();

        console.log("Document loaded:", data);

        documentTitle.innerText = data.title || ("Document " + documentId);

        editor.value = data.content || "";

        documentVersion = data.version == null ? 0 : data.version;

        console.log("Document version:", documentVersion);

        const permission = extractPermission(data);

        if (permission === "VIEWER") {

            canEdit = false;
            editor.setAttribute("readonly", "true");
            viewerBanner.classList.add("is-visible");
        }

    } catch (error) {

        console.error("Load document error:", error);
    }
}


// ==========================================
// WEBSOCKET
// ==========================================

const client = new StompJs.Client({

    brokerURL: "ws://" + window.location.host + "/ws",

    connectHeaders: {
        Authorization: "Bearer " + token
    },

    reconnectDelay: 5000

});


// ==========================================
// CONNECT
// ==========================================

client.onConnect = function () {

    console.log("WebSocket connected");

    status.innerText = "Connected";
    statusDot.className = "status-dot is-connected";


    // ==================================
    // DOCUMENT EDIT SUBSCRIPTION
    // ==================================

    client.subscribe("/topic/document/" + documentId, function (message) {

        console.log("Document message:", message.body);

        const data = JSON.parse(message.body);


        // ==========================
        // CONFLICT
        // ==========================

        if (data.type === "CONFLICT") {

            documentVersion = data.version;
            editor.value = data.content || "";

            showMessage("Another edit was received. The latest document content has been loaded.");

            return;
        }


        // ==========================
        // EDIT
        // ==========================

        if (data.type === "EDIT") {

            if (data.version != null) {
                documentVersion = data.version;
            }

            if (data.userEmail !== userEmail) {
                editor.value = data.content || "";
            }
        }

    });


    // ==================================
    // PRESENCE SUBSCRIPTION
    // ==================================

    client.subscribe("/topic/document/" + documentId + "/presence", function (message) {

        console.log("Presence update:", message.body);

        const users = JSON.parse(message.body);

        renderActiveUsers(users);

    });


    // ==================================
    // JOIN DOCUMENT
    // ==================================

    console.log("Joining document:", documentId);

    client.publish({

        destination: "/app/join",

        body: JSON.stringify({
            documentId: documentId,
            userEmail: userEmail,
            type: "JOIN"
        })

    });

};


// ==========================================
// RENDER ACTIVE USERS
// ==========================================

function renderActiveUsers(users) {

    activeUsers.innerHTML = "";
    avatarStack.innerHTML = "";

    // Only ever render users that actually came back over the
    // WebSocket presence channel — never invent placeholder users.
    const others = (users || []).filter(function (u) {
        return (u.email || u.name) !== userEmail;
    });

    if (others.length === 0) {

        activeUsersEmpty.style.display = "block";
        return;
    }

    activeUsersEmpty.style.display = "none";

    others.forEach(function (user) {

        const name = user.name || user.email || "Unknown user";
        const userStatus = user.status || "VIEWING";
        const dotClass = userStatus === "EDITING" ? "is-editing" : "is-viewing";
        const color = colorForName(name);
        const initials = initialsFor(name);

        // expanded presence row (name + real status)
        const chip = document.createElement("div");
        chip.className = "presence-chip";

        chip.innerHTML =
            '<span class="avatar" style="background:' + color + '">' + initials + '</span>' +
            '<span class="presence-dot ' + dotClass + '"></span>' +
            name + ' — ' + (userStatus === "EDITING" ? "Editing" : "Viewing");

        activeUsers.appendChild(chip);

        // compact avatar stack in the top bar
        const stackAvatar = document.createElement("span");
        stackAvatar.className = "avatar";
        stackAvatar.style.background = color;
        stackAvatar.innerText = initials;
        stackAvatar.title = name + " — " + (userStatus === "EDITING" ? "Editing" : "Viewing");

        avatarStack.appendChild(stackAvatar);

    });

}


// ==========================================
// WEBSOCKET ERROR
// ==========================================

client.onWebSocketError = function (error) {

    console.error("WebSocket error:", error);

    status.innerText = "Connection error";
    statusDot.className = "status-dot is-error";
};


// ==========================================
// WEBSOCKET CLOSE
// ==========================================

client.onWebSocketClose = function () {

    console.log("WebSocket disconnected");

    status.innerText = "Disconnected";
    statusDot.className = "status-dot";
};


// ==========================================
// HANDLE INPUT
// ==========================================

function handleEditorInput() {

    if (!canEdit) {
        return;
    }

    console.log("INPUT EVENT FIRED");

    // Immediately show EDITING
    sendStatus("EDITING");

    clearTimeout(typingTimer);
    clearTimeout(viewingTimer);

    typingTimer = setTimeout(function () {
        sendEdit();
    }, debounceTime);

    // After 2 seconds of no typing show VIEWING
    viewingTimer = setTimeout(function () {
        sendStatus("VIEWING");
    }, 2000);

}


// ==========================================
// SEND STATUS
// ==========================================

function sendStatus(currentStatus) {

    if (!client.connected) {
        return;
    }

    console.log("Sending status:", currentStatus);

    client.publish({

        destination: "/app/status",

        body: JSON.stringify({
            documentId: documentId,
            userEmail: userEmail,
            status: currentStatus,
            type: "STATUS"
        })

    });

}


// ==========================================
// SEND EDIT
// ==========================================

function sendEdit() {

    console.log("sendEdit() called");

    if (!client.connected) {
        console.log("WebSocket not connected");
        return;
    }

    const content = editor.value;

    console.log("Sending edit:", {
        documentId: documentId,
        userEmail: userEmail,
        version: documentVersion,
        content: content
    });

    client.publish({

        destination: "/app/edit",

        body: JSON.stringify({
            documentId: documentId,
            userEmail: userEmail,
            content: content,
            version: documentVersion,
            type: "EDIT"
        })

    });

}


// ==========================================
// LEAVE DOCUMENT
// ==========================================

window.addEventListener("beforeunload", function () {

    if (client.connected) {

        client.publish({

            destination: "/app/leave",

            body: JSON.stringify({
                documentId: documentId,
                userEmail: userEmail,
                type: "LEAVE"
            })

        });

    }

});


// ==========================================
// MESSAGE
// ==========================================

function showMessage(text) {

    messageBox.innerText = text;
    messageBox.classList.add("is-visible");

    setTimeout(function () {
        messageBox.classList.remove("is-visible");
    }, 4000);

}


// ==========================================
// START
// ==========================================

client.activate();
loadDocument();