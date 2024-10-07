#!/usr/bin/env python
import pika
import threading
import os

def recibir_mensajes():
    credentials = pika.PlainCredentials('myuser', 'mypassword')
    connection = pika.BlockingConnection(pika.ConnectionParameters('172.20.0.3', 5672, '/', credentials))
    channel = connection.channel()

    channel.queue_declare(queue='confirmacion')

    def callback(ch, method, properties, body):
        #print(" [x] Received %r" % body.decode())
        if(body.decode() == "0"):
            print("CANTIDAD NO DISPONIBLE")
        elif(body.decode() == "2"):
            print("COMPRA EXITOSA!")
        else:
            subcadenas = body.decode().split(",")
            channel.basic_publish(exchange='', routing_key='recibo', body=nombre+","+correo+","+subcadenas[0]+","+subcadenas[1]+ "," + subcadenas[2])


    channel.basic_consume(queue='confirmacion', on_message_callback=callback, auto_ack=True)

    #print(' [*] Waiting for messages. To exit press CTRL+C')
    channel.start_consuming()

def enviar_mensaje():
    credentials = pika.PlainCredentials('myuser', 'mypassword')
    connection = pika.BlockingConnection(pika.ConnectionParameters('172.20.0.3', 5672, '/', credentials))
    channel = connection.channel()
    channel.queue_declare(queue='Cliente')

    while True:
        id = input("Ingrese el número de un producto\nProductos: \n1. Manzanas\n2. Uva\n3. Platano\n")
        cantidad = input("Ingrese la cantidad: \n") 
        mensaje = id + "," +cantidad
        channel.basic_publish(exchange='', routing_key='consulta', body=mensaje) # 1,2 1,3
        #connection.close()
        #os.system('cls')

if __name__ == '__main__':

    
    nombre = input("Escriba su nombre: ")
    correo = input("Escriba su correo: ")
    while True:
        # Crear y ejecutar hilos para enviar y recibir mensajes simultáneamente
        enviar_hilo = threading.Thread(target=enviar_mensaje)
        recibir_hilo = threading.Thread(target=recibir_mensajes)

        enviar_hilo.start()
        recibir_hilo.start()

        # Esperar a que ambos hilos finalicen
        enviar_hilo.join()
        recibir_hilo.join()
