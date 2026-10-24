import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../services/api";

export default function Register() {
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      await api.post("/auth/register", {
        name: name.trim(),
        email: email.trim(),
        password,
      });
      navigate("/login", {
        state: { message: "Registration successful. Please log in." },
      });
    } catch (err) {
      setError(
        err.response?.data?.message ||
        "Registration failed. Please try again."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-container">
      <h1>Create an account</h1>
      <form onSubmit={handleSubmit}>
        <label htmlFor="name">Full name</label>
        <input id="name" value={name} onChange={e => setName(e.target.value)}
          required maxLength={100} autoComplete="name" />

        <label htmlFor="email">Email address</label>
        <input id="email" type="email" value={email}
          onChange={e => setEmail(e.target.value)}
          required maxLength={255} autoComplete="email" />

        <label htmlFor="password">Password</label>
        <input id="password" type="password" value={password}
          onChange={e => setPassword(e.target.value)}
          required minLength={8} autoComplete="new-password" />

        {error && <p role="alert">{error}</p>}
        <button type="submit" disabled={loading}>
          {loading ? "Creating account..." : "Register"}
        </button>
      </form>
      <p>Already have an account? <Link to="/login">Log in</Link></p>
    </main>
  );
}
