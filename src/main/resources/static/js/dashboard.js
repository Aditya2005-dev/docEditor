let currentDocumentId = null;


// Get JWT

const token =
    localStorage.getItem("token");


// If user isn't logged in

if (!token) {

    window.location.href =
        "/login.html";
}


// Load documents

loadDocuments();


// ----------------------------------
// LOAD DOCUMENTS
// ----------------------------------

async function loadDocuments() {

    try {

        const response = await fetch(
            "/documents",
            {
                headers: {
                    "Authorization":
                        "Bearer " + token
                }
            }
        );


        if (response.status === 401) {

            logout();

            return;
        }


        const documents =
            await response.json();


        const container =
            document.getElementById("documents");


        container.innerHTML = "";


        if (documents.length === 0) {

            container.innerHTML = `
                <p class="empty">
                    You don't have any documents yet.
                </p>
            `;

            return;
        }


        documents.forEach(function(document) {

            const card =
                document.createElement("div");


            card.className =
                "document-card";


            card.innerHTML = `

                <div class="document-info">

                    <h3>
                        ${document.title}
                    </h3>

                    <p>
                        ${document.content || "Empty document"}
                    </p>

                </div>

                <button
                    onclick="openDocument(${document.id})"
                >
                    Open
                </button>

            `;


            container.appendChild(card);

        });


    } catch (error) {

        console.error(error);

    }
}


// ----------------------------------
// CREATE DOCUMENT
// ----------------------------------

async function createDocument() {

    const title =
        document.getElementById("title").value;

    const content =
        document.getElementById("content").value;


    if (!title.trim()) {

        document.getElementById("createMessage")
            .innerText =
            "Please enter a title.";

        return;
    }


    try {

        const response = await fetch(
            "/documents",
            {
                method: "POST",

                headers: {

                    "Authorization":
                        "Bearer " + token,

                    "Content-Type":
                        "application/json"
                },

                body: JSON.stringify({

                    title: title,

                    content: content

                })
            }
        );


        if (response.ok) {

            document.getElementById("title").value =
                "";

            document.getElementById("content").value =
                "";

            document.getElementById("createMessage")
                .innerText =
                "Document created successfully.";

            loadDocuments();

        } else {

            document.getElementById("createMessage")
                .innerText =
                "Could not create document.";
        }


    } catch (error) {

        console.error(error);

    }
}


// ----------------------------------
// OPEN DOCUMENT
// ----------------------------------

async function openDocument(id) {

    try {

        const response = await fetch(
            "/documents/" + id,
            {
                headers: {
                    "Authorization":
                        "Bearer " + token
                }
            }
        );


        if (!response.ok) {

            alert("Could not open document.");

            return;
        }


        const document =
            await response.json();


        currentDocumentId =
            document.id;


        document.getElementById("editTitle").value =
            document.title;


        document.getElementById("editContent").value =
            document.content || "";


        document.getElementById("editor")
            .style.display =
            "block";


        window.scrollTo({
            top: document.body.scrollHeight,
            behavior: "smooth"
        });


    } catch (error) {

        console.error(error);

    }
}


// ----------------------------------
// UPDATE DOCUMENT
// ----------------------------------

async function updateDocument() {

    if (!currentDocumentId) {

        return;
    }


    const title =
        document.getElementById("editTitle").value;

    const content =
        document.getElementById("editContent").value;


    try {

        const response = await fetch(

            "/documents/" +
            currentDocumentId,

            {
                method: "PUT",

                headers: {

                    "Authorization":
                        "Bearer " + token,

                    "Content-Type":
                        "application/json"
                },

                body: JSON.stringify({

                    title: title,

                    content: content

                })
            }
        );


        if (response.ok) {

            document.getElementById("editMessage")
                .innerText =
                "Saved successfully.";

            loadDocuments();

        } else {

            document.getElementById("editMessage")
                .innerText =
                "Could not save document.";
        }


    } catch (error) {

        console.error(error);

    }
}


// ----------------------------------
// DELETE DOCUMENT
// ----------------------------------

async function deleteDocument() {

    if (!currentDocumentId) {

        return;
    }


    const confirmed =
        confirm(
            "Are you sure you want to delete this document?"
        );


    if (!confirmed) {

        return;
    }


    try {

        const response = await fetch(

            "/documents/" +
            currentDocumentId,

            {
                method: "DELETE",

                headers: {

                    "Authorization":
                        "Bearer " + token
                }
            }
        );


        if (response.ok) {

            closeEditor();

            loadDocuments();

        } else {

            alert("Could not delete document.");
        }


    } catch (error) {

        console.error(error);

    }
}


// ----------------------------------
// CLOSE EDITOR
// ----------------------------------

function closeEditor() {

    currentDocumentId = null;

    document.getElementById("editor")
        .style.display =
        "none";

    document.getElementById("editMessage")
        .innerText = "";
}


// ----------------------------------
// LOGOUT
// ----------------------------------

function logout() {

    localStorage.removeItem("token");

    window.location.href =
        "/login.html";
}