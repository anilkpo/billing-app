const form = document.getElementById("feeForm");
const tableBody = document.getElementById("feeTableBody");
const message = document.getElementById("message");
const refreshBtn = document.getElementById("refreshBtn");

function todayIso() {
  return new Date().toISOString().split("T")[0];
}

document.getElementById("paymentDate").value = todayIso();

function statusClass(status) {
  const normalized = (status || "").toUpperCase();
  if (normalized === "PAID") return "badge badge-paid";
  if (normalized === "PENDING") return "badge badge-pending";
  return "badge badge-failed";
}

function setMessage(text, isError = false) {
  message.textContent = text;
  message.style.color = isError ? "#ff8e8e" : "#83ffb7";
}

async function loadFees() {
  tableBody.innerHTML = "";
  const response = await fetch("/api/fees");
  if (!response.ok) {
    throw new Error("Failed to load fee records");
  }

  const fees = await response.json();
  if (!fees.length) {
    tableBody.innerHTML = '<tr><td colspan="7">No fee records yet.</td></tr>';
    return;
  }

  fees.sort((a, b) => b.id - a.id).forEach((fee) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${fee.id}</td>
      <td>${fee.studentName}</td>
      <td>${fee.courseName}</td>
      <td>${Number(fee.amount).toFixed(2)}</td>
      <td>${fee.paymentDate}</td>
      <td><span class="${statusClass(fee.status)}">${fee.status}</span></td>
      <td><a class="bill-link" href="/api/fees/${fee.id}/bill" target="_blank">Download PDF</a></td>
    `;
    tableBody.appendChild(row);
  });
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  setMessage("Saving fee entry...");

  const payload = {
    studentName: document.getElementById("studentName").value.trim(),
    studentEmail: document.getElementById("studentEmail").value.trim(),
    courseName: document.getElementById("courseName").value.trim(),
    amount: Number(document.getElementById("amount").value),
    paymentDate: document.getElementById("paymentDate").value,
    status: document.getElementById("status").value
  };

  try {
    const response = await fetch("/api/fees", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || "Failed to save fee entry");
    }

    form.reset();
    document.getElementById("paymentDate").value = todayIso();
    document.getElementById("status").value = "PAID";
    setMessage("Fee entry created successfully.");
    await loadFees();
  } catch (error) {
    setMessage(error.message, true);
  }
});

refreshBtn.addEventListener("click", async () => {
  try {
    await loadFees();
    setMessage("Records refreshed.");
  } catch (error) {
    setMessage(error.message, true);
  }
});

loadFees().catch((error) => setMessage(error.message, true));
