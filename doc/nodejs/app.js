
//URLium Nodejs RCON responder

const http = require('http');
const querystring = require('querystring');

// Using rcon Library, install with:  $ npm install rcon

const server = http.createServer((req, res) => {
  if (req.method === 'POST') {
    let body = '';

    req.on('data', (data) => {
      body += data;
    });

    req.on('end', () => {
        const postData = querystring.parse(body);
        console.log('Received POST data:', postData);

        const device = postData.device;
        const X = postData.x;
        const Y = postData.y;
        const Z = postData.z;

        res.writeHead(200, { 'Content-Type': 'text/plain' });

        res.end('Data from '+device+' at '+X+' '+Y+' '+Z+' received successfully in NodeJS!\n');
        

        var Rcon = require('rcon');

        var conn = new Rcon('10.0.1.100', 25575, 'Your_RCON_Password');

        conn.on('auth', function() {
          // You must wait until this event is fired before sending any commands,
          // otherwise those commands will fail.
          conn.send("ugetblock "+ X + " " + Y + " " + Z);

        }).on('response', function(str) {

          console.log("Response: " + str);

          // Consider Game logic would "conn.send()" again from here

          // handle response if needed.
          if (str.startsWith("minecraft")) {

              conn.send("tellraw @a {\"text\":" + JSON.stringify("NodeJS received data from a " + str) + ",\"color\":\"aqua\"}");

          }

        }).on('error', function(err) {

          console.log("Error: " + err);

        }).on('end', function() {

          console.log("Connection closed");
          process.exit();

        });

        conn.connect();

    });
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found\n');
  }
});

const PORT = 8080;
server.listen(PORT, () => {
  console.log(`Server is listening on port ${PORT}`);
});




