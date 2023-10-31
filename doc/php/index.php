<?php 
/*
 Sample PHP RCON Script for URLium Fabric Mod

    This is a script that would receive data from the URLium 1.2.0 Fabric Mod 
        and respond back to the server using RCON with custom commands.

    Be sure that your Minecraft Server's RCON port is open to the webserver.

    Preferrably served on localhost for security (prevent outside hits) - protect/firewall as needed.

    
 * @copyright 2023 Jacob Munoz
 * @author ElectricShmoo
 * @link https://github.com/JacobMunoz/urlium_1_20_1_public

    This example offers users "Wand Modes"  (Corral, Trap, and Select)

    The user runs "/webcom corral Cow pen" to create a deposit-point for Cows, and creates an 8x8 pen for them 
    The user runs "/webcom corral Chicken" to create a deposit-point for Chickens

    ...with the user in this "Corral Mode", the user can hit animals with the Post Wand and /tp them to the correct corral

    The user runs "/webcom trap" to set "Trap Mode"...
    ...with the user in "Trap Mode", using the Post Wand on an entity will create a small pen to trap the mob.

    The user runs "/webcom select" to set "Select Mode"...
    ...with the user in "Select Mode", using the Post Wand on an entity will enable the glowing effect on the mob.

*/
use Thedudeguy\Rcon;

include_once("rcon.php");
include_once("init.php");

$device = $_REQUEST['device'];

if ($rcon->connect()) {
    switch($device) {
        case "message":
            $username = filterNonAlphanumeric($_REQUEST['user']);
            $message = $_REQUEST['message'];
            $instructions = explode(' ', $message);

            switch($instructions[0]){
                case "corral":
                    if (!is_dir("usermode")) mkdir("usermode");
                    if (!is_dir("corral")) mkdir("corral");
                    if (!is_dir("corral/".$username)) mkdir("corral/".$username);

                    setUserMode("corral");

                    if (isset($instructions[1])) {

                        $animal = filterNonAlphanumeric($instructions[1]);

                        $coordinates = playerPosition( $rcon, $username);

                        $coordinateString = implode(" ", $coordinates);

                        file_put_contents("corral/" . $username . "/" . $animal, $coordinateString);

                        $response = 'User: '.$_REQUEST['user'].' created a corral for: '.$animal.' at: '.$coordinateString;
                        $command = '/tellraw '.$username.' {"text":'.json_encode($response).',"color":"aqua"}'; 
                        $rcon->sendCommand($command);
                        usleep(2000);

                        if (isset($instructions[2])) {
     
                            switch($instructions[2]) {
                                case "pen":
                                    
                                    $sequence = createAnimalPen( $coordinates[0], $coordinates[1], $coordinates[2], 8, 8);
                                    foreach ($sequence as $command) {
                                        $rcon->sendCommand($command);
                                        usleep(2000);
                                    }
                                    break;
                            }
                        }
                    }
                    break;

                case "trap":
                    setUserMode("trap");
                    break;

                case "select":
                    setUserMode("select");
                    break;
            }
            break;

        case "entity":
                $username = '@a';
                $response = "Interacted with entity: ".$_REQUEST['entitytype'].' = '.print_r($_REQUEST,true);
                $command = '/tellraw '.$username.' {"text":'.json_encode($response).',"color":"white"}'; 
                $rcon->sendCommand($command);
            break;

        case "configwand":
                $username = $_REQUEST['user'];
                $response = "received: ".print_r($_REQUEST,true);
                $command = '/tellraw '.$username.' {"text":'.json_encode($response).',"color":"gold"}'; 
                $rcon->sendCommand($command);
            break;

        case "wand":
            switch( getUserMode() ) {
                case "corral":
                    switch($_REQUEST['target']) {
                        case "entity":

                            $x = intval($_REQUEST['x']);
                            $y = intval($_REQUEST['y']);
                            $z = intval($_REQUEST['z']);
                            $username = $_REQUEST['user'];
                            $animal = $_REQUEST['entitytype'];

                            if (file_exists("corral/" . $username . "/" . $animal)) {
                                $command = "/tp ".$_REQUEST['entityid']." ".file_get_contents("corral/" . $username . "/" . $animal);
                                $rcon->sendCommand($command);
                                usleep(2000);

                                $command = "/playsound minecraft:block.shroomlight.break master ".$username." ".$x." ".$y." ".$z;
                                $rcon->sendCommand($command);
                            } else {
                                $response = "There is no corral for: ".$animal;
                                $command = '/tellraw '.$username.' {"text":'.json_encode($response).',"color":"gold"}'; 
                                $rcon->sendCommand($command);
                            }
                            break;
                    }
                    break; //end of 'corral' mode

                case "trap":
                    switch($_REQUEST['target']) {
                        case "entity":
                            $x = intval($_REQUEST['x']);
                            $y = intval($_REQUEST['y']);
                            $z = intval($_REQUEST['z']);
                            $sequence = createAnimalPen( $x, $y, $z, 4, 4);
                            foreach ($sequence as $command) {
                                $rcon->sendCommand($command);
                                usleep(2000);
                            }
                            break; // end of entity interaction
                    }
                    break; //end of 'trap' mode
                case "select":
                    switch($_REQUEST['target']) {
                        case "entity":
                            $x = intval($_REQUEST['x']);
                            $y = intval($_REQUEST['y']);
                            $z = intval($_REQUEST['z']);

                            $command = '/effect give '.$_REQUEST['entityid'].' minecraft:glowing'; 
                            $rcon->sendCommand($command);

                            break; // end of entity interaction
                        case "block":
                            $response = "Detected wand on block: ".print_r($_REQUEST,true);
                            $command = '/tellraw @a {"text":'.json_encode($response).',"color":"green"}'; 
                            $rcon->sendCommand($command);
                            break;
                    }
                    break; //end of 'select' mode

                default:
                    $response = "No wand mode set.";
                    $command = '/title '.$username.' title {"text":'.json_encode($response).',"color":"red"}'; 
                    $rcon->sendCommand($command);
                    break;
            } // close switch getUserMode
            break;

        case "block":
            $x = intval($_REQUEST['x']);
            $y = intval($_REQUEST['y']);
            $z = intval($_REQUEST['z']);
            $username = $_REQUEST['user'];

            // shroomlight.break
            $command = '/playsound minecraft:block.amethyst_block.hit master '.$username.' '.$x.' '.$y.' '.$z;
            $rcon->sendCommand($command);
            usleep(2000);
            break;

        default: // handle all other 'devices' by printing via RCON
            $response = "Detected: ".print_r($_REQUEST,true);
            $command = '/tellraw @a {"text":'.json_encode($response).',"color":"white"}'; 
            $rcon->sendCommand($command);
            break;
    }
}

function filterNonAlphanumeric($inputString) {
    $filteredString = preg_replace("/[^a-zA-Z0-9_]/", '', $inputString);
    return $filteredString;
}

function getUserMode(){
    $usermode = 'unset';
    $username = filterNonAlphanumeric($_REQUEST['user']);
    if (file_exists("usermode/".$username)) $usermode = file_get_contents("usermode/".$username);
    return $usermode;
}

function setUserMode($mode) {
    global $rcon;
    if (getUserMode() != $mode) {
        $username = filterNonAlphanumeric($_REQUEST['user']);

        file_put_contents("usermode/" . $username, $mode); // set user wand mode = 'corral'

        $response = ucwords($mode)." Mode";
        $command = '/title '.$username.' title {"text":'.json_encode($response).',"color":"blue"}'; 
        $rcon->sendCommand($command);
        usleep(2000);
    }
}

function playerPosition( &$rconConn, $playerName) {
    $rawoutput = $rconConn->sendCommand("/data get entity @p[name=".$playerName."] Pos");
    $rpone = explode("[",$rawoutput); // trim,
    $rptwo = explode("]",$rpone[1]);  // trim,
    $rpthree = str_replace(',','',$rptwo[0]); // clean,
    $rpfour = explode(" ",$rpthree); // split into x y z
    $coords = array_map('intval', $rpfour);
    return $coords;
};

function createAnimalPen($x, $y, $z, $width, $depth) {
    $commands = [];
    for ($i = 0; $i < $width; $i++) {
        $commands[] = "/setblock " . ($x - floor($width/2) + $i) . " $y " . ($z  - floor($depth/2)) . " minecraft:oak_fence";
        $commands[] = "/setblock " . ($x - floor($width/2) + $i) . " $y " . ($z  + floor($depth/2)) . " minecraft:oak_fence";
    }
    for ($j = 0; $j <= $depth; $j++) {
        $commands[] = "/setblock " . ($x - floor($width/2)) . " $y " . ($z - floor($depth/2) + $j) . " minecraft:oak_fence";
        $commands[] = "/setblock " . ($x + floor($width/2)) . " $y " . ($z - floor($depth/2) + $j) . " minecraft:oak_fence";
    }
    return $commands;
}










