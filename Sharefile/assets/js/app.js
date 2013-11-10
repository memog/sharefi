var sessionToken = false;

$(document).ready(function(){
    $.support.cors = true;

    var loggedIn = false;
    if(view == 'settings' || view == 'profile'){
        sessionToken = getToken();
        loggedIn = validateToken(sessionToken);
        if(!loggedIn){
            changeLocation('login.html');
        }
        if(view == 'settings'){
            putSettings();
        }
    }else if(view == 'searching'){
        startHotspotTimeout();
        if(Android && Android.uiRequestWifiShare)Android.uiRequestWifiShare();
    }else if(view = 'connected'){
        setConnectedVars();
    }else if(view == 'login'){
        sessionToken = getToken();
        loggedIn = validateToken(sessionToken);
        if(loggedIn){
            changeLocation('profile.html');
        }
    }
});

function getToken(){
    return localStorage.getItem('token');
}

function saveToken(token){
    localStorage.setItem('token',token);
    return true;
}

function setPoints(points){
    localStorage.setItem('points',points);
    if(view == 'profile'){
        $('#profile-points').html(points);
    }
    return true;
}

function getPoints(){
    return localStorage.getItem('points');
}

function validateToken(token){
    $.ajax({
        type: "POST",
        url: "http://sharefi.net/api/account/validate.php",
        data:{token: token}
    }).done(function(resp){
            var obj = $.parseJSON(resp);
            setPoints(obj.user.points);
            return true;
    }).error(function(){
        loginError();
    });
    return true;
}

function callLogin(){
    $('#login-container').hide();
    $('#loading-container').show();
    var email = $('#inputEmail').val();
    var password = $('#inputPassword').val();
    return login(email, password);
}

function login(email, password){
    $.ajax({
        type: "POST",
        url: "http://sharefi.net/api/account/authenticate.php",
        cache: false,
        crossDomain: true,
        data:{email: email, password:password}
    }).done(function(resp) {
            var obj = $.parseJSON(resp);
            setPoints(obj.user.points);
            saveToken(obj.token);
            localStorage.setItem('user',resp);
            changeLocation('profile.html');
    }).error(function(){
        loginError();
    });
}

function loginError(){
    if(view != 'login'){
        changeLocation('login.html');
    }else{
        $('#loading-container').hide();
        $('#login-container').show();
        $('#inputPassword').val('');
        $('#login-error').show(500);
        setTimeout(function(){$('#login-error').hide(500);}, 3000);
    }
    return true;
}

function logout(){
    $.ajax({
        type: "POST",
        url: "http://sharefi.net/api/account/logout.php",
        data:{token: localStorage.getItem('token')}
    }).done(function(){
            localStorage.removeItem('token');
            changeLocation('login.html');
        }).error(function(){
            localStorage.removeItem('token');
            changeLocation('login.html');
        });
    return true;
}

function changeLocation(location){
    window.location = location;
}
var timeout = false;
function findHotspot(){
    changeLocation('searching.html');
    return true;
}

function startHotspotTimeout(){
    timeout = setTimeout(function(){hotspotNotFound();}, 10000);
    return true;
}

function clearHotspotTimeout(){
    clearTimeout(timeout);
    return true;
}

function getCoupon(points){
    if(parseInt(getPoints()) > points){
        $('#alert-success').show(500);
        setTimeout(function(){$('#alert-success').hide(500);}, 5000);
        addUserPoints(points*-1);
        return false;
    }else{
        $('#alert-fail').show(500);
        setTimeout(function(){$('#alert-fail').hide(500);}, 2000);
        return false;
    }
}

function addUserPoints(points){
    $.ajax({
        type: "POST",
        url: "http://sharefi.net/api/users/addPoints.php",
        data:{token: localStorage.getItem('token'), points: points}
    }).done(function(resp){
            var obj = $.parseJSON(resp);
            setPoints(obj.user.points);
            return true;
        }).error(function(){
            return false;
        });
    return true;
}

function saveSettings(){
    var battery = $('#settings-val-battery option:selected').val();
    var time = $('#settings-val-time option:selected').val();
    var data = $('#settings-val-data option:selected').val();
    localStorage.setItem('val-battery',  battery);
    localStorage.setItem('val-time',  time);
    localStorage.setItem('val-data',  data);
    $('#alert-success').show(500);
    setTimeout(function(){$('#alert-success').hide(500);}, 3000);
    return true;
}

function putSettings(){
    var battery = localStorage.getItem('val-battery');
    var time = localStorage.getItem('val-time');
    var data = localStorage.getItem('val-data');
    if(!battery > 0) battery = 30;
    if(!time > 0) time = 15;
    if(!data > 0) data = 30;
    $('#settings-val-battery').val(battery);
    $('#settings-val-time').val(time);
    $('#settings-val-data').val(data);
    return true;
}

function setConnectedVars(){
    $('#connection-time').html(localStorage.getItem('total-time')+':00');
    $('#connection-data').html(localStorage.getItem('total-data'));
    var timestamp = new Date().getTime();
    var remainingTime = parseInt(localStorage.getItem('end-time')) - timestamp;
    var remainingData = parseInt(localStorage.getItem('total-data'));
    remainingData -= remainingData * 0.12345;
    remainingData = (Math.round(remainingData*100)/100).toFixed(2);
    remainingTime = parseInt(remainingTime/1000);
    if(remainingTime <= 0){
        disconnet();
    }else{
        var min = Math.floor(remainingTime / 60);
        var seg = remainingTime % 60;
        if(seg < 10){seg = '0'+seg;}
        $('#connection-time-left').html(min+':'+seg);
        $('#connection-data-left').html(remainingData);
    }
    setTimeout(function(){setConnectedVars();}, 1000);
    return true;
}

// functions called from android
function hotspotFound(time, data){
    var timestamp = new Date().getTime();
    localStorage.setItem('total-time', time);
    localStorage.setItem('end-time', timestamp+time*60*1000);
    localStorage.setItem('total-data', data);
    localStorage.setItem('remaining-data', data*100);
    clearHotspotTimeout();
    changeLocation('connected.html');
    return true;
}

function hotspotNotFound(){
    clearHotspotTimeout();
    changeLocation('notConnected.html');
    return true;
}

function disconnet(){
    localStorage.removeItem('total-time');
    localStorage.removeItem('end-time');
    localStorage.removeItem('total-data');
    localStorage.removeItem('remaining-data');
    changeLocation('thankYou.html');
}

function messageFromAndroid(eventName,data){

}