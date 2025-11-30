import { useState, useEffect, useRef } from "react";
import axios from "axios";

const API_BASE = "http://localhost:8001/api";

export default function MonitorPanel() {
    const [apiKey, setApiKey] = useState("");
    const [manufactureId, setManufactureId] = useState("");
    const [intervalMs, setIntervalMs] = useState(2000);

    const [data, setData] = useState(null);
    const [error, setError] = useState(null); // состояние для ошибок
    const timerRef = useRef(null);

    const startPolling = () => {
        if (!apiKey) return alert("Введите API ключ");
        if (!manufactureId) return alert("Введите manufactureId");

        stopPolling();

        timerRef.current = setInterval(async () => {
            try {
                const response = await axios.get(`${API_BASE}/${manufactureId}/show`, {
                    headers: {
                        "X-API-KEY": apiKey
                    }
                });

                setData(response.data);
                setError(null); // сброс ошибки при успешном ответе
            } catch (err) {
                console.error(err);

                // Формируем текст ошибки для вывода
                if (err.response) {
                    // Сервер вернул статус != 2xx
                    const status = err.response.status;
                    if (status === 403) {
                        setError("Ошибка 403: Доступ запрещён. Проверьте API-ключ.");
                    } else if (status === 500) {
                        setError("Ошибка 500: Внутренняя ошибка сервера.");
                    } else if (status === 400) {
                        setError("Ошибка 500: Bead request (проверьте корректность ID).");
                    } else {
                        setError(`Ошибка ${status}: ${err.response.statusText}`);
                    }
                } else if (err.request) {
                    // Запрос был сделан, но ответа нет
                    setError("Ошибка сети: сервер не отвечает.");
                } else {
                    // Другая ошибка
                    setError("Произошла ошибка: " + err.message);
                }
            }
        }, intervalMs);
    };

    const stopPolling = () => {
        if (timerRef.current) clearInterval(timerRef.current);
    };

    useEffect(() => stopPolling, []);

    return (
        <div style={{ padding: 20 }}>
            <h2>📊 Monitoring manufactureId</h2>

            <div style={{ marginBottom: 10 }}>
                <label>API Key: </label>
                <input
                    value={apiKey}
                    onChange={(e) => setApiKey(e.target.value)}
                    placeholder="Введите API ключ"
                    style={{ width: 350, padding: 8 }}
                />
            </div>

            <div style={{ marginBottom: 10 }}>
                <label>UUID товара: </label>
                <input
                    value={manufactureId}
                    onChange={(e) => setManufactureId(e.target.value)}
                    placeholder="UUID товара..."
                    style={{ width: 350, padding: 8 }}
                />
            </div>

            <div style={{ marginBottom: 20 }}>
                <label>Интервал опроса (мс): </label>
                <input
                    type="number"
                    value={intervalMs}
                    onChange={(e) => setIntervalMs(Number(e.target.value))}
                    style={{ width: 120, padding: 8 }}
                />
            </div>

            <button onClick={startPolling} style={{ marginRight: 10 }}>
                ▶ Start
            </button>
            <button onClick={stopPolling}>
                ⏹ Stop
            </button>

            <hr />

            <h3>📈 Результаты</h3>

            <pre style={{
                background: "#222",
                color: error ? "#f00" : "#0f0", // красный если есть ошибка, зелёный если всё ок
                padding: 20,
                borderRadius: 8,
                maxHeight: 400,
                overflowY: "auto"
            }}>
                {error ? error : (data ? JSON.stringify(data, null, 2) : "Нет данных")}
            </pre>
        </div>
    );
}