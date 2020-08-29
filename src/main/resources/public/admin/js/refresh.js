// Setup UI
$(function () {
  $("#tabs-container").tabs();
  $("#datepicker").datepicker();
});

const $_refreshUrl = "/data-sources/refresh/all";
const $_getDataUrl = "/events";
const $headers = {
  'Content-Type': "application/json"
}

function $refreshEventData ($data) {

  $showAjaxSpinner();

  $.ajax({
    method: "POST",
    url: $_refreshUrl,
    headers: $headers,
    data: JSON.stringify($data),
    success: function (data, status) {
      console.log("Refresh:");
      console.log("Data: ", data);
      console.log("Status: ", status);
      $getEventData();
      $hideAjaxSpinner();
    },
    error: function (xhr, data, status) {
      console.log("ERROR!");
      console.log("Data: ", data);
      console.log("Status: ", status);
      console.log("XHR: ", xhr);
      $hideAjaxSpinner();
    }
  });
}

function $getEventData() {

  $.ajax({
    method: "GET",
    url: $_getDataUrl,
    headers: $headers,
    success: function(data, status) {
      console.log("Status", status);
      // Get Events
      let $events = data._embedded.events;
      $formatEventData($events);
    },
    error: function(data, status) {
      console.log("ERROR! Status: ", status);
    }
  });
}

function $formatEventData($events) {

  let eventContainer = $('#events');
  // Clear output
  eventContainer.html('');
  // Container
  let html = '';
  // Iterate over Event objects
  for (const i in $events) {
    let $event = $events[i];
    if ($event.hasOwnProperty("homeTeam")) {
      html += $getMatch($event);
    } else {
      html += $getHighlight($event);
    }
  }
  // Add container to GUI
  eventContainer.html(html);
  // Update total counter
  $('#event-count').text($events.length);
}

function $getMatch($event) {
  let $date = new Date($event.date);
  // $date.to
  return '<div class="event-container">' +
    '<p class="event-title match-title-container">' +
    '<span class="match-title">' +
    `${$event.competition.name}: ${$event.homeTeam.name} vs. ${$event.awayTeam.name}` +
    '</span>' +
    ` &mdash; ${$date.toDateString()}` +
    '</p>' +
    '</div>';
}

function $getHighlight($event) {
  let $result = '';
  if ($event.hasOwnProperty("title")) {
    $result = '<div class="event-container">' +
      `<p>${$event["title"]}</p>` +
      '</div>';
  }
  return $result;
}

function $showAjaxSpinner() {
  // Set button to disabled
  $('.ajax-button').attr('disabled', true);
  $('.ajax-loader').css('visibility', 'visible');
}

function $hideAjaxSpinner() {
  // Set button to disabled
  $('.ajax-button').attr('disabled', false);
  $('.ajax-loader').css('visibility', 'hidden');
}

// Buttons
$('#general-refresh-button').on('click', function () {

  let $data = {
    endDate: "",
    startDate: "",
    fetchBodies: true,
    fetchImages: false,
    maxResults: 50,
    labels: [],
    orderBy: "",
    pageToken: "",
    status: ""
  };

  $refreshEventData($data);
});
$('#label-refresh-button').on('click', function() {
  let $label = $('#refresh-label').val();
  console.log("Label Refresh", $label);
  let $data = {
    endDate: "",
    startDate: "",
    fetchBodies: true,
    fetchImages: false,
    maxResults: 50,
    labels: [$label],
    orderBy: "",
    pageToken: "",
    status: ""
  };

  $refreshEventData($data);
});
$('#date-refresh-button').on('click', function () {

  let $datepicker = $('#datepicker').datepicker('getDate');
  let $date = $datepicker.toISOString().substring(0,19);
  console.log("Date refresh", $date);
  let $data = {
    endDate: $date,
    startDate: "",
    fetchBodies: true,
    fetchImages: false,
    maxResults: 50,
    labels: [],
    orderBy: "",
    pageToken: "",
    status: ""
  };

  $refreshEventData($data);
});
