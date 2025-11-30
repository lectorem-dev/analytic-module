import { useState, useEffect, useRef } from "react";
import axios from "axios";
import { Line } from "react-chartjs-2";
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend
} from "chart.js";

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend
);

const API_BASE = "http://localhost:8001/api";
const MAX_POINTS = 60;

export default function MonitorPanel() {
    const [apiKey, setApiKey] = useState("secret");
    const [manufactureId, setManufactureId] = useState("");
    const [intervalMs, setIntervalMs] = useState(2000);

    const [data, setData] = useState(null);
    const [error, setError] = useState(null);
    const timerRef = useRef(null);
    const [history, setHistory] = useState([]);

    const startPolling = () => {
        if (!apiKey) return alert("Введите API ключ");
        if (!manufactureId) return alert("Введите manufactureId");

        stopPolling();

        timerRef.current = setInterval(async () => {
            try {
                const response = await axios.get(`${API_BASE}/${manufactureId}/show`, {
                    headers: { "X-API-KEY": apiKey }
                });

                setData(response.data);
                setError(null);

                setHistory(prev => {
                    const next = [
                        ...prev,
                        {
                            timestamp: new Date(),
                            averageRank: parseFloat(response.data.averageRank),
                            globalCount: parseInt(response.data.globalCount, 10),
                            referCount: parseInt(response.data.referCount, 10)
                        }
                    ];
                    return next.slice(-MAX_POINTS);
                });

            } catch (err) {
                console.error(err);
                if (err.response) {
                    const status = err.response.status;
                    if (status === 403) setError("Ошибка 403: доступ запрещён. Проверьте API-ключ.");
                    else if (status === 500) setError("Ошибка 500: внутренняя ошибка сервера.");
                    else setError(`Ошибка ${status}: ${err.response.statusText}`);
                } else if (err.request) setError("Сервер не отвечает. Проверьте подключение или API URL.");
                else setError("Произошла ошибка: " + err.message);
            }
        }, intervalMs);
    };

    const stopPolling = () => {
        if (timerRef.current) clearInterval(timerRef.current);
    };

    useEffect(() => stopPolling, []);

    const chartData = {
        labels: history.map(h => h.timestamp.toLocaleTimeString()),
        datasets: [
            {
                label: "averageRank",
                data: history.map(h => h.averageRank),
                borderColor: "rgba(0,255,0,0.7)",
                backgroundColor: "rgba(0,255,0,0.3)"
            },
            {
                label: "globalCount",
                data: history.map(h => h.globalCount),
                borderColor: "rgba(0,150,255,0.7)",
                backgroundColor: "rgba(0,150,255,0.3)"
            },
            {
                label: "referCount",
                data: history.map(h => h.referCount),
                borderColor: "rgba(255,0,0,0.7)",
                backgroundColor: "rgba(255,0,0,0.3)"
            }
        ]
    };

    const chartOptions = {
        responsive: true,
        plugins: {
            legend: {
                position: "top",
                labels: {
                    color: "#fff",
                    font: { family: "'Source Code Pro', monospace", size: 12 }
                }
            },
            title: {
                display: true,
                text: "Динамика параметров",
                color: "#fff",
                font: { family: "'Source Code Pro', monospace", size: 16, weight: "bold" }
            },
            tooltip: {
                bodyFont: { family: "'Source Code Pro', monospace", size: 12 },
                titleFont: { family: "'Source Code Pro', monospace", size: 14, weight: "bold" }
            }
        },
        scales: {
            x: {
                ticks: { color: "#fff", font: { family: "'Source Code Pro', monospace", size: 12 } },
                grid: { color: "rgba(255,255,255,0.1)" }
            },
            y: {
                beginAtZero: true,
                ticks: { color: "#fff", font: { family: "'Source Code Pro', monospace", size: 12 } },
                grid: { color: "rgba(255,255,255,0.1)" }
            }
        }
    };

    const inputStyle = {
        width: 450,
        padding: 8,
        borderRadius: 6,
        border: "1px solid rgba(0,150,255,0.7)",
        background: "#111",
        color: "#fff",
        outline: "none",
        fontFamily: "'Source Code Pro', monospace"
    };

    const smallInputStyle = {
        width: 120,
        padding: 8,
        borderRadius: 6,
        border: "1px solid rgba(0,150,255,0.7)",
        background: "#111",
        color: "#fff",
        outline: "none",
        fontFamily: "'Source Code Pro', monospace"
    };

    const buttonStyle = {
        padding: "8px 16px",
        borderRadius: 6,
        border: "none",
        cursor: "pointer",
        fontWeight: "bold",
        marginRight: 10,
        fontFamily: "'Source Code Pro', monospace"
    };

    const startButtonStyle = {
        ...buttonStyle,
        backgroundColor: "rgba(0,150,255,0.7)",
        color: "#fff"
    };

    const stopButtonStyle = {
        ...buttonStyle,
        backgroundColor: "rgba(255,0,0,0.7)",
        color: "#fff"
    };

    // Стили для таблицы с метками и полями
    const formRowStyle = {
        display: "flex",
        alignItems: "center",
        marginBottom: 10
    };

    const labelStyle = {
        width: 220,       // фиксированная ширина для выравнивания
        marginRight: 10,
        textAlign: "left" // выравнивание текста по левой стороне
    };

    return (
        <div style={{ padding: 20, color: "#fff", background: "#222", borderRadius: 8 }}>
            <div style={formRowStyle}>
                <label style={labelStyle}>API Key:</label>
                <input
                    value={apiKey}
                    onChange={(e) => setApiKey(e.target.value)}
                    placeholder="Введите API ключ"
                    style={inputStyle}
                />
            </div>

            <div style={formRowStyle}>
                <label style={labelStyle}>UUID товара:</label>
                <input
                    value={manufactureId}
                    onChange={(e) => setManufactureId(e.target.value)}
                    placeholder="UUID товара..."
                    style={inputStyle}
                />
            </div>

            <div style={formRowStyle}>
                <label style={labelStyle}>Интервал опроса (мс):</label>
                <input
                    type="number"
                    value={intervalMs}
                    onChange={(e) => setIntervalMs(Number(e.target.value))}
                    style={smallInputStyle}
                />
            </div>

            <div style={{ marginBottom: 20 }}>
                <button onClick={startPolling} style={startButtonStyle}>Start</button>
                <button onClick={stopPolling} style={stopButtonStyle}>Stop</button>
            </div>

            <hr style={{ borderColor: "rgba(255,255,255,0.1)" }} />

            <h3>Результаты</h3>
            <pre style={{
                background: "#111",
                color: error ? "#f00" : "#0f0",
                padding: 20,
                borderRadius: 8,
                maxHeight: 150,
                overflowY: "auto"
            }}>
                {error ? error : (data ? JSON.stringify(data, null, 2) : "Нет данных")}
            </pre>

            <h3>График динамики</h3>
            <div style={{ background: "#111", padding: 20, borderRadius: 8 }}>
                <Line data={chartData} options={chartOptions} />
            </div>
        </div>
    );
}