<?php 

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
                case "type":
                    if (isset($instructions[1])) {
                        $wallType = filterNonAlphanumeric($instructions[1]);
                        setUserPref('type',$wallType);
                        $rcon->sendCommand('/title '.$username.' title {"text":'.json_encode('Wall type set to '. $wallType).',"color":"blue"}');
                    } else {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing wall type","color":"orange"}');
                    }
                    break;

                case "height":
                    if (isset($instructions[1])) {

                        $wallHeight = intval($instructions[1]);
                        setUserPref('height',$wallHeight);
                        $rcon->sendCommand('/title '.$username.' title {"text":'.json_encode('Height set to '. $wallHeight).',"color":"blue"}');

                    } else {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing wall height","color":"orange"}');
                    }
                    break;

                case "radius":
                    if (isset($instructions[1])) {

                        $towerRadius = intval($instructions[1]);
                        setUserPref('radius',$towerRadius);
                        $rcon->sendCommand('/title '.$username.' title {"text":'.json_encode('Radius set to '. $towerRadius).',"color":"blue"}');

                    } else {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing tower radius","color":"orange"}');
                    }
                    break;

                case "step":
                    if (isset($instructions[1])) {
                        $step = filterNonAlphanumeric($instructions[1]);
                        setUserPref('step',$step);
                        $rcon->sendCommand('/title '.$username.' title {"text":'.json_encode('Tower angle step set to '. $step).',"color":"blue"}');
                    } else {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing wall type","color":"orange"}');
                    }
                    break;

                case "flare":
                    if (isset($instructions[1])) {
                        $flare = filterNonAlphanumeric($instructions[1]);
                        setUserPref('flare',$flare);
                        $rcon->sendCommand('/title '.$username.' title {"text":'.json_encode('Tower flare set to '. $flare).',"color":"blue"}');
                    } else {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing wall flare #","color":"orange"}');
                    }
                    break;

                case "extend":
                    if (isset($instructions[1])) {
                        $extend = filterNonAlphanumeric($instructions[1]);
                        setUserPref('extend',$extend);
                        $rcon->sendCommand('/title '.$username.' title {"text":'.json_encode('Tower extension set to '. $extend).',"color":"blue"}');
                    } else {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing wall extension #","color":"orange"}');
                    }
                    break;

                case "clear":
                        unsetUserPref('position1');
                        unsetUserPref('position2');
                        $rcon->sendCommand('/title '.$username.' title {"text":"Positions cleared","color":"blue"}');
                    break;

                case "wall":

                    $height = getUserPref("height");
                    $startPosition = getUserPref("position1");
                    $stopPosition = getUserPref("position2");

                    if (!$height) {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing wall height. use: /webcom height #","color":"orange"}');
                    } elseif (!$startPosition) {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing start position. use wand","color":"orange"}');
                    } elseif (!$stopPosition) {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing stop position. use wand again","color":"orange"}');
                    } else {
                        $blockSequence = createWallSequence($height, $startPosition, $stopPosition);

                        foreach($blockSequence as $command){
                            $rcon->sendCommand($command);
                            usleep(2000);
                        }

                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Complete","color":"green"}');
                        setUserPref("position1", $stopPosition );
                    }
                    break;
                case "tower":

                    $height = getUserPref("height");
                    $radius = getUserPref("radius");
                    $step = getUserPref("step");
                    $centerPosition = getUserPref("position2");
                    $flare = getUserPref("flare");
                    $extend = getUserPref("extend");

                    if (!$height) {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing wall height. use: /webcom height #","color":"orange"}');
                    } elseif (!$centerPosition) {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing center position. use wand again","color":"orange"}');
                    } elseif (!$step) {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing angle step. use: /webcom step #","color":"orange"}');
                    } elseif (!$radius) {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing center position. use: /webcom radius #","color":"orange"}');
                    } elseif ($flare && !$extend) {
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Missing extension. use: \'/webcom extend #\' OR set flare to 0","color":"orange"}');
                    } else {

                        $blockSequence = createTowerSequence($height, $centerPosition, $step, $radius);
                        foreach($blockSequence as $command){
                            $rcon->sendCommand($command);
                            usleep(2000);
                        }
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Complete","color":"green"}');
                    }
                    break;
            }
            break;

        case "wand":
            switch($_REQUEST['target']) {
                case "block":

                    $x = intval($_REQUEST['x']);
                    $y = intval($_REQUEST['y']);
                    $z = intval($_REQUEST['z']);
                    $username = $_REQUEST['user'];

                    $position = array("x"=>$x, "y"=>$y, "z"=>$z);

                    $startPosition = getUserPref("position1");

                    if ($startPosition) {

                        setUserPref("position2",$position);
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"End position set.","color":"aqua"}');

                    } else { // if cleared, no position was ever set

                        setUserPref("position1",$position);
                        $rcon->sendCommand('/tellraw '.$username.' {"text":"Start position set.","color":"aqua"}');
                    }

                    break;
            }
            break; //end of 'build wall' function

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

function getUserPref($pref){

    $userPrefData = false;
    $username = filterNonAlphanumeric($_REQUEST['user']);

    if (file_exists("prefs/".$username)) {

        $userPrefData = json_decode( file_get_contents("prefs/".$username) , true);

        if ($userPrefData && isset($userPrefData[ $pref ])) {
            return $userPrefData[ $pref ];
        }
    } 
    return false;
}

function setUserPref($pref, $value) {

    if (!is_dir("prefs/")) mkdir("prefs");

    $username = filterNonAlphanumeric($_REQUEST['user']);
    $userPrefData = array();

    if (file_exists("prefs/".$username)) {

        $userPrefData = json_decode( file_get_contents("prefs/".$username) , true);
    }
    $userPrefData[ $pref ] = $value;

    file_put_contents("prefs/" . $username, json_encode($userPrefData));  
}

function unsetUserPref($pref) {

    $username = filterNonAlphanumeric($_REQUEST['user']);
    $userPrefData = array();

    if (file_exists("prefs/".$username)) {

        $userPrefData = json_decode(file_get_contents("prefs/".$username), true);
    }
    unset($userPrefData[ $pref ]);

    file_put_contents("prefs/" . $username, json_encode($userPrefData));  
}


function createWallSequence($height, $start, $stop) {
    $commands = [];

    $blockTypes = json_decode( file_get_contents("blocksets/". getUserPref("type"). ".json" ), true);

    // calculate the differences in each dimension
    $dx = $stop['x'] - $start['x'];
    $dy = $stop['y'] - $start['y'];
    $dz = $stop['z'] - $start['z'];

    // calculate the maximum length for iteration
    $length = max(abs($dx), abs($dy), abs($dz));

    for ($i = 0; $i < $height; $i++) {

        for ($k = 0; $k <= $length; $k++) {

            // calculate the interpolation for each coordinate
            $interpolatedX = $start['x'] + ($dx * ($k / $length));
            $interpolatedY = $start['y'] + ($dy * ($k / $length)) + $i;
            $interpolatedZ = $start['z'] + ($dz * ($k / $length));

            $intX = (int) round($interpolatedX);
            $intY = (int) round($interpolatedY);
            $intZ = (int) round($interpolatedZ);

            $block = getGenBlock($i, $height, $blockTypes);

            $commands[] = "/usetblock " . $intX . " " . $intY . " " . $intZ . " minecraft:overworld minecraft:" . $block . " replace";
        }
    }
    return $commands;
}

function createTowerSequence($height, $center, $step, $radius) {
    $commands = [];

    $blockTypes = json_decode( file_get_contents("blocksets/" . getUserPref("type") . ".json" ), true);

    $flare = getUserPref("flare");
    $extend = getUserPref("extend");
    $triggerFloor = 0;  // create floor?
    $triggerFlare = 0;  // extend walls?
    $floorRadius = 0;

    for ($i = 0; $i < $height; $i++) {

        if ($flare && ($i > $height - $flare) ) {

            $triggerFlare = 1;             
            if (!$triggerFloor) {
                $triggerFloor = 1;
            }
        }

        do { // run once for ALL elevations

            if ($triggerFloor == 1) {

                if ($floorRadius) { // floor is being extended

                    $floorRadius += 1;
                    if ($floorRadius >= $radius + $extend) {
                        $triggerFloor = 2;
                    }

                } else { // was 0/unset
                    $floorRadius = $radius;
                }
                $wallRadius = $floorRadius;

            } else {
                $wallRadius = $radius + ( $triggerFlare ? $extend : 0 );
            }

            for ($angle = 0 ; $angle<360; $angle += $step){
                $circX = $center['x'] + (int)($wallRadius * cos(deg2rad($angle)));
                $circY = $center['y'] + $i;
                $circZ = $center['z'] + (int)($wallRadius * sin(deg2rad($angle)));
                $block = getGenBlock($i, $height, $blockTypes);
                $commands[] = "/usetblock " . $circX . " " . $circY . " " . $circZ . " minecraft:overworld minecraft:" . $block . " replace";
            }

        } while ($triggerFloor == 1); // loop if we are in 'floor-building mode 1'

    }
    return $commands;
}

function getGenBlock($row, $height, $blockTypes) {

    $totalWeight = 0;
    $validBlocks = array();

    // calculate the total weight of valid blocks for this height:   ( 'hmax'=>1, 'hmin'=>0.9, 'w':1 ,'b': 'stone' )
    foreach( $blockTypes as $bk => $bv){
        if ( $row <= ($bv['hmax'] * $height) && $row >= ($bv['hmin'] * $height) ) {
            $validBlocks[] = $bv;
            $totalWeight += $bv['w'];
        }
    }

    // generate a random floating point value and scale by the weight to generate a selection
    $randomValue = (float)mt_rand() / (float)mt_getrandmax() * $totalWeight;
    
    $selectSum = 0;
    foreach ($validBlocks as $item) {

        $selectSum += $item['w'];
        if ($randomValue <= $selectSum) {
            return $item['b'];
        }
    }
}


