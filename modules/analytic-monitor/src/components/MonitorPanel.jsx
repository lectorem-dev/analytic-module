import { useEffect, useRef, useState } from "react";
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
const WS_BASE = API_BASE.replace(/^http/, "ws").replace(/\/api$/, "/ws/analytics");
const MAX_POINTS = 60;

export default function MonitorPanel() {
    const [apiKey, setApiKey] = useState("secret");
    const [manufactureId, setManufactureId] = useState("");
    const [intervalMs, setIntervalMs] = useState(2000);
    const [connectionMode, setConnectionMode] = useState("rest");

    const [data, setData] = useState(null);
    const [error, setError] = useState(null);
    const [isRunning, setIsRunning] = useState(false);
    const [connectionStatus, setConnectionStatus] = useState("idle");
    const timerRef = useRef(null);
    const socketRef = useRef(null);
    const activeConfigRef = useRef(null);
    const expectedSocketClosuresRef = useRef(new Set());
    const [history, setHistory] = useState([]);

    const applyAnalyticsSnapshot = (analytics, timestamp = new Date()) => {
        setData(analytics);
        setError(null);

        setHistory(prev => {
            const next = [
                ...prev,
                {
                    timestamp,
                    averageRank: Number.parseFloat(analytics.averageRank ?? "0") || 0,
                    globalCount: Number.parseInt(analytics.globalCount ?? "0", 10) || 0,
                    referCount: Number.parseInt(analytics.referCount ?? "0", 10) || 0
                }
            ];
            return next.slice(-MAX_POINTS);
        });
    };

    const handleRequestError = (err) => {
        console.error(err);

        if (err.response) {
            const status = err.response.status;
            if (status === 403) setError("Ошибка 403: доступ запрещён. Проверьте API-ключ.");
            else if (status === 500) setError("Ошибка 500: внутренняя ошибка сервера.");
            else setError(`Ошибка ${status}: ${err.response.statusText}`);
            return;
        }

        if (err.request) {
            setError("Сервер не отвечает. Проверьте подключение или API URL.");
            return;
        }

        setError("Произошла ошибка: " + err.message);
    };

    const fetchAnalytics = async (config) => {
        try {
            const response = await axios.get(`${API_BASE}/${config.manufactureId}/show`, {
                headers: { "X-API-KEY": config.apiKey }
            });
            applyAnalyticsSnapshot(response.data);
        } catch (err) {
            handleRequestError(err);
        }
    };

    const stopActiveConnection = () => {
        if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
        }

        if (socketRef.current) {
            const socket = socketRef.current;
            socketRef.current = null;
            expectedSocketClosuresRef.current.add(socket);

            if (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING) {
                socket.close(1000, "Client stopped connection");
            } else {
                expectedSocketClosuresRef.current.delete(socket);
            }
        }
    };

    const startRestPolling = (config) => {
        setConnectionStatus("polling");
        void fetchAnalytics(config);

        timerRef.current = setInterval(() => {
            void fetchAnalytics(config);
        }, config.intervalMs);
    };

    const startWebSocket = (config) => {
        setConnectionStatus("connecting");

        const url = `${WS_BASE}?manufactureId=${encodeURIComponent(config.manufactureId)}&apiKey=${encodeURIComponent(config.apiKey)}`;
        const socket = new WebSocket(url);
        socketRef.current = socket;

        socket.onopen = () => {
            if (socketRef.current !== socket) {
                return;
            }

            setError(null);
            setConnectionStatus("connected");
        };

        socket.onmessage = (event) => {
            if (socketRef.current !== socket) {
                return;
            }

            try {
                const message = JSON.parse(event.data);
                const analytics = message.analytics ?? message;
                const generatedAt = message.generatedAt ? new Date(message.generatedAt) : new Date();

                applyAnalyticsSnapshot(analytics, generatedAt);
                setConnectionStatus("connected");
            } catch (parseError) {
                console.error(parseError);
                setError("Получено некорректное сообщение WebSocket.");
                setConnectionStatus("error");
            }
        };

        socket.onerror = () => {
            if (socketRef.current !== socket) {
                return;
            }

            setError("Ошибка WebSocket соединения.");
            setConnectionStatus("error");
        };

        socket.onclose = (event) => {
            if (expectedSocketClosuresRef.current.has(socket)) {
                expectedSocketClosuresRef.current.delete(socket);
                return;
            }

            if (socketRef.current === socket) {
                socketRef.current = null;
            }

            setIsRunning(false);
            setConnectionStatus("stopped");

            if (event.code === 1000) {
                return;
            }

            if (event.code === 1006) {
                setError("WebSocket соединение разорвано.");
                return;
            }

            setError(`WebSocket закрыт с кодом ${event.code}.`);
        };
    };

    const startConnection = (config) => {
        stopActiveConnection();
        activeConfigRef.current = config;

        if (config.connectionMode === "ws") {
            startWebSocket(config);
            return;
        }

        startRestPolling(config);
    };

    const handleStart = () => {
        if (!apiKey) return alert("Введите API ключ");
        if (!manufactureId) return alert("Введите manufactureId");

        const config = {
            apiKey,
            manufactureId,
            intervalMs,
            connectionMode
        };

        setIsRunning(true);
        setError(null);
        startConnection(config);
    };

    const handleStop = () => {
        stopActiveConnection();
        activeConfigRef.current = null;
        setIsRunning(false);
        setConnectionStatus("stopped");
    };

    useEffect(() => () => {
        stopActiveConnection();
    }, []);

    useEffect(() => {
        if (!isRunning) {
            return;
        }

        if (!activeConfigRef.current) {
            return;
        }

        const config = {
            ...activeConfigRef.current,
            connectionMode
        };

        startConnection(config);
    }, [connectionMode, isRunning]);

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

    const selectStyle = {
        width: 200,
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

    const statusColor =
        connectionStatus === "connected" || connectionStatus === "polling"
            ? "#0f0"
            : connectionStatus === "error"
                ? "#f00"
                : "#fff";

    const statusText = {
        idle: "Не запущено",
        connecting: "Подключение по WebSocket...",
        connected: "WebSocket подключён",
        polling: "REST polling активен",
        stopped: "Остановлено",
        error: "Ошибка подключения"
    }[connectionStatus];

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
                <label style={labelStyle}>Режим подключения:</label>
                <select
                    value={connectionMode}
                    onChange={(e) => setConnectionMode(e.target.value)}
                    style={selectStyle}
                >
                    <option value="rest">REST polling</option>
                    <option value="ws">WebSocket</option>
                </select>
            </div>

            <div style={formRowStyle}>
                <label style={labelStyle}>Интервал опроса (мс):</label>
                <input
                    type="number"
                    value={intervalMs}
                    onChange={(e) => setIntervalMs(Number(e.target.value))}
                    disabled={connectionMode !== "rest"}
                    style={smallInputStyle}
                />
            </div>

            <div style={{ marginBottom: 10 }}>
                <button onClick={handleStart} style={startButtonStyle}>{isRunning ? "Restart" : "Start"}</button>
                <button onClick={handleStop} style={stopButtonStyle}>Stop</button>
            </div>

            <div style={{ marginBottom: 20, color: statusColor }}>
                Статус: {statusText}
                {connectionMode === "ws" && (
                    <div style={{ marginTop: 8, color: "rgba(255,255,255,0.8)" }}>
                        WebSocket получает только новые обновления после подключения.
                    </div>
                )}
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
