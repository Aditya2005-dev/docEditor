async function login(event) {

    event.preventDefault();


    const email =
        document.getElementById("email").value;

    const password =
        document.getElementById("password").value;


    try {

        const response = await fetch(
            "/auth/login",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    email: email,
                    password: password
                })
            }
        );


        if (!response.ok) {

            document.getElementById("message").innerText =
                "Invalid email or password.";

            return;
        }


        const data =
            await response.json();


        // Store JWT in browser

        localStorage.setItem(
            "token",
            data.token
        );


        // Go to dashboard

        window.location.href =
            "/dashboard.html";


    } catch (error) {

        document.getElementById("message").innerText =
            "Server error. Please try again.";

        console.error(error);
    }
}