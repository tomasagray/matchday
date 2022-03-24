/*
 * Copyright (c) 2022.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

const $baseUrl = "/file-servers/";

$loadServers = function () {
    $.ajax({
        type: "GET",
        url: $baseUrl + "enabled",
        dataType: "json",
        success: function (data) {
            console.log("Load servers: Success", data);

            // Get data
            let $servers = data._embedded.fileservers;
            // Clear list
            $("#server-list").html("");

            // Add servers to list
            $servers.forEach(function (server) {
                let $listItem =
                    '<li class="server" data-id="' + server.id + '">' +
                    server.title +
                    '</li>';
                $('#server-list').append($listItem);
            });

            // Auto-select first server
            $loadServerUsers($servers[0].id);
            $('#server-title').text($servers[0].title);
            $('#server-list').children().first().addClass("selected");
        },
        error: function (data) {
            console.log("ERROR loading servers!", data);
        }
    });
}

$loadServerUsers = function ($serverId) {

    // Set hidden form field
    $('#server-id').val($serverId);
    // Clear user list
    $('#user-list').html("");

    $.ajax({
        type: "GET",
        url: $baseUrl + "file-server/" + $serverId + "/users",
        dataType: 'json',
        success: function (data) {
            console.log("Load Users", "Success", data);

            // Clear user list
            $('#user-list').html("");
            // Get data
            let $users = data._embedded.users;
            // Add each user to list
            $users.forEach(function ($user) {
                // Determine if logged in checkbox should be checked
                let $checked = ($user.loggedIn === true ? 'checked' : '');
                let $loggedInClass = ($user.loggedIn === true ? ' logged-in' : ' logged-out');
                $('#user-list').append(
                    '<li class="user' + $loggedInClass + '" ' +
                    'data-user-id="' + $user.id + '" ' +
                    'data-server-id="' + $serverId + '" ' +
                    'data-logged-in="' + $user.loggedIn + '" ' +
                    '>' +
                    $user.username +
                    '<input type="checkbox" class="logged-checkbox" ' +
                    $checked + '/>' +
                    '</li>'
                );
            });
        },
        error: function (data) {
            console.log("Load Users", "No users found", data);
            $('#user-list').html(
                '<p style="font-style: italic; color: #666">' +
                'No users for this server' +
                '</p>'
            );
        }
    });
}

// Add user
$loginUser = function ($user) {

    $disableLoginButton();
    console.log("Processing login request", $user);

    let $request = {
        type: 'POST',
        processData: false,
        contentType: false,
        cache: false,
    }
    let $url = $baseUrl + "file-server/" + $user.serverId;
    let $data;

    if (!(shouldUploadCookies())) {
        // Construct URL
        $url += "/login";
        $data = JSON.stringify($user);
        $request.contentType = 'application/json; charset=utf-8';
    } else {
        $url += "/login-with-cookies";
        $data = new FormData($('#login-form')[0]);
        $data.append("username", $user['username']);
        $data.append("password", $user['password']);
    }

    $request.url = $url;
    $request.data = $data;

    // let $jsonData = JSON.stringify($data);
    console.log("URL", $url, "Data", $data);

    // Post login request
    $.ajax($request)
        .done(function (data) {
            console.log("Success: ", data);

            // Update user table
            $loadServerUsers($user.serverId);

            let $message = {
                status: 200,
                responseJSON: data
            };
            displayMessage($message);

            $enableLoginButton();

        }).fail(function (data) {
        displayMessage(data);
        $enableLoginButton();
    });
};

$logoutUser = function ($user, $callback, $args) {

    // Construct URL
    let $url = $baseUrl + "file-server/" + $user.serverId + "/logout";

    $.ajax({
        type: "POST",
        url: $url,
        data: JSON.stringify($user),
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        success: function (data) {
            console.log("User logout SUCCESS", data, $args);
            // Call callback
            $callback($args);
        },
        error: function (data) {
            console.log("User logout FAILURE!", data);
        }
    }).done(function (data, statusText, xhr) {
        displayMessage(xhr);
    });
}

$reloginUser = function ($user, $callback, $args) {

    // Construct URL
    let $url = $baseUrl + "file-server/" + $user.serverId + "/relogin";

    $.ajax({
        type: "POST",
        url: $url,
        data: JSON.stringify($user),
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        success: function (data) {
            console.log("Relogin SUCCESS", data, $args);
            // Call callback
            $callback($args);
        },
        error: function (data) {
            console.log("Relogin FAILED!!!", data);
            displayMessage(data);
        }
    }).done(function (data, statusText, xhr) {
        displayMessage(xhr);
    });
}

$disableLoginButton = function () {

    // Disable login button
    $('#login-button').attr('disabled', true);
    // Show spinner
    $('#login-spinner').css('display', 'inline');

};

$enableLoginButton = function () {
    console.log("Enabling login button..");

    // Hide spinner
    $('#login-spinner').css('display', 'none');
    // Enable login button
    $('#login-button').attr('disabled', false);

};

let shouldUploadCookies = function () {
    // get file upload status
    let cookieFile = $('#cookie-file').val();
    return cookieFile !== "";
};

// Display message modal
function displayMessage(data) {

    console.log("Displaying message", data);
    let $messageContainer = $("#message-container");

    // Determine message type
    switch (data.status) {
        case 200:
            // Remove other class
            $messageContainer.removeClass('error-message');
            $messageContainer.addClass('success-message');
            break;
        case 400:
        case 403:
        case 404:
        case 415:
        case 500:
            // Remove other class
            $messageContainer.removeClass('success-message');
            $messageContainer.addClass('error-message');
            break;
    }

    // Set message text
    let $message = data.responseJSON.message;
    $('#message-text').html($message);

    // Display message
    $messageContainer.css('display', 'flex');
    $messageContainer.animate({
        opacity: 1
    }, 500)
        .delay(2500)
        .animate({
            opacity: 0
        }, {
            duration: 500,
            complete: function () {
                $messageContainer.css('display', 'none');
            }
        });
}