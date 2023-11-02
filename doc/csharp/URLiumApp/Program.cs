using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Hosting;
using System.Collections.Specialized;
using System.IO;
using System.Net;
using CoreRCON;
using CoreRCON.Parsers.Standard;

// May need to install via:  dotnet add package CoreRCON --version 5.2.1

namespace URLiumApp
{
    public class Program
    {
        public static void Main(string[] args)
        {
            CreateHostBuilder(args).Build().Run();
        }

        public static IHostBuilder CreateHostBuilder(string[] args) =>
            Host.CreateDefaultBuilder(args)
                .ConfigureWebHostDefaults(webBuilder =>
                {
                    webBuilder
                        .UseUrls("http://10.0.1.10:8080") // Set the web/app hostname and port here
                        .UseStartup<Startup>();
                });

        public static async Task GameLogicAsync (Dictionary<string, Microsoft.Extensions.Primitives.StringValues>? formData){

            if (formData != null && formData.ContainsKey("device")) {

                var deviceValue = formData["device"].First(); 

                Console.WriteLine($"Posted device: {deviceValue}");

                // Set RCON host, port, and password here
                var rcon = new RCON(IPAddress.Parse("10.0.1.50"), 25575, "Your_RCON_Password"); 
                await rcon.ConnectAsync();

                string rconCommand = "tellraw @a {\"text\":\"The C# server received signal from: " + deviceValue + "\",\"color\":\"aqua\"}";

                // Send a simple command and retrive response as string
                string response = await rcon.SendCommandAsync( rconCommand );

                // Insert Game Command Logic Here :)

            }
        }
    }
}

