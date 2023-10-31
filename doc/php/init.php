<?php 


use Thedudeguy\Rcon;

$host = '127.0.0.1';       // Server host name or IP.  
$port = 25575;             // Port rcon is listening on
$password = 'your_rcon_password'; // rcon.password setting set in server.properties
$timeout = 3;              // How long to timeout from RCON.  Perform actions quickly and disconnect.
$rcon = new Rcon($host, $port, $password, $timeout);

