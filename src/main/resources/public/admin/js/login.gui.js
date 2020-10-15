
$setUserLoggedIn = function($user) {
  // Set check box checked
  $user
    .children('input[type="checkbox"]')
    .first()
    .prop("checked", true);
  // Set data
  $user.data("logged-in", true);
  // Set CSS classes
  $user.removeClass('logged-out').addClass('logged-in');
}

$setUserLoggedOut = function($user) {
  // Set check box unchecked
  $user
    .children('input[type="checkbox"]')
    .first()
    .prop("checked", false);
  $user.data("logged-in", false);
  // Set CSS classes
  $user.removeClass('logged-in').addClass('logged-out');
}

$(document).on('click', '.server', function() {
  console.log("Loading Users for Server");

  // Load data
  let $serverId = $(this).data("id");
  $loadServerUsers($serverId);
  // Set title
  $('#server-title').html($(this).html());
  // Set CSS classes
  $('.server').removeClass("selected");
  $(this).addClass("selected");

  console.log("Loaded Users for Server:", $serverId);
});

$(document).on('click', '.user', function() {
  // Get data
  let $userId = $(this).data("user-id");
  let $serverId = $(this).data("server-id");
  let $loggedIn = $(this).data("logged-in");
  console.log("Logged in", $loggedIn);

  // Construct data
  let $user = {
    "userId": $userId,
    "serverId": $serverId
  };
  // Toggle login
  if ($loggedIn === 'true') {
    // Logout
    console.log("Logging out", $user);
    $logoutUser($user, $setUserLoggedOut, $(this));

  } else {
    // Re-login
    console.log("Re-logging in", $user);
    $reloginUser($user, $setUserLoggedIn, $(this));
  }
});

$(document).on('click', '#login-button', function() {

  // Get data
  let $username = $('#username').val();
  let $password = $('#password').val();
  let $serverId = $('#server-id').val();

  // Construct user object
  let $user = {
    "userName": $username,
    "password": $password,
    "serverId": $serverId
  };

  // Perform login request
  $loginUser($user);
});