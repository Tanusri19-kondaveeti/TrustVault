
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

function DocumentActions({ document, onMessage }) {
  const [recipientEmail, setRecipientEmail] = useState("");
  const [permission, setPermission] = useState("READ");
  const [recipientId, setRecipientId] = useState("");
  const [sharing, setSharing] = useState(false);
  const [revoking, setRevoking] = useState(false);
  const navigate = useNavigate();

  const handleShare = async (event) => {
    event.preventDefault();
    onMessage("");

    if (!recipientEmail.trim()) {
      onMessage("Enter the recipient's email address.");
      return;
    }

    setSharing(true);

    try {
      const response = await api.post(
        `/documents/${document.id}/share`,
        {
          recipientEmail: recipientEmail.trim(),
          permission,
        }
      );

      onMessage(
        response.data?.message ||
          `Document "${document.filename}" shared successfully.`
      );

      setRecipientEmail("");
      setPermission("READ");
    } catch (err) {
      if (err.response?.status === 401) {
        onMessage("Your session expired. Please log in again.");
      } else if (err.response?.status === 403) {
        onMessage("Only the document owner can share this document.");
      } else {
        onMessage(
          err.response?.data?.message ||
            "Sharing failed. Check the recipient email and try again."
        );
      }
    } finally {
      setSharing(false);
    }
  };

  const handleRevoke = async (event) => {
    event.preventDefault();
    onMessage("");

    const id = recipientId.trim();

    if (!/^[1-9]\d*$/.test(id)) {
      onMessage("Enter a valid recipient user ID.");
      return;
    }

    if (!window.confirm(
      `Revoke access to "${document.filename}" for user ID ${id}?`
    )) {
      return;
    }

    setRevoking(true);

    try {
      await api.delete(
        `/documents/${document.id}/share/${id}`
      );

      onMessage(
        `Access revoked successfully for user ID ${id}.`
      );

      setRecipientId("");
    } catch (err) {
      if (err.response?.status === 401) {
        onMessage("Your session expired. Please log in again.");
      } else if (err.response?.status === 403) {
        onMessage("Only the document owner can revoke access.");
      } else {
        onMessage(
          err.response?.data?.message ||
            "Revocation failed. Check the document and recipient IDs."
        );
      }
    } finally {
      setRevoking(false);
    }
  };

  return (
    <div style={{ minWidth: 240 }}>
      <form
        onSubmit={handleShare}
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 8,
          marginBottom: 16,
        }}
      >
        <strong>Share document</strong>

        <input
          type="email"
          value={recipientEmail}
          onChange={(event) => setRecipientEmail(event.target.value)}
          placeholder="Recipient email"
          required
          maxLength={255}
        />

        <select
          value={permission}
          onChange={(event) => setPermission(event.target.value)}
        >
          <option value="READ">READ — View and download</option>
          <option value="EDIT">EDIT — Edit permission</option>
        </select>

        <button type="submit" disabled={sharing}>
          {sharing ? "Sharing..." : "Share"}
        </button>
      </form>

      <form
        onSubmit={handleRevoke}
        style={{
          display: "flex",
          flexDirection: "column",
          gap: 8,
        }}
      >
        <strong>Revoke access</strong>

        <input
          type="number"
          min="1"
          step="1"
          value={recipientId}
          onChange={(event) => setRecipientId(event.target.value)}
          placeholder="Recipient user ID"
          required
        />

        <button type="submit" disabled={revoking}>
          {revoking ? "Revoking..." : "Revoke access"}
        </button>
      </form>
    </div>
  );
}

export default function Dashboard() {
  const navigate = useNavigate();

  const [documents, setDocuments] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);
  const [message, setMessage] = useState("");

  const loadDocuments = async (query = "") => {
    setLoading(true);
    setError("");

    try {
      const response = await api.get("/documents", {
        params: query ? { search: query } : {},
      });

      setDocuments(response.data.documents ?? []);
    } catch (err) {
      if (err.response?.status === 401) {
        sessionStorage.clear();
        navigate("/login");
        return;
      }

      setError(
        err.response?.data?.message ||
          "Unable to load documents. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async (event) => {
    event.preventDefault();
    setMessage("");

    if (!selectedFile) {
      setMessage("Please select a file first.");
      return;
    }

    const form = event.currentTarget;
    const formData = new FormData();
    formData.append("file", selectedFile);

    setUploading(true);

    try {
      await api.post("/documents", formData);

      setMessage("Document uploaded successfully!");
      setSelectedFile(null);
      form.reset();

      await loadDocuments(search.trim());
    } catch (err) {
      if (err.response?.status === 401) {
        sessionStorage.clear();
        navigate("/login");
        return;
      }

      setMessage(
        err.response?.data?.message ||
          "Upload failed. Please try again."
      );
    } finally {
      setUploading(false);
    }
  };

  const handleDownload = async (document) => {
    setDownloadingId(document.id);
    setMessage("");

    try {
      const response = await api.get(
        `/documents/${document.id}/download`,
        { responseType: "blob" }
      );

      const blobUrl = window.URL.createObjectURL(response.data);
      const link = window.document.createElement("a");

      link.href = blobUrl;
      link.download =
        document.filename || `document-${document.id}`;

      window.document.body.appendChild(link);
      link.click();
      link.remove();

      setTimeout(() => {
        window.URL.revokeObjectURL(blobUrl);
      }, 1000);
    } catch (err) {
      if (err.response?.status === 401) {
        sessionStorage.clear();
        navigate("/login");
        return;
      }

      setMessage(
        err.response?.status === 403
          ? "You do not have permission to download this document."
          : "Download failed. Please try again."
      );

      console.error("Download error:", err);
    } finally {
      setDownloadingId(null);
    }
  };

  useEffect(() => {
    loadDocuments();
  }, []);

  const handleLogout = () => {
    sessionStorage.clear();
    navigate("/login");
  };

  return (
    <main style={{ maxWidth: 1200, margin: "40px auto", padding: 20 }}>
      <header
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          gap: 16,
          flexWrap: "wrap",
        }}
      >
        <div>
          <h1>TrustVault</h1>
          <p>Your secure document workspace</p>
        </div>
        {sessionStorage.getItem("role") === "ADMIN" && (
  <button onClick={() => navigate("/admin/activity")}>
    View Admin Activity Logs
  </button>
)}
        <button onClick={handleLogout}>Logout</button>
      </header>

      <hr />

      <section style={{ marginBottom: 28 }}>
        <h2>Upload a Document</h2>

        <form onSubmit={handleUpload}>
          <input
            type="file"
            accept=".pdf,.jpg,.jpeg,.png,.txt"
            onChange={(event) =>
              setSelectedFile(event.target.files?.[0] || null)
            }
          />

          <button type="submit" disabled={uploading}>
            {uploading ? "Uploading..." : "Upload"}
          </button>
        </form>
      </section>

      {message && (
        <p role="status" aria-live="polite">
          {message}
        </p>
      )}

      <section>
        <h2>My Documents</h2>

        <form
          onSubmit={(event) => {
            event.preventDefault();
            loadDocuments(search.trim());
          }}
          style={{
            display: "flex",
            gap: 8,
            marginBottom: 20,
            flexWrap: "wrap",
          }}
        >
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search by filename"
            style={{ padding: 10, flex: 1 }}
          />

          <button type="submit">Search</button>

          <button
            type="button"
            onClick={() => {
              setSearch("");
              loadDocuments("");
            }}
          >
            Clear
          </button>
        </form>

        {loading && <p>Loading documents...</p>}
        {error && <p style={{ color: "crimson" }}>{error}</p>}

        {!loading && !error && documents.length === 0 && (
          <p>No documents found. Upload a document to get started.</p>
        )}

        {!loading && documents.length > 0 && (
          <div style={{ overflowX: "auto" }}>
            <table
              style={{
                width: "100%",
                borderCollapse: "collapse",
                textAlign: "left",
              }}
            >
              <thead>
                <tr>
                  <th style={{ padding: 12 }}>Filename</th>
                  <th style={{ padding: 12 }}>Access</th>
                  <th style={{ padding: 12 }}>Size</th>
                  <th style={{ padding: 12 }}>Download</th>
                  <th style={{ padding: 12 }}>Sharing</th>
                </tr>
              </thead>

              <tbody>
                {documents.map((document) => (
                  <tr key={document.id}>
                    <td style={{ padding: 12 }}>
                      {document.filename}
                    </td>

                    <td style={{ padding: 12 }}>
                      {document.accessType}
                    </td>

                    <td style={{ padding: 12 }}>
                      {document.fileSize != null
                        ? `${(document.fileSize / 1024).toFixed(1)} KB`
                        : "—"}
                    </td>

                    <td style={{ padding: 12 }}>
                      <button
                        onClick={() => handleDownload(document)}
                        disabled={downloadingId === document.id}
                      >
                        {downloadingId === document.id
                          ? "Downloading..."
                          : "Download"}
                      </button>
                    </td>

                    <td style={{ padding: 12 }}>
                      {document.accessType === "OWNER" ? (
                        <DocumentActions
                          document={document}
                          onMessage={setMessage}
                        />
                      ) : (
                        <span>Owner manages access</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <button
          onClick={() => loadDocuments(search.trim())}
          disabled={loading}
          style={{ marginTop: 16 }}
        >
          Refresh documents
        </button>
      </section>
    </main>
  );
}
