/**
 * @file index.tsx
 * @description Entry point for the HG2Gui Application.
 * Handles the DOM mounting and React StrictMode initialization.
 */

import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// locate the root div in index.html
const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error("Could not find root element to mount to");
}

// Initialize React Root
const root = ReactDOM.createRoot(rootElement);

// Render the main App component wrapped in StrictMode for best practices
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);