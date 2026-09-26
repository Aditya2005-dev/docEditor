const token = localStorage.getItem("token");

if (!token) {
    window.location.href = "/login.html";
}

let activeShareDocumentId = null;

// Guards against the "Send request" button firing more than one
// POST /documents/{id}/share while a request is already in flight.
let shareRequestInFlight = false;


// ==========================================
// SMALL HELPERS
// ==========================================

function escapeHtml(str) {
    const div = document.createElement("div");
    div.innerText = str;
    return div.innerHTML;
}

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

const docIconSvg =
    '<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">' +
    '<path d="M6 2h9l5 5v15a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.6"/>' +
    '<path d="M14 2v6h6" stroke="currentColor" stroke-width="1.6"/>' +
    '</svg>';


// ==========================================
// SHOW LOGGED-IN USER
// ==========================================

function showUserEmail() {

    try {

        const payload = token.split(".")[1];

        const decoded = JSON.parse(
            atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
        );

        const email = decoded.sub || "";

        document.getElementById("userEmail").innerText = email;

        const avatar = document.getElementById("userAvatar");
        avatar.innerText = initialsFor(email);
        avatar.style.background = colorForName(email);

    } catch (error) {

        console.error("JWT decode error:", error);
    }
}


// ==========================================
// TABS
// ==========================================

function switchTab(tab) {

    const mineView = document.getElementById("myDocumentsView");
    const sharedView = document.getElementById("sharedDocumentsView");
    const tabMine = document.getElementById("tabMine");
    const tabShared = document.getElementById("tabShared");

    if (tab === "shared") {

        mineView.style.display = "none";
        sharedView.style.display = "block";

        tabMine.classList.remove("is-active");
        tabShared.classList.add("is-active");

        loadSharedDocuments();

    } else {

        mineView.style.display = "block";
        sharedView.style.display = "none";

        tabShared.classList.remove("is-active");
        tabMine.classList.add("is-active");
    }
}


// ==========================================
// CREATE PANEL TOGGLE
// ==========================================

function toggleCreatePanel() {

    const panel = document.getElementById("createPanel");

    panel.style.display =
        panel.style.display === "none" ? "block" : "none";
}


// ==========================================
// LOAD MY DOCUMENTS
// ==========================================

async function loadDocuments() {

    const container = document.getElementById("documents");

    try {

        const response = await fetch("/documents", {
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        if (response.status === 401) {
            logout();
            return;
        }

        if (!response.ok) {
            console.error("Failed to load documents:", response.status);
            return;
        }

        const documents = await response.json();

        container.innerHTML = "";

        if (documents.length === 0) {

            container.innerHTML =
                '<div class="empty-state">' +
                    emptyStateIcon() +
                    "<p>You don't have any documents yet.</p>" +
                "</div>";

            return;
        }

        documents.forEach(function (doc) {
            container.appendChild(buildOwnedDocCard(doc));
        });

    } catch (error) {

        console.error("Error loading documents:", error);
    }
}

function emptyStateIcon() {

    return '<svg width="40" height="40" viewBox="0 0 24 24" fill="none" ' +
        'xmlns="http://www.w3.org/2000/svg">' +
        '<path d="M6 2h9l5 5v15a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1Z" ' +
        'stroke="#83809c" stroke-width="1.3"/>' +
        '<path d="M14 2v6h6" stroke="#83809c" stroke-width="1.3"/>' +
        '</svg>';
}

function buildOwnedDocCard(doc) {

    const card = document.createElement("div");
    card.className = "doc-card";

    const preview = doc.content ? doc.content : "Empty document";

    card.innerHTML =
        '<div class="doc-icon">' + docIconSvg + '</div>' +
        '<div class="doc-info">' +
            '<h3>' + escapeHtml(doc.title || "Untitled") + '</h3>' +
            '<p>' + escapeHtml(preview) + '</p>' +
        '</div>' +
        '<div class="doc-actions">' +
            '<button class="btn-secondary btn-sm" onclick="openShareModal(' + doc.id + ')">Share</button>' +
            '<button class="btn-primary btn-sm" onclick="openDocument(' + doc.id + ')">Open</button>' +
            '<button class="btn-danger btn-sm" onclick="deleteDocument(' + doc.id + ')">Delete</button>' +
        '</div>';

    return card;
}


// ==========================================
// LOAD SHARED WITH ME
// ==========================================

async function loadSharedDocuments() {

    const container = document.getElementById("sharedDocuments");
    container.innerHTML = '<p class="loading-state">Loading documents...</p>';

    try {

        const response = await fetch("/documents/shared", {
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        if (response.status === 401) {
            logout();
            return;
        }

        if (!response.ok) {
            console.error("Failed to load shared documents:", response.status);
            container.innerHTML = '<p class="empty-state">Could not load shared documents.</p>';
            return;
        }

        const documents = await response.json();

        container.innerHTML = "";

        if (!documents || documents.length === 0) {

            container.innerHTML =
                '<div class="empty-state">' +
                    emptyStateIcon() +
                    "<p>No documents have been shared with you yet.</p>" +
                "</div>";

            return;
        }

        documents.forEach(function (doc) {
            container.appendChild(buildSharedDocCard(doc));
        });

    } catch (error) {

        console.error("Error loading shared documents:", error);
    }
}

function buildSharedDocCard(doc) {

    const card = document.createElement("div");
    card.className = "doc-card";

    const preview = doc.content ? doc.content : "Empty document";

    // The shared-documents endpoint may return the caller's permission
    // under one of a few field names — read defensively.
    const permission = (doc.permission || doc.accessLevel || doc.role || "").toUpperCase();

    const tag = permission
        ? '<span class="badge ' + (permission === "EDITOR" ? "tag-editor" : "tag-viewer") + '">' + permission + '</span>'
        : "";

    card.innerHTML =
        '<div class="doc-icon">' + docIconSvg + '</div>' +
        '<div class="doc-info">' +
            '<h3>' + escapeHtml(doc.title || "Untitled") + tag + '</h3>' +
            '<p>' + escapeHtml(preview) + '</p>' +
        '</div>' +
        '<div class="doc-actions">' +
            '<button class="btn-primary btn-sm" onclick="openDocument(' + doc.id + ')">Open</button>' +
        '</div>';

    return card;
}


// ==========================================
// SHARE REQUESTS
// ==========================================

async function loadShareRequests() {

    try {

        const response = await fetch("/documents/share-requests", {
            headers: {
                "Authorization": "Bearer " + token
            }
        });

        if (response.status === 401) {
            logout();
            return;
        }

        if (!response.ok) {
            console.error("Failed to load share requests:", response.status);
            return;
        }

        const requests = await response.json();

        renderShareRequests(requests || []);

    } catch (error) {

        console.error("Error loading share requests:", error);
    }
}

function renderShareRequests(requests) {

    const panel = document.getElementById("requestsPanel");
    const list = document.getElementById("requestsList");
    const badge = document.getElementById("requestsBadge");

    if (requests.length === 0) {
        panel.style.display = "none";
        return;
    }

    panel.style.display = "block";
    badge.innerText = requests.length;

    list.innerHTML = "";

    requests.forEach(function (req) {

        const row = document.createElement("div");
        row.className = "request-row";

        const docTitle =
            (req.document && req.document.title) ||
            req.documentTitle ||
            ("Document " + (req.documentId || ""));

        const sender =
            (req.sender && (req.sender.name || req.sender.email)) ||
            req.senderEmail ||
            req.senderName ||
            "Someone";

        const permission = req.permission || "";

        row.innerHTML =
            '<div class="request-info">' +
                '<p>' + escapeHtml(sender) + ' wants to share "' + escapeHtml(docTitle) + '" with you</p>' +
                '<span>' + escapeHtml(permission) + ' access</span>' +
            '</div>' +
            '<div class="request-actions">' +
                '<button class="btn-secondary btn-sm" onclick="declineRequest(' + req.id + ')">Decline</button>' +
                '<button class="btn-primary btn-sm" onclick="acceptRequest(' + req.id + ')">Accept</button>' +
            '</div>';

        list.appendChild(row);
    });
}

async function acceptRequest(requestId) {

    try {

        const response = await fetch(
            "/documents/share-requests/" + requestId + "/accept",
            {
                method: "POST",
                headers: {
                    "Authorization": "Bearer " + token
                }
            }
        );

        if (response.ok) {
            loadShareRequests();
            loadDocuments();
        } else {
            console.error("Failed to accept request:", response.status);
        }

    } catch (error) {

        console.error("Error accepting request:", error);
    }
}

async function declineRequest(requestId) {

    try {

        const response = await fetch(
            "/documents/share-requests/" + requestId + "/decline",
            {
                method: "POST",
                headers: {
                    "Authorization": "Bearer " + token
                }
            }
        );

        if (response.ok) {
            loadShareRequests();
        } else {
            console.error("Failed to decline request:", response.status);
        }

    } catch (error) {

        console.error("Error declining request:", error);
    }
}


// ==========================================
// CREATE DOCUMENT
// ==========================================

async function createDocument() {

    const title = document.getElementById("title").value;
    const content = document.getElementById("content").value;
    const message = document.getElementById("createMessage");

    message.className = "form-message";

    if (!title.trim()) {
        message.classList.add("is-error");
        message.innerText = "Please enter a title.";
        return;
    }

    try {

        const response = await fetch("/documents", {

            method: "POST",

            headers: {
                "Authorization": "Bearer " + token,
                "Content-Type": "application/json"
            },

            body: JSON.stringify({
                title: title,
                content: content
            })

        });

        if (response.ok) {

            document.getElementById("title").value = "";
            document.getElementById("content").value = "";

            message.classList.add("is-success");
            message.innerText = "Document created successfully.";

            loadDocuments();

        } else {

            message.classList.add("is-error");
            message.innerText = "Could not create document.";
        }

    } catch (error) {

        console.error("Error creating document:", error);
    }
}


// ==========================================
// DELETE DOCUMENT
// ==========================================

async function deleteDocument(id) {

    if (!confirm("Delete this document? This cannot be undone.")) {
        return;
    }

    try {

        const response = await fetch("/documents/" + id, {

            method: "DELETE",

            headers: {
                "Authorization": "Bearer " + token
            }

        });

        if (response.ok) {
            loadDocuments();
        } else {
            console.error("Failed to delete document:", response.status);
            alert("Could not delete document.");
        }

    } catch (error) {

        console.error("Error deleting document:", error);
    }
}


// ==========================================
// OPEN DOCUMENT
// ==========================================

function openDocument(id) {
    window.location.href = "/editor.html?id=" + id;
}


// ==========================================
// SHARE MODAL
//
// Fixes the "share fires twice / 500 error" bug: the button has a
// single onclick handler (no <form> submit involved), submitShare()
// refuses to run again while a request is already in flight, and the
// button is disabled + relabeled for the whole round trip so a
// double-click can only ever produce one POST.
// ==========================================

function openShareModal(documentId) {

    activeShareDocumentId = documentId;
    shareRequestInFlight = false;

    document.getElementById("shareEmail").value = "";
    document.getElementById("sharePermission").value = "VIEWER";

    const message = document.getElementById("shareMessage");
    message.innerText = "";
    message.className = "form-message";

    const submitBtn = document.getElementById("shareSubmitBtn");
    submitBtn.disabled = false;
    submitBtn.innerText = "Send request";

    document.getElementById("shareCancelBtn").disabled = false;

    document.getElementById("shareModal").hidden = false;
}

function closeShareModal() {
    document.getElementById("shareModal").hidden = true;
    activeShareDocumentId = null;
    shareRequestInFlight = false;
}

async function submitShare() {

    // Prevent double clicks
    if (shareRequestInFlight) {
        return;
    }

    const email =
        document.getElementById("shareEmail").value.trim();

    const permission =
        document.getElementById("sharePermission").value;

    const message =
        document.getElementById("shareMessage");

    const submitBtn =
        document.getElementById("shareSubmitBtn");

    const cancelBtn =
        document.getElementById("shareCancelBtn");


    message.className = "form-message";


    // Validate email
    if (!email) {

        message.classList.add("is-error");
        message.innerText =
            "Please enter an email address.";

        return;
    }


    shareRequestInFlight = true;

    submitBtn.disabled = true;
    cancelBtn.disabled = true;

    submitBtn.innerText = "Sending...";


    try {

        /*
         * Backend expects:
         *
         * POST /documents/{id}/share
         *      ?email=...
         *      &permission=VIEWER
         *
         * NOT JSON request body.
         */

        const url =
            "/documents/" +
            activeShareDocumentId +
            "/share" +
            "?email=" +
            encodeURIComponent(email) +
            "&permission=" +
            encodeURIComponent(permission);


        const response =
            await fetch(url, {

                method: "POST",

                headers: {
                    "Authorization":
                        "Bearer " + token
                }
            });


        const result =
            await response.text();


        if (response.ok) {

            message.classList.add("is-success");

            message.innerText =
                result || "Share request sent.";


            // Refresh requests
            loadShareRequests();


            // Close modal after success
            setTimeout(
                closeShareModal,
                900
            );


        } else {

            message.classList.add("is-error");

            message.innerText =
                result ||
                "Could not share document (status " +
                response.status +
                ").";


            submitBtn.disabled = false;
            cancelBtn.disabled = false;

            submitBtn.innerText =
                "Send request";

            shareRequestInFlight = false;
        }


    } catch (error) {

        console.error(
            "Share error:",
            error
        );


        message.classList.add("is-error");

        message.innerText =
            "Error sharing document.";


        submitBtn.disabled = false;
        cancelBtn.disabled = false;

        submitBtn.innerText =
            "Send request";

        shareRequestInFlight = false;
    }
}

// ==========================================
// LOGOUT
// ==========================================

function logout() {
    localStorage.removeItem("token");
    window.location.href = "/login.html";
}


// ==========================================
// START
// ==========================================

showUserEmail();
loadDocuments();
loadShareRequests();