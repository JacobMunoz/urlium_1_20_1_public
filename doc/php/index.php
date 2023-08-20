<?php 
/*
 Sample PHP RCON Script for URLium Fabric Mod

    This is a script that would receive data from the URLium 1.0.2 Fabric Mod 
        and respond back to the server using RCON with custom commands.

    Be sure that your Minecraft Server's RCON port is open to the webserver.
    Preferrably served on localhost for security, or at minimum - local network.

    Consider timing and confirming timestamps.  
    The same data may be sent in multiple requests, as MC server changes/saves state frequently.
    For important events, record and compare timestamps to ensure latest requests are processed and delayed requests are dropped.

    Item blockstate and inventory data is roughly formatted, but not strictly JSON.  
        This will be improved in future versions, mod config will specify format in the future (JSON, XML, CSV).


 * @copyright 2023 Jacob Munoz
 * @author ElectricShmoo
 * @link https://github.com/JacobMunoz/urlium_1_20_1_public

*/

use Thedudeguy\Rcon;
include_once("rcon.php");

$host = '127.0.0.1';       // Server host name or IP
$port = 25575;             // Port rcon is listening on
$password = 'rcon_password_here'; // rcon.password setting set in server.properties
$timeout = 3;              // How long to timeout from RCON.  Perform actions quickly and disconnect.

/*

// If you fear your script will take a long time to complete, consider calling this script as a server background process.
// These four lines in a separate PHP script would receive a request, save it, pass it to this main processing script, and return immediately.
// While it is not necessary, it would relieve the URLium mod from having to keep connections open while running this script.
// URLium treats the response async, so there should be no lag - but it is best to respond quickly.
// You could use a combination of rapid response with background process as well.

// proxy index.php:
$ts = intval($_REQUEST['ts']);
file_put_contents($ts.'.req', json_encode($_REQUEST));
exec("php processor.php " . $ts . " >> /dev/null & "); //spawn background thread so response is immediate
echo "HIT THE SERVER with ".json_encode($_REQUEST); //optional response message to log in MC server log


// ...then use these request/requestString values in this script instead.

$ts = $argv[1];
$requestString = file_get_contents($ts.'.req');
$request = json_decode( $requestString, true);

*/
$requestString = print_r($_REQUEST,true);
$request = $_REQUEST; 


// --Customize your script here:

$device = $request['device']; // mandatory field
$x = intval($request['x']); // common field
$y = intval($request['y']); // common field
$z = intval($request['z']); // common field

$rcon = new Rcon($host, $port, $password, $timeout);

if ($rcon->connect()) {
    switch($device) {
        case "block":
            $power = intval($request['p']); // get redstone power
            $radius = 10;

            if ( $power  ) {
//when an URLium block is powered, create a sphere of glowstone 25 blocks above, radius 10
                for ($angleT = 0 ; $angleT<180; $angleT += 5){
                    for ($angleU = 0 ; $angleU<360; $angleU += 5){
                        $circX = $x + (int)($radius * sin(deg2rad($angleT)) * cos(deg2rad($angleU)));
                        $circY = $y + 25 + (int)($radius * cos(deg2rad($angleT)));
                        $circZ = $z + (int)($radius * sin(deg2rad($angleT)) * sin(deg2rad($angleU)));
                        $command = "/setblock ".$circX." ".$circY." ".$circZ." minecraft:glowstone replace";
                        $rcon->sendCommand($command);
                        usleep(2000); 
                    }
                }
            } else {
//when an URLium block is unpowered, clear the sphere with air
                for ($angleT = 0 ; $angleT<180; $angleT += 5){
                    for ($angleU = 0 ; $angleU<360; $angleU += 5){
                        $circX = $x + (int)($radius * sin(deg2rad($angleT)) * cos(deg2rad($angleU)));
                        $circY = $y + 25 + (int)($radius * cos(deg2rad($angleT)));
                        $circZ = $z + (int)($radius * sin(deg2rad($angleT)) * sin(deg2rad($angleU)));
                        $command = "/setblock ".$circX." ".$circY." ".$circZ." minecraft:air replace";
                        $rcon->sendCommand($command);
                        usleep(2000);
                    }
                }
            }
            break;

        case "chest":
                $blockdata = $request['inventory']; //read inventory data
                $command = "/say A PHP script received inventory for ".$x." ".$y." ".$z." = ".$blockdata;
                $rcon->sendCommand($command);
            break;


        case "message":
            $username = $request['user'];
            $message = $request['message'];
                $command = "/say A PHP script received this message from user ".$username.": ".$message;
                $rcon->sendCommand($command);
            break;

        case "wand":
                $power = intval($request['p']);

                $command = "/ugetblock ".$x." ".$y." ".$z;
                $blockstring = $rcon->sendCommand($command);

                $command = "/say A PHP script received block data for ".$x." ".$y." ".$z;
                $rcon->sendCommand($command);

                if ( $power  ) {
// when a powered block is clicked with a wand, turn it into diamond_block
                    $command = "/setblock ".$x." ".$y." ".$z." minecraft:diamond_block replace";

// Consider a function that builds structres based on location and parameters: 
//                  $sequence = createVillageHouse( $x, $y, $z, 5, 5, 5);
                    //foreach ($sequence as $command) {

                        $rcon->sendCommand($command);
                        usleep(1500);

                    //}

                } else {
// when an unpowered block is clicked with a wand, turn it into gold
                    $command = "/setblock ".$x." ".$y." ".$z." minecraft:gold_block replace";
                    $rcon->sendCommand($command);
                    usleep(2000);
                }
            break;
        case "oak_sign":
                $command = "/say A PHP script received data from an oak_sign: ".$requestString;
                $rcon->sendCommand($command);
            break;
        default:
                $command = "/say A PHP script received data from a: ".$device;
                $rcon->sendCommand($command);
            break;
    }
}


function playerPosition( &$rconConn, $playerName) {
    $rawoutput = $rconConn->sendCommand("/data get entity @p[name=".$playerName."] Pos");
    $rpone = explode("[",$rawoutput); // trim,
    $rptwo = explode("]",$rpone[1]);  // trim,
    $rpthree = str_replace(',','',$rptwo[0]); // clean,
    $rpfour = explode(" ",$rpthree); // split into x y z
    return $rpfour;
};


/* This function was written by ChatGPT... so it's both stupidly horrible and terrifyingly amazing */

function createVillageHouse($x, $y, $z, $height, $width, $depth) {
    $commands = [];

    // Build the foundation (wooden planks)
    for ($i = 0; $i < $width; $i++) {
        for ($j = 0; $j < $depth; $j++) {
            $commands[] = "/setblock " . ($x + $i) . " $y " . ($z + $j) . " minecraft:oak_planks";
        }
    }

    // Build the walls (wooden logs)
    for ($i = 0; $i < $height; $i++) {
        for ($j = 0; $j < $width; $j++) {
            $commands[] = "/setblock " . ($x + $j) . " " . ($y + $i) . " $z minecraft:oak_log[axis=y]";
            $commands[] = "/setblock " . ($x + $j) . " " . ($y + $i) . " " . ($z + $depth - 1) . " minecraft:oak_log[axis=y]";
        }
        for ($j = 0; $j < $depth; $j++) {
            $commands[] = "/setblock $x " . ($y + $i) . " " . ($z + $j) . " minecraft:oak_log[axis=y]";
            $commands[] = "/setblock " . ($x + $width - 1) . " " . ($y + $i) . " " . ($z + $j) . " minecraft:oak_log[axis=y]";
        }
    }

    // Build the roof (wooden stairs)
    for ($i = 0; $i < $width; $i++) {
        $commands[] = "/setblock " . ($x + $i) . " " . ($y + $height) . " $z minecraft:oak_stairs[facing=east,half=bottom]";
        $commands[] = "/setblock " . ($x + $i) . " " . ($y + $height) . " " . ($z + $depth - 1) . " minecraft:oak_stairs[facing=west,half=bottom]";
    }
    for ($i = 0; $i < $depth; $i++) {
        $commands[] = "/setblock $x " . ($y + $height) . " " . ($z + $i) . " minecraft:oak_stairs[facing=north,half=bottom]";
        $commands[] = "/setblock " . ($x + $width - 1) . " " . ($y + $height) . " " . ($z + $i) . " minecraft:oak_stairs[facing=south,half=bottom]";
    }

    // Build the door (wooden door)
    $commands[] = "/setblock " . ($x + $width / 2) . " $y " . ($z + $depth - 1) . " minecraft:oak_door[hinge=left,facing=south,half=lower]";
    $commands[] = "/setblock " . ($x + $width / 2) . " " . ($y + 1) . " " . ($z + $depth - 1) . " minecraft:oak_door[hinge=left,facing=south,half=upper]";

    // Add a torch (torches)
    $commands[] = "/setblock " . ($x + $width / 2) . " " . ($y + $height + 1) . " " . ($z + $depth / 2) . " minecraft:torch";

    // Add a window (glass panes)
    for ($i = 0; $i < $height - 1; $i++) {
        $commands[] = "/setblock " . ($x + $width - 1) . " " . ($y + $i) . " " . ($z + $depth / 2) . " minecraft:glass_pane";
    }

    // Floor (wooden planks)
    for ($i = 0; $i < $width; $i++) {
        for ($j = 0; $j < $depth; $j++) {
            $commands[] = "/setblock " . ($x + $i) . " $y " . ($z + $j) . " minecraft:oak_planks";
        }
    }

    return $commands;
}

