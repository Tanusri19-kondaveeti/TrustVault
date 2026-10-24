
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

export default function AdminActivity() {
  const [logs, setLogs] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    api
      .get("/admin/audit-logs")
      .then((response) => {
        setLogs(response.data);
      })
      .catch((err) => {
        setError(
          err.response?.status === 403
            ? "You do not have permission to view audit logs."
            : "Could not load audit logs. Please try again."
        );
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <main style={{ padding: "30px", fontFamily: "Arial" }}>
      <button onClick={() => navigate("/dashboard")}>
        Back to Dashboard
      </button>

      <h1>TrustVault Admin Activity</h1>
      <p>Review document activity and access attempts.</p>

      {loading && <p>Loading audit logs...</p>}
      {error && <p style={{ color: "red" }}>{error}</p>}

      {!loading && !error && (
        <div style={{ overflowX: "auto" }}>
          <table
            style={{
              width: "100%",
              borderCollapse: "collapse",
              marginTop: "20px",
            }}
          >
            <thead>
              <tr>
                {[
                  "Actor",
                  "Document ID",
                  "Action",
                  "Outcome",
                  "Details",
                  "Timestamp",
                ].map((heading) => (
                  <th
                    key={heading}
                    style={{
                      border: "1px solid #ddd",
                      padding: "10px",
                      textAlign: "left",
                      background: "#f2f2f2",
                    }}
                  >
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td style={cellStyle}>
                    {log.actor?.email ?? "Unknown"}
                  </td>
                  <td style={cellStyle}>
                    {log.document?.id ?? "—"}
                  </td>
                  <td style={cellStyle}>{log.action}</td>
                  <td
                    style={{
                      ...cellStyle,
                      color:
                        log.outcome === "DENIED" ? "#c62828" : "#237a3b",
                    }}
                  >
                    {log.outcome}
                  </td>
                  <td style={cellStyle}>{log.details ?? "—"}</td>
                  <td style={cellStyle}>
                    {log.createdAt
                      ? new Date(log.createdAt).toLocaleString()
                      : "—"}
                  </td>
                </tr>
              ))}

              {logs.length === 0 && (
                <tr>
                  <td colSpan="6" style={cellStyle}>
                    No audit records found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </main>
  );
}

const cellStyle = {
  border: "1px solid #ddd",
  padding: "10px",
};
