import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8082', // Cổng Keycloak chạy trong docker-compose
  realm: 'lfs',
  clientId: 'my-keycloak-client',
});

export default keycloak;