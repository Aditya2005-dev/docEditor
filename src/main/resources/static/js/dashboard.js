let currentDocumentId = null;

const token =
    localStorage.getItem("token");


if (!token) {

    window.location.href =
        "/login.html";

}


loadDocuments();


// ==========================================
// LOAD DOCUMENTS
// ==========================================

async function loadDocuments() {

    try {

        const response =
            await fetch(
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


        if (!response.ok) {

            console.error(
                "Failed to load documents:",
                response.status
            );

            return;

        }


        const documents =
            await response.json();


        const container =
            document.getElementById(
                "documents"
            );


        container.innerHTML = "";


        if (documents.length === 0) {

            container.innerHTML =
                `<p class="empty">
                    You don't have any documents yet.
                </p>`;

            return;

        }


        documents.forEach(
            function (doc) {

                const card =
                    document.createElement(
                        "div"
                    );


                card.className =
                    "document-card";


                card.innerHTML = `

                    <div class="document-info">

                        <h3>
                            ${doc.title}
                        </h3>

                        <p>
                            ${doc.content || "Empty document"}
                        </p>

                    </div>


                    <button
                        onclick="openDocument(${doc.id})"
                    >
                        Open
                    </button>

                `;


                container.appendChild(
                    card
                );

            }
        );


    } catch (error) {

        console.error(
            "Error loading documents:",
            error
        );

    }

}


// ==========================================
// CREATE DOCUMENT
// ==========================================

async function createDocument() {

    const title =
        document.getElementById(
            "title"
        ).value;


    const content =
        document.getElementById(
            "content"
        ).value;


    if (!title.trim()) {

        document.getElementById(
            "createMessage"
        ).innerText =
            "Please enter a title.";

        return;

    }


    try {

        const response =
            await fetch(
                "/documents",
                {

                    method: "POST",

                    headers: {

                        "Authorization":
                            "Bearer " + token,

                        "Content-Type":
                            "application/json"

                    },

                    body:
                        JSON.stringify({

                            title:
                                title,

                            content:
                                content

                        })

                }
            );


        if (response.ok) {

            document.getElementById(
                "title"
            ).value = "";


            document.getElementById(
                "content"
            ).value = "";


            document.getElementById(
                "createMessage"
            ).innerText =
                "Document created successfully.";


            loadDocuments();

        } else {

            document.getElementById(
                "createMessage"
            ).innerText =
                "Could not create document.";

        }


    } catch (error) {

        console.error(
            "Error creating document:",
            error
        );

    }

}


// ==========================================
// OPEN DOCUMENT
// ==========================================

function openDocument(id) {

    window.location.href =
        "/editor.html?id=" + id;

}


// ==========================================
// LOGOUT
// ==========================================

function logout() {

    localStorage.removeItem(
        "token"
    );


    window.location.href =
        "/login.html";

}