-- Crea la base de datos almacen
CREATE DATABASE almacen;

-- Conecta a la base de datos almacen
\c almacen;

-- Crea la tabla productos en la base de datos almacen
CREATE TABLE productos (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    stock INT,
    price DECIMAL(10, 2)
);

-- Crea la base de datos facturas
CREATE DATABASE facturas;

-- Conecta a la base de datos facturas
\c facturas;

-- Crea la tabla facturas en la base de datos facturas
CREATE TABLE facturas (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    correo VARCHAR(255),
    producto VARCHAR(255),
    cantidad INT,
    dinero INT
);
