#
# Python 3 server example
#

# Install RCONClient via: $ pip install mctools

from mctools import RCONClient  # Import the RCONClient

from http.server import BaseHTTPRequestHandler, HTTPServer
import time
import cgi

hostName = "127.0.0.1"
serverPort = 8080

class MyServer(BaseHTTPRequestHandler):

    def do_POST(self):
        if (self.path == "/"):
            form = cgi.FieldStorage(fp=self.rfile,
                                    headers=self.headers,
                                    environ={
                                        'REQUEST_METHOD':'POST',
                                        'CONTENT_TYPE': self.headers['Content-Type']
                                    })
            self.send_response(200)
            self.send_header("Content-type", "text")
            self.end_headers()
            self.wfile.write(bytes( form.getfirst("device") , "utf-8"))

            HOST = '10.0.1.50'  # Hostname of the Minecraft server
            PORT = 25575  # Port number of the RCON server

            # Create the RCONClient:
            rcon = RCONClient(HOST, port=PORT)

            # Login to RCON:
            if rcon.login("Your_RCON_Password"):

                deviceType = form.getfirst("device")

                if (deviceType == "configwand" ):
                    playerName = form.getfirst("user")
                    resp = rcon.command("tellraw @a {\"text\":\"Received URL Config Wand change from " + playerName + "\",\"color\":\"yellow\"}")

                elif (deviceType == "wand"):
                    playerName = form.getfirst("user")
                    resp = rcon.command("tellraw "+playerName+" {\"text\":\"Received POST signal from URL Post Wand\",\"color\":\"aqua\"}")

                else:
                    resp = rcon.command("tellraw @a {\"text\":\"Received "+deviceType+" signal - in Python3! :)\",\"color\":\"green\"}")
                    print("Reported "+deviceType)

        else:
            SimpleHTTPRequestHandler.do_POST(self)

if __name__ == "__main__":        
    webServer = HTTPServer((hostName, serverPort), MyServer)
    print("Server started http://%s:%s" % (hostName, serverPort))

    try:
        webServer.serve_forever()
    except KeyboardInterrupt:
        pass

    webServer.server_close()
    print("Server stopped.")







