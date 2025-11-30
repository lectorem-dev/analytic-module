import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import RegularFont from "./fonts/SourceCodePro-Regular.ttf"; // путь к локальному файлу

// Глобальные стили с @font-face
const globalStyles = `
  @font-face {
    font-family: 'Source Code Pro';
    src: url(${RegularFont}) format('truetype');
    font-weight: 400;
    font-style: normal;
  }

  html, body {
    margin: 0;
    padding: 0;
    background: #222;
    color: #fff;
    font-family: 'Source Code Pro', monospace;
    height: 100%;
    width: 100%;
  }

  #root {
    min-height: 100vh;
    display: flex;
    justify-content: center; /* центрирует панель горизонтально */
    align-items: flex-start; /* панель сверху */
    padding: 20px;
    box-sizing: border-box;
  }

  button {
    cursor: pointer;
  }
`;

// Добавляем стили в head
const styleTag = document.createElement("style");
styleTag.innerHTML = globalStyles;
document.head.appendChild(styleTag);

const container = document.getElementById("root");
const root = createRoot(container);
root.render(<App />);