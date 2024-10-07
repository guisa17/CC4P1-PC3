package main

import (
    "context"
    "database/sql"
    "fmt"
    _ "github.com/lib/pq"
    "log"
    amqp "github.com/rabbitmq/amqp091-go"
    "strconv"
    "strings"
    "time"
)

func failOnError(err error, msg string) {
    if err != nil {
        log.Panicf("%s: %s", msg, err)
    }
}

func main() {
    conn, err := amqp.Dial("amqp://myuser:mypassword@172.20.0.3:5672/")
    failOnError(err, "Failed to connect to RabbitMQ")
    defer conn.Close()

    ch, err := conn.Channel()
    failOnError(err, "Failed to open a channel")
    defer ch.Close()

    q, err := ch.QueueDeclare(
        "consulta", // name
        false,      // durable
        false,      // delete when unused
        false,      // exclusive
        false,      // no-wait
        nil,        // arguments
    )
    failOnError(err, "Failed to declare a queue")

    msgs, err := ch.Consume(
        q.Name, // queue
        "",     // consumer
        true,   // auto-ack
        false,  // exclusive
        false,  // no-local
        false,  // no-wait
        nil,    // args
    )
    failOnError(err, "Failed to register a consumer")

    var forever chan struct{}
    var data string
    go func() {
        for d := range msgs {
            log.Printf("Received a message: %s", d.Body)
            data = string(d.Body)
            values := strings.Split(data, ",")
            var1, err := strconv.Atoi(values[0])
            failOnError(err, "FALLO EN LA VARIABLE 1")
            var2, err := strconv.Atoi(values[1])
            failOnError(err, "FALLO EN LA VARIABLE 2")
            Purchase(var1, var2)
        }
    }()

    log.Printf(" [*] Waiting for messages. To exit press CTRL+C")
    <-forever
}

func devolverMensaje(mensaje string) {
    conn, err := amqp.Dial("amqp://myuser:mypassword@172.20.0.3:5672/")
    failOnError(err, "Failed to connect to RabbitMQ")
    defer conn.Close()

    ch, err := conn.Channel()
    failOnError(err, "Failed to open a channel")
    defer ch.Close()

    q, err := ch.QueueDeclare(
        "confirmacion", // name
        false,          // durable
        false,          // delete when unused
        false,          // exclusive
        false,          // no-wait
        nil,            // arguments
    )
    failOnError(err, "Failed to declare a queue")
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    body := mensaje
    err = ch.PublishWithContext(ctx,
        "",     // exchange
        q.Name, // routing key
        false,  // mandatory
        false,  // immediate
        amqp.Publishing{
            ContentType: "text/plain",
            Body:        []byte(body),
        })
    failOnError(err, "Failed to publish a message")
}

func Purchase(id int, quantity int) {
    var enough bool
    enough, err := canPurchase(id, quantity)
    if err != nil {
        fmt.Println(err)
        return
    }
    if enough {
        db, err := sql.Open("postgres", "postgres://dbadmin:dbadmin@172.20.0.2:5432/almacen?sslmode=disable")
        if err != nil {
            fmt.Println(err)
            return
        }
        _, err2 := db.Exec("UPDATE productos SET stock = stock - $1 WHERE id = $2", quantity, id)
        if err2 != nil {
            fmt.Errorf("canPurchase %d: %v", id, err2)
        }
        fmt.Println("Enough:", enough)
        query := "SELECT price FROM productos WHERE id = $1"
        var precioProducto string
        err = db.QueryRow(query, id).Scan(&precioProducto)
        if err != nil {
            panic(err.Error())
        }
        query2 := "SELECT name FROM productos WHERE id = $1"
        var nombreProducto string
        err = db.QueryRow(query2, id).Scan(&nombreProducto)
        if err != nil {
            panic(err.Error())
        }
        fmt.Printf(nombreProducto)
        devolverMensaje(nombreProducto + "," + strconv.Itoa(quantity) + "," + precioProducto)
    } else {
        fmt.Println("OE SE ACABO")
        devolverMensaje("0")
    }
}

func canPurchase(id int, quantity int) (bool, error) {
    var enough bool
    db, err := sql.Open("postgres", "postgres://dbadmin:dbadmin@172.20.0.2:5432/almacen?sslmode=disable")
    if err != nil {
        panic(err.Error())
    }
    if err := db.QueryRow("SELECT (stock >= $1) FROM productos WHERE id = $2", quantity, id).Scan(&enough); err != nil {
        if err == sql.ErrNoRows {
            return false, fmt.Errorf("canPurchase %d: unknown product", id)
        }
        return false, fmt.Errorf("canPurchase %d: %v", id, err)
    }
    return enough, nil
}

