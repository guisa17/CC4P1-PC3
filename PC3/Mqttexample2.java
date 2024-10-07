import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MqttExample2 {

    private static final String RECEIVE_QUEUE_NAME = "recibo";
    private static final String SEND_QUEUE_NAME = "confirmacion";
    public static Database db = new Database();

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.1.6");
        factory.setUsername("admin");
        factory.setPassword("admin");
        factory.setVirtualHost("ventas");

        com.rabbitmq.client.Connection connection = factory.newConnection();
        Channel receiveChannel = connection.createChannel();
        Channel sendChannel = connection.createChannel();

        receiveChannel.queueDeclare(RECEIVE_QUEUE_NAME, false, false, false, null);
        sendChannel.queueDeclare(SEND_QUEUE_NAME, false, false, false, null);

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");



        // Configurar el callback para recibir mensajes
        DeliverCallback receiveCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
            String[] data = message.split(",");
            if (data.length == 5) {
                String nombre = data[0];
                String correo = data[1];
		String id  = data[2];
		String cantidad = data[3];
		String precio = data[4];
                db.insertData(nombre, correo, id, cantidad,Integer.parseInt(cantidad) * Integer.parseInt(precio) );
		String tempmessage = "2";
		System.out.println(nombre + " con correo " + correo + " compro "  + cantidad + " "+ id + " pagando " + Integer.parseInt(cantidad) * Integer.parseInt(precio));
		sendChannel.basicPublish("", SEND_QUEUE_NAME, null, tempmessage.getBytes(StandardCharsets.UTF_8));
            } else {
                System.out.println("Invalid message format: " + message);
            }
        };
        receiveChannel.basicConsume(RECEIVE_QUEUE_NAME, true, receiveCallback, consumerTag -> {});

        // Configurar el hilo para leer la entrada de la terminal y enviarla
        Thread sendThread = new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    String input = reader.readLine();
                    if (input.equalsIgnoreCase("exit")) {
                        break;
                    }
                    sendChannel.basicPublish("", SEND_QUEUE_NAME, null, input.getBytes(StandardCharsets.UTF_8));
                    System.out.println(" [x] Sent '" + input + "'");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        sendThread.start();

        // Esperar a que el hilo de envio termine antes de cerrar la conexion
        sendThread.join();

        // Cerrar la conexion y los canales
        sendChannel.close();
        receiveChannel.close();
        connection.close();
    }
}


class Database {

    public void insertData(String nombre, String correo, String producto, String cantidad, int dinero) {
    
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            // Registrar el driver JDBC
            Class.forName("org.sqlite.JDBC");

            // Establecer la conexion con la base de datos
            String url = "jdbc:sqlite:/home/ubuntu/rabbit/facutaras.db";
            connection = DriverManager.getConnection(url);

            // Crear un objeto PreparedStatement
            String query = "INSERT INTO facturas(name, correo, producto, cantidad, dinero) VALUES (?, ?, ?, ?, ?)";
            statement = connection.prepareStatement(query);
            statement.setString(1, nombre);
            statement.setString(2, correo);
        	statement.setString(3, producto);
        	statement.setString(4, cantidad);
            statement.setInt(5, dinero);

            // Ejecutar la consulta de insercion
            int rowsAffected = statement.executeUpdate();

            System.out.println("Filas afectadas: " + rowsAffected);
            
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            
        } catch (SQLException e) {
            e.printStackTrace();
            
        } finally {
            // Cerrar la conexion y el statement
            try {
            
                if (statement != null) {
                    statement.close();
                }
                
                if (connection != null) {
                    connection.close();
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}