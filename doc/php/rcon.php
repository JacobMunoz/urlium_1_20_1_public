<?php
/**
 * See https://developer.valvesoftware.com/wiki/Source_RCON_Protocol for
 * more information about Source RCON Packets
 *
 * PHP Version 7
 *
 * @copyright 2013-2017 Chris Churchwell
 * @author thedudeguy
 * @link https://github.com/thedudeguy/PHP-Minecraft-Rcon
 * 
 * Modified by Jacob Munoz to include readAllPackets()
 */

namespace Thedudeguy;

class Rcon
{
    private $host;
    private $port;
    private $password;
    private $timeout;

    private $socket;
    private $responseDelay = 1000;

    private $authorized = false;
    private $lastResponse = '';

    const PACKET_AUTHORIZE = 5;
    const PACKET_COMMAND = 6;

    const SERVERDATA_AUTH = 3;
    const SERVERDATA_AUTH_RESPONSE = 2;
    const SERVERDATA_EXECCOMMAND = 2;
    const SERVERDATA_RESPONSE_VALUE = 0;

    /**
     * Create a new instance of the Rcon class.
     *
     * @param string $host
     * @param integer $port
     * @param string $password
     * @param integer $timeout
     * @param integer $responseDelay
     */
    public function __construct($host, $port, $password, $timeout, $responseDelay = 1000)
    {
        $this->host = $host;
        $this->port = $port;
        $this->password = $password;
        $this->timeout = $timeout;
        $this->responseDelay = $responseDelay;
    }

    /**
     * Get the latest response from the server.
     *
     * @return string
     */
    public function getResponse()
    {
        return $this->lastResponse;
    }

    /**
     * Connect to a server.
     *
     * @return boolean
     */
    public function connect()
    {
        $this->socket = fsockopen($this->host, $this->port, $errno, $errstr, $this->timeout);

        if (!$this->socket) {
            $this->lastResponse = $errstr;
            return false;
        }

        //set timeout
        stream_set_timeout($this->socket, 3, 0);

        // check authorization
        return $this->authorize();
    }

    /**
     * Disconnect from server.
     *
     * @return void
     */
    public function disconnect()
    {
        if ($this->socket) {
            fclose($this->socket);
        }
    }

    /**
     * True if socket is connected and authorized.
     *
     * @return boolean
     */
    public function isConnected()
    {
        return $this->authorized;
    }

    /**
     * Send a command to the connected server.
     *
     * @param string $command
     *
     * @return boolean|mixed
     */
    public function sendCommand($command)
    {
        if (!$this->isConnected()) {
            return false;
        }
        // send command packet
        $this->writePacket(self::PACKET_COMMAND, self::SERVERDATA_EXECCOMMAND, $command);
        // get response
        return $this->readAllPackets();

    }

    /**
     * Log into the server with the given credentials.
     *
     * @return boolean
     */
    private function authorize()
    {
        $this->writePacket(self::PACKET_AUTHORIZE, self::SERVERDATA_AUTH, $this->password);
        $response_packet = $this->readPacket();

        if ($response_packet['type'] == self::SERVERDATA_AUTH_RESPONSE) {
            if ($response_packet['id'] == self::PACKET_AUTHORIZE) {
                $this->authorized = true;

                return true;
            }
        }

        $this->disconnect();
        return false;
    }

    /**
     * Writes a packet to the socket stream.
     *
     * @param $packetId
     * @param $packetType
     * @param string $packetBody
     *
     * @return void
     */
    private function writePacket($packetId, $packetType, $packetBody)
    {
        /*
		Size			32-bit little-endian Signed Integer	 	Varies, see below.
		ID				32-bit little-endian Signed Integer		Varies, see below.
		Type	        32-bit little-endian Signed Integer		Varies, see below.
		Body		    Null-terminated ASCII String			Varies, see below.
		Empty String    Null-terminated ASCII String			0x00
		*/

        //create packet
        $packet = pack('VV', $packetId, $packetType);
        $packet = $packet.$packetBody."\x00";
        $packet = $packet."\x00";

        // get packet size.
        $packet_size = strlen($packet);

        // attach size to packet.
        $packet = pack('V', $packet_size).$packet;

        // write packet.
        fwrite($this->socket, $packet, strlen($packet));
    }

    /**
     * Read a packet from the socket stream.
     *
     * @return array
     */
    private function readPacket()
    {
        //get packet size.
        $size_data = fread($this->socket, 4);
        $size_pack = unpack('V1size', $size_data);
        $size = $size_pack['size'];
        $packet_data = fread($this->socket, $size);
        $packet_pack = unpack('V1id/V1type/a*body', $packet_data);
        return $packet_pack;
    }

    /**
     * Read all returned packets.
     *
     * @return string
     */
    private function readAllPackets()
    {
        $firstPak = true;
        $morePaks = false;
        $packData = '';
        while($firstPak || $morePaks ) {
            $firstPak = false;
            $packet_pack = $this->readPacket();

            if ($packet_pack['id'] == self::PACKET_COMMAND) {
                if ($packet_pack['type'] == self::SERVERDATA_RESPONSE_VALUE) {
                    $this->lastResponse = $packet_pack['body'];
                    $packData .= substr($packet_pack['body'], 0, -2);
                    if (strlen($packet_pack['body']) > 3507) $morePaks = true;  // modify this '3507' value if needed - possible utf16 conversion issue.
                    else $morePaks = false;
                    usleep($this->responseDelay);
                }
            }
        }
        return $packData;
    }
}


