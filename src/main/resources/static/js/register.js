async function register(event) {

    event.preventDefault();

    const name =
        document.getElementById("name").value;

    const email =
        document.getElementById("email").value;

    const password =
        document.getElementById("password").value;


    try {

        const response = await fetch(
            "/auth/register",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    name: name,
                    email: email,
                    password: password
                })
            }
        );


        const result = await response.text();


        document.getElementById("message").innerText =
            result;


        if (response.ok &&
            result === "User registered successfully") {

            setTimeout(function () {

                window.location.href =
                    "/login.html";

            }, 1000);
        }

    } catch (error) {

        document.getElementById("message").innerText =
            "Server error. Please try again.";

        console.error(error);
    }
}