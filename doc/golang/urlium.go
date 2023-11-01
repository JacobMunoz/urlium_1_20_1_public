package main

// install using:  go get github.com/gorcon/rcon

import (
	"log"
	"fmt"
    "net/http"
	"github.com/gorcon/rcon"
)

func main() {
    http.HandleFunc("/", handler)
    http.ListenAndServe(":8080", nil)
}

func handler(w http.ResponseWriter, r *http.Request) {

    if r.Method == "POST" {

        err := r.ParseForm()
        if err != nil {
            http.Error(w, "Failed to parse data", http.StatusBadRequest)
            return
        }

        device := r.FormValue("device")

	    conn, err := rcon.Dial("10.0.1.50:25575", "Your_RCON_Password")
	    if err != nil {
		    log.Fatal(err)
	    }
	    defer conn.Close()

	    response, err := conn.Execute("tellraw @a {\"text\":\"Golang super magic power device: "+ device + "\",\"color\":\"aqua\"}")
	    if err != nil {
		    log.Fatal(err)
	    }
	    
         fmt.Println(response) // use to read data returned by RCON command
    }
}










