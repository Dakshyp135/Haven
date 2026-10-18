(function () {
  var base = (window.HAVEN_API_BASE_URL || "").replace(/\/$/, "");
  var $ = function (selector) { return document.querySelector(selector); };
  var setMessage = function (text, error) {
    var node = $("#form-message");
    node.textContent = text;
    node.classList.toggle("error", Boolean(error));
  };
  var api = async function (path, options) {
    options = options || {};
    if (!base) throw new Error("API address is not set. Configure frontend/config.js with your deployed API URL.");
    var response = await fetch(base + path, {
      ...options,
      headers: Object.assign({ "Content-Type": "application/json" }, options.headers || {})
    });
    var data = response.status === 204 ? null : await response.json().catch(function () { return null; });
    if (!response.ok) throw new Error((data && (data.detail || data.message || data.error)) || "The request could not be completed.");
    return data;
  };
  var escapeHtml = function (value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, function (character) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[character];
    });
  };
  var pretty = function (value) {
    return String(value || "").toLowerCase().replace(/\b\w/g, function (letter) { return letter.toUpperCase(); });
  };

  $("#today").textContent = new Intl.DateTimeFormat(undefined, { month: "long", year: "numeric" }).format(new Date());
  $("#booking-form").elements.checkIn.min = new Date().toISOString().slice(0, 10);
  $("#booking-form").elements.checkIn.addEventListener("change", function (event) {
    $("#booking-form").elements.checkOut.min = event.target.value;
  });

  async function loadProperty() {
    try {
      var results = await Promise.all([api("/api/rooms"), api("/api/guests")]);
      var rooms = results[0], guests = results[1];
      $("#connection-label").textContent = "Hotel data connected";
      $("#room-count").textContent = rooms.length;
      $("#available-count").textContent = rooms.filter(function (room) { return room.room_status === "AVAILABLE"; }).length;
      $("#guest-count").textContent = guests.length;
      var grid = $("#rooms-grid");
      grid.innerHTML = rooms.length ? rooms.map(function (room) {
        var status = room.room_status || "UNKNOWN";
        return '<article class="room-card"><div class="room-image"><span class="room-status ' + status.toLowerCase() + '">' + escapeHtml(pretty(status)) + '</span></div><div class="room-body"><h3>' + escapeHtml(room.type_name) + ' · ' + escapeHtml(room.room_number) + '</h3><div class="room-meta"><span>Floor ' + escapeHtml(room.floor_number) + '</span><span>Up to ' + escapeHtml(room.capacity) + ' guests</span></div><div class="room-rate">$' + Number(room.standard_rate).toFixed(2) + ' <small>/ night</small></div></div></article>';
      }).join("") : '<div class="empty-state">No rooms are listed in the connected database yet.</div>';
      var select = $("#room-select");
      select.innerHTML = '<option value="">Choose a room</option>' + rooms.filter(function (room) { return room.room_status !== "MAINTENANCE"; }).map(function (room) {
        return '<option value="' + escapeHtml(room.room_id) + '">' + escapeHtml(room.room_number) + ' · ' + escapeHtml(room.type_name) + ' ($' + Number(room.standard_rate).toFixed(2) + '/night)</option>';
      }).join("");
    } catch (error) {
      $("#connection-label").textContent = base ? "API connection needed" : "API URL not configured";
      $("#rooms-grid").innerHTML = '<div class="empty-state">' + escapeHtml(error.message) + '<br><br>Rooms and guest totals will appear once the backend is deployed and connected.</div>';
      $("#room-count").textContent = "—";
      $("#available-count").textContent = "—";
      $("#guest-count").textContent = "—";
    }
  }

  $("#booking-form").addEventListener("submit", async function (event) {
    event.preventDefault();
    setMessage("");
    var form = new FormData(event.currentTarget);
    var submit = event.currentTarget.querySelector('button[type="submit"]');
    submit.disabled = true;
    submit.textContent = "Saving…";
    try {
      var guest = await api("/api/guests", {
        method: "POST",
        body: JSON.stringify({
          name: form.get("name"),
          email: form.get("email") || null,
          phone: form.get("phone") || null,
          type: form.get("type")
        })
      });
      var reservation = await api("/api/reservations", {
        method: "POST",
        body: JSON.stringify({
          guestId: guest.personId,
          roomId: Number(form.get("roomId")),
          checkIn: form.get("checkIn"),
          checkOut: form.get("checkOut")
        })
      });
      setMessage("Reservation #" + reservation.reservationId + " saved for " + form.get("name") + ".");
      event.currentTarget.reset();
      $("#booking-form").elements.checkOut.min = "";
      await loadProperty();
    } catch (error) {
      setMessage(error.message, true);
    } finally {
      submit.disabled = false;
      submit.innerHTML = 'Save reservation <span>↗</span>';
    }
  });

  loadProperty();
})();
