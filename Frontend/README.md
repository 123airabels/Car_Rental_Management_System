# Byahe Rentals – Front End

Plain HTML + Tailwind + JavaScript. Talks to the Spring Boot API in `../Application`.

Run it over http (not by double-clicking), so the browser can call the API:

    cd Frontend
    python -m http.server 5500

Then open http://localhost:5500. Start the backend first (see the main README).

`js/config.js` -> `USE_MOCK: true` runs the site on sample data without a backend.
