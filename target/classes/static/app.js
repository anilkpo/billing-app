const form = document.getElementById("feeForm");
const tableBody = document.getElementById("feeTableBody");
const message = document.getElementById("message");
const refreshBtn = document.getElementById("refreshBtn");
const searchInput = document.getElementById("searchInput");
const searchBtn = document.getElementById("searchBtn");

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

async function downloadBill(id) {
  const response = await fetch(`/api/fees/bill?id=${encodeURIComponent(id)}`);
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || "Failed to download PDF");
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `bill-${id}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

async function loadFees(query = "") {
  tableBody.innerHTML = "";
  const endpoint = query.trim() ? `/api/fees?query=${encodeURIComponent(query.trim())}` : "/api/fees";
  const response = await fetch(endpoint);
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
      <td><button class="bill-link" data-id="${fee.id}" type="button">Download PDF</button></td>
    `;
    tableBody.appendChild(row);
  });
}

tableBody.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLElement) || !target.classList.contains("bill-link")) {
    return;
  }

  const id = Number(target.dataset.id);
  if (!Number.isInteger(id)) {
    setMessage("Invalid candidate ID for PDF download.", true);
    return;
  }

  try {
    await downloadBill(id);
    setMessage(`Downloaded bill for candidate ID ${id}.`);
  } catch (error) {
    setMessage(error.message, true);
  }
});

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
    searchInput.value = "";
    await loadFees();
    setMessage("Records refreshed.");
  } catch (error) {
    setMessage(error.message, true);
  }
});

searchBtn.addEventListener("click", async () => {
  try {
    await loadFees(searchInput.value);
    setMessage(searchInput.value.trim() ? "Search complete." : "Showing all records.");
  } catch (error) {
    setMessage(error.message, true);
  }
});

searchInput.addEventListener("keydown", async (event) => {
  if (event.key !== "Enter") {
    return;
  }
  event.preventDefault();
  try {
    await loadFees(searchInput.value);
    setMessage(searchInput.value.trim() ? "Search complete." : "Showing all records.");
  } catch (error) {
    setMessage(error.message, true);
  }
});

loadFees().catch((error) => setMessage(error.message, true));
