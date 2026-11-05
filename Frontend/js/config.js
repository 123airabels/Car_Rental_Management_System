// Set USE_MOCK to false once the Spring Boot API is running.
// Where is the API?
//  - Opened from the backend itself (http://localhost:8080, a LAN address on port 8080, or a public tunnel / domain): same address, /api.
//  - Opened from a separate dev server on port 5500 (python -m http.server 5500, Live Server): same host, port 8080.
// Override by setting window.API_URL_OVERRIDE before this file loads.
const CONFIG = {
  API_URL: window.API_URL_OVERRIDE || (location.protocol === 'file:' ? 'http://localhost:8080/api'
    : (location.port === '5500' || location.port === '5501') ? location.protocol + '//' + location.hostname + ':8080/api'
    : location.origin + '/api'),
  USE_MOCK: false,
  CURRENCY: '\u20b1'
};
