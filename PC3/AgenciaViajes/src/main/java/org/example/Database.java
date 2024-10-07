package org.example;
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



class Database {

    public void insertData(String nombre, String correo, String producto, String cantidad, int dinero) {

        Connection connection = null;
        PreparedStatement statement = null;

        try {
            // Registrar el driver JDBC
            Class.forName("org.sqlite.JDBC");

            // Establecer la conexion con la base de datos
            String url = "jdbc:postgresql://172.17.0.3:5432/facturas";
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