using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.WebUtilities;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using System;
using System.Collections.Specialized;



// May need to install via:  dotnet add package CoreRCON --version 5.2.1

namespace URLiumApp
{
    public class Startup {
        public Startup(IConfiguration configuration) {
            Configuration = configuration;
        }

        public IConfiguration Configuration { get; }

        public void ConfigureServices(IServiceCollection services) { }

        public void Configure(IApplicationBuilder app, IWebHostEnvironment env) {
            if (env.IsDevelopment()) {
                app.UseDeveloperExceptionPage();
            }

            app.UseRouting();
            app.UseEndpoints(endpoints => {

                endpoints.MapPost("/", async context => {

                    using (var reader = new StreamReader(context.Request.Body)) {
                        var body = await reader.ReadToEndAsync();
                        
                        var formData = QueryHelpers.ParseQuery(body);

                        Program.GameLogicAsync(formData);

                    }
                });
            });
        }
    }
}








