import React from 'react';
import ReactDOM from 'react-dom/client';
import AppLayout from './components/AppLayout';
import './index.css';
import keycloak from './keycloak';

keycloak.init({ onLoad: 'login-required', checkLoginIframe: false })
  .then((authenticated: boolean) => {
    if (authenticated) {
      ReactDOM.createRoot(document.getElementById('root')!).render(
        <React.StrictMode>
          <AppLayout />
        </React.StrictMode>
      );
    } else {
      window.location.reload();
    }
  })
  .catch((error: unknown) => {
    console.error("Keycloak Init Error", error);
  });