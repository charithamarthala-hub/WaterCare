function logout() {

    localStorage.removeItem(
        "waterCareLoggedIn"
    );

    window.location.href =
        "index.html";
}